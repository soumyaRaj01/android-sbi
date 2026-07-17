package io.android.sbi.sdk;

import android.content.Context;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import ai.tech5.finger.utils.DeviceStatus;
import ai.tech5.finger.utils.T5FingerCaptureController;

import io.android.sbi.utility.DeviceConstants;

public final class T5DeviceStatus {

    private static final long STATUS_TIMEOUT_SECONDS = 5;

    private T5DeviceStatus() {
    }

    public static String getFingerDeviceId() {
        try {
            return T5FingerCaptureController.getInstance().getDeviceID();
        } catch (Exception e) {
            return null;
        }
    }

    public static DeviceConstants.ServiceStatus getFingerStatus(Context ctx, DeviceConstants.ServiceStatus configured) {
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
            T5FingerCaptureController.getInstance().getDeviceStatus(ctx, status -> {
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
