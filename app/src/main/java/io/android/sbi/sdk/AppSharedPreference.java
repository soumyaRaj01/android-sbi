package io.android.sbi.sdk;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class AppSharedPreference {


    public int DEFAULT_PITCH_THRESHOLD = 15;
    public int DEFAULT_YAW_THRESHOLD = 15;
    public int DEFAULT_ROLL_THRESHOLD = 10;
    public float DEFAULT_MASK_THRESHOLD = 0.5f;
    public float DEFAULT_ANY_GLASS_THRESHOLD = 0.5f;
    public int DEFAULT_BRISQUE_THRESHOLD = 60;
    public float DEFAULT_LIVENESS_THRESHOLD = 0.5f;
    public float DEFAULT_EYE_CLOSE_THRESHOLD = 0.4f;

    public int DEFAULT_ENABLE_CAPTURE_AFTER = 6;

    public int DEFAULT_DELAY_BETWEEN_FRAMES = 0;
    public int DEFAULT_COMPRESSION_QUALITY = 80;


    public float DEFAULT_MIN_BLUR = 0.0f;
    public float DEFAULT_MAX_BLUR = 0.5f;
    public float DEFAULT_MIN_EXPOSURE = 0.2f;
    public float DEFAULT_MAX_EXPOSURE = 0.7f;
    public float DEFAULT_MIN_BRIGHTNESS = 0.2f;
    public float DEFAULT_MAX_BRIGHTNESS = 0.7f;

    public float DEFAULT_SKIN_TONE = 0.5f;
    public float DEFAULT_HOTSPOTS = 0.5f;
    public float DEFAULT_RED_EYES = 0.5f;
    public float DEFAULT_MOUTH_OPEN = 0.5f;
    public float DEFAULT_LAUGH = 0.5f;
    public float DEFAULT_UNIFORM_BACKGROUND = 0.5f;
    public float DEFAULT_UNIFORM_BACKGROUND_COLOR = 0.5f;
    public float DEFAULT_UNIFORM_ILLUMINATION = 0.5f;
    public float DEFAULT_FACE_BACKGROUND_DIFFERENCE = 0.5f;

    public float DEFAULT_HAT_THRESHOLD = 0.4f;
    public float DEFAULT_HEAD_PHONES_THRESHOLD = 0.4f;
    public float DEFAULT_HAND_OCCLUSION_THRESHOLD = 0.1f;


    private static final boolean DEFAULT_IS_OCCLUSION_ENABLED = true;
    private static final boolean DEFAULT_IS_EYE_CLOSED_ENABLED = true;
    private static final boolean DEFAULT_IS_LIVENESS_ENABLED = true;

    private static final int DEFAULT_CAPTURE_TIMEOUT = 60;
    private static final String CAPTURE_TIMEOUT = "CAPTURE_TIMEOUT";


    private final SharedPreferences sharedPref;
    private SharedPreferences.Editor editor;


    private static final String IS_OCCLUSION_ENABLED = "IS_OCCLUSION_ENABLED";
    private static final String IS_EYE_CLOSED_ENABLED = "IS_EYE_CLOSED_ENABLED";
    private static final String IS_LIVENESS_ENABLED = "IS_LIVENESS_ENABLED ";

    private static final String ENABLE_CAPTURE_AFTER = "ENABLE_CAPTURE_AFTER";


    private static final String PITCH_THRESHOLD = "PITCH_THRESHOLD";
    private static final String YAW_THRESHOLD = "YAW_THRESHOLD";
    private static final String ROLL_THRESHOLD = "ROLL_THRESHOLD";
    private static final String MASK_THRESHOLD = "MASK_THRESHOLD";

    private static final String ANY_GLASS_THRESHOLD = "ANY_GLASS_THRESHOLD";
    private static final String BRISQUE_THRESHOLD = "BRISQUE_THRESHOLD";
    private static final String LIVENESS_THRESHOLD = "LIVENESS_THRESHOLD";
    private static final String EYE_CLOSE_THRESHOLD = "EYE_CLOSE_THRESHOLD";


    private static final String DELAY_BETWEEN_FRAMES = "DELAY_BETWEEN_FRAMES";
    private static final String COMPRESSION_QUALITY = "COMPRESSION_QUALITY";
    private static final String TARGET_SIZE = "TARGET_SIZE";

    private static final String MIN_BLUR = "MIN_BLUR";
    private static final String MAX_BLUR = "MAX_BLUR";
    private static final String MIN_EXPOSURE = "MIN_EXPOSURE";
    private static final String MAX_EXPOSURE = "MAX_EXPOSURE";
    private static final String MIN_BRIGHTNESS = "MIN_BRIGHTNESS";
    private static final String MAX_BRIGHTNESS = "MAX_BRIGHTNESS";

    private static final String SKIN_TONE = "SKIN_TONE";
    private static final String HOTSPOTS = "HOTSPOTS";

    private static final String RED_EYES = "RED_EYES";
    private static final String MOUTH_OPEN = "MOUTH_OPEN";
    private static final String LAUGH = "LAUGH";
    private static final String UNIFORM_BACKGROUND = "UNIFORM_BACKGROUND";
    private static final String UNIFORM_BACKGROUND_COLOR = "UNIFORM_BACKGROUND_COLOR";
    private static final String UNIFORM_ILLUMINATION = "UNIFORM_ILLUMINATION";
    private static final String FACE_BACKGROUND_DIFFERENCE = "FACE_BACKGROUND_DIFFERENCE";

    private static final String HAT = "HAT";
    private static final String HAND_OCCLUSION = "HAND_OCCLUSION";
    private static final String HEAD_PHONES = "HEAD_PHONES";


    private final String IMAGE_CENTRE_TO_FACE_CENTRE_TOLERANCE = "IMAGE_CENTRE_TO_FACE_CENTRE_TOLERANCE";

    public float DEFAULT_IMAGE_CENTRE_TO_FACE_CENTRE_TOLERANCE = 10;

    private static final float DEFAULT_FACE_WIDTH_TOLERANCE = 10;
    private static final String FACE_WIDTH_TOLERANCE = "FACE_WIDTH_TOLERANCE";

    private static final String IS_COMPRESS_BY_COMPRESSION_RATE = "IS_COMPRESS_BY_COMPRESSION_RATE";
    private static final String IS_SUN_GLASS_DETECTION = "IS_SUN_GLASS_DETECTION";

    private static final String ENABLE_CAMERA_SWITCHING = "ENABLE_CAMERA_SWITCHING";
    private static final Boolean DEFAULT_VAL_FOR_ENABLE_CAMERA_SWITCHING = true;


    public AppSharedPreference(Context context) {
        sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
    }

    public void setIsLivenessEnabled(boolean isLivenessEnabled) {
        sharedPref.edit().putBoolean(IS_LIVENESS_ENABLED, isLivenessEnabled).apply();
    }

    public boolean getIsLivenessEnabled() {
        return sharedPref.getBoolean(IS_LIVENESS_ENABLED, DEFAULT_IS_LIVENESS_ENABLED);
    }


    public void setIsOcculusionEnabled(boolean isOcculusionEnabled) {
        sharedPref.edit().putBoolean(IS_OCCLUSION_ENABLED, isOcculusionEnabled).apply();
    }

    public boolean getIsOcculusionEnabled() {
        return sharedPref.getBoolean(IS_OCCLUSION_ENABLED, DEFAULT_IS_OCCLUSION_ENABLED);
    }


    public void setIsEyeClosedEnabled(boolean isEyeClosedEnabled) {
        sharedPref.edit().putBoolean(IS_EYE_CLOSED_ENABLED, isEyeClosedEnabled).apply();
    }

    public boolean getIsEyeClosedEnabled() {
        return sharedPref.getBoolean(IS_EYE_CLOSED_ENABLED, DEFAULT_IS_EYE_CLOSED_ENABLED);
    }


    public int getPitchThreshold() {
        return (sharedPref.getInt(PITCH_THRESHOLD, DEFAULT_PITCH_THRESHOLD));
    }

    public void setPitchThreshold(int pitchThreshold) {

        sharedPref.edit().putInt(PITCH_THRESHOLD, pitchThreshold).apply();

    }


    public int getYawThreshold() {
        return (sharedPref.getInt(YAW_THRESHOLD, DEFAULT_YAW_THRESHOLD));
    }

    public void setYawThreshold(int yawThreshold) {

        sharedPref.edit().putInt(YAW_THRESHOLD, yawThreshold).apply();

    }

    public int getRollThreshold() {
        return (sharedPref.getInt(ROLL_THRESHOLD, DEFAULT_ROLL_THRESHOLD));
    }

    public void setRollThreshold(int rollThreshold) {

        sharedPref.edit().putInt(ROLL_THRESHOLD, rollThreshold).apply();

    }


    public float getMaskThreshold() {

        return (sharedPref.getFloat(MASK_THRESHOLD, DEFAULT_MASK_THRESHOLD));
    }

    public void setMaskThreshold(float maskThreshold) {

        sharedPref.edit().putFloat(MASK_THRESHOLD, maskThreshold).apply();
    }


    public int getCaptureTimeout() {

        return (sharedPref.getInt(CAPTURE_TIMEOUT, DEFAULT_CAPTURE_TIMEOUT));
    }

    public void setCaptureTimeout(int timeout) {

        sharedPref.edit().putInt(CAPTURE_TIMEOUT, timeout).apply();
    }


    public float getAnySunGlassThreshold() {

        return (sharedPref.getFloat(ANY_GLASS_THRESHOLD, DEFAULT_ANY_GLASS_THRESHOLD));
    }

    public void setAnySunGlassThreshold(float anyGlassesThreshold) {

        sharedPref.edit().putFloat(ANY_GLASS_THRESHOLD, anyGlassesThreshold).apply();
    }


    public int getBrisqueThreshold() {
        return (sharedPref.getInt(BRISQUE_THRESHOLD, DEFAULT_BRISQUE_THRESHOLD));
    }

    public void setBrisqueThreshold(int brisqueThreshold) {

        sharedPref.edit().putInt(BRISQUE_THRESHOLD, brisqueThreshold).apply();

    }


    public int getEnableCaptureAfter() {
        return (sharedPref.getInt(ENABLE_CAPTURE_AFTER, DEFAULT_ENABLE_CAPTURE_AFTER));
    }

    public void setEnableCaptureAfter(int enableCaptureAfter) {

        sharedPref.edit().putInt(ENABLE_CAPTURE_AFTER, enableCaptureAfter).apply();

    }


    public float getEyeCloseThreshold() {

        return (sharedPref.getFloat(EYE_CLOSE_THRESHOLD, DEFAULT_EYE_CLOSE_THRESHOLD));
    }

    public void setEyeCloseThreshold(float eyeCloseThreshold) {

        sharedPref.edit().putFloat(EYE_CLOSE_THRESHOLD, eyeCloseThreshold).apply();
    }


    public float getLivenessThreshold() {

        return (sharedPref.getFloat(LIVENESS_THRESHOLD, DEFAULT_LIVENESS_THRESHOLD));
    }

    public void setLivenessThreshold(float livenessThreshold) {

        sharedPref.edit().putFloat(LIVENESS_THRESHOLD, livenessThreshold).apply();
    }


    public int getDelayBetweenFrames() {
        return (sharedPref.getInt(DELAY_BETWEEN_FRAMES, DEFAULT_DELAY_BETWEEN_FRAMES));
    }

    public void setDelayBetweenFrames(int delay) {

        sharedPref.edit().putInt(DELAY_BETWEEN_FRAMES, delay).apply();

    }


    public void setIsSunGlassesDetection(boolean isSunGlassesDetection) {
        this.sharedPref.edit().putBoolean(IS_SUN_GLASS_DETECTION, isSunGlassesDetection).apply();
    }

    public boolean getIsSunGlassesDetection() {
        return this.sharedPref.getBoolean(IS_SUN_GLASS_DETECTION, true);
    }


    public int getCompressionQuality() {
        return (sharedPref.getInt(COMPRESSION_QUALITY, DEFAULT_COMPRESSION_QUALITY));
    }

    public void setCompressionQuality(int compressionQuality) {

        sharedPref.edit().putInt(COMPRESSION_QUALITY, compressionQuality).apply();

    }


    public int getTargetSize() {
        return this.sharedPref.getInt(TARGET_SIZE, 1024);
    }

    public void setTargetSize(int targetSize) {
        this.sharedPref.edit().putInt(TARGET_SIZE, targetSize).apply();
    }


    public float getMinBlur() {

        return (sharedPref.getFloat(MIN_BLUR, DEFAULT_MIN_BLUR));
    }

    public void setMinBlur(float minBlur) {

        sharedPref.edit().putFloat(MIN_BLUR, minBlur).apply();
    }


    public float getMaxBlur() {

        return (sharedPref.getFloat(MAX_BLUR, DEFAULT_MAX_BLUR));
    }

    public void setMaxBlur(float maxBlur) {

        sharedPref.edit().putFloat(MAX_BLUR, maxBlur).apply();
    }


    public float getMinExposure() {

        return (sharedPref.getFloat(MIN_EXPOSURE, DEFAULT_MIN_EXPOSURE));
    }

    public void setMinExposure(float minExposure) {

        sharedPref.edit().putFloat(MIN_EXPOSURE, minExposure).apply();
    }


    public float getMaxExposure() {

        return (sharedPref.getFloat(MAX_EXPOSURE, DEFAULT_MAX_EXPOSURE));
    }

    public void setMaxExposure(float maxExposure) {

        sharedPref.edit().putFloat(MAX_EXPOSURE, maxExposure).apply();
    }


    public float getMinBrightness() {

        return (sharedPref.getFloat(MIN_BRIGHTNESS, DEFAULT_MIN_BRIGHTNESS));
    }

    public void setMinBrightness(float minBrightness) {

        sharedPref.edit().putFloat(MIN_BRIGHTNESS, minBrightness).apply();
    }


    public float getMaxBrightness() {

        return (sharedPref.getFloat(MAX_BRIGHTNESS, DEFAULT_MAX_BRIGHTNESS));
    }

    public void setMaxBrightness(float maxBrightness) {

        sharedPref.edit().putFloat(MAX_BRIGHTNESS, maxBrightness).apply();
    }


    public float getSkinToneThreshold() {

        return (sharedPref.getFloat(SKIN_TONE, DEFAULT_SKIN_TONE));
    }

    public void setSkinToneThreshold(float skinToneThreshold) {

        sharedPref.edit().putFloat(SKIN_TONE, skinToneThreshold).apply();
    }


    public float getHotspotsThreshold() {

        return (sharedPref.getFloat(HOTSPOTS, DEFAULT_HOTSPOTS));
    }

    public void setHotspotsThreshold(float hotspots) {

        sharedPref.edit().putFloat(HOTSPOTS, hotspots).apply();
    }


    public float getRedEyesThreshold() {

        return (sharedPref.getFloat(RED_EYES, DEFAULT_RED_EYES));
    }

    public void setRedEyesThreshold(float redEyesThreshold) {

        sharedPref.edit().putFloat(RED_EYES, redEyesThreshold).apply();
    }


    public float getMouthOpenThreshold() {

        return (sharedPref.getFloat(MOUTH_OPEN, DEFAULT_MOUTH_OPEN));
    }

    public void setMouthOpenThreshold(float mouthOpenThreshold) {

        sharedPref.edit().putFloat(MOUTH_OPEN, mouthOpenThreshold).apply();
    }


    public float getLaughThreshold() {

        return (sharedPref.getFloat(LAUGH, DEFAULT_LAUGH));
    }

    public void setLaughThreshold(float laughThreshold) {

        sharedPref.edit().putFloat(LAUGH, laughThreshold).apply();
    }


    public float getUniformBackgroundThreshold() {

        return (sharedPref.getFloat(UNIFORM_BACKGROUND, DEFAULT_UNIFORM_BACKGROUND));
    }

    public void setUniformBackgroundThreshold(float uniformBackground) {

        sharedPref.edit().putFloat(UNIFORM_BACKGROUND, uniformBackground).apply();
    }


    public float getUniformBackgroundColorThreshold() {

        return (sharedPref.getFloat(UNIFORM_BACKGROUND_COLOR, DEFAULT_UNIFORM_BACKGROUND_COLOR));
    }

    public void setUniformBackgroundColorThreshold(float uniformBackgroundColor) {

        sharedPref.edit().putFloat(UNIFORM_BACKGROUND, uniformBackgroundColor).apply();
    }

    public float getIlluminationThreshold() {

        return (sharedPref.getFloat(UNIFORM_ILLUMINATION, DEFAULT_UNIFORM_ILLUMINATION));
    }

    public void setIlluminationThreshold(float illuminationThreshold) {

        sharedPref.edit().putFloat(UNIFORM_ILLUMINATION, illuminationThreshold).apply();
    }

    public float getFaceBackDiffThreshold() {

        return (sharedPref.getFloat(FACE_BACKGROUND_DIFFERENCE, DEFAULT_FACE_BACKGROUND_DIFFERENCE));
    }

    public void setFaceBackDiffThreshold(float FaceBackDiffThreshold) {

        sharedPref.edit().putFloat(FACE_BACKGROUND_DIFFERENCE, FaceBackDiffThreshold).apply();
    }


    public float getHatThreshold() {

        return (sharedPref.getFloat(HAT, DEFAULT_HAT_THRESHOLD));
    }

    public void setHatThreshold(float hatThreshold) {

        sharedPref.edit().putFloat(HAT, hatThreshold).apply();
    }


    public float getHandOcclusionThreshold() {

        return (sharedPref.getFloat(HAND_OCCLUSION, DEFAULT_HAND_OCCLUSION_THRESHOLD));
    }

    public void getHandOcclusionThreshold(float handOcclusionThreshold) {

        sharedPref.edit().putFloat(HAND_OCCLUSION, handOcclusionThreshold).apply();
    }


    public float getHeadPhonesThreshold() {

        return (sharedPref.getFloat(HEAD_PHONES, DEFAULT_HEAD_PHONES_THRESHOLD));
    }

    public void setHeadPhonesThreshold(float headPhonesThreshold) {

        sharedPref.edit().putFloat(HEAD_PHONES, headPhonesThreshold).apply();
    }


    public void setImageCentreToFaceCentreTolerance(Float tolerancePercentage) {

        sharedPref.edit().putFloat(IMAGE_CENTRE_TO_FACE_CENTRE_TOLERANCE, tolerancePercentage).apply();
    }


    public Float getImageCentreToFaceCentreTolerance() {

        return (sharedPref.getFloat(IMAGE_CENTRE_TO_FACE_CENTRE_TOLERANCE, DEFAULT_IMAGE_CENTRE_TO_FACE_CENTRE_TOLERANCE));

    }


    public void setFaceWidthTolerance(Float percentage) {

        sharedPref.edit().putFloat(FACE_WIDTH_TOLERANCE, percentage).apply();
    }


    public Float getFaceWidthTolerance() {


        return (sharedPref.getFloat(FACE_WIDTH_TOLERANCE, DEFAULT_FACE_WIDTH_TOLERANCE));

    }


    public void setIsCompressByCompressionRate(boolean compressBy) {
        this.sharedPref.edit().putBoolean(IS_COMPRESS_BY_COMPRESSION_RATE, compressBy).apply();
    }

    public boolean getIsCompressByCompressionRate() {
        return this.sharedPref.getBoolean(IS_COMPRESS_BY_COMPRESSION_RATE, true);
    }


    public void setEnableCameraSwitching(boolean enableCameraSwitching) {
        this.sharedPref.edit().putBoolean(ENABLE_CAMERA_SWITCHING, enableCameraSwitching).apply();
    }

    public boolean getEnableCameraSwitching() {
        return this.sharedPref.getBoolean(ENABLE_CAMERA_SWITCHING, DEFAULT_VAL_FOR_ENABLE_CAMERA_SWITCHING);
    }


}