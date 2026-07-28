package io.android.sbi.crypto;

import android.app.Application;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.concurrent.Executors;

import io.android.sbi.constants.ClientConstants;
import io.android.sbi.utility.DeviceConstants;
import io.android.sbi.utility.Logger;

public class CryptoApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        Executors.newSingleThreadExecutor()
                .execute(() -> {
                    try {
                        CryptoProvider cryptoProvider = new AndroidKeystoreCryptoProvider(this);
                        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
                        String dmsBaseUrl = sharedPreferences.getString(ClientConstants.DMS_BASE_URL, ClientConstants.DEFAULT_DMS_BASE_URL);
                        DmsClient dmsClient = new DmsClient(dmsBaseUrl);
                        ProvisioningManager provisioningManager = new ProvisioningManager(cryptoProvider, dmsClient);
                        provisioningManager.initialize();
                    } catch (Exception e) {
                        e.printStackTrace();
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
                    Logger.d(DeviceConstants.LOG_TAG, "FCM Token retrieved: " + token);

                    SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
                    String dmsBaseUrl = sharedPreferences.getString(ClientConstants.DMS_BASE_URL, ClientConstants.DEFAULT_DMS_BASE_URL);
                    DmsClient dmsClient = new DmsClient(dmsBaseUrl, sharedPreferences);
                    dmsClient.sendTokenToDmsServer(token);
                });
    }
}
