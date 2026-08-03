package client;

import android.Manifest;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.preference.PreferenceManager;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.android.gms.common.util.IOUtils;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textview.MaterialTextView;

import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import io.android.sbi.R;
import io.android.sbi.constants.ClientConstants;
import io.android.sbi.dto.CaptureDetail;
import io.android.sbi.dto.CaptureRequestDeviceDetailDto;
import io.android.sbi.dto.CaptureRequestDto;
import io.android.sbi.dto.CaptureResponse;
import io.android.sbi.dto.DeviceDiscoveryRequestDetail;
import io.android.sbi.dto.DeviceInfoResponse;
import io.android.sbi.dto.DiscoverDto;
import io.android.sbi.dto.Error;
import io.android.sbi.secureLib.DeviceKeystore;
import io.android.sbi.utility.DeviceConstants;

/**
 * @author NPrime Technologies
 */

public class ClientActivity extends AppCompatActivity {

    private static final String MODALITY = "Finger";
    private static final int REQUEST_CODE_PERMISSION_INTERNET = 1001;
    private static final int REQUEST_DISCOVER = 1;
    private static final int REQUEST_INFO = 2;
    private static final int REQUEST_REG_CAPTURE = 3;
    private static final int REQUEST_AUTH_CAPTURE = 4;

    private static final int DEFAULT_TIMEOUT_SECONDS = 60;
    private static final int DEFAULT_SCORE = 30;


    private static final int HANDLER_DISPLAY_TOAST = 0;
    private static final int HANDLER_DISPLAY_CAPTURE_RESPONSE = 1;
    private static final int HANDLER_DISPLAY_EMPTY_SCREEN = 2;
    private static final int HANDLER_DISPLAY_INFO_RESPONSE = 3;
    private static final int HANDLER_DISPLAY_PROGRESS_BAR_SCREEN = 4;


    MaterialButton btnInfo, btnDiscover, btnCapture;
    MaterialTextView textBox, manufacturer, modelId, deviceId, deviceStatus, textBoxLabel;
    ImageButton btnShareResponse;
    Spinner deviceTypeSpinner;
    EditText timeoutInput, scoreInput;
    TableRow deviceIdRow, deviceTypeRow;
    ConstraintLayout emptyScreen, responseScreen, progressBarScreen;

    static String appID = null;
    String serialNo = null;
    String deviceIdValue = null;   // deviceId received from Info, echoed back in the capture request
    private String responseData = null;
    private String selectedDeviceType = MODALITY;
    private final List<DiscoverDto> discoveredDevices = new ArrayList<>();
    SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_client);

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        Toolbar toolbar = findViewById(R.id.toolbar_client);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(true);

        btnInfo = findViewById(R.id.info);
        btnDiscover = findViewById(R.id.discover);
        btnCapture = findViewById(R.id.capture);
        textBoxLabel = findViewById(R.id.response_label);
        textBox = findViewById(R.id.textbox);
        manufacturer = findViewById(R.id.manufacturer);
        modelId = findViewById(R.id.model_id);
        deviceId = findViewById(R.id.device_id);
        deviceStatus = findViewById(R.id.device_status);
        deviceIdRow = findViewById(R.id.device_id_row);
        deviceTypeRow = findViewById(R.id.device_type_row);
        deviceTypeSpinner = findViewById(R.id.device_type_spinner);
        emptyScreen = findViewById(R.id.empty_layout);
        responseScreen = findViewById(R.id.response_layout);
        progressBarScreen = findViewById(R.id.client_progress_layout);
        btnShareResponse = findViewById(R.id.share_response);
        timeoutInput = findViewById(R.id.timeout_input);
        scoreInput = findViewById(R.id.score_input);

        textBox.setMovementMethod(new ScrollingMovementMethod());
        btnInfo.setEnabled(false);
        btnCapture.setEnabled(false);
        deviceTypeSpinner.setEnabled(false);

        deviceTypeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position >= 0 && position < discoveredDevices.size()) {
                    try {
                        updateSelectedDevice(discoveredDevices.get(position));
                    } catch (Exception e) {
                        e.printStackTrace();
                        Toast.makeText(ClientActivity.this, "Digital ID error", Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                appID = null;
            }
        });

        initViews();
        btnInfo.setOnClickListener(view -> {
            if (null == appID) {
                discover();
            } else {
                textBox.setText("");
                info();
            }
        });



        btnDiscover.setOnClickListener(view -> {
            textBox.setText("");
            discover();
        });

        btnCapture.setOnClickListener(view -> {
            textBox.setText("");
            if ("Finger".equalsIgnoreCase(selectedDeviceType)) {
                showFingerCaptureTypeDialog();
            } else {
                capture(".Capture", REQUEST_AUTH_CAPTURE, null);
            }
        });


        btnShareResponse.setOnClickListener(view -> {
            try {
                if (null != responseData && !responseData.isEmpty()) {
                    String path = ClientActivity.this.getFilesDir().getAbsolutePath();
                    File file = new File(path);
                    File txtFile = new File(file, "response.txt");

                    FileOutputStream fOut = new FileOutputStream(txtFile);
                    OutputStreamWriter myOutWriter = new OutputStreamWriter(fOut);
                    myOutWriter.append(responseData);
                    myOutWriter.close();
                    fOut.flush();
                    fOut.close();

                    Uri uri = FileProvider.getUriForFile(ClientActivity.this, "io.tech.sbi.fileprovider", txtFile);
                    Intent share = new Intent(Intent.ACTION_SEND);
                    share.setType("plain/*");
                    share.putExtra(Intent.EXTRA_STREAM, uri);
                    startActivity(Intent.createChooser(share, "Share file"));
                } else {
                    showEmptyScreen();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private void initViews() {
        String deviceUsage = sharedPreferences.getString(ClientConstants.DEVICE_USAGE
                , DeviceConstants.DeviceUsage.Authentication.getDeviceUsage());

        if (!DeviceConstants.DeviceUsage.Registration.getDeviceUsage().equalsIgnoreCase(deviceUsage)) {
            btnCapture.setEnabled(appID != null);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        initViews();
    }

    private void discover() {
        try {
            resetDiscoveredDeviceControls();
            Intent intent = new Intent();
            intent.setAction("io.sbi.device");

            PackageManager packageManager = this.getPackageManager();
            List<ResolveInfo> activities = packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
            final boolean isIntentSafe = activities.size() > 0;

            if (isIntentSafe) {
                String packageName = null;
                for (ResolveInfo activity : activities) {
                    if (activity.activityInfo.applicationInfo.packageName.equals("io.tech.sbi")) {
                        packageName = activity.activityInfo.applicationInfo.packageName;
                        intent.setComponent(new ComponentName(packageName, activity.activityInfo.name));
                        DeviceDiscoveryRequestDetail discoverRequestDto = new DeviceDiscoveryRequestDetail();
                        discoverRequestDto.type = "Biometric Device";

                        intent.putExtra("input", new ObjectMapper().writeValueAsBytes(discoverRequestDto));
                        startActivityForResult(intent, REQUEST_DISCOVER);
                        break;
                    }
                }
                if (null == packageName) {
                    Toast.makeText(ClientActivity.this, "Supported app not found", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(ClientActivity.this, "Supported apps not found", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void info() {
        Intent intent = new Intent();
        intent.setAction(appID + ".Info");

        PackageManager packageManager = this.getPackageManager();
        List<ResolveInfo> activities = packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
        activities.sort(new ResolveInfo.DisplayNameComparator(packageManager));
        final boolean isIntentSafe = activities.size() > 0;

        if (isIntentSafe) {
            String packageName;
            for (ResolveInfo activity : activities) {
                if (appID.startsWith(activity.activityInfo.applicationInfo.packageName)) {
                    packageName = activity.activityInfo.applicationInfo.packageName;
                    intent.setComponent(new ComponentName(packageName, activity.activityInfo.name));
                    startActivityForResult(intent, REQUEST_INFO);
                    break;
                }
            }
        } else {
            Toast.makeText(ClientActivity.this, "Supported apps not found", Toast.LENGTH_SHORT).show();
        }
    }

    private int parseOrDefault(String text, int def, int min, int max) {
        try {
            int value = Integer.parseInt(text.trim());
            return Math.max(min, Math.min(max, value));
        } catch (Exception e) {
            return def;
        }
    }

    private void capture(String action, int requestCode, FingerInput input) {
        try {
            Intent intent = new Intent();
            intent.setAction(appID + action);

            PackageManager packageManager = this.getPackageManager();
            List<ResolveInfo> activities = packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
            activities.sort(new ResolveInfo.DisplayNameComparator(packageManager));
            final boolean isIntentSafe = activities.size() > 0;
            if (isIntentSafe) {
                if (null == serialNo) {
                    Toast.makeText(ClientActivity.this, "Perform info request", Toast.LENGTH_SHORT).show();
                    return;
                }

                int timeoutSeconds = parseOrDefault(timeoutInput.getText().toString(), DEFAULT_TIMEOUT_SECONDS, 1, Integer.MAX_VALUE);
                int requestedScore = parseOrDefault(scoreInput.getText().toString(), DEFAULT_SCORE, 0, 100);

                CaptureRequestDto captureRequestDto = new CaptureRequestDto();
                captureRequestDto.env = DeviceConstants.ENVIRONMENT;
                captureRequestDto.purpose = DeviceConstants.DeviceUsage.Authentication.getDeviceUsage();
                captureRequestDto.specVersion = DeviceConstants.MDS_VERSION;
                captureRequestDto.timeout = timeoutSeconds * 1000;
                captureRequestDto.captureTime = "2021-07-18T17:56:11Z";
                captureRequestDto.domainUri = DeviceConstants.DOMAIN_URI;
                captureRequestDto.transactionId = "1234567890";
                CaptureRequestDeviceDetailDto bio = new CaptureRequestDeviceDetailDto();
                bio.type = selectedDeviceType;
                bio.count = "0";
                bio.bioSubType = new String[]{"UNKNOWN"};
                bio.requestedScore = requestedScore;
                // Echo the deviceId received from Info so it matches discover/info.
                bio.deviceId = (deviceIdValue != null) ? deviceIdValue : serialNo;
                bio.deviceSubId = "0";
                bio.previousHash = "";

                if (input != null) {
                    bio.bioSubType = input.bioSubType;
                    bio.deviceSubId = input.deviceSubId;
                    bio.count = input.count;
                    bio.exception = new String[0];
                }

                List<CaptureRequestDeviceDetailDto> bioRequest = new ArrayList<>();
                bioRequest.add(bio);
                captureRequestDto.bio = bioRequest;
                captureRequestDto.customOpts = null;

                String packageName;
                for (ResolveInfo activity : activities) {
                    if (appID.startsWith(activity.activityInfo.applicationInfo.packageName)) {
                        packageName = activity.activityInfo.applicationInfo.packageName;
                        intent.setComponent(new ComponentName(packageName, activity.activityInfo.name));
                        intent.putExtra("input", new ObjectMapper().writeValueAsBytes(captureRequestDto));
                        startActivityForResult(intent, requestCode);
                        break;
                    }
                }
            } else {
                Toast.makeText(ClientActivity.this, "Supported apps not found", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /** All ten fingers offered for selection, in ANSI position order. */
    private static final String[] FINGER_SUB_TYPES = {
            DeviceConstants.BIO_NAME_RIGHT_THUMB, DeviceConstants.BIO_NAME_RIGHT_INDEX,
            DeviceConstants.BIO_NAME_RIGHT_MIDDLE, DeviceConstants.BIO_NAME_RIGHT_RING,
            DeviceConstants.BIO_NAME_RIGHT_LITTLE, DeviceConstants.BIO_NAME_LEFT_THUMB,
            DeviceConstants.BIO_NAME_LEFT_INDEX, DeviceConstants.BIO_NAME_LEFT_MIDDLE,
            DeviceConstants.BIO_NAME_LEFT_RING, DeviceConstants.BIO_NAME_LEFT_LITTLE
    };

    /** First step for a finger capture: ask whether to capture single fingers or a slap. */
    private void showFingerCaptureTypeDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Capture Type")
                .setItems(new String[]{"Single", "Slap"}, (dialog, which) -> {
                    if (which == 0) {
                        showFingerSelectionDialog();
                    } else {
                        showFingerSlapDialog();
                    }
                })
                .show();
    }

    /** Single capture: pick one or more individual fingers, then fire the capture with that set. */
    private void showFingerSelectionDialog() {
        final boolean[] checked = new boolean[FINGER_SUB_TYPES.length];
        new AlertDialog.Builder(this)
                .setTitle("Select Finger(s)")
                .setMultiChoiceItems(FINGER_SUB_TYPES, checked,
                        (dialog, which, isChecked) -> checked[which] = isChecked)
                .setPositiveButton("Capture", (dialog, which) -> {
                    List<String> selected = new ArrayList<>();
                    for (int i = 0; i < FINGER_SUB_TYPES.length; i++) {
                        if (checked[i]) {
                            selected.add(FINGER_SUB_TYPES[i]);
                        }
                    }
                    if (selected.isEmpty()) {
                        Toast.makeText(this, "Select at least one finger", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    capture(".Capture", REQUEST_AUTH_CAPTURE, FingerInput.single(selected.toArray(new String[0])));
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    /** Slap capture: pick a slap; deviceSubId drives the slap segmentation on the device. */
    private void showFingerSlapDialog() {
        FingerSlab[] slabs = FingerSlab.values();
        String[] labels = new String[slabs.length];
        for (int i = 0; i < slabs.length; i++) {
            labels[i] = slabs[i].label;
        }
        new AlertDialog.Builder(this)
                .setTitle("Select Slap")
                .setItems(labels, (dialog, which) ->
                        capture(".Capture", REQUEST_AUTH_CAPTURE,
                                FingerInput.slap(slabs[which].deviceSubId, slabs[which].count)))
                .show();
    }

    /** Resolved finger-capture parameters for a request: single (bioSubType) or slap (deviceSubId). */
    private static final class FingerInput {
        final String[] bioSubType;   // the specific fingers for single capture; null for a slap
        final String deviceSubId;    // "0" for single; "1"/"2"/"3" for a slap
        final String count;

        private FingerInput(String[] bioSubType, String deviceSubId, String count) {
            this.bioSubType = bioSubType;
            this.deviceSubId = deviceSubId;
            this.count = count;
        }

        static FingerInput single(String[] fingers) {
            return new FingerInput(fingers, "0", String.valueOf(fingers.length));
        }

        static FingerInput slap(int deviceSubId, int count) {
            return new FingerInput(null, String.valueOf(deviceSubId), String.valueOf(count));
        }
    }

    /** Slaps offered in the slap dialog, with the deviceSubId and finger count each maps to. */
    private enum FingerSlab {
        LEFT_FOUR("Left 4 Fingers", DeviceConstants.DEVICE_FINGER_SLAP_SUB_TYPE_ID_LEFT, 4),
        RIGHT_FOUR("Right 4 Fingers", DeviceConstants.DEVICE_FINGER_SLAP_SUB_TYPE_ID_RIGHT, 4),
        TWO_THUMBS("2 Thumbs", DeviceConstants.DEVICE_FINGER_SLAP_SUB_TYPE_ID_THUMB, 2);

        final String label;
        final int deviceSubId;
        final int count;

        FingerSlab(String label, int deviceSubId, int count) {
            this.label = label;
            this.deviceSubId = deviceSubId;
            this.count = count;
        }
    }

    private void showResponse(String responseLabel, String data) {
        emptyScreen.setVisibility(View.GONE);
        progressBarScreen.setVisibility(View.GONE);
        responseScreen.setVisibility(View.VISIBLE);
        textBoxLabel.setText(responseLabel);
        if (data.length() > 1000) {
            data = data.substring(0, 900) + "....||...." + data.substring(data.length() - 90, data.length());
        }
        textBox.setText(data);
    }

    private void showEmptyScreen() {
        textBoxLabel.setText("Response : ");
        textBox.setText("");
        responseScreen.setVisibility(View.GONE);
        progressBarScreen.setVisibility(View.GONE);
        emptyScreen.setVisibility(View.VISIBLE);
        responseData = null;
    }

    private void showProgressBarScreen() {
        emptyScreen.setVisibility(View.GONE);
        responseScreen.setVisibility(View.GONE);
        progressBarScreen.setVisibility(View.VISIBLE);
        textBox.setText("");
        //responseData = null;
    }

    private void updateDiscoveredDevices(List<DiscoverDto> devices, ObjectMapper objectMapper) throws Exception {
        discoveredDevices.clear();
        discoveredDevices.addAll(devices);

        List<String> deviceTypes = new ArrayList<>();
        for (DiscoverDto device : devices) {
            deviceTypes.add(getDeviceType(device));
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, deviceTypes);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        deviceTypeSpinner.setAdapter(adapter);
        deviceTypeSpinner.setEnabled(true);
        deviceTypeRow.setVisibility(View.VISIBLE);
        btnInfo.setEnabled(true);
        // Capture stays disabled until an Info request reports the device as Ready.
        btnCapture.setEnabled(false);

        updateSelectedDevice(devices.get(0));
        showResponse("Discover response :", objectMapper.writeValueAsString(devices));
    }

    private void resetDiscoveredDeviceControls() {
        appID = null;
        serialNo = null;
        discoveredDevices.clear();
        deviceTypeSpinner.setAdapter(null);
        deviceTypeSpinner.setEnabled(false);
        deviceTypeRow.setVisibility(View.GONE);
        btnInfo.setEnabled(false);
        btnCapture.setEnabled(false);
    }

    private void updateSelectedDevice(DiscoverDto device) throws Exception {
        JSONObject digitalIDObj = getDigitalIdObject(device);
        if (digitalIDObj.has("make")) {
            manufacturer.setText(digitalIDObj.getString("make"));
        }
        if (digitalIDObj.has("model")) {
            modelId.setText(digitalIDObj.getString("model"));
        }
        if (device.deviceStatus != null) {
            deviceStatus.setText(device.deviceStatus);
        }
        // Selecting a (different) device requires a fresh Info before capture is allowed.
        btnCapture.setEnabled(false);
        String strDeviceId = device.deviceId;
        if (!strDeviceId.isEmpty()) {
            deviceIdRow.setVisibility(View.VISIBLE);
            deviceId.setText(strDeviceId);
        } else {
            deviceId.setText("");
            deviceIdRow.setVisibility(View.GONE);
        }
        appID = device.callbackId;
        selectedDeviceType = digitalIDObj.optString("type", MODALITY);
    }

    private String getDeviceType(DiscoverDto device) throws Exception {
        String deviceType = getDigitalIdObject(device).optString("type", "");
        if (!deviceType.isEmpty()) {
            return deviceType;
        }
        return device.callbackId == null ? "" : device.callbackId;
    }

    private JSONObject getDigitalIdObject(DiscoverDto device) throws Exception {
        String encodedDigitalID = device.digitalId;
        if (encodedDigitalID == null || encodedDigitalID.isEmpty()) {
            throw new Exception("Digital ID error");
        }
        byte[] digitalIdBytes = Base64.getUrlDecoder().decode(encodedDigitalID);
        return new JSONObject(new String(digitalIdBytes));
    }

    public void sendMessage(int what, Object obj) {
        Message message = new Message();
        message.what = what;
        message.obj = obj;
        handler.sendMessage(message);
    }

    private final Handler handler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case HANDLER_DISPLAY_CAPTURE_RESPONSE:
                    String captureData = (String) msg.obj;
                    showResponse("Capture Response", captureData);
                    break;
                case HANDLER_DISPLAY_TOAST:
                    String toastText = (String) msg.obj;
                    Toast.makeText(ClientActivity.this, toastText, Toast.LENGTH_SHORT).show();
                    break;
                case HANDLER_DISPLAY_EMPTY_SCREEN:
                    showEmptyScreen();
                    break;
                case HANDLER_DISPLAY_INFO_RESPONSE:
                    String infoData = (String) msg.obj;
                    showResponse("Info Response", infoData);
                    break;
                case HANDLER_DISPLAY_PROGRESS_BAR_SCREEN:
                    showProgressBarScreen();
                    break;
                default:
            }

        }
    };

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (REQUEST_DISCOVER == requestCode) {
            if (Activity.RESULT_OK == resultCode) {
                try {
                    if (null != data) {
                        if (data.hasExtra("response")) {
                            byte[] response = data.getByteArrayExtra("response");
                            ObjectMapper ob = new ObjectMapper();
                            List<DiscoverDto> list = ob.readValue(response,
                                    new TypeReference<List<DiscoverDto>>() {
                                    });

                            if (!list.isEmpty()) {
                                updateDiscoveredDevices(list, ob);
                            } else {
                                Toast.makeText(ClientActivity.this, "Discover failed - No Devices", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(ClientActivity.this, "Discover failed", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(ClientActivity.this, "Discover failed", Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } else if (REQUEST_INFO == requestCode) {
            if (Activity.RESULT_OK == resultCode) {
                try {
                    if (null != data) {
                        if (data.hasExtra("response")) {
                            byte[] response = data.getByteArrayExtra("response");
                            List<DeviceInfoResponse> list = new ObjectMapper().readValue(response,
                                    new TypeReference<List<DeviceInfoResponse>>() {
                                    });
                            Error errorObject = list.get(0).error;
                            if ("0".equals(errorObject.errorCode)) {
                                String deviceInfo = list.get(0).deviceInfo;
                                byte[] payload = getPayloadBufferFromJwt(deviceInfo);

                                JSONObject infoObject = new JSONObject(new String(payload));
                                String digitalId = infoObject.getString("digitalId");
                                byte[] digitalIdPayload = getPayloadBufferFromJwt(digitalId);

                                JSONObject digitalIdObj = new JSONObject(new String(digitalIdPayload));
                                serialNo = digitalIdObj.getString("serialNo");
                                if (infoObject.has("deviceId")) {
                                    deviceIdValue = infoObject.getString("deviceId");
                                    deviceIdRow.setVisibility(View.VISIBLE);
                                    deviceId.setText(deviceIdValue);
                                }
                                String status = infoObject.has("deviceStatus")
                                        ? infoObject.getString("deviceStatus") : "";
                                deviceStatus.setText(status);
                                // Capture is only allowed when the device reports Ready.
                                btnCapture.setEnabled(
                                        DeviceConstants.ServiceStatus.READY.getStatus().equalsIgnoreCase(status));
                                showResponse("Info response :", list.get(0).toString());
                                responseData = list.get(0).toString();
                            } else {
                                showResponse("Info response", list.get(0).toString());
                                deviceId.setText("");
                                deviceIdRow.setVisibility(View.GONE);
                                deviceStatus.setText(errorObject.errorInfo);
                                btnCapture.setEnabled(false);
                                responseData = list.get(0).toString();
                            }
                        } else {
                            showEmptyScreen();
                            Toast.makeText(ClientActivity.this, "Response Not found", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        showEmptyScreen();
                        Toast.makeText(ClientActivity.this, "Response Not found", Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } else if (REQUEST_REG_CAPTURE == requestCode || REQUEST_AUTH_CAPTURE == requestCode) {
            if (Activity.RESULT_OK == resultCode) {
                if (null != data) {
                    Uri uri = data.getParcelableExtra("response");
                    if (null != uri) {
                        new Thread(() -> {
                            try {
                                sendMessage(HANDLER_DISPLAY_PROGRESS_BAR_SCREEN, null);
                                InputStream respData = getContentResolver().openInputStream(uri);
                                byte[] bytes = IOUtils.toByteArray(respData);
                                CaptureResponse resposeObject = new ObjectMapper().readValue(bytes, CaptureResponse.class);
                                if (resposeObject.biometrics != null && !resposeObject.biometrics.isEmpty()) {
                                    List<CaptureDetail> biometrics = resposeObject.biometrics;
                                    Error errObject = (biometrics.get(0)).error;
                                    responseData = resposeObject.toString();
                                    sendMessage(HANDLER_DISPLAY_CAPTURE_RESPONSE, responseData);
                                    if (!errObject.errorCode.equals("0")) {
                                        sendMessage(HANDLER_DISPLAY_TOAST, errObject.toString());
                                    }
                                } else {
                                    responseData = resposeObject.toString();
                                    sendMessage(HANDLER_DISPLAY_CAPTURE_RESPONSE, responseData);
                                }
                            } catch (Exception e) {
                                sendMessage(HANDLER_DISPLAY_EMPTY_SCREEN, null);
                                sendMessage(HANDLER_DISPLAY_TOAST, "Capture error");
                                e.printStackTrace();
                            }
                        }).start();
                    } else {
                        showEmptyScreen();
                        Toast.makeText(ClientActivity.this, "Response Not found", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    showEmptyScreen();
                    Toast.makeText(ClientActivity.this, "Response Not found", Toast.LENGTH_SHORT).show();
                }
            } else {
                showEmptyScreen();
            }
        }
    }

    public byte[] getPayloadBufferFromJwt(String responseToken) {
        byte[] payLoad = null;
        try {
            String[] responseTokenArray = responseToken.split("\\.");
            payLoad = Base64.getUrlDecoder().decode(responseTokenArray[1]);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return payLoad;
    }

    public void openSettings(View view) {
        Intent intent = new Intent(this, ConfigurationActivity.class);
        startActivity(intent);
    }

    public void askPermissionAndValidateCertificates(View view) {
        // Check if we have Call permission
        int permission = ActivityCompat.checkSelfPermission(this.getApplicationContext(),
                Manifest.permission.INTERNET);

        if (permission != PackageManager.PERMISSION_GRANTED) {
            // If don't have permission so prompt the user.
            this.requestPermissions(
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    REQUEST_CODE_PERMISSION_INTERNET
            );
            return;
        }
        validateCertificates();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_PERMISSION_INTERNET) {
            // Note: If request is cancelled, the result arrays are empty.
            // Permissions granted (CALL_PHONE).
            validateCertificates();
        }
    }

    private void validateCertificates() {
        DeviceKeystore keystore = new DeviceKeystore(this);

        if (keystore.checkCertificateCredentials(ClientConstants.DEVICE_P12_FILE_NAME, "", "")) {
            Toast.makeText(this, "Device key credentials are valid.", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Device key validation failed.", Toast.LENGTH_SHORT).show();
        }

        if (keystore.checkCertificateCredentials(ClientConstants.FTM_P12_FILE_NAME, "", "")) {
            Toast.makeText(this, "FTM key credentials are valid.", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "FTM key validation failed.", Toast.LENGTH_SHORT).show();
        }

        keystore.loadCertificateFromIDA(() -> {
            this.runOnUiThread(() -> {
                String certificateStr = sharedPreferences.getString(ClientConstants.CERTIFICATE_TO_ENCRYPT_BIO, "");

                if (certificateStr.equals("")) {
                    Toast.makeText(this, "Certificate to encrypt failed to load.", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Certificate to encrypt loaded successfully.", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

}
