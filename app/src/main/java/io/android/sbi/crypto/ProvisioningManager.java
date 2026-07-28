package io.android.sbi.crypto;

import io.android.sbi.utility.Logger;

import java.io.ByteArrayInputStream;

public class ProvisioningManager {

    private final CryptoProvider cryptoProvider;
    private final DmsClient dmsClient;

    public ProvisioningManager(CryptoProvider cryptoProvider, DmsClient  dmsClient) {
        this.cryptoProvider = cryptoProvider;
        this.dmsClient = dmsClient;
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

    public void rotateCertificate() throws Exception {
        if (!cryptoProvider.hasDeviceKey()) {
            cryptoProvider.generateDeviceKeyPair();
        }

        provisionDeviceCertificate();
    }

    public void removeCertificate() throws Exception {
        cryptoProvider.removeDeviceCertificate();
    }
}
