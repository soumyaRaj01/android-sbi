package client;

import static client.KeyCredentialFragment.ARG_KEY_LABEL;
import static client.KeyCredentialFragment.KEY_TYPE_DEVICE;
import static client.KeyCredentialFragment.KEY_TYPE_FTM;
import static client.KeyCredentialFragment.KEY_TYPE_IDA;
import static io.android.sbi.constants.ClientConstants.*;
import static io.android.sbi.utility.DeviceConstants.DEFAULT_TIME_DELAY;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;

import com.google.firebase.messaging.FirebaseMessaging;

import java.util.ArrayList;
import java.util.concurrent.Executors;

import io.android.sbi.R;
import io.android.sbi.constants.ClientConstants;
import io.android.sbi.crypto.AndroidKeystoreCryptoProvider;
import io.android.sbi.crypto.CryptoProvider;
import io.android.sbi.crypto.DmsClient;
import io.android.sbi.crypto.ProvisioningManager;
import io.android.sbi.secureLib.DeviceKeystore;
import io.android.sbi.utility.DeviceConstants;
import io.android.sbi.utility.Logger;

/**
 * @author Anshul.Vanawat
 */

public class ConfigurationActivity extends AppCompatActivity {

    private KeyCredentialFragment deviceKeyFragment;
    private KeyCredentialFragment ftmKeyFragment;
    private KeyCredentialFragment idaCertFragment;

    private int currentFaceScore;
    private int currentFingerScore;
    private int currentIrisScore;
    private String currentFaceDeviceStatus;
    private String currentFingerDeviceStatus;
    private String currentIrisDeviceStatus;
    private String currentDeviceUsage;
    private int currentFaceResponseDelay;
    private int currentFingerResponseDelay;
    private int currentIrisResponseDelay;
//    private String currentDmsBaseUrl;
    private EditText dmsBaseUrlInput;

    SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_configuration);
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
//        dmsBaseUrlInput = findViewById(R.id.dms_base_url_input);

        ArrayList<String> deviceUsage = new ArrayList<>();
        deviceUsage.add(DeviceConstants.DeviceUsage.Authentication.getDeviceUsage());
        deviceUsage.add(DeviceConstants.DeviceUsage.Registration.getDeviceUsage());

        ArrayAdapter<String> deviceUsageAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, deviceUsage);
        deviceUsageAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        //default values
        currentFaceScore = sharedPreferences.getInt(FACE_SCORE, 30);
        currentFingerScore = sharedPreferences.getInt(FINGER_SCORE, 30);
        currentIrisScore = sharedPreferences.getInt(IRIS_SCORE, 30);
        currentFaceDeviceStatus = sharedPreferences.getString(FACE_DEVICE_STATUS, DeviceConstants.ServiceStatus.READY.getStatus());
        currentFingerDeviceStatus = sharedPreferences.getString(FINGER_DEVICE_STATUS, DeviceConstants.ServiceStatus.READY.getStatus());
        currentIrisDeviceStatus = sharedPreferences.getString(IRIS_DEVICE_STATUS, DeviceConstants.ServiceStatus.READY.getStatus());
        currentFaceResponseDelay = sharedPreferences.getInt(FACE_RESPONSE_DELAY, DEFAULT_TIME_DELAY);
        currentFingerResponseDelay = sharedPreferences.getInt(FINGER_RESPONSE_DELAY, DEFAULT_TIME_DELAY);
        currentIrisResponseDelay = sharedPreferences.getInt(IRIS_RESPONSE_DELAY, DEFAULT_TIME_DELAY);
        currentDeviceUsage = sharedPreferences.getString(DEVICE_USAGE, DeviceConstants.DeviceUsage.Authentication.getDeviceUsage());
//        currentDmsBaseUrl = sharedPreferences.getString(DMS_BASE_URL, DEFAULT_DMS_BASE_URL);

        FragmentManager fragmentManager = this.getSupportFragmentManager();
        deviceKeyFragment = (KeyCredentialFragment) fragmentManager.findFragmentById(R.id.deviceKeyFragment);
        configureKeyFragment(deviceKeyFragment, KEY_TYPE_DEVICE);

        ftmKeyFragment = (KeyCredentialFragment) fragmentManager.findFragmentById(R.id.ftmKeyFragment);
        configureKeyFragment(ftmKeyFragment, KEY_TYPE_FTM);

        idaCertFragment = (KeyCredentialFragment) fragmentManager.findFragmentById(R.id.idaCertFragment);
        configureKeyFragment(idaCertFragment, KEY_TYPE_IDA);

        resetScreen();
    }

    public void onSave(View view) {
//        currentDmsBaseUrl = normalizeDmsBaseUrl(dmsBaseUrlInput.getText().toString());
//        if (currentDmsBaseUrl.isEmpty()) {
//            Toast.makeText(this, "DMS Base URL is required", Toast.LENGTH_LONG).show();
//            return;
//        }

        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt(FACE_SCORE, currentFaceScore);
        editor.putInt(FINGER_SCORE, currentFingerScore);
        editor.putInt(IRIS_SCORE, currentIrisScore);
        editor.putString(FACE_DEVICE_STATUS, currentFaceDeviceStatus);
        editor.putString(FINGER_DEVICE_STATUS, currentFingerDeviceStatus);
        editor.putString(IRIS_DEVICE_STATUS, currentIrisDeviceStatus);
        editor.putInt(FACE_RESPONSE_DELAY, currentFaceResponseDelay);
        editor.putInt(FINGER_RESPONSE_DELAY, currentFingerResponseDelay);
        editor.putInt(IRIS_RESPONSE_DELAY, currentIrisResponseDelay);
        editor.putString(DEVICE_USAGE, currentDeviceUsage);
//        editor.putString(DMS_BASE_URL, currentDmsBaseUrl);

        editor.apply();

        //navigate back to previous activity
        Toast.makeText(this, "Settings saved", Toast.LENGTH_SHORT).show();
        loadAndValidateCertificates();
        finish();
    }

    public void onReset(View view) {
        resetScreen();
    }

    public void onCheckNow(View view) {
        view.setEnabled(false);
        showCheckingStatus();

        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                CryptoProvider cryptoProvider = new AndroidKeystoreCryptoProvider(getApplicationContext());
                DeviceKeystore deviceKeystore = new DeviceKeystore(getApplicationContext());
                DmsClient dmsClient = new DmsClient(getApplicationContext());
                ProvisioningManager provisioningManager = new ProvisioningManager(cryptoProvider, dmsClient, deviceKeystore);

                provisioningManager.initialize();
                syncFcmToken(dmsClient);
            } catch (Exception e) {
                Logger.e(DeviceConstants.LOG_TAG, "Check Now provisioning failed: " + e.getMessage());
            }

            runOnUiThread(() -> {
                refreshKeyCredentialStatuses();
                view.setEnabled(true);
            });
        });
    }

    private void syncFcmToken(DmsClient dmsClient) {
        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        Logger.w(DeviceConstants.LOG_TAG, "Fetching FCM registration token failed" + task.getException());
                        return;
                    }

                    dmsClient.sendTokenToDmsServer(task.getResult());
                });
    }

    private void resetScreen() {
//        dmsBaseUrlInput.setText(currentDmsBaseUrl);
    }

    private void configureKeyFragment(KeyCredentialFragment fragment, String keyLabel) {
        if (fragment == null) {
            return;
        }

        Bundle bundle = new Bundle();
        bundle.putString(ARG_KEY_LABEL, keyLabel);
        fragment.setArguments(bundle);
        fragment.setKeyLabel(keyLabel);
    }

    private void showCheckingStatus() {
        if (deviceKeyFragment != null) {
            deviceKeyFragment.showCheckingStatus();
        }
        if (ftmKeyFragment != null) {
            ftmKeyFragment.showCheckingStatus();
        }
        if (idaCertFragment != null) {
            idaCertFragment.showCheckingStatus();
        }
    }

    private void refreshKeyCredentialStatuses() {
        if (deviceKeyFragment != null) {
            deviceKeyFragment.refreshStatus();
        }
        if (ftmKeyFragment != null) {
            ftmKeyFragment.refreshStatus();
        }
        if (idaCertFragment != null) {
            idaCertFragment.refreshStatus();
        }
    }

    private String normalizeDmsBaseUrl(String url) {
        String normalizedUrl = url == null ? "" : url.trim();
        while (normalizedUrl.endsWith("/")) {
            normalizedUrl = normalizedUrl.substring(0, normalizedUrl.length() - 1);
        }
        return normalizedUrl;
    }

    private void setSpinner(Spinner spinner, String value) {
        int position = ((ArrayAdapter<String>) spinner.getAdapter()).getPosition(value);
        spinner.setSelection(position);
    }

    private void loadAndValidateCertificates() {
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
