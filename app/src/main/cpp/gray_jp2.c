// JNI bridge: encode an 8-bit single-component (grayscale) image to a JPEG2000 (.jp2)
// byte array using OpenJPEG. This is the encoder our finger pipeline needs — jp2library
// only emits colour (3/4-component) JP2, which the MOSIP ISO 19794-4 validator rejects.
//
// Output: a full JP2 file (with jP/ftyp/jp2h/jp2c boxes), ihdr NC=1, BPC=8, colr EnumCS=17.

#include <jni.h>
#include <stdlib.h>
#include <string.h>
#include <openjpeg.h>

// Growable in-memory buffer used as the OpenJPEG output stream. We track the current
// write position separately from the total length because the JP2 writer seeks back to
// patch box lengths after the data is written.
typedef struct {
    unsigned char *data;
    size_t pos;   // current cursor
    size_t len;   // highest byte written (= final size)
    size_t cap;   // allocated capacity
} mem_buf;

static OPJ_BOOL mem_ensure(mem_buf *m, size_t need) {
    if (need <= m->cap) {
        return OPJ_TRUE;
    }
    size_t newcap = m->cap ? m->cap : 65536;
    while (newcap < need) {
        newcap *= 2;
    }
    unsigned char *nd = (unsigned char *) realloc(m->data, newcap);
    if (!nd) {
        return OPJ_FALSE;
    }
    m->data = nd;
    m->cap = newcap;
    return OPJ_TRUE;
}

static OPJ_SIZE_T mem_write(void *p_buffer, OPJ_SIZE_T p_nb_bytes, void *p_user_data) {
    mem_buf *m = (mem_buf *) p_user_data;
    if (!mem_ensure(m, m->pos + p_nb_bytes)) {
        return (OPJ_SIZE_T) -1;
    }
    memcpy(m->data + m->pos, p_buffer, p_nb_bytes);
    m->pos += p_nb_bytes;
    if (m->pos > m->len) {
        m->len = m->pos;
    }
    return p_nb_bytes;
}

static OPJ_OFF_T mem_skip(OPJ_OFF_T p_nb_bytes, void *p_user_data) {
    mem_buf *m = (mem_buf *) p_user_data;
    if (p_nb_bytes < 0) {
        return -1;
    }
    if (!mem_ensure(m, m->pos + (size_t) p_nb_bytes)) {
        return -1;
    }
    m->pos += (size_t) p_nb_bytes;
    if (m->pos > m->len) {
        m->len = m->pos;
    }
    return p_nb_bytes;
}

static OPJ_BOOL mem_seek(OPJ_OFF_T p_nb_bytes, void *p_user_data) {
    mem_buf *m = (mem_buf *) p_user_data;
    if (p_nb_bytes < 0) {
        return OPJ_FALSE;
    }
    if (!mem_ensure(m, (size_t) p_nb_bytes)) {
        return OPJ_FALSE;
    }
    m->pos = (size_t) p_nb_bytes;
    return OPJ_TRUE;
}

JNIEXPORT jbyteArray JNICALL
Java_io_mosip_mock_sbi_sdk_GrayJp2Encoder_encodeGray8ToJp2(JNIEnv *env, jclass clazz,
                                                           jbyteArray grayArr, jint width,
                                                           jint height, jint compressionRatio) {
    if (grayArr == NULL || width <= 0 || height <= 0) {
        return NULL;
    }
    jsize arrLen = (*env)->GetArrayLength(env, grayArr);
    if (arrLen < (jsize) width * height) {
        return NULL;
    }

    jbyte *gray = (*env)->GetByteArrayElements(env, grayArr, NULL);
    if (gray == NULL) {
        return NULL;
    }

    // One 8-bit unsigned grayscale component.
    opj_image_cmptparm_t cmptparm;
    memset(&cmptparm, 0, sizeof(cmptparm));
    cmptparm.prec = 8;
    cmptparm.bpp = 8;
    cmptparm.sgnd = 0;
    cmptparm.dx = 1;
    cmptparm.dy = 1;
    cmptparm.w = (OPJ_UINT32) width;
    cmptparm.h = (OPJ_UINT32) height;

    opj_image_t *image = opj_image_create(1, &cmptparm, OPJ_CLRSPC_GRAY);
    if (image == NULL) {
        (*env)->ReleaseByteArrayElements(env, grayArr, gray, JNI_ABORT);
        return NULL;
    }
    image->x0 = 0;
    image->y0 = 0;
    image->x1 = (OPJ_UINT32) width;
    image->y1 = (OPJ_UINT32) height;

    int npix = width * height;
    for (int i = 0; i < npix; i++) {
        image->comps[0].data[i] = (OPJ_INT32) ((unsigned char) gray[i]);
    }
    (*env)->ReleaseByteArrayElements(env, grayArr, gray, JNI_ABORT);

    opj_cparameters_t parameters;
    opj_set_default_encoder_parameters(&parameters);
    parameters.tcp_numlayers = 1;
    parameters.cp_disto_alloc = 1;
    // Use the 9/7 irreversible wavelet => a LOSSY codestream (COD transformation byte = 0x00),
    // matching the ISO 19794-4 compression code 0x04 (JPEG2000 lossy) we declare for Auth. The
    // default (reversible 5/3) would be classified lossless and rejected by the MOSIP validator.
    parameters.irreversible = 1;
    // compressionRatio <= 1 means lossless (rate 0); otherwise lossy at that ratio.
    parameters.tcp_rates[0] = (compressionRatio > 1) ? (float) compressionRatio : 0.0f;

    // Clamp resolution levels so the smallest dimension supports them (avoids encoder error
    // on small segmented-finger images).
    int minwh = width < height ? width : height;
    int numres = 6;
    while (numres > 1 && (1 << (numres - 1)) > minwh) {
        numres--;
    }
    parameters.numresolution = numres;

    opj_codec_t *codec = opj_create_compress(OPJ_CODEC_JP2);
    if (codec == NULL) {
        opj_image_destroy(image);
        return NULL;
    }
    if (!opj_setup_encoder(codec, &parameters, image)) {
        opj_destroy_codec(codec);
        opj_image_destroy(image);
        return NULL;
    }

    mem_buf m;
    m.data = NULL;
    m.pos = 0;
    m.len = 0;
    m.cap = 0;

    opj_stream_t *stream = opj_stream_default_create(OPJ_FALSE); // OPJ_FALSE = output stream
    if (stream == NULL) {
        opj_destroy_codec(codec);
        opj_image_destroy(image);
        free(m.data);
        return NULL;
    }
    opj_stream_set_user_data(stream, &m, NULL);
    opj_stream_set_write_function(stream, mem_write);
    opj_stream_set_skip_function(stream, mem_skip);
    opj_stream_set_seek_function(stream, mem_seek);

    jbyteArray result = NULL;
    if (opj_start_compress(codec, image, stream) &&
        opj_encode(codec, stream) &&
        opj_end_compress(codec, stream)) {
        result = (*env)->NewByteArray(env, (jsize) m.len);
        if (result != NULL) {
            (*env)->SetByteArrayRegion(env, result, 0, (jsize) m.len, (const jbyte *) m.data);
        }
    }

    opj_stream_destroy(stream);
    opj_destroy_codec(codec);
    opj_image_destroy(image);
    free(m.data);
    return result;
}
