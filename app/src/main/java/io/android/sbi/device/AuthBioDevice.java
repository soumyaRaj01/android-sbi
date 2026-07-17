package io.android.sbi.device;

import static io.android.sbi.utility.DeviceConstants.DEVICE_IRIS_DOUBLE_SUB_TYPE_ID_BOTH;
import static io.android.sbi.utility.DeviceConstants.DEVICE_IRIS_DOUBLE_SUB_TYPE_ID_LEFT;
import static io.android.sbi.utility.DeviceConstants.DEVICE_IRIS_DOUBLE_SUB_TYPE_ID_RIGHT;
import static io.android.sbi.utility.DeviceConstants.DEVICE_IRIS_SINGLE_SUB_TYPE_ID;

import android.content.Context;
import android.net.Uri;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.android.sbi.utility.DeviceConstants;

public class AuthBioDevice extends BioDevice {
    public AuthBioDevice(Context appContext) {
        super(appContext);
    }

    @Override
    public Map<String, Uri> captureIrisModality(int deviceSubId, String[] bioSubType, String[] exception) {
        List<String> segmentsToCapture = null;
        switch (deviceSubId) {
            case DEVICE_IRIS_DOUBLE_SUB_TYPE_ID_LEFT:
            case DEVICE_IRIS_DOUBLE_SUB_TYPE_ID_RIGHT:
            case DEVICE_IRIS_DOUBLE_SUB_TYPE_ID_BOTH:
                break; // double not implemented for auth
            case DEVICE_IRIS_SINGLE_SUB_TYPE_ID:
                segmentsToCapture = getSegmentsToCapture(Arrays.asList(
                                DeviceConstants.BIO_NAME_LEFT_IRIS,
                                DeviceConstants.BIO_NAME_RIGHT_IRIS),
                        bioSubType == null ? null : Arrays.asList(bioSubType),
                        exception == null ? null : Arrays.asList(exception));
                break;
        }
        Map<String, Uri> uris = new HashMap<>();
        if (segmentsToCapture == null || segmentsToCapture.isEmpty()) {
            return uris;
        }

        segmentsToCapture.forEach(segment -> uris.put(segment,
                getBioAttributeURI(segmentUriMapping.get(segment))));
        return uris;
    }

    @Override
    protected Uri getBioAttributeURI(String file) {
        byte[] isoRecord = getIsoDataFromAssets(DeviceConstants.DeviceUsage.Authentication.getDeviceUsage() + "/" + file);
        Uri isoUri = Uri.fromFile(getTempFile(appContext));
        saveByteArray(isoRecord, isoUri);
        return isoUri;
    }
}
