package io.mosip.mock.sbi.sdk;

import android.content.Context;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import ai.tech5.pheonix.capture.controller.DeviceStatus;
import ai.tech5.pheonix.capture.controller.FaceCaptureController;

import io.mosip.mock.sbi.utility.DeviceConstants;

public final class T5FaceDeviceStatus {

    private static final long STATUS_TIMEOUT_SECONDS = 5;

    private T5FaceDeviceStatus() {
    }

    public static DeviceConstants.ServiceStatus getFaceStatus(Context ctx, DeviceConstants.ServiceStatus configured) {
        if (configured == DeviceConstants.ServiceStatus.NOT_REGISTERED) {
            return configured; // registration is a keystore/config concern; the SDK only reports device state
        }
        DeviceStatus status = queryDeviceStatus(ctx);
        if (status == null) {
            return DeviceConstants.ServiceStatus.NOT_READY;
        }
        switch (status) {
            case SDK_INIT_SUCCESS_CAMERA_AVAILABLE:
                return DeviceConstants.ServiceStatus.READY;
            case CAMERA_NOT_AVAILABLE:
                return DeviceConstants.ServiceStatus.BUSY;
            case SDK_INIT_FAILED:
            case INIT_ERROR:
            default:
                return DeviceConstants.ServiceStatus.NOT_READY;
        }
    }

    private static DeviceStatus queryDeviceStatus(Context ctx) {
        final DeviceStatus[] result = new DeviceStatus[1];
        final CountDownLatch latch = new CountDownLatch(1);
        try {
            FaceCaptureController.getInstance().getDeviceStatus(ctx, "", status -> {
                result[0] = status;
                latch.countDown();
            });
            latch.await(STATUS_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (Exception ignore) {
            // fall through - null result maps to NOT_READY
        }
        return result[0];
    }
}
