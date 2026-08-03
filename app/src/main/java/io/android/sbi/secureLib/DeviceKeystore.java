package io.android.sbi.secureLib;

import static io.android.sbi.constants.ClientConstants.CERTIFICATE_TO_ENCRYPT_BIO;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import io.android.sbi.crypto.AndroidKeystoreCryptoProvider;
import io.android.sbi.crypto.CryptoProvider;
import org.apache.commons.codec.digest.DigestUtils;
import org.jose4j.jws.JsonWebSignature;
import org.jose4j.lang.JoseException;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import io.android.sbi.constants.ClientConstants;
import io.android.sbi.utility.DeviceConstants;
import io.android.sbi.utility.Logger;

/**
 * @author NPrime Technologies
 */

public class DeviceKeystore {

    private final Context context;
    private static final String signAlgorithm = "RS256";
    private final String device_keystorePwd;
    private final String device_keyAlias;
    private final String ftm_keystorePwd;
    private final String ftm_keyAlias;
    SharedPreferences sharedPreferences;
    CryptoProvider provider;

    private final static String AUTH_REQ_TEMPLATE = "{ \"id\": \"string\",\"metadata\": {},\"request\": { \"appId\": \"%s\", \"clientId\": \"%s\", \"secretKey\": \"%s\" }, \"requesttime\": \"%s\", \"version\": \"string\"}";

    public DeviceKeystore(Context context) {
        this.context = context;
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        device_keyAlias = sharedPreferences.getString(ClientConstants.DEVICE_KEY_ALIAS, "");
        device_keystorePwd = sharedPreferences.getString(ClientConstants.DEVICE_KEY_STORE_PASSWORD, "");
        ftm_keyAlias = sharedPreferences.getString(ClientConstants.FTM_KEY_ALIAS, "");
        ftm_keystorePwd = sharedPreferences.getString(ClientConstants.FTM_KEY_STORE_PASSWORD, "");
        provider = new AndroidKeystoreCryptoProvider(context);
    }

    public String getJwt(byte[] data, boolean signWithFTMKey) {
        try {
            return provider.signData(data, signWithFTMKey);
        } catch (Exception e) {
            Logger.e(DeviceConstants.LOG_TAG, "getJwt: " + e.getMessage());
        }

        String fileName;
        String keyAlias;
        String keystorePwd;
        if (signWithFTMKey) {
            fileName = ClientConstants.FTM_P12_FILE_NAME;
            keyAlias = ftm_keyAlias;
            keystorePwd = ftm_keystorePwd;
        } else {
            fileName = ClientConstants.DEVICE_P12_FILE_NAME;
            keyAlias = device_keyAlias;
            keystorePwd = device_keystorePwd;
        }
        PrivateKey privateKey;
        Certificate x509Certificate;

        File file = new File(context.getFilesDir(), fileName);

        try (InputStream inputStream = Files.newInputStream(file.toPath())) {
            KeyStore keystore = KeyStore.getInstance("PKCS12");
            keystore.load(inputStream, keystorePwd.toCharArray());
            privateKey = (PrivateKey) keystore.getKey(keyAlias, keystorePwd.toCharArray());
            x509Certificate = keystore.getCertificate(keyAlias);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return getJwt(data, privateKey, (X509Certificate) x509Certificate);
    }

    private String getJwt(byte[] data, PrivateKey privateKey, X509Certificate x509Certificate) {
        String jwsToken = null;
        JsonWebSignature jws = new JsonWebSignature();

        if (x509Certificate != null) {
            List<X509Certificate> certList = new ArrayList<>();
            certList.add(x509Certificate);
            X509Certificate[] certArray = certList.toArray(new X509Certificate[]{});
            jws.setCertificateChainHeaderValue(certArray);
        }

        jws.setPayloadBytes(data);
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

    public boolean checkCertificateCredentials(String fileName, String keyAlias, String keystorePwd) {
        if(ClientConstants.DEVICE_P12_FILE_NAME.equals(fileName)) {
            return provider.hasDeviceCertificate();
        } else{
            return provider.hasFtmCertificate();
        }
    }

    public Certificate getCertificateToEncryptCaptureBioValue() throws CertificateException {
        String certificateStr = sharedPreferences.getString(ClientConstants.CERTIFICATE_TO_ENCRYPT_BIO, "");

        if (certificateStr.equals("")) {
            throw new RuntimeException("Fail to fetch Certificate to Encrypt Captured BIO");
        }

        certificateStr = trimBeginEnd(certificateStr);
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        return cf.generateCertificate(
                new ByteArrayInputStream(Base64.getDecoder().decode(certificateStr)));
    }

    public boolean loadCertificateFromFile(String fileName) {
        File file = new File(context.getFilesDir(), fileName);

        try {
            return storeCertificateBytes(Files.readAllBytes(file.toPath()));
        } catch (Exception e) {
            Logger.e(DeviceConstants.LOG_TAG, "loadCertificateFromFile: " + e.getMessage());
            return false;
        }
    }

    public void loadCertificateFromIDA(Runnable onLoadCompleted) {
        new Thread(() -> {
            File uploadedCertificateFile = new File(context.getFilesDir(), ClientConstants.IDA_FIR_CERTIFICATE_FILE_NAME);
            if (uploadedCertificateFile.exists()) {
                loadCertificateFromFile(ClientConstants.IDA_FIR_CERTIFICATE_FILE_NAME);
            }

            onLoadCompleted.run();
        }).start();
    }

    private boolean storeCertificateBytes(byte[] certificateBytes) throws CertificateException, CertificateEncodingException {
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        Certificate certificate = cf.generateCertificate(new ByteArrayInputStream(certificateBytes));
        String certificateStr = Base64.getEncoder().encodeToString(certificate.getEncoded());
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(CERTIFICATE_TO_ENCRYPT_BIO, certificateStr);
        editor.apply();
        return true;
    }

    private static String trimBeginEnd(String pKey) {
        pKey = pKey.replace("\\n", "\n");
        pKey = pKey.replaceAll("-*BEGIN([^-]*)-*(\r?\n)?", "");
        pKey = pKey.replaceAll("-*END([^-]*)-*(\r?\n)?", "");
        pKey = pKey.replaceAll("\\s", "");
        return pKey;
    }

    public static byte[] getCertificateThumbprint(Certificate cert) throws CertificateEncodingException {
        return DigestUtils.sha256(cert.getEncoded());
    }

    public boolean isDeviceKeyAvailable() {
        return provider.hasDeviceCertificate();
    }
}
