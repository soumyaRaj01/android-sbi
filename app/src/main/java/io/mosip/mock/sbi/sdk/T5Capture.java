package io.mosip.mock.sbi.sdk;

import ai.tech5.finger.utils.*;
import android.Manifest;
import android.content.Context;
import android.util.Log;
import androidx.annotation.NonNull;

import io.mosip.mock.sbi.utility.DeviceConstants;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;

public class T5Capture {

    SettingsPrefManager settingsPrefManager;

    private static final String[] APP_PERMISSIONS = {Manifest.permission.CAMERA};

    private static final SegmentationMode[] UNKNOWN_FINGER_ORDER = {
            SegmentationMode.SEGMENTATION_MODE_LEFT_INDEX,  SegmentationMode.SEGMENTATION_MODE_LEFT_MIDDLE,
            SegmentationMode.SEGMENTATION_MODE_LEFT_RING,   SegmentationMode.SEGMENTATION_MODE_LEFT_LITTLE,
            SegmentationMode.SEGMENTATION_MODE_RIGHT_INDEX, SegmentationMode.SEGMENTATION_MODE_RIGHT_MIDDLE,
            SegmentationMode.SEGMENTATION_MODE_RIGHT_RING,  SegmentationMode.SEGMENTATION_MODE_RIGHT_LITTLE,
            SegmentationMode.SEGMENTATION_MODE_LEFT_THUMB,  SegmentationMode.SEGMENTATION_MODE_RIGHT_THUMB
    };

    private static String m_rootDirectory;
    private static ExecutorService m_service = null;
    private LightSensorHelper m_lightSensorHelper = null;

    public T5Capture(Context context){
        m_lightSensorHelper = new LightSensorHelper(context);
        m_lightSensorHelper.start();
    }

    public void capture(Context context, T5FingerCapturedListener listener, int deviceSubId, String[] bioSubType, int requestedScore, int captureTimeout) {
        T5FingerCaptureController t5FingerCaptureController = T5FingerCaptureController.getInstance();
        settingsPrefManager = new SettingsPrefManager(context);
        t5FingerCaptureController.setsavesdklogs(true);
        t5FingerCaptureController.setZoomFactor(settingsPrefManager.getZoomRatio());
        t5FingerCaptureController.setLicense("");

        t5FingerCaptureController.showElipses(settingsPrefManager.isShowEllipsesEnabled());
        t5FingerCaptureController.setLivenessCheck(settingsPrefManager.isLivenessEnabled());

        t5FingerCaptureController.setIsGetQuality(settingsPrefManager.isGetQualityEnabled());
        t5FingerCaptureController.setIsGetNist2Quality(settingsPrefManager.isGetNfiq2QualityEnabled());


        t5FingerCaptureController.setDetectorThreshold(0.9f);
        t5FingerCaptureController.setUsername("SBI");
        LinkedHashSet<SegmentationMode> segmentationModeSet = new LinkedHashSet<>();
        ArrayList<Integer> missingFingers = new ArrayList<>();

         if (deviceSubId != 0) {
            switch (deviceSubId) {
                case DeviceConstants.DEVICE_FINGER_SLAP_SUB_TYPE_ID_LEFT:
                    segmentationModeSet.add(SegmentationMode.SEGMENTATION_MODE_LEFT_SLAP);
                    break;
                case DeviceConstants.DEVICE_FINGER_SLAP_SUB_TYPE_ID_RIGHT:
                    segmentationModeSet.add(SegmentationMode.SEGMENTATION_MODE_RIGHT_SLAP);
                    break;
                case DeviceConstants.DEVICE_FINGER_SLAP_SUB_TYPE_ID_THUMB:
                    segmentationModeSet.add(SegmentationMode.SEGMENTATION_MODE_LEFT_AND_RIGHT_THUMBS);
                    break;
                default:
                    break;
            }
        } else if (bioSubType != null) {
            SegmentationMode dual = (bioSubType.length == 2) ? toDualSegmentationMode(bioSubType[0], bioSubType[1]) : null;
            if (dual != null) {
                segmentationModeSet.add(dual);
            } else {
                for (String name : bioSubType) {
                    SegmentationMode mode = toSegmentationMode(name);
                    if (mode != null) {
                        segmentationModeSet.add(mode);
                    }
                }
            }
        }

        if (segmentationModeSet.isEmpty()) {
            boolean isUnknown = bioSubType != null && bioSubType.length > 0
                    && DeviceConstants.BIO_NAME_UNKNOWN.equals(bioSubType[0]);
            if (isUnknown) {
                for (int i = 0; i < bioSubType.length && i < UNKNOWN_FINGER_ORDER.length; i++) {
                    segmentationModeSet.add(UNKNOWN_FINGER_ORDER[i]);
                }
            }
            if (segmentationModeSet.isEmpty()) {
                segmentationModeSet.add(SegmentationMode.SEGMENTATION_MODE_LEFT_SLAP);
            }
        }

        t5FingerCaptureController.setSegmentationModes(segmentationModeSet);

        t5FingerCaptureController.setNfiq1QualityThreshold(requestedScore);

        CaptureMode captureMode = CaptureMode.CAPTURE_MODE_SELF;

        int mode = settingsPrefManager.getCaptureModeId();
        if (mode == 0) {

            captureMode = CaptureMode.CAPTURE_MODE_SELF;
        } else if (mode == 1) {
            captureMode = CaptureMode.CAPTURE_MODE_OPERATOR;

        }

//        t5FingerCaptureController.setDetectorThreshold(settingsPrefManager.getDetectorThreshold());
        t5FingerCaptureController.setCaptureMode(captureMode);
        t5FingerCaptureController.setTitle("Finger Capture");
        t5FingerCaptureController.setShowBackButton(false);

        if (!missingFingers.isEmpty()) {
            t5FingerCaptureController.setMissingFingers(missingFingers);
        }
        int getspeed = settingsPrefManager.getCaptureSpeedId();

        CaptureSpeed captureSpeed = CaptureSpeed.CAPTURE_SPEED_MEDIUM;

        if (getspeed == 0) {
            captureSpeed = CaptureSpeed.CAPTURE_SPEED_VERYSLOW;
        } else if (getspeed == 1) {
            captureSpeed = CaptureSpeed.CAPTURE_SPEED_SLOW;
        } else if (getspeed == 2) {
            captureSpeed = CaptureSpeed.CAPTURE_SPEED_MEDIUM;
        } else if (getspeed == 3) {
            captureSpeed = CaptureSpeed.CAPTURE_SPEED_FAST;
        } else if (getspeed == 4) {
            captureSpeed = CaptureSpeed.CAPTURE_SPEED_VERYFAST;
        }

        t5FingerCaptureController.setCaptureSpeed(captureSpeed);
        t5FingerCaptureController.setPropDenoise(settingsPrefManager.isProprietaryDenoiseEnabled());
        t5FingerCaptureController.setCleanFingerPrints(settingsPrefManager.isCleanFingerprintsEnabled());

        float luxOutsideThreshold = 200.0f;
        float luxCurrentValue = m_lightSensorHelper.getCurrentLightValue();

        boolean outsideCapture = ((luxCurrentValue > luxOutsideThreshold) ||
                (luxCurrentValue < 0.0f));


        t5FingerCaptureController.setOutsideCaptureFlag(outsideCapture);

        ImageConfiguration segmentedFingersConfiguration = getImageConfiguration();


        t5FingerCaptureController.setSegmentedFingerImagesConfig(segmentedFingersConfiguration);


        ImageConfiguration slapConfig = new ImageConfiguration();
        slapConfig.setPrimaryImageType(ImageType.IMAGE_TYPE_BMP);
//            slapConfig.setCompressionRatio(10);

        slapConfig.setIsCropImage(false);
//            slapConfig.setCroppedImageWidth(1600);
//            slapConfig.setCroppedImageHeight(1500);
//            //0->Black color padding; 255->white color padding
//            slapConfig.setPaddingColor(0);

        t5FingerCaptureController.setSlapImagesConfig(slapConfig);

        t5FingerCaptureController.setTimeoutInSecs(Math.max(1, captureTimeout / 1000));

//        t5FingerCaptureController.setsavesdklogs(settingsPrefManager.isSaveSdkLogEnabled());
//        t5FingerCaptureController.setSavefingerprints(settingsPrefManager.isSaveFingerprintsEnabled());

        t5FingerCaptureController.setSavefingerprints(true);
        t5FingerCaptureController.setReversefingerprints(settingsPrefManager.isGetFingerReverseEnabled());

        t5FingerCaptureController.captureFingers(context, listener);
    }

    private static SegmentationMode toDualSegmentationMode(String a, String b) {
        Set<String> pair = new HashSet<>(Arrays.asList(a, b));
        if (isPair(pair, DeviceConstants.BIO_NAME_LEFT_INDEX, DeviceConstants.BIO_NAME_LEFT_MIDDLE)) {
            return SegmentationMode.SEGMENTATION_MODE_LEFT_INDEX_MIDDLE;
        }
        if (isPair(pair, DeviceConstants.BIO_NAME_RIGHT_INDEX, DeviceConstants.BIO_NAME_RIGHT_MIDDLE)) {
            return SegmentationMode.SEGMENTATION_MODE_RIGHT_INDEX_MIDDLE;
        }
        if (isPair(pair, DeviceConstants.BIO_NAME_LEFT_RING, DeviceConstants.BIO_NAME_LEFT_LITTLE)) {
            return SegmentationMode.SEGMENTATION_MODE_LEFT_RING_LITTLE;
        }
        if (isPair(pair, DeviceConstants.BIO_NAME_RIGHT_RING, DeviceConstants.BIO_NAME_RIGHT_LITTLE)) {
            return SegmentationMode.SEGMENTATION_MODE_RIGHT_RING_LITTLE;
        }
        return null;
    }

    private static boolean isPair(Set<String> pair, String x, String y) {
        return pair.size() == 2 && pair.contains(x) && pair.contains(y);
    }

    /** Maps a MOSIP finger bio sub-type name to the T5 single-finger segmentation mode. */
    private static SegmentationMode toSegmentationMode(String bioName) {
        if (bioName == null) {
            return null;
        }
        switch (bioName) {
            case DeviceConstants.BIO_NAME_RIGHT_THUMB:  return SegmentationMode.SEGMENTATION_MODE_RIGHT_THUMB;
            case DeviceConstants.BIO_NAME_RIGHT_INDEX:  return SegmentationMode.SEGMENTATION_MODE_RIGHT_INDEX;
            case DeviceConstants.BIO_NAME_RIGHT_MIDDLE: return SegmentationMode.SEGMENTATION_MODE_RIGHT_MIDDLE;
            case DeviceConstants.BIO_NAME_RIGHT_RING:   return SegmentationMode.SEGMENTATION_MODE_RIGHT_RING;
            case DeviceConstants.BIO_NAME_RIGHT_LITTLE: return SegmentationMode.SEGMENTATION_MODE_RIGHT_LITTLE;
            case DeviceConstants.BIO_NAME_LEFT_THUMB:   return SegmentationMode.SEGMENTATION_MODE_LEFT_THUMB;
            case DeviceConstants.BIO_NAME_LEFT_INDEX:   return SegmentationMode.SEGMENTATION_MODE_LEFT_INDEX;
            case DeviceConstants.BIO_NAME_LEFT_MIDDLE:  return SegmentationMode.SEGMENTATION_MODE_LEFT_MIDDLE;
            case DeviceConstants.BIO_NAME_LEFT_RING:    return SegmentationMode.SEGMENTATION_MODE_LEFT_RING;
            case DeviceConstants.BIO_NAME_LEFT_LITTLE:  return SegmentationMode.SEGMENTATION_MODE_LEFT_LITTLE;
            default: return null;
        }
    }

    private @NonNull ImageConfiguration getImageConfiguration() {
        ImageConfiguration segmentedFingersConfiguration = new ImageConfiguration();
        segmentedFingersConfiguration.setPrimaryImageType(ImageType.IMAGE_TYPE_PNG);
        segmentedFingersConfiguration.setRequireDisplayImage(false);
        segmentedFingersConfiguration.setDisplayImageType(ImageType.IMAGE_TYPE_BMP);

        //compresion ratio is only applicable for IMAGE_TYPE_WSQ
        segmentedFingersConfiguration.setCompressionRatio(10);

        segmentedFingersConfiguration.setIsCropImage(false);
        segmentedFingersConfiguration.setCroppedImageWidth(512);
        segmentedFingersConfiguration.setCroppedImageHeight(512);
        //0->Black color padding; 255->white color padding
        segmentedFingersConfiguration.setPaddingColor(255);
        return segmentedFingersConfiguration;
    }

//    @Override
//    public void onSuccess(FingerCaptureResult result) {
////        if (settingsPrefManager.isGetFingerReverseEnabled() && result.reversefingerScores != SE_OK) {
////            Toast.makeText(MainActivity.this, "Something went wrong. Try again!", Toast.LENGTH_LONG).show();
////            return;
////        }
////        Result captureResult = new Result();
////        captureResult.livenessScores = result.livenessScores;
////
////        m_rootDirectory = Objects.requireNonNull(getExternalFilesDir(null)).getAbsolutePath() + File.separator + System.currentTimeMillis();
////        if (result.fingers != null && !result.fingers.isEmpty()) {
////            captureResult.fingers = saveFingerImages(result.fingers);
////        }
////
////        Intent intent = new Intent(MainActivity.this, ResultScreen.class);
////        intent.putExtra("result", captureResult);
////        startActivity(intent);
//    }
//
//    @Override
//    public void onTimedout() {
////        Toast.makeText(T5CaptureActivity.this, "capture timedout ", Toast.LENGTH_LONG).show();
//    }
//
//    @Override
//    public void onFailure(String errorMessage) {
////        Toast.makeText(T5CaptureActivity.this, "error " + errorMessage, Toast.LENGTH_LONG).show();
//        Log.e("TAG", errorMessage);
//    }
//
//    @Override
//    public void onCancelled() {
////        Toast.makeText(T5CaptureActivity.this, "User cancelled ", Toast.LENGTH_LONG).show();
//    }

//    private ArrayList<FingerData> saveFingerImages(ArrayList<Finger> fingers) {
//        ArrayList<FingerData> list = new ArrayList<>();
//
//        int threadCount = fingers.size();
//        SaveImageThread[] saveImageThreads = null;
//
//        try {
//            saveImageThreads = new SaveImageThread[threadCount];
//            ArrayList<Future<Runnable>> futures = new ArrayList<>();
//            int threadIndex;
//            for (threadIndex = 0; threadIndex < threadCount; threadIndex++) {
//
//
//                Finger finger = fingers.get(threadIndex);
//
//                Log.d("TAG", "finger pos " + finger.pos + " type " + finger.primaryImageType + " prop quality " + finger.quality + " nist2 quality " + finger.nist2Quality + " nist quality " + finger.nistQuality + " image size " + (finger.primaryImage == null ? "null" : finger.primaryImage.length));
//
//                saveImageThreads[threadIndex] = new SaveImageThread(finger);
//
//                Future future = m_service.submit(saveImageThreads[threadIndex]);
//                futures.add(future);
//            }
//
//            for (Future<Runnable> future : futures) {
//                future.get();
//            }
//
//            for (threadIndex = 0; threadIndex < threadCount; threadIndex++) {
//                SaveImageThread thread = saveImageThreads[threadIndex];
//
//                FingerData fingerData = thread.getFingerData();
//                list.add(fingerData);
//
//            }
//
//        } catch (Exception ignore) {
//        }
//
//        return list;
//    }


//    public static class SaveImageThread implements Runnable {
//        private final Finger finger;
//        private FingerData fingerData;
//
//        public FingerData getFingerData() {
//            return fingerData;
//        }
//
//        public SaveImageThread(Finger finger) {
//            this.finger = finger;
//        }
//
//
//        @Override
//        public void run() {
//
//
//            fingerData = new FingerData(finger);
//
//            String extn = ".wsq";
//
//            if (finger.primaryImageType == ImageType.IMAGE_TYPE_BMP) {
//                extn = ".bmp";
//            } else if (finger.primaryImageType == ImageType.IMAGE_TYPE_PNG) {
//                extn = ".png";
//            }
//
//
//            String fingerImgPath = m_rootDirectory + File.separator + "prim_finger_" + finger.pos + extn;
//
////            File rootDir = this.getExternalFilesDir(null);
////            File sessionDir = new File(rootDir, String.valueOf(System.currentTimeMillis()));
////            File file = new File(sessionDir, "prim_finger_" + finger.pos + extn);
//
//            writeToFile(finger.primaryImage, fingerImgPath);
//
//            fingerData.primaryImagePath = fingerImgPath;
//
//
//            if (finger.displayImage != null && finger.displayImage.length > 0) {
//
//                String displImgextn = ".wsq";
//
//                if (finger.displayImageType == ImageType.IMAGE_TYPE_BMP) {
//                    displImgextn = ".bmp";
//                } else if (finger.displayImageType == ImageType.IMAGE_TYPE_PNG) {
//                    displImgextn = ".png";
//                }
//
//
//                String displayfingerImgPath = m_rootDirectory + File.separator + "disp_finger_" + finger.pos + displImgextn;
//
//                writeToFile(finger.displayImage, displayfingerImgPath);
//
//                fingerData.displayImagePath = displayfingerImgPath;
//
//
//            }
//        }
//    }
}