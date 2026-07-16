package io.mosip.mock.sbi.device;

import static io.mosip.mock.sbi.utility.DeviceConstants.*;

import ai.tech5.finger.utils.Finger;
import ai.tech5.finger.utils.FingerCaptureResult;
import ai.tech5.finger.utils.T5FingerCapturedListener;
import ai.tech5.pheonix.capture.controller.FaceCaptureListener;
import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.widget.ImageView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

import io.mosip.mock.sbi.R;
import io.mosip.mock.sbi.constants.ClientConstants;
import io.mosip.mock.sbi.faceCaptureApi.CaptureResult;
import io.mosip.mock.sbi.sdk.T5Capture;
import io.mosip.mock.sbi.sdk.T5FaceCapture;
import io.mosip.mock.sbi.utility.DeviceConstants;

import com.phoenixcapture.camerakit.FaceBox;
import io.mosip.mock.sbi.utility.DeviceErrorCodes;
import io.mosip.mock.sbi.utility.FingerPosition;

/**
 * @author NPrime Technologies
 */

public class CaptureActivity extends AppCompatActivity implements T5FingerCapturedListener, FaceCaptureListener {
    private static final int CAMERA_PERMISSION_CODE = 100;
    private static final String[] APP_PERMISSIONS = {Manifest.permission.CAMERA};

    private static final int CAPTURE_FAILURE_STATUS = Integer.parseInt(DeviceErrorCodes.INVALID_JSON);

    private int faceQualityScore;
    private int fingerQualityScore;
    private int irisQualityScore;
    private BioDevice bioDevice;

    // Store these for use in callbacks
    private int deviceSubId;
    private String[] bioSubType;
    private String[] exception;
    private String modality;
    private long responseDelay;
    private int captureTimeout;
    private int requestedScore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        faceQualityScore = sharedPreferences.getInt(ClientConstants.FACE_SCORE, 30);
        fingerQualityScore = sharedPreferences.getInt(ClientConstants.FINGER_SCORE, 30);
        irisQualityScore = sharedPreferences.getInt(ClientConstants.IRIS_SCORE, 30);

        modality = getIntent().getStringExtra("modality");
        deviceSubId = getIntent().getIntExtra("deviceSubId", 1);
        captureTimeout = getIntent().getIntExtra("CaptureTimeout", Integer.MAX_VALUE);
        bioSubType = getIntent().getStringArrayExtra("bioSubType");
        exception = getIntent().getStringArrayExtra("exception");
        requestedScore = getIntent().getIntExtra("requestedScore", fingerQualityScore);

        // Registration removed — this mock performs auth capture only.
        bioDevice = new AuthBioDevice(this);

        switch (modality.toLowerCase()) {
            case "face":
                responseDelay = sharedPreferences.getInt(ClientConstants.FACE_RESPONSE_DELAY, DEFAULT_TIME_DELAY);
                break;
            case "finger":
                responseDelay = sharedPreferences.getInt(ClientConstants.FINGER_RESPONSE_DELAY, DEFAULT_TIME_DELAY);
                break;
            case "iris":
                responseDelay = sharedPreferences.getInt(ClientConstants.IRIS_RESPONSE_DELAY, DEFAULT_TIME_DELAY);
                break;
            default:
                responseDelay = DEFAULT_TIME_DELAY;
        }

        if (captureTimeout < responseDelay) {
            new Handler().postDelayed(() -> {
                captureFailed(CaptureResult.CAPTURE_TIMEOUT, "");
            }, captureTimeout);
            return;
        }

        // Request camera permission if needed
        if ("finger".equalsIgnoreCase(modality) || "face".equalsIgnoreCase(modality)) {
            if (hasAllPermissionsGranted()) {
                startCapture();
            } else {
                requestPermissionLauncher.launch(APP_PERMISSIONS);
            }
        }
    }

    private boolean hasAllPermissionsGranted() {
        for (String permission : APP_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    private final ActivityResultLauncher<String[]> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                boolean isAllPermissionsGranted = true;

                for (Boolean isGranted : result.values()) {
                    if (Boolean.FALSE.equals(isGranted)) {
                        isAllPermissionsGranted = false;
                        break;
                    }
                }

                if (isAllPermissionsGranted) {
                    startCapture();
                }
            });

    private void startCapture() {
        new Handler().postDelayed(() -> {
            try {
                int qualityScore = 30;
                Map<String, Uri> uris;

                switch (modality.toLowerCase()) {
                    case "face":
                        T5FaceCapture faceCapture = new T5FaceCapture(this);
                        faceCapture.startFaceCapture(this, this, requestedScore ,captureTimeout);
                        // The flow will continue in the callback methods below
                        break;
                    case "finger":
                        T5Capture capture = new T5Capture(this);
                        capture.capture(this, this, deviceSubId, bioSubType, convertToNfiq1(requestedScore), captureTimeout);
                        // The flow will continue in the callback methods below
                        break;
                    case "iris":
                        ((ImageView) findViewById(R.id.img)).setImageResource(R.drawable.iris);
                        uris = bioDevice.captureIrisModality(deviceSubId, bioSubType, exception);
                        qualityScore = irisQualityScore;
                        captureSuccessful(uris, qualityScore);
                        break;
                    default:
                        uris = new HashMap<>();
                        captureSuccessful(uris, 0);
                        break;
                }
            } catch (Exception e) {
                e.printStackTrace();
                captureFailed(CAPTURE_FAILURE_STATUS, e.getMessage());
            }
        }, responseDelay);
    }

    private int convertToNfiq1(int score) {
        if (score >= 81) return 1;
        else if (score >= 61) return 2;
        else if (score >= 41) return 3;
        else if (score >= 21) return 4;
        else return 5;
    }

    @Override
    public void onSuccess(FingerCaptureResult result) {
        handleFingerResult(result, false);
    }

    @Override
    public void onFailure(String errorMessage) {
        captureFailed(CAPTURE_FAILURE_STATUS, errorMessage);
    }

    @Override
    public void onTimedout(FingerCaptureResult result) {
        handleFingerResult(result, true);
    }

    private void handleFingerResult(FingerCaptureResult result, boolean timedOut) {
        try {
            if (result == null || result.fingers == null || result.fingers.isEmpty()) {
                captureFailed(timedOut ? CaptureResult.CAPTURE_TIMEOUT : CAPTURE_FAILURE_STATUS,
                        timedOut ? "Capture timeout" : "No finger data captured");
                return;
            }
            Map<String, Uri> uris = bioDevice.generateFingerIsoUris(result.fingers);
            if (uris.isEmpty()) {
                captureFailed(CAPTURE_FAILURE_STATUS, "No valid finger data captured");
                return;
            }
            captureSuccessful(uris, averageFingerQuality(result.fingers));
        } catch (Exception e) {
            captureFailed(CAPTURE_FAILURE_STATUS, e.getMessage());
        }
    }

    private int averageFingerQuality(List<Finger> fingers) {
        if (fingers == null || fingers.isEmpty()) {
            return 0;
        }
        int sum = 0;
        for (Finger finger : fingers) {
            sum += nfiqToScore(finger.quality);
        }
        return sum / fingers.size();
    }

    private int nfiqToScore(int nfiq) {
        switch (nfiq) {
            case 1:  return 90;
            case 2:  return 70;
            case 3:  return 50;
            case 4:  return 30;
            default: return 10;
        }
    }

    @Override
    public void onCancelled() {
        captureFailed(CAPTURE_FAILURE_STATUS, "Capture cancelled by user");
    }

    @Override
    public void onFaceCaptured(byte[] faceImage, byte[] fullFrameImage, FaceBox faceBox) {
        try {
            byte[] capturedData = (faceImage != null && faceImage.length > 0) ? faceImage : fullFrameImage;
            if (capturedData == null || capturedData.length == 0) {
                captureFailed(CAPTURE_FAILURE_STATUS, "No face data captured");
                return;
            }
            Map<String, Uri> uris = new HashMap<>();
            uris.put("", bioDevice.generateFaceIsoUri(capturedData));

            int quality = faceBox != null ? Math.round(faceBox.mUnifiedQualityScore * 100) : faceQualityScore;
            captureSuccessful(uris, quality);
        } catch (Exception e) {
            captureFailed(CAPTURE_FAILURE_STATUS, e.getMessage());
        }
    }

    @Override
    public void OnFaceCaptureFailed(String errorMessage) {
        captureFailed(CAPTURE_FAILURE_STATUS, errorMessage);
    }

    @Override
    public void onTimedout(byte[] bytes) {
        try {
            if (bytes == null || bytes.length == 0) {
                captureFailed(CaptureResult.CAPTURE_TIMEOUT, "Capture timeout");
                return;
            }

            Map<String, Uri> uris = new HashMap<>();
            uris.put("", bioDevice.generateFaceIsoUri(bytes));
            captureSuccessful(uris, faceQualityScore);
        } catch (Exception e) {
            captureFailed(CAPTURE_FAILURE_STATUS, e.getMessage());
        }
    }

    public void captureSuccessful(Map<String, Uri> uris, int quality) {
        Intent intent = new Intent();
        ArrayList<String> segmentNames = new ArrayList<>();

        if (uris != null) {
            for (String attribute : uris.keySet()) {
                segmentNames.add(attribute);
                intent.putExtra(attribute, uris.get(attribute));
            }
        }

        intent.putExtra("segmentNames", segmentNames);
        intent.putExtra("Status", CaptureResult.CAPTURE_SUCCESS);
        intent.putExtra("Quality", quality);
        setResult(Activity.RESULT_OK, intent);
        finish();
    }

    public void captureFailed(int status, String msg) {
        Intent intent = new Intent();
        intent.putExtra("Status", status);
        intent.putExtra("msg", msg);
        intent.putExtra("Quality", 0);
        setResult(Activity.RESULT_CANCELED, intent);
        finish();
    }
}