package io.mosip.mock.sbi.utility;

import android.provider.Settings;

import io.mosip.mock.sbi.mds.MDServiceActivity;

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
