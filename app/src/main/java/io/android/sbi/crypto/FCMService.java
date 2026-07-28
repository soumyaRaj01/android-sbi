package io.android.sbi.crypto;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import io.android.sbi.constants.ClientConstants;
import io.android.sbi.utility.DeviceConstants;
import io.android.sbi.utility.Logger;

public class FCMService extends FirebaseMessagingService {

    private DmsClient dmsClient;
    private ProvisioningManager provisioningManager;

    @Override
    public void onCreate() {
        super.onCreate();

        CryptoProvider cryptoProvider = new AndroidKeystoreCryptoProvider(getApplicationContext());
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        String dmsBaseUrl = sharedPreferences.getString(ClientConstants.DMS_BASE_URL, ClientConstants.DEFAULT_DMS_BASE_URL);
        this.dmsClient = new DmsClient(dmsBaseUrl, sharedPreferences);
        this.provisioningManager = new ProvisioningManager(cryptoProvider, dmsClient);
    }

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        if (remoteMessage.getData().size() > 0) {
            String action = remoteMessage.getData().get("action");
            Logger.d(DeviceConstants.LOG_TAG, "Message received with action: " + action);

            if ("RENEW_CERTIFICATE".equals(action) || "ACTIVATE".equals(action)) {
                try {
                    provisioningManager.rotateCertificate();
                    Logger.d(DeviceConstants.LOG_TAG, "Key rotation completed successfully.");
                } catch (Exception e) {
                    Logger.e(DeviceConstants.LOG_TAG, "Error during key rotation in background push" + e);
                }
            } else if("DEACTIVATE".equals(action)) {
                try {
                    provisioningManager.removeCertificate();
                } catch (Exception e) {
                    Logger.e(DeviceConstants.LOG_TAG, "Error during certificate removal in background push" + e);
                }
            }
        }
    }

    /**
     * Called automatically when the device's unique FCM token is updated.
     */
    @Override
    public void onNewToken(String token) {
        super.onNewToken(token);
        Logger.d(DeviceConstants.LOG_TAG, "New FCM Registration Token: " + token);
        dmsClient.sendTokenToDmsServer(token);
    }
}
