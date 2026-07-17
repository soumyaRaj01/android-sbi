package io.android.sbi.utility;

import android.provider.Settings;

import io.android.sbi.mds.MDServiceActivity;

/**
 * @author NPrime Technologies
 */

public class CommonDeviceAPI {

    public String getISOTimeStamp() {

        return CryptoUtility.getTimestamp();
    }

    public String getSerialNumber() {
        return Settings.Secure.getString(MDServiceActivity.applicationContext.getContentResolver(), Settings.Secure.ANDROID_ID);
    }

}
