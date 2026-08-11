package io.android.sbi.crypto;

import io.android.sbi.secureLib.DeviceKeystore;
import io.android.sbi.utility.Logger;

import java.io.ByteArrayInputStream;
import java.util.concurrent.Executors;

public class ProvisioningManager {

    private final CryptoProvider cryptoProvider;
    private final DmsClient dmsClient;
    private final DeviceKeystore deviceKeystore;

    public ProvisioningManager(CryptoProvider cryptoProvider, DmsClient dmsClient, DeviceKeystore deviceKeystore) {
        this.cryptoProvider = cryptoProvider;
        this.dmsClient = dmsClient;
        this.deviceKeystore = deviceKeystore;
    }

    public void initialize() throws Exception {
        if (!cryptoProvider.hasFtmKey()) {
            cryptoProvider.generateFtmKeyPair();
        }

        if (!cryptoProvider.hasDeviceKey()) {
            cryptoProvider.generateDeviceKeyPair();
        }

        if (!cryptoProvider.hasFtmCertificate()) {
            provisionFtmCertificate();
        }

        if (!cryptoProvider.hasDeviceCertificate()) {
            provisionDeviceCertificate();
        }

        if (!deviceKeystore.hasIdaCertificate()) {
            provisionIdaCertificate();
        }
    }

    private void provisionFtmCertificate() {
        try {
            String csr = cryptoProvider.generateFtmCSR();
            String certificate = dmsClient.generateSignedCertificate(csr, "Ftm");
            cryptoProvider.storeFtmCertificate(new ByteArrayInputStream(certificate.getBytes()));
        } catch (Exception e) {
            Logger.e("ProvisioningManager", "Error provisioning FTM certificate: " + e.getMessage());
        }
    }

    private void provisionDeviceCertificate() {
        try {
            String csr = cryptoProvider.generateDeviceCSR();
            String deviceCertificate = dmsClient.generateSignedCertificate(csr, "Device");
            cryptoProvider.storeDeviceCertificate(new ByteArrayInputStream(deviceCertificate.getBytes()));
        } catch (Exception e) {
            Logger.e("ProvisioningManager", "Error provisioning Device certificate: " + e.getMessage());
        }
    }

    private void provisionIdaCertificate() {
        try {
            String certificate = dmsClient.fetchIdaCertificate();
            deviceKeystore.storeCertificateBytes(certificate.getBytes());
        } catch (Exception e) {
            Logger.e("ProvisioningManager", "Error provisioning IDA certificate: " + e.getMessage());
        }
    }

    public void rotateCertificate() {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                if (!cryptoProvider.hasDeviceKey()) {
                    cryptoProvider.generateDeviceKeyPair();
                }
                provisionDeviceCertificate();
            } catch (Exception e) {
                Logger.e("ProvisioningManager", "Error rotating certificate: " + e.getMessage());
            }
        });
    }

    public void removeDeviceCertificate() {
        cryptoProvider.removeDeviceCertificate();
    }

    public void removeFtmCertificate() {
        cryptoProvider.removeFtmCertificate();
    }

    public void removeIdaCertificate() {
        deviceKeystore.removeIdaCertificate();
    }

    public void removeToken() {
        dmsClient.clearCachedToken();
    }
}
