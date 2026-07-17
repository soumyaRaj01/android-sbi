package io.android.sbi.utility;


public enum FingerPosition {
    RIGHT_THUMB(1, DeviceConstants.BIO_NAME_RIGHT_THUMB),
    RIGHT_INDEX(2, DeviceConstants.BIO_NAME_RIGHT_INDEX),
    RIGHT_MIDDLE(3, DeviceConstants.BIO_NAME_RIGHT_MIDDLE),
    RIGHT_RING(4, DeviceConstants.BIO_NAME_RIGHT_RING),
    RIGHT_LITTLE(5, DeviceConstants.BIO_NAME_RIGHT_LITTLE),
    LEFT_THUMB(6, DeviceConstants.BIO_NAME_LEFT_THUMB),
    LEFT_INDEX(7, DeviceConstants.BIO_NAME_LEFT_INDEX),
    LEFT_MIDDLE(8, DeviceConstants.BIO_NAME_LEFT_MIDDLE),
    LEFT_RING(9, DeviceConstants.BIO_NAME_LEFT_RING),
    LEFT_LITTLE(10, DeviceConstants.BIO_NAME_LEFT_LITTLE);

    private final int position;     // ANSI position 1..10
    private final String bioName;   // bio sub-type name

    FingerPosition(int position, String bioName) {
        this.position = position;
        this.bioName = bioName;
    }

    public static int positionOf(String bioName) {
        if (bioName != null) {
            for (FingerPosition fp : values()) {
                if (fp.bioName.equals(bioName)) {
                    return fp.position;
                }
            }
        }
        return -1;
    }

    public static String nameOf(int position) {
        for (FingerPosition fp : values()) {
            if (fp.position == position) {
                return fp.bioName;
            }
        }
        return null;
    }
}
