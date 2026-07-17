package io.android.sbi.sdk;

import ai.tech5.pheonix.capture.controller.*;
import android.content.Context;
import androidx.annotation.NonNull;

public class T5FaceCapture {
    private AppSharedPreference sharedPreference = null;
    private int colorR = 125, colorG = 125, colorB = 125;

    public T5FaceCapture(Context context){
        sharedPreference = new AppSharedPreference(context);
    }

    public void startFaceCapture(Context context, FaceCaptureListener listener, int requestedScore, int captureTimeout) {
        FaceCaptureController controller = FaceCaptureController.getInstance();

        controller.setQualityThreshold(requestedScore / 100f);
        controller.setCaptureTimeoutInSecs(Math.max(1, captureTimeout / 1000));
        controller.setLivenessEnabled(sharedPreference.getIsLivenessEnabled());

        //No need to call this if Tech5 license portal is used to get the  license. Only needed in case of  license portal url chanhged or license portal is hosted on customer premise
        //controller.setUrl("https://pheonix-lic.tech5.tech");


        controller.setUseBackCamera(false);
        controller.setAutoCapture(true);

        controller.setOcclusionEnabled(sharedPreference.getIsOcculusionEnabled());
        controller.setEyeClosedEnabled(sharedPreference.getIsEyeClosedEnabled());

        controller.setGlassDetection(sharedPreference.getIsSunGlassesDetection() ? GlassDetection.SUN_GLASSES : GlassDetection.ANY_GLASSES);

        controller.writeLogs(false);

        controller.setMessagesFrequency(6);
        controller.setFontSize(23);


        // controller.setTitle("Face Capture");
        controller.setShowBackButton(true);

        //to enable/disable camera switching in face capture screen, by default disabled
        controller.setEnableCameraSwitching(sharedPreference.getEnableCameraSwitching());

        controller.setFrameCapture(true);


        controller.setCompression(true);

        if (true) {

            CompressionConfig compressionConfig = getCompressionConfig();

            controller.setCompressionConfig(compressionConfig);
        }


        controller.setIsISOEnabled(true);

        controller.setIsGetFullFrontalCrop(true);

        FullFrontalCropConfig config = new FullFrontalCropConfig();

        config.getSegmentedImage(true);
        config.setSegmentedImageBackgroundColor(colorR, colorG, colorB);

        controller.setFullFrontalCropConfig(config);


        AirsnapFaceThresholds thresholds = getAirsnapFaceThresholds();
        controller.setAirsnapFaceThresholds(thresholds);


        controller.setEnableCaptureAfter(sharedPreference.getEnableCaptureAfter());

        controller.startFaceCapture("", context, listener);
    }

    @NonNull
    private CompressionConfig getCompressionConfig() {
        CompressionConfig compressionConfig = new CompressionConfig();
        if (sharedPreference.getIsCompressByCompressionRate()) {
            compressionConfig.setCompressBy(CompressBy.COMPRESS_BY_COMPRESSION_RATE);
            compressionConfig.setCompressionRate(sharedPreference.getCompressionQuality());
        } else {
            compressionConfig.setCompressBy(CompressBy.COMPRESS_BY_TARGET_SIZE);
            compressionConfig.setTargetSizeInKbs(sharedPreference.getTargetSize());
        }
        return compressionConfig;
    }

    @NonNull
    private AirsnapFaceThresholds getAirsnapFaceThresholds() {
        AirsnapFaceThresholds thresholds = new AirsnapFaceThresholds();
        thresholds.setPITCH_THRESHOLD(sharedPreference.getPitchThreshold());
        thresholds.setYAW_THRESHOLD(sharedPreference.getYawThreshold());
        thresholds.setRollThreshold(sharedPreference.getRollThreshold());
        thresholds.setBRISQUE_THRESHOLD(sharedPreference.getBrisqueThreshold());
        thresholds.setMASK_THRESHOLD(sharedPreference.getMaskThreshold());
        thresholds.setANYGLASS_THRESHOLD(sharedPreference.getAnySunGlassThreshold());
        thresholds.setSUNGLASS_THRESHOLD(sharedPreference.getAnySunGlassThreshold());
        thresholds.setEYE_CLOSE_THRESHOLD(sharedPreference.getEyeCloseThreshold());
        thresholds.setLIVENESS_THRESHOLD(sharedPreference.getLivenessThreshold());

        thresholds.setFaceCentreToImageCentreTolerance(sharedPreference.getImageCentreToFaceCentreTolerance());

        thresholds.setFaceWidthToImageWidthRatioTolerance(sharedPreference.getFaceWidthTolerance());
        return thresholds;
    }
}
