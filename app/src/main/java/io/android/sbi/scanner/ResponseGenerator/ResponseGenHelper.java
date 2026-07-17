package io.android.sbi.scanner.ResponseGenerator;

import static io.android.sbi.utility.DeviceConstants.CERTIFICATION_L1;
import static io.android.sbi.utility.DeviceConstants.ServiceStatus.NOT_REGISTERED;

import android.os.Build;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.bouncycastle.util.Strings;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import io.android.sbi.dto.CaptureDetail;
import io.android.sbi.dto.CaptureRequestDeviceDetailDto;
import io.android.sbi.dto.CaptureRequestDto;
import io.android.sbi.dto.CaptureResponse;
import io.android.sbi.dto.DeviceInfo;
import io.android.sbi.dto.DeviceInfoResponse;
import io.android.sbi.dto.DiscoverDto;
import io.android.sbi.dto.Error;
import io.android.sbi.dto.NewBioDto;
import io.android.sbi.faceCaptureApi.CaptureResult;
import io.android.sbi.secureLib.DeviceKeystore;
import io.android.sbi.utility.CommonDeviceAPI;
import io.android.sbi.utility.CryptoUtility;
import io.android.sbi.utility.DeviceConstants;
import io.android.sbi.utility.DeviceErrorCodes;
import io.android.sbi.utility.DeviceUtil;
import io.android.sbi.utility.Logger;

/**
 * @author NPrime Technologies
 */

public class ResponseGenHelper {
    ObjectMapper oB;
    DeviceUtil deviceUtil;

    public ResponseGenHelper(DeviceUtil _deviceUtil) {
        oB = new ObjectMapper();
        deviceUtil = _deviceUtil;
    }

    public List<DeviceInfoResponse> getDeviceDriverInfo(DeviceConstants.ServiceStatus currentStatus,
                                                        String szTimeStamp, String requestType,
                                                        DeviceConstants.BioType bioType, DeviceKeystore keystore,
                                                        String deviceId) {
        List<String> listOfModalities = Collections.singletonList("FAC");

        List<DeviceInfoResponse> infoList = new ArrayList<>();
        try {
            if(!keystore.isDeviceKeyAvailable()){
                currentStatus = DeviceConstants.ServiceStatus.NOT_REGISTERED;
            }

            Error error;
            switch (currentStatus) {
                case NOT_READY:
                    error = new Error("110", "Device not ready");
                    break;
                case BUSY:
                    error = new Error("111", "Device busy");
                    break;
                case NOT_REGISTERED:
                    error = new Error("100", "Device not registered");
                    break;
                default:
                    error = new Error("0", "Success");
                    break;
            }
            //Purpose for Not Registered device should be empty
            String purpose = currentStatus != NOT_REGISTERED ? deviceUtil.DEVICE_USAGE.getDeviceUsage() : "";

            DeviceConstants.ServiceStatus finalCurrentStatus = currentStatus;
            listOfModalities.forEach(value -> {
                byte[] deviceInfoData = getDeviceInfo(keystore, finalCurrentStatus, szTimeStamp, requestType, bioType, purpose, deviceId);

                String encodedDeviceInfo;
                if(finalCurrentStatus == NOT_REGISTERED){
                    encodedDeviceInfo = CryptoUtility.encodeToURLSafeBase64(deviceInfoData);
                } else{
                    encodedDeviceInfo = keystore.getJwt(deviceInfoData, false);
                }

                infoList.add(new DeviceInfoResponse(encodedDeviceInfo, error));
            });
        } catch (Exception ex) {
            ex.printStackTrace();
            infoList.add(new DeviceInfoResponse(null, new Error("UNKNOWN",
                    ex.getMessage())));
        }
        return infoList;
    }

    public List<DiscoverDto> getDeviceDiscovery(
            DeviceConstants.ServiceStatus currentStatus,
            String szTimeStamp, String requestType, DeviceConstants.BioType bioType, String deviceId) {
        List<DiscoverDto> list = new ArrayList<>();
        try {
            DiscoverDto discoverDto = new DiscoverDto();
            CommonDeviceAPI devCommonDeviceAPI = new CommonDeviceAPI();
            String serialNumber = devCommonDeviceAPI.getSerialNumber();
            // deviceId is the internal device id (from the SDK when available); deviceCode stays serialNo.
            discoverDto.deviceId = (deviceId != null && !deviceId.isEmpty()) ? deviceId : serialNumber;
            discoverDto.deviceStatus = currentStatus.getStatus();
            discoverDto.certification = deviceUtil.CERTIFICATION_LEVEL;
            discoverDto.serviceVersion = DeviceConstants.MDS_VERSION;

            switch (bioType) {
                case Face:
                    discoverDto.deviceSubId = new String[]{"0"};
                    break;
                case Finger:
                    discoverDto.deviceSubId = new String[]{"1", "2", "3"};
                    break;
                case Iris:
                    discoverDto.deviceSubId = new String[]{"3"};
                    break;
            }

            switch (currentStatus) {
                case NOT_READY:
                    discoverDto.error = new Error("110", "Device not ready");
                    break;
                case BUSY:
                    discoverDto.error = new Error("111", "Device busy");
                    break;
                case NOT_REGISTERED:
                    discoverDto.deviceId = "";
                    discoverDto.deviceCode = "";
                    discoverDto.purpose = "";
                    discoverDto.error = new Error("100", "Device not registered");
                    break;
                default:
                    discoverDto.error = new Error("0", "Success");
                    break;
            }

            discoverDto.callbackId = requestType;
            String payLoad = getDigitalID(serialNumber, szTimeStamp, bioType);

            discoverDto.digitalId = CryptoUtility.getBase64encodeString(payLoad);
            discoverDto.deviceCode = serialNumber;
            discoverDto.specVersion = new String[]{DeviceConstants.REG_SERVER_VERSION};
            discoverDto.purpose = deviceUtil.DEVICE_USAGE.getDeviceUsage();
            discoverDto.error = new Error("0", "Success");

            list.add(discoverDto);
        } catch (Exception ex) {
            Logger.e(DeviceConstants.LOG_TAG, "Face SBI :: " + "Failed to process exception");
        }
        return list;
    }

    private byte[] getDeviceInfo(
            DeviceKeystore deviceKeystore,
            DeviceConstants.ServiceStatus currentStatus,
            String szTimeStamp, String requestType, DeviceConstants.BioType bioType,
            String deviceUsage, String deviceId) {
        byte[] deviceInfoData = null;
        try {
            byte[] fwVersion;
            fwVersion = DeviceConstants.FIRMWARE_VER.getBytes();
            CommonDeviceAPI devCommonDeviceAPI = new CommonDeviceAPI();
            String serialNumber = devCommonDeviceAPI.getSerialNumber();

            DeviceInfo info = new DeviceInfo();
            info.callbackId = requestType.replace(".Info", "");
            info.certification = deviceUtil.CERTIFICATION_LEVEL;
            info.deviceCode = serialNumber;
            // deviceId is the internal device id (from the SDK when available); deviceCode stays serialNo.
            info.deviceId = (deviceId != null && !deviceId.isEmpty()) ? deviceId : serialNumber;
            info.deviceStatus = currentStatus.getStatus();
            info.deviceSubId = new String[]{"0"};
            String payLoad = getDigitalID(serialNumber, szTimeStamp, bioType);

            if(currentStatus == NOT_REGISTERED){
                info.digitalId = CryptoUtility.getBase64encodeString(payLoad);
            } else{
                info.digitalId = deviceKeystore.getJwt(payLoad.getBytes(), deviceUtil.CERTIFICATION_LEVEL.equals(CERTIFICATION_L1));
            }

            info.specVersion = new String[]{DeviceConstants.REG_SERVER_VERSION};
            info.serviceVersion = DeviceConstants.MDS_VERSION;
            info.purpose = deviceUsage;
            info.firmware = new String(fwVersion).replaceAll("\0", "").trim();
            info.env = DeviceConstants.ENVIRONMENT;

            deviceInfoData = oB.writeValueAsString(info).getBytes();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception ex) {
            deviceInfoData = "".getBytes();
            ex.printStackTrace();
        }

        return deviceInfoData;
    }

    public String getDigitalID(String serialNumber, String szTS, DeviceConstants.BioType bioType) {
        String digiID;
        JSONObject jsonobject = new JSONObject();

        try {
            jsonobject.put("serialNo", serialNumber);

            String make = Build.BRAND;
            String model = Build.MODEL;
//            String manufacturer = Build.MANUFACTURER;

            switch (bioType) {
                case Face:
                    jsonobject.put("make", make);
                    jsonobject.put("model", model);
                    jsonobject.put("type", DeviceConstants.BioType.Face.getBioType());
                    jsonobject.put("deviceSubType", deviceUtil.FACE_DEVICE_SUBTYPE);
                    break;
                case Finger:
                    jsonobject.put("make", make);
                    jsonobject.put("model", model);
                    jsonobject.put("type", DeviceConstants.BioType.Finger.getBioType());
                    jsonobject.put("deviceSubType", deviceUtil.FINGER_DEVICE_SUBTYPE);
                    break;
                case Iris:
                    jsonobject.put("make", DeviceConstants.DEVICE_MAKE_IRIS);
                    jsonobject.put("model", DeviceConstants.DEVICE_MODEL_IRIS);
                    jsonobject.put("type", DeviceConstants.BioType.Iris.getBioType());
                    jsonobject.put("deviceSubType", deviceUtil.IRIS_DEVICE_SUBTYPE);
                    break;
            }
            jsonobject.put("deviceProvider", DeviceConstants.PROVIDER_NAME);
            jsonobject.put("deviceProviderId", DeviceConstants.PROVIDER_ID);
            jsonobject.put("dateTime", szTS);
        } catch (Exception ex) {
            Logger.e(DeviceConstants.LOG_TAG, "Face SBI :: " + "Error occurred while retreiving Digital ID ");
        }

        digiID = jsonobject.toString();
        return digiID;
    }

    public CaptureResponse getCaptureBiometrics(CaptureResult captureResult,
                                                CaptureRequestDto captureRequestDto, DeviceKeystore keystore) {
        CaptureResponse captureResponse = new CaptureResponse();
        try {
            CommonDeviceAPI mdCommonDeviceAPI = new CommonDeviceAPI();

            List<CaptureDetail> listOfBiometric = new ArrayList<>();

            for (CaptureRequestDeviceDetailDto bio : captureRequestDto.bio) {
                int captureStatus = captureResult.getStatus();
                int qualityScore = captureResult.getQualityScore();
                String previousHash = bio.previousHash;

                DeviceConstants.BioType bioType = DeviceConstants.BioType.getBioType(bio.type);

                if (bioType == null || bioType == DeviceConstants.BioType.BioDevice) {
                    continue;
                }

                for (String bioSubType : captureResult.getBiometricRecords().keySet()) {
                    byte[] bioValue = captureResult.getBiometricRecords().get(bioSubType);
                    CaptureDetail biometricData = getCaptureDetail(mdCommonDeviceAPI.getSerialNumber(), bio.type, bioSubType, captureRequestDto,
                            bioValue, bio.requestedScore, keystore, bioType, previousHash
                            , captureStatus, qualityScore);
                    listOfBiometric.add(biometricData);
                    previousHash = biometricData.hash;
                }
            }
            captureResponse.biometrics = listOfBiometric;
        } catch (Exception exception) {
            CaptureDetail captureDetail = new CaptureDetail();
            captureDetail.specVersion = DeviceConstants.REG_SERVER_VERSION;
            captureDetail.error = new Error("UNKNOWN", exception.getMessage());
        }
        return captureResponse;
    }

    private CaptureDetail getCaptureDetail(String deviceSerialNumber, String bioType, String bioSubType,
                                           CaptureRequestDto captureRequestDto, byte[] captureBioValue,
                                           int requestedScore, DeviceKeystore keystore,
                                           DeviceConstants.BioType bioTypeAtt, String previousHash,
                                           int captureStatus, int capturedQualityScore) throws CertificateException {

        String domainUri;
        String bioValueString;
        String sessionKey;
        String timeStamp;
        String thumbprint;
        String digitalID;
        if (deviceUtil.DEVICE_USAGE == DeviceConstants.DeviceUsage.Authentication) {
            Certificate certificate = keystore.getCertificateToEncryptCaptureBioValue();
            PublicKey publicKey = certificate.getPublicKey();
            Map<String, String> cryptoResult = CryptoUtility.encrypt(publicKey, captureBioValue, captureRequestDto.transactionId);
            byte[] crt = DeviceKeystore.getCertificateThumbprint(certificate);

            bioValueString = cryptoResult.getOrDefault("ENC_DATA", null);
            sessionKey = cryptoResult.getOrDefault("ENC_SESSION_KEY", null);
            timeStamp = cryptoResult.getOrDefault("TIMESTAMP", CryptoUtility.getTimestamp());
            domainUri = captureRequestDto.domainUri == null ? "" : captureRequestDto.domainUri;
            thumbprint = CryptoUtility.toHex(crt).replace("-", "").toUpperCase();

            String payLoad = getDigitalID(deviceSerialNumber, timeStamp, bioTypeAtt);
            digitalID = keystore.getJwt(payLoad.getBytes(StandardCharsets.UTF_8), true);
        } else {
            bioValueString = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(captureBioValue);
            sessionKey = null;
            timeStamp = CryptoUtility.getTimestamp();
            domainUri = null;
            thumbprint = null;

            String payLoad = getDigitalID(deviceSerialNumber, timeStamp, bioTypeAtt);
            digitalID = keystore.getJwt(payLoad.getBytes(StandardCharsets.UTF_8), false);
        }

        //bioSubType should be null for face
        bioSubType = "face".equalsIgnoreCase(bioType) ? null : bioSubType;

        NewBioDto bioDto = getBioResponse(deviceSerialNumber, bioType, bioSubType, captureRequestDto, capturedQualityScore,
                requestedScore, domainUri, bioValueString, timeStamp, digitalID);
        return getMinimalResponse(captureRequestDto.specVersion, bioDto,
                previousHash, captureStatus, keystore, sessionKey, thumbprint, captureBioValue);
    }

    private NewBioDto getBioResponse(String deviceSerialNumber, String bioType, String bioSubType,
                                     CaptureRequestDto captureRequestDto, int capturedQualityScore, int requestedScore,
                                     String domainUri, String bioValueStr, String timeStamp, String digitalID) {
        NewBioDto bioResponse = new NewBioDto();
        bioResponse.setBioSubType(bioSubType);
        bioResponse.setBioType(bioType);
        bioResponse.setDeviceCode(deviceSerialNumber);
        //Device service version should be read from file
        bioResponse.setDeviceServiceVersion(DeviceConstants.MDS_VERSION);
        bioResponse.setEnv(captureRequestDto.env);
        bioResponse.setPurpose(deviceUtil.DEVICE_USAGE.getDeviceUsage());
        bioResponse.setRequestedScore(String.valueOf(requestedScore));
        bioResponse.setQualityScore(String.valueOf(capturedQualityScore));
        bioResponse.setTransactionId(captureRequestDto.transactionId);
        bioResponse.setDigitalId(digitalID);
        bioResponse.setTimestamp(timeStamp);
        bioResponse.setDomainUri(domainUri);
        bioResponse.setBioValue(bioValueStr);

        return bioResponse;
    }

    private CaptureDetail getMinimalResponse(String specVersion, NewBioDto data, String previousHash,
                                             int captureStatus, DeviceKeystore keystore,
                                             String sessionKey, String thumbprint, byte[] nonEncryptedCapturedBioValue) {
        CaptureDetail biometricData = new CaptureDetail();
        try {
            if (CaptureResult.CAPTURE_SUCCESS == captureStatus) {
                biometricData.error = new Error("0", "Success");
            } else if (CaptureResult.CAPTURE_TIMEOUT == captureStatus) {
                biometricData.error = new Error(DeviceErrorCodes.MDS_CAPTURE_TIMEOUT, "Capture Timeout");
            } else {
                biometricData.error = new Error(DeviceErrorCodes.MDS_CAPTURE_FAILED, "Capture Failed");
            }

            biometricData.specVersion = specVersion;

            biometricData.data = keystore.getJwt(oB.writeValueAsBytes(data), false);
            byte[] previousBioDataHash;
            byte[] currentBioDataHash;

            //instead of BioData, bioValue (before encrytion in case of Capture response) is used for computing the hash.
            currentBioDataHash = CryptoUtility.generateHash(nonEncryptedCapturedBioValue);

            if (previousHash == null || previousHash.trim().length() == 0) {
                byte[] previousDataByteArr = Strings.toUTF8ByteArray("");
                previousBioDataHash = CryptoUtility.generateHash(previousDataByteArr);
            } else {
                previousBioDataHash = CryptoUtility.decodeHex(previousHash);
            }

            byte[] finalBioDataHash = new byte[previousBioDataHash.length + currentBioDataHash.length];
            System.arraycopy(previousBioDataHash, 0, finalBioDataHash, 0, previousBioDataHash.length);
            System.arraycopy(currentBioDataHash, 0, finalBioDataHash, previousBioDataHash.length, currentBioDataHash.length);

            biometricData.hash = CryptoUtility.toHex(CryptoUtility.generateHash(finalBioDataHash));
            biometricData.sessionKey = sessionKey;
            biometricData.thumbprint = thumbprint;
        } catch (Exception ex) {
            ex.printStackTrace();
            biometricData.error = new Error("UNKNOWN", ex.getMessage());
        }
        return biometricData;
    }
}