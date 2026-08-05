package io.android.sbi.crypto;

import android.app.Application;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.concurrent.Executors;

import io.android.sbi.secureLib.DeviceKeystore;
import io.android.sbi.utility.DeviceConstants;
import io.android.sbi.utility.Logger;

public class CryptoApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        Executors.newSingleThreadExecutor()
                .execute(() -> {
                    try {
                        CryptoProvider cryptoProvider = new AndroidKeystoreCryptoProvider(getApplicationContext());
                        DmsClient dmsClient = new DmsClient(getApplicationContext());
                        DeviceKeystore deviceKeystore = new DeviceKeystore(getApplicationContext());
                        ProvisioningManager provisioningManager = new ProvisioningManager(cryptoProvider, dmsClient, deviceKeystore);
                        provisioningManager.initialize();
                    } catch (Exception e) {
                        Logger.e(DeviceConstants.LOG_TAG, "Error while key provisioning" +  e.getMessage());
                    }
                });

        fetchFcmToken();
    }

    private void fetchFcmToken() {
        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        Logger.w(DeviceConstants.LOG_TAG, "Fetching FCM registration token failed" + task.getException());
                        return;
                    }

                    String token = task.getResult();

                    DmsClient dmsClient = new DmsClient(getApplicationContext());
                    dmsClient.sendTokenToDmsServer(token);
                });
    }
}
