package io.mosip.mock.sbi.sdk;

import android.content.Context;
import android.content.SharedPreferences;

public class SettingsPrefManager {

    private static final String PREF_NAME = "SettingsPrefs";

    // Preference keys (one per view)
    private static final String KEY_LIVENESS = "liveness";
    private static final String KEY_SHOW_ELLIPSES = "showEllipses";
    private static final String KEY_CREATE_TEMPLATES = "createTemplates";
    private static final String KEY_ORIENTATION = "orientation";
    private static final String KEY_SAVE_SDK_LOG = "saveSdkLog";
    private static final String KEY_PROPRIETARY_DENOISE = "proprietaryDenoise";
    private static final String KEY_CLEAN_FINGERPRINTS = "cleanFingerprints";
    private static final String KEY_SAVE_FINGERPRINTS = "saveFingerprints";
    private static final String KEY_CAPTURE_SPEED_ID = "captureSpeedId";
    private static final String KEY_DETECTOR_THRESHOLD = "detectorThreshold";
    private static final String KEY_INDEX_FINGER = "indexFinger";
    private static final String KEY_MIDDLE_FINGER = "middleFinger";
    private static final String KEY_RING_FINGER = "ringFinger";
    private static final String KEY_LITTLE_FINGER = "littleFinger";

    // Default values
//    private static final boolean DEFAULT_TRUE = true;
//    private static final boolean DEFAULT_FALSE = true;
    private static final int DEFAULT_RADIO_ID = 2;
    private static final float DEFAULT_THRESHOLD = 0.9f;

    private SharedPreferences sharedPref;

    public SettingsPrefManager(Context context) {
        sharedPref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }
    private static final String KEY_ZOOM_RATIO = "zoom_ratio";


    public void setZoomRatio(float zoom) {
        sharedPref.edit().putFloat(KEY_ZOOM_RATIO, zoom).apply();
    }

    public float getZoomRatio() {
        return sharedPref.getFloat(KEY_ZOOM_RATIO, 1.5f);
    }
    // Getters and setters

    public boolean isLivenessEnabled() {
        return sharedPref.getBoolean(KEY_LIVENESS, true);
    }
    public void setLivenessEnabled(boolean value) {
        sharedPref.edit().putBoolean(KEY_LIVENESS, value).apply();
    }

    public boolean isShowEllipsesEnabled() {
        return sharedPref.getBoolean(KEY_SHOW_ELLIPSES, true);
    }
    public void setShowEllipsesEnabled(boolean value) {
        sharedPref.edit().putBoolean(KEY_SHOW_ELLIPSES, value).apply();
    }

//    public boolean isCreateTemplatesEnabled() {
//        return sharedPref.getBoolean(KEY_CREATE_TEMPLATES, DEFAULT_FALSE);
//    }
//    public void setCreateTemplatesEnabled(boolean value) {
//        sharedPref.edit().putBoolean(KEY_CREATE_TEMPLATES, value).apply();
//    }

//    public boolean isOrientationEnabled() {
//        return sharedPref.getBoolean(KEY_ORIENTATION, DEFAULT_FALSE);
//    }
//    public void setOrientationEnabled(boolean value) {
//        sharedPref.edit().putBoolean(KEY_ORIENTATION, value).apply();
//    }

    public boolean isSaveSdkLogEnabled() {
        return sharedPref.getBoolean(KEY_SAVE_SDK_LOG, false);
    }
    public void setSaveSdkLogEnabled(boolean value) {
        sharedPref.edit().putBoolean(KEY_SAVE_SDK_LOG, value).apply();
    }

    public boolean isProprietaryDenoiseEnabled() {
        return sharedPref.getBoolean(KEY_PROPRIETARY_DENOISE, true);
    }
    public void setProprietaryDenoiseEnabled(boolean value) {
        sharedPref.edit().putBoolean(KEY_PROPRIETARY_DENOISE, value).apply();
    }

    public boolean isCleanFingerprintsEnabled() {
        return sharedPref.getBoolean(KEY_CLEAN_FINGERPRINTS, false);
    }
    public void setCleanFingerprintsEnabled(boolean value) {
        sharedPref.edit().putBoolean(KEY_CLEAN_FINGERPRINTS, value).apply();
    }

//    public boolean isSaveFingerprintsEnabled() {
//        return sharedPref.getBoolean(KEY_SAVE_FINGERPRINTS, true);
//    }
//    public void setSaveFingerprintsEnabled(boolean value) {
//        sharedPref.edit().putBoolean(KEY_SAVE_FINGERPRINTS, value).apply();
//    }

    public int getCaptureSpeedId() {
        return sharedPref.getInt(KEY_CAPTURE_SPEED_ID, DEFAULT_RADIO_ID);
    }
    public void setCaptureSpeedId(int id) {
        sharedPref.edit().putInt(KEY_CAPTURE_SPEED_ID, id).apply();
    }

//    public float getDetectorThreshold() {
//        return sharedPref.getFloat(KEY_DETECTOR_THRESHOLD, DEFAULT_THRESHOLD);
//    }
//    public void setDetectorThreshold(float threshold) {
//        sharedPref.edit().putFloat(KEY_DETECTOR_THRESHOLD, threshold).apply();
//    }

//    public boolean isIndexFingerChecked() {
//        return sharedPref.getBoolean(KEY_INDEX_FINGER, DEFAULT_TRUE);
//    }
//    public void setIndexFingerChecked(boolean value) {
//        sharedPref.edit().putBoolean(KEY_INDEX_FINGER, value).apply();
//    }
//
//    public boolean isMiddleFingerChecked() {
//        return sharedPref.getBoolean(KEY_MIDDLE_FINGER, DEFAULT_TRUE);
//    }
//    public void setMiddleFingerChecked(boolean value) {
//        sharedPref.edit().putBoolean(KEY_MIDDLE_FINGER, value).apply();
//    }
//
//    public boolean isRingFingerChecked() {
//        return sharedPref.getBoolean(KEY_RING_FINGER, DEFAULT_TRUE);
//    }
//    public void setRingFingerChecked(boolean value) {
//        sharedPref.edit().putBoolean(KEY_RING_FINGER, value).apply();
//    }
//
//    public boolean isLittleFingerChecked() {
//        return sharedPref.getBoolean(KEY_LITTLE_FINGER, DEFAULT_TRUE);
//    }
//    public void setLittleFingerChecked(boolean value) {
//        sharedPref.edit().putBoolean(KEY_LITTLE_FINGER, value).apply();
//    }

    // New keys
    private static final String KEY_GET_FINGER_REVERSE = "finger_reverse";

    private static final String KEY_GET_QUALITY = "get_quality";
    private static final String KEY_GET_NFIQ2_QUALITY = "get_nfiq2_quality";
    private static final String KEY_CAPTURE_MODE_ID = "capture_mode_id";

    // Default values
    private static final boolean DEFAULT_GET_QUALITY = true;
    private static final boolean DEFAULT_GET_NFIQ2_QUALITY = false;
//    private static final int DEFAULT_CAPTURE_MODE_ID = R.id.rad_btn_cap_mode_self;  // default capture mode
private static final int DEFAULT_CAPTURE_MODE_ID = 0;  // default capture mode

    // New getter/setter for checkboxes
    public boolean isGetQualityEnabled() {
        return sharedPref.getBoolean(KEY_GET_QUALITY, DEFAULT_GET_QUALITY);
    }

    public void setGetQualityEnabled(boolean enabled) {
        sharedPref.edit().putBoolean(KEY_GET_QUALITY, enabled).apply();
    }

    public boolean isGetNfiq2QualityEnabled() {
        return sharedPref.getBoolean(KEY_GET_NFIQ2_QUALITY, DEFAULT_GET_NFIQ2_QUALITY);
    }

    public void setGetNfiq2QualityEnabled(boolean enabled) {
        sharedPref.edit().putBoolean(KEY_GET_NFIQ2_QUALITY, enabled).apply();
    }

    // Getter/setter for capture mode radio group selected id
    public int getCaptureModeId() {
        return sharedPref.getInt(KEY_CAPTURE_MODE_ID, DEFAULT_CAPTURE_MODE_ID);
    }

    public void setCaptureModeId(int id) {
        sharedPref.edit().putInt(KEY_CAPTURE_MODE_ID, id).apply();
    }

    private static final String KEY_LIVENESS_THRESHOLD = "liveness_threshold";
    private static final String KEY_QUALITY_THRESHOLD = "quality_threshold";
    private static final float DEFAULT_LIVENESS_THRESHOLD = 0.75f;
    private static final float DEFAULT_QUALITY_THRESHOLD = 35f;

    public float getLivenessThreshold() {
        return sharedPref.getFloat(KEY_LIVENESS_THRESHOLD, DEFAULT_LIVENESS_THRESHOLD);
    }

    public void setLivenessThreshold(float value) {
        sharedPref.edit().putFloat(KEY_LIVENESS_THRESHOLD, value).apply();
    }

    public float getQualityThreshold() {
        return sharedPref.getFloat(KEY_QUALITY_THRESHOLD, DEFAULT_QUALITY_THRESHOLD);
    }

    public void setQualityThreshold(float value) {
        sharedPref.edit().putFloat(KEY_QUALITY_THRESHOLD, value).apply();
    }

    private static final String KEY_LITTLE_FINGER_THRESHOLD = "little_finger_threshold";
    private static final float DEFAULT_LITTLE_FINGER_THRESHOLD = 20f;

    public float getLittleFingerThreshold() {
        return sharedPref.getFloat(KEY_LITTLE_FINGER_THRESHOLD, DEFAULT_LITTLE_FINGER_THRESHOLD);
    }

    public void setLittleFingerThreshold(float value) {
        sharedPref.edit().putFloat(KEY_LITTLE_FINGER_THRESHOLD, value).apply();
    }

    private static final String KEY_RING_FINGER_THRESHOLD = "ring_finger_threshold";
    private static final float DEFAULT_RING_FINGER_THRESHOLD = 30f;

    public float getRingFingerThreshold() {
        return sharedPref.getFloat(KEY_RING_FINGER_THRESHOLD, DEFAULT_RING_FINGER_THRESHOLD);
    }

    public void setRingFingerThreshold(float value) {
        sharedPref.edit().putFloat(KEY_RING_FINGER_THRESHOLD, value).apply();
    }
    private String IS_Preview_Done = "IS_Preview_Done";
    private boolean DEFAULT_IS_Preivew_Done = false;

    public boolean is_Dialogue_PreviewEnabled() {
        return sharedPref.getBoolean(IS_Preview_Done, DEFAULT_IS_Preivew_Done);
    }

    public void setIs_Dialogue_PreviewEnabled(boolean enableTitle) {
        sharedPref.edit().putBoolean(IS_Preview_Done, enableTitle).apply();

    }

    public boolean isGetFingerReverseEnabled() {
        return sharedPref.getBoolean(KEY_GET_FINGER_REVERSE, false);
    }

    public void setGetFingerReverseEnabled(boolean enabled) {
        sharedPref.edit().putBoolean(KEY_GET_FINGER_REVERSE, enabled).apply();
    }


}
