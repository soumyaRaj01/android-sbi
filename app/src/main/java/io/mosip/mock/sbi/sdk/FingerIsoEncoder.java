package io.mosip.mock.sbi.sdk;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.util.Locale;

import npr.util.BioFinger;

/**
 * Builds a MOSIP ISO 19794-4:2011 finger record from T5's 8-bit grayscale PNG: extracts the
 * luminance channel, encodes a 1-component grayscale JPEG2000-lossy stream via {@link GrayJp2Encoder}
 * (the format MOSIP requires), wraps it via {@link BioFinger}, and patches the spatial sampling-rate
 * fields {@code BioFinger} leaves at zero.
 */
public final class FingerIsoEncoder {

    /** Spatial sampling rate written into the record, in pixels per inch (within the 500-1000 range). */
    private static final int FINGER_SAMPLING_RATE_PPI = 508;
    /** Target JPEG2000 compression ratio for the finger image. */
    private static final int FINGER_JP2_COMPRESSION_RATIO = 15;
    /** ISO 19794-4 image compression code for JPEG2000 (lossy). */
    private static final String ISO_COMPRESSION_JP2_LOSSY = "04";

    private FingerIsoEncoder() {
    }

    /**
     * Encodes a captured grayscale fingerprint PNG into an ISO 19794-4 finger record.
     *
     * @param primaryImagePng the captured finger image (8-bit grayscale PNG from T5)
     * @param position        ANSI finger position (1..10)
     * @return the ISO 19794-4 record bytes
     * @throws RuntimeException if the image cannot be decoded or grayscale JPEG2000 encoding fails
     */
    public static byte[] encode(byte[] primaryImagePng, int position) {
        Bitmap bmp = BitmapFactory.decodeByteArray(primaryImagePng, 0, primaryImagePng.length);
        if (bmp == null) {
            throw new RuntimeException("Unable to decode captured finger image");
        }
        int width = bmp.getWidth();
        int height = bmp.getHeight();

        byte[] gray = toGray8(bmp);
        byte[] jp2bytes = GrayJp2Encoder.encodeGray8ToJp2(gray, width, height, FINGER_JP2_COMPRESSION_RATIO);
        if (jp2bytes == null || jp2bytes.length == 0) {
            throw new RuntimeException("Grayscale JPEG2000 encoding failed");
        }

        // position as a 2-digit hex string (ANSI position 1..10 -> "01".."0A").
        String positionHex = String.format(Locale.ROOT, "%02X", position);
        byte[] isoBytes = BioFinger.generateFingerprintISO2011(
                jp2bytes, height, width, positionHex, ISO_COMPRESSION_JP2_LOSSY);

        applySpatialSamplingRate(isoBytes);
        return isoBytes;
    }

    /** Row-major 8-bit luminance (the T5 finger image is already grayscale, so R=G=B). */
    private static byte[] toGray8(Bitmap bmp) {
        int width = bmp.getWidth();
        int height = bmp.getHeight();
        int[] pixels = new int[width * height];
        bmp.getPixels(pixels, 0, width, 0, 0, width, height);
        byte[] gray = new byte[width * height];
        for (int i = 0; i < pixels.length; i++) {
            int p = pixels[i];
            int r = (p >> 16) & 0xFF;
            int g = (p >> 8) & 0xFF;
            int b = p & 0xFF;
            gray[i] = (byte) ((r * 299 + g * 587 + b * 114) / 1000);
        }
        return gray;
    }

    /**
     * Patches the spatial sampling-rate fields in place. {@code BioFinger} writes 0 for the four
     * sampling-rate fields and leaves scale-units unset, which fails the MOSIP ISO 19794-4
     * validator; this writes a valid resolution instead.
     */
    private static void applySpatialSamplingRate(byte[] iso) {
        try {
            if (iso == null || iso.length < 36) {
                return;
            }
            int certFlag = iso[14] & 0xFF;          // general-header certification flag
            int pos = 16;                            // representation starts after the 16-byte general header
            pos += 4;                                // representation length
            pos += 9;                                // capture date/time
            pos += 1;                                // capture device technology id
            pos += 2;                                // capture device vendor id
            pos += 2;                                // capture device type id
            int numQuality = iso[pos] & 0xFF;        // quality blocks: count + count*5
            pos += 1 + numQuality * 5;
            if (certFlag != 0) {                     // certification blocks present only if the flag is set
                int numCert = iso[pos] & 0xFF;       // count + count*3
                pos += 1 + numCert * 3;
            }
            // pos -> finger position(1), representation number(1), scale units(1), then 4x2-byte sampling rates
            int scaleUnitsOff = pos + 2;
            int samplingOff = scaleUnitsOff + 1;
            int bitDepthOff = samplingOff + 8;
            // anchor: BioFinger hard-codes bit depth = 0x08 immediately after the sampling rates
            if (bitDepthOff >= iso.length || (iso[bitDepthOff] & 0xFF) != 0x08) {
                return;                              // unexpected layout - leave the record untouched
            }
            iso[scaleUnitsOff] = 0x01;               // 1 = pixels per inch
            byte hi = (byte) ((FINGER_SAMPLING_RATE_PPI >> 8) & 0xFF);
            byte lo = (byte) (FINGER_SAMPLING_RATE_PPI & 0xFF);
            for (int i = 0; i < 4; i++) {
                iso[samplingOff + i * 2] = hi;
                iso[samplingOff + i * 2 + 1] = lo;
            }
        } catch (Exception ignore) {
            // never break capture because of the compliance patch
        }
    }
}
