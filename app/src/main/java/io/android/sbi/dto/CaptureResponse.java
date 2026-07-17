package io.android.sbi.dto;

import java.util.List;

public class CaptureResponse {

    public List<CaptureDetail> biometrics;

    @Override
    public String toString() {
        return "CaptureResponse{" +
                "biometrics=" + biometrics +
                '}';
    }
}
