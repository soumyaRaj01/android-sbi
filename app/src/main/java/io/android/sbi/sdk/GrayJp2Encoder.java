package io.android.sbi.sdk;

/**
 * Encodes an 8-bit single-component (grayscale) image to a JPEG2000 (.jp2) byte array via
 * OpenJPEG (native {@code libgrayjp2.so}). This produces a 1-component greyscale JP2 (ihdr NC=1, BPC=8,
 * colr EnumCS=17) — the format MOSIP's ISO 19794-4 finger validator requires.
 */
public final class GrayJp2Encoder {

    static {
        System.loadLibrary("grayjp2");
    }

    private GrayJp2Encoder() {
    }

    /**
     * @param gray             row-major 8-bit grayscale samples, length must be >= width*height
     * @param width            image width in pixels
     * @param height           image height in pixels
     * @param compressionRatio JPEG2000 target compression ratio (e.g. 15); &lt;= 1 means lossless
     * @return the encoded grayscale JP2 bytes, or null on failure
     */
    public static native byte[] encodeGray8ToJp2(byte[] gray, int width, int height, int compressionRatio);
}
