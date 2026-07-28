package client;

import static client.FileChooserFragment.ARG_LAST_UPLOAD_DATE;
import static client.KeyCredentialFragment.ARG_KEY_ALIAS;
import static client.KeyCredentialFragment.ARG_KEY_LABEL;
import static client.KeyCredentialFragment.ARG_PASSWORD;
import static client.KeyCredentialFragment.KEY_TYPE_DEVICE;
import static client.KeyCredentialFragment.KEY_TYPE_FTM;
import static io.android.sbi.constants.ClientConstants.*;
import static io.android.sbi.utility.DeviceConstants.DEFAULT_TIME_DELAY;

import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;

import java.util.ArrayList;

import io.android.sbi.R;
import io.android.sbi.constants.ClientConstants;
import io.android.sbi.secureLib.DeviceKeystore;
import io.android.sbi.utility.DateUtil;
import io.android.sbi.utility.DeviceConstants;
import io.android.sbi.utility.FileUtils;

/**
 * @author Anshul.Vanawat
 */

public class ConfigurationActivity extends AppCompatActivity {

    private static final String TAG = ConfigurationActivity.class.getName();
    private static final String LAST_UPDATE = "Last updated : ";

    private KeyCredentialFragment deviceKeyFragment;
    private KeyCredentialFragment ftmKeyFragment;
    private FileChooserFragment idaFirCertificateFragment;

    private String device_currentKeyAlias;
    private String device_currentKeyPassword;
    private String ftm_currentKeyAlias;
    private String ftm_currentKeyPassword;
    private int currentFaceScore;
    private int currentFingerScore;
    private int currentIrisScore;
    private String device_lastUploadDate;
    private String ftm_lastUploadDate;
    private String idaFirCertificateLastUploadDate;
    private String currentFaceDeviceStatus;
    private String currentFingerDeviceStatus;
    private String currentIrisDeviceStatus;
    private String currentDeviceUsage;
    private int currentFaceResponseDelay;
    private int currentFingerResponseDelay;
    private int currentIrisResponseDelay;
    private String currentDmsBaseUrl;
    private EditText dmsBaseUrlInput;

    SharedPreferences sharedPreferences;
    DateUtil dateUtil;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_configuration);
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        dateUtil = new DateUtil(this);
        dmsBaseUrlInput = findViewById(R.id.dms_base_url_input);

        ArrayList<String> deviceUsage = new ArrayList<>();
        deviceUsage.add(DeviceConstants.DeviceUsage.Authentication.getDeviceUsage());
        deviceUsage.add(DeviceConstants.DeviceUsage.Registration.getDeviceUsage());

        ArrayAdapter<String> deviceUsageAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, deviceUsage);
        deviceUsageAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        //default values
        device_currentKeyAlias = sharedPreferences.getString(DEVICE_KEY_ALIAS, "");
        device_currentKeyPassword = sharedPreferences.getString(DEVICE_KEY_STORE_PASSWORD, "");
        ftm_currentKeyAlias = sharedPreferences.getString(FTM_KEY_ALIAS, "");
        ftm_currentKeyPassword = sharedPreferences.getString(FTM_KEY_STORE_PASSWORD, "");
        currentFaceScore = sharedPreferences.getInt(FACE_SCORE, 30);
        currentFingerScore = sharedPreferences.getInt(FINGER_SCORE, 30);
        currentIrisScore = sharedPreferences.getInt(IRIS_SCORE, 30);
        device_lastUploadDate = sharedPreferences.getString(DEVICE_LAST_UPLOAD_DATE, "");
        ftm_lastUploadDate = sharedPreferences.getString(FTM_LAST_UPLOAD_DATE, "");
        idaFirCertificateLastUploadDate = sharedPreferences.getString(IDA_FIR_CERTIFICATE_LAST_UPLOAD_DATE, "");
        currentFaceDeviceStatus = sharedPreferences.getString(FACE_DEVICE_STATUS, DeviceConstants.ServiceStatus.READY.getStatus());
        currentFingerDeviceStatus = sharedPreferences.getString(FINGER_DEVICE_STATUS, DeviceConstants.ServiceStatus.READY.getStatus());
        currentIrisDeviceStatus = sharedPreferences.getString(IRIS_DEVICE_STATUS, DeviceConstants.ServiceStatus.READY.getStatus());
        currentFaceResponseDelay = sharedPreferences.getInt(FACE_RESPONSE_DELAY, DEFAULT_TIME_DELAY);
        currentFingerResponseDelay = sharedPreferences.getInt(FINGER_RESPONSE_DELAY, DEFAULT_TIME_DELAY);
        currentIrisResponseDelay = sharedPreferences.getInt(IRIS_RESPONSE_DELAY, DEFAULT_TIME_DELAY);
        currentDeviceUsage = sharedPreferences.getString(DEVICE_USAGE, DeviceConstants.DeviceUsage.Authentication.getDeviceUsage());
        currentDmsBaseUrl = sharedPreferences.getString(DMS_BASE_URL, DEFAULT_DMS_BASE_URL);

        FragmentManager fragmentManager = this.getSupportFragmentManager();
        deviceKeyFragment = (KeyCredentialFragment) fragmentManager.findFragmentById(R.id.deviceKeyFragment);

        if (deviceKeyFragment != null) {
            Bundle bundle = new Bundle();
            bundle.putString(ARG_KEY_LABEL, KEY_TYPE_DEVICE);
            bundle.putString(ARG_KEY_ALIAS, device_currentKeyAlias);
            bundle.putString(ARG_PASSWORD, device_currentKeyPassword);
            bundle.putString(ARG_LAST_UPLOAD_DATE, device_lastUploadDate);
            deviceKeyFragment.setArguments(bundle);
        }

        ftmKeyFragment = (KeyCredentialFragment) fragmentManager.findFragmentById(R.id.ftmKeyFragment);

        if (ftmKeyFragment != null) {
            Bundle bundle = new Bundle();
            bundle.putString(ARG_KEY_LABEL, KEY_TYPE_FTM);
            bundle.putString(ARG_KEY_ALIAS, ftm_currentKeyAlias);
            bundle.putString(ARG_PASSWORD, ftm_currentKeyPassword);
            bundle.putString(ARG_LAST_UPLOAD_DATE, ftm_lastUploadDate);
            ftmKeyFragment.setArguments(bundle);
        }

        idaFirCertificateFragment = (FileChooserFragment) fragmentManager.findFragmentById(R.id.idaFirCertificateFragment);

        if (idaFirCertificateFragment != null) {
            Bundle bundle = new Bundle();
            bundle.putString(ARG_LAST_UPLOAD_DATE, idaFirCertificateLastUploadDate);
            idaFirCertificateFragment.setArguments(bundle);
        }

        resetScreen();
    }

    public void onSave(View view) {
//        Uri device_fileUri = device_fileChooserFragment.getSelectedUri();
        Uri device_fileUri = deviceKeyFragment.getSelectedUri();
        Uri ftm_fileUri = ftmKeyFragment.getSelectedUri();
        Uri idaFirCertificateUri = idaFirCertificateFragment != null ? idaFirCertificateFragment.getSelectedUri() : null;

        if (device_fileUri != null && !saveFile(device_fileUri, ClientConstants.DEVICE_P12_FILE_NAME)) {
            Toast.makeText(this, "Failed to save reg p.12 file! Please try again.", Toast.LENGTH_LONG).show();
            return;
        } else {
            device_lastUploadDate = dateUtil.getDateTime(System.currentTimeMillis());
        }

        if (ftm_fileUri != null && !saveFile(ftm_fileUri, ClientConstants.FTM_P12_FILE_NAME)) {
            Toast.makeText(this, "Failed to save auth p.12 file! Please try again.", Toast.LENGTH_LONG).show();
            return;
        } else {
            ftm_lastUploadDate = dateUtil.getDateTime(System.currentTimeMillis());
        }

        if (idaFirCertificateUri != null) {
            if (!saveFile(idaFirCertificateUri, ClientConstants.IDA_FIR_CERTIFICATE_FILE_NAME)) {
                Toast.makeText(this, "Failed to save IDA_FIR certificate! Please try again.", Toast.LENGTH_LONG).show();
                return;
            }
            DeviceKeystore keystore = new DeviceKeystore(this);
            if (!keystore.loadCertificateFromFile(ClientConstants.IDA_FIR_CERTIFICATE_FILE_NAME)) {
                Toast.makeText(this, "Invalid IDA_FIR certificate. Please upload a valid .crt file.", Toast.LENGTH_LONG).show();
                return;
            }
            idaFirCertificateLastUploadDate = dateUtil.getDateTime(System.currentTimeMillis());
        } else{
            idaFirCertificateLastUploadDate = dateUtil.getDateTime(System.currentTimeMillis());
        }

        device_currentKeyAlias = deviceKeyFragment.getKeyAlias();
        device_currentKeyPassword = deviceKeyFragment.getPassword();
        ftm_currentKeyAlias = ftmKeyFragment.getKeyAlias();
        ftm_currentKeyPassword = ftmKeyFragment.getPassword();
        currentDmsBaseUrl = normalizeDmsBaseUrl(dmsBaseUrlInput.getText().toString());
        if (currentDmsBaseUrl.isEmpty()) {
            Toast.makeText(this, "DMS Base URL is required", Toast.LENGTH_LONG).show();
            return;
        }

        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(DEVICE_KEY_ALIAS, device_currentKeyAlias);
        editor.putString(DEVICE_KEY_STORE_PASSWORD, device_currentKeyPassword);
        editor.putString(FTM_KEY_ALIAS, ftm_currentKeyAlias);
        editor.putString(FTM_KEY_STORE_PASSWORD, ftm_currentKeyPassword);
        editor.putInt(FACE_SCORE, currentFaceScore);
        editor.putInt(FINGER_SCORE, currentFingerScore);
        editor.putInt(IRIS_SCORE, currentIrisScore);
        editor.putString(DEVICE_LAST_UPLOAD_DATE, LAST_UPDATE + device_lastUploadDate);
        editor.putString(FTM_LAST_UPLOAD_DATE, LAST_UPDATE + ftm_lastUploadDate);
        editor.putString(IDA_FIR_CERTIFICATE_LAST_UPLOAD_DATE, LAST_UPDATE + idaFirCertificateLastUploadDate);
        editor.putString(FACE_DEVICE_STATUS, currentFaceDeviceStatus);
        editor.putString(FINGER_DEVICE_STATUS, currentFingerDeviceStatus);
        editor.putString(IRIS_DEVICE_STATUS, currentIrisDeviceStatus);
        editor.putInt(FACE_RESPONSE_DELAY, currentFaceResponseDelay);
        editor.putInt(FINGER_RESPONSE_DELAY, currentFingerResponseDelay);
        editor.putInt(IRIS_RESPONSE_DELAY, currentIrisResponseDelay);
        editor.putString(DEVICE_USAGE, currentDeviceUsage);
        editor.putString(DMS_BASE_URL, currentDmsBaseUrl);

        editor.apply();

        //navigate back to previous activity
        Toast.makeText(this, "Settings saved", Toast.LENGTH_SHORT).show();
        loadAndValidateCertificates();
        finish();
    }

    public void onReset(View view) {
        resetScreen();
    }

    private void resetScreen() {
        deviceKeyFragment.setValues(device_currentKeyAlias, device_currentKeyPassword, device_lastUploadDate);
        ftmKeyFragment.setValues(ftm_currentKeyAlias, ftm_currentKeyPassword, ftm_lastUploadDate);
        if (idaFirCertificateFragment != null)
            idaFirCertificateFragment.resetSelection(idaFirCertificateLastUploadDate);
        dmsBaseUrlInput.setText(currentDmsBaseUrl);
    }

    private String normalizeDmsBaseUrl(String url) {
        String normalizedUrl = url == null ? "" : url.trim();
        while (normalizedUrl.endsWith("/")) {
            normalizedUrl = normalizedUrl.substring(0, normalizedUrl.length() - 1);
        }
        return normalizedUrl;
    }

    private boolean saveFile(Uri fileUri, String fileName) {
        try {
            FileUtils.SaveFileInAppStorage(getApplicationContext(), fileUri, fileName);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error: " + e);
            return false;
        }
    }

    private void setSpinner(Spinner spinner, String value) {
        int position = ((ArrayAdapter<String>) spinner.getAdapter()).getPosition(value);
        spinner.setSelection(position);
    }

    private void loadAndValidateCertificates() {
        DeviceKeystore keystore = new DeviceKeystore(this);

        String keyAlias = sharedPreferences.getString(ClientConstants.DEVICE_KEY_ALIAS, "");
        String keystorePwd = sharedPreferences.getString(ClientConstants.DEVICE_KEY_STORE_PASSWORD, "");
        String ftm_keyAlias = sharedPreferences.getString(ClientConstants.FTM_KEY_ALIAS, "");
        String ftm_keystorePwd = sharedPreferences.getString(ClientConstants.FTM_KEY_STORE_PASSWORD, "");

        if (keystore.checkCertificateCredentials(ClientConstants.DEVICE_P12_FILE_NAME, keyAlias, keystorePwd)) {
            Toast.makeText(this, "Device key credentials are valid.", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Device key validation failed.", Toast.LENGTH_SHORT).show();
        }

        if (keystore.checkCertificateCredentials(ClientConstants.FTM_P12_FILE_NAME, ftm_keyAlias, ftm_keystorePwd)) {
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
