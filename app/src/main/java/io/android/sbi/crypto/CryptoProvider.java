package io.android.sbi.crypto;

import java.io.InputStream;

public interface CryptoProvider {

    // Key generation
    void generateFtmKeyPair() throws Exception;

    void generateDeviceKeyPair() throws Exception;

    // CSR generation
    String generateFtmCSR() throws Exception;

    String generateDeviceCSR() throws Exception;

    // Certificate storage
    void storeFtmCertificate(InputStream certificateStream) throws Exception;

    void storeDeviceCertificate(InputStream certificateStream) throws Exception;

    // Signing
    String signData(byte[] payload, boolean signWithFTMKey) throws Exception;

    // Utility
    boolean hasFtmKey() throws Exception;

    boolean hasDeviceKey() throws Exception;

    boolean hasFtmCertificate();

    boolean hasDeviceCertificate();

    void removeDeviceCertificate();
}
