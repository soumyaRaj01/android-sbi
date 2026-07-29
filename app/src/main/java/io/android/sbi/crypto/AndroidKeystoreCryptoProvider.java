package io.android.sbi.crypto;

import android.content.Context;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import io.android.sbi.constants.CryptoConstants;
import io.android.sbi.utility.DeviceConstants;
import io.android.sbi.utility.Logger;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.bouncycastle.pkcs.jcajce.JcaPKCS10CertificationRequestBuilder;
import org.jose4j.jws.JsonWebSignature;
import org.jose4j.jwx.HeaderParameterNames;
import org.jose4j.lang.JoseException;

import java.io.*;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

public class AndroidKeystoreCryptoProvider
        implements CryptoProvider {

    private static final String signAlgorithm = "RS256";
    private final Context context;

    public AndroidKeystoreCryptoProvider(Context context) {
        this.context = context;
    }

    @Override
    public void generateFtmKeyPair() throws Exception {
        generateKeyPair(CryptoConstants.FTM_KEY_ALIAS);
    }

    @Override
    public void generateDeviceKeyPair() throws Exception {
        generateKeyPair(CryptoConstants.DEVICE_KEY_ALIAS);
    }

    @Override
    public boolean hasFtmKey() throws Exception {
        return aliasExists(CryptoConstants.FTM_KEY_ALIAS);
    }

    @Override
    public boolean hasDeviceKey() throws Exception {
        return aliasExists(CryptoConstants.DEVICE_KEY_ALIAS);
    }

    @Override
    public boolean hasFtmCertificate() {
        return certificateExists(CryptoConstants.FTM_CERT_FILE);
    }

    @Override
    public boolean hasDeviceCertificate() {
        return certificateExists(CryptoConstants.DEVICE_CERT_FILE);
    }

    @Override
    public X509Certificate getDeviceCertificate() {
        return safeLoadCertificate(CryptoConstants.DEVICE_CERT_FILE);
    }

    @Override
    public X509Certificate getFtmCertificate() {
        return safeLoadCertificate(CryptoConstants.FTM_CERT_FILE);
    }

    @Override
    public String generateFtmCSR() throws Exception {
        return generateCSR(
                CryptoConstants.FTM_KEY_ALIAS,
                "CN=FTM_Device");
    }

    @Override
    public String generateDeviceCSR() throws Exception {
        return generateCSR(
                CryptoConstants.DEVICE_KEY_ALIAS,
                "CN=Biometric_Device");
    }

    @Override
    public void storeFtmCertificate(
            InputStream certificateStream)
            throws Exception {

        saveCertificate(
                certificateStream,
                CryptoConstants.FTM_CERT_FILE);
    }

    @Override
    public void storeDeviceCertificate(
            InputStream certificateStream)
            throws Exception {

        saveCertificate(
                certificateStream,
                CryptoConstants.DEVICE_CERT_FILE);
    }

    @Override
    public void removeDeviceCertificate() {
        File file = new File(context.getFilesDir(), CryptoConstants.DEVICE_CERT_FILE);

        if (file.exists()) {
            boolean deleted = file.delete();
            if (!deleted) {
                Logger.e(DeviceConstants.LOG_TAG, "Failed to delete device certificate");
            }
        }
    }

    @Override
    public String signData(
            byte[] payload, boolean signWithFTMKey)
            throws Exception {

        return signJwt(
                payload,
                signWithFTMKey ? CryptoConstants.FTM_KEY_ALIAS :  CryptoConstants.DEVICE_KEY_ALIAS,
                signWithFTMKey ? CryptoConstants.FTM_CERT_FILE : CryptoConstants.DEVICE_CERT_FILE);
    }

    private void generateKeyPair(String alias)
            throws Exception {

        if (aliasExists(alias)) {
            return;
        }

        KeyPairGenerator generator =
                KeyPairGenerator.getInstance(
                        KeyProperties.KEY_ALGORITHM_RSA,
                        "AndroidKeyStore");

        KeyGenParameterSpec spec =
                new KeyGenParameterSpec.Builder(
                        alias,
                        KeyProperties.PURPOSE_SIGN)
                        .setDigests(
                                KeyProperties.DIGEST_SHA256)
                        .setSignaturePaddings(
                                KeyProperties.SIGNATURE_PADDING_RSA_PKCS1)
                        .setKeySize(4096)
                        .build();

        generator.initialize(spec);
        generator.generateKeyPair();
    }

    private boolean aliasExists(String alias)
            throws Exception {

        KeyStore ks =
                KeyStore.getInstance("AndroidKeyStore");

        ks.load(null);

        return ks.containsAlias(alias);
    }

    private String generateCSR(
            String alias,
            String subjectDn)
            throws Exception {

        KeyStore ks =
                KeyStore.getInstance("AndroidKeyStore");

        ks.load(null);

        PrivateKey privateKey =
                (PrivateKey) ks.getKey(alias, null);

        PublicKey publicKey =
                ks.getCertificate(alias)
                        .getPublicKey();

        X500Name subject =
                new X500Name(subjectDn);

        ContentSigner signer =
                new JcaContentSignerBuilder(
                        "SHA256withRSA")
                        .build(privateKey);

        PKCS10CertificationRequest csr =
                new JcaPKCS10CertificationRequestBuilder(
                        subject,
                        publicKey)
                        .build(signer);

        return convertToPem(csr.getEncoded());
    }

    private void saveCertificate(
            InputStream in,
            String fileName)
            throws Exception {

        File file =
                new File(
                        context.getFilesDir(),
                        fileName);

        try (OutputStream out =
                     new FileOutputStream(file)) {

            byte[] buffer =
                    new byte[8192];

            int len;

            while ((len = in.read(buffer)) > 0) {
                out.write(buffer, 0, len);
            }
        }
    }

    private X509Certificate loadCertificate(
            String fileName)
            throws Exception {

        File file =
                new File(
                        context.getFilesDir(),
                        fileName);

        CertificateFactory cf =
                CertificateFactory.getInstance(
                        "X.509");

        try (InputStream in =
                     new FileInputStream(file)) {

            return (X509Certificate)
                    cf.generateCertificate(in);
        }
    }

    private boolean certificateExists(String fileName) {
        return safeLoadCertificate(fileName) != null;
    }

    private X509Certificate safeLoadCertificate(String fileName) {
        File file = new File(context.getFilesDir(), fileName);
        if (!file.exists()) {
            return null;
        }

        try {
            return loadCertificate(fileName);
        } catch (Exception e) {
            Logger.e(DeviceConstants.LOG_TAG, "Invalid certificate " + fileName + ": " + e.getMessage());
            return null;
        }
    }

    private String signJwt(
            byte[] payload,
            String alias,
            String certificateFile)
            throws Exception {

        KeyStore ks =
                KeyStore.getInstance(
                        "AndroidKeyStore");

        ks.load(null);

        PrivateKey privateKey =
                (PrivateKey) ks.getKey(alias, null);

        X509Certificate x509Certificate =
                loadCertificate(certificateFile);

        String jwsToken = null;
        JsonWebSignature jws = new JsonWebSignature();

        if (x509Certificate != null) {
            List<X509Certificate> certList = new ArrayList<>();
            certList.add(x509Certificate);
            X509Certificate[] certArray = certList.toArray(new X509Certificate[]{});
            jws.setCertificateChainHeaderValue(certArray);
        }

        jws.setPayloadBytes(payload);
        jws.setAlgorithmHeaderValue(signAlgorithm);
        jws.setHeader(org.jose4j.jwx.HeaderParameterNames.TYPE, "JWT");
        jws.setKey(privateKey);
        jws.setDoKeyValidation(false);
        try {
            jwsToken = jws.getCompactSerialization();
        } catch (JoseException e) {
            Logger.e(DeviceConstants.LOG_TAG, "checkCertificateCredentials: " + e.getMessage());
        }
        return jwsToken;
    }

    private String convertToPem(byte[] csrBytes) {

        String base64 = Base64.encodeToString(
                csrBytes,
                Base64.NO_WRAP);

        StringBuilder sb = new StringBuilder();

        sb.append("-----BEGIN CERTIFICATE REQUEST-----\n");

        for (int i = 0; i < base64.length(); i += 64) {
            sb.append(
                    base64,
                    i,
                    Math.min(i + 64, base64.length()));
            sb.append("\n");
        }

        sb.append("-----END CERTIFICATE REQUEST-----");

        return sb.toString();
    }
}
