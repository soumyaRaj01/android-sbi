package io.android.sbi.crypto;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import io.android.sbi.secureLib.DeviceKeystore;
import io.android.sbi.utility.DeviceConstants;
import io.android.sbi.utility.Logger;

public class FCMService extends FirebaseMessagingService {

    private DmsClient dmsClient;
    private ProvisioningManager provisioningManager;

    @Override
    public void onCreate() {
        super.onCreate();

        CryptoProvider cryptoProvider = new AndroidKeystoreCryptoProvider(getApplicationContext());
        this.dmsClient = new DmsClient(getApplicationContext());
        DeviceKeystore deviceKeystore = new DeviceKeystore(getApplicationContext());
        this.provisioningManager = new ProvisioningManager(cryptoProvider, dmsClient, deviceKeystore);
    }

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        if (remoteMessage.getData().size() > 0) {
            String action = remoteMessage.getData().get("action");
            Logger.d(DeviceConstants.LOG_TAG, "Message received with action: " + action);

            if ("RENEW_CERTIFICATE".equals(action) || "ACTIVATE".equals(action)) {
                provisioningManager.rotateCertificate();
            } else if("DEACTIVATE".equals(action) || "REMOVE_DEVICE_CERT".equals(action)) {
                try {
                    provisioningManager.removeDeviceCertificate();
                } catch (Exception e) {
                    Logger.e(DeviceConstants.LOG_TAG, "Error during device certificate removal in background push" + e);
                }
            } else if ("REMOVE_FTM_CERT".equals(action)) {
                try {
                    provisioningManager.removeFtmCertificate();
                } catch (Exception e) {
                    Logger.e(DeviceConstants.LOG_TAG, "Error during ftm certificate removal in background push" + e);
                }
            } else if ("REMOVE_IDA_CERT".equals(action)) {
                try {
                    provisioningManager.removeIdaCertificate();
                } catch (Exception e) {
                    Logger.e(DeviceConstants.LOG_TAG, "Error during IDA certificate removal in background push" + e);
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
        Logger.d(DeviceConstants.LOG_TAG, "New FCM Token generated");
        dmsClient.sendTokenToDmsServer(token);
    }
}
