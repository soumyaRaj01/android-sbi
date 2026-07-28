package io.android.sbi.crypto;

import android.content.SharedPreferences;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.android.sbi.constants.ClientConstants;
import io.android.sbi.utility.CommonDeviceAPI;
import io.android.sbi.utility.Logger;
import okhttp3.*;
import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

public class DmsClient {

    private final String baseUrl;
    private final SharedPreferences sharedPreferences;
    private final CommonDeviceAPI devCommonDeviceAPI;

    private final OkHttpClient client;

    private final CookieJar cookieJar = new CookieJar() {
        private List<Cookie> cookies = new ArrayList<>();

        @Override
        public void saveFromResponse(HttpUrl url, List<Cookie> cookies) {
            this.cookies = cookies;
        }

        @Override
        public List<Cookie> loadForRequest(HttpUrl url) {
            return cookies;
        }
    };

    public DmsClient(String baseUrl) {
        this(baseUrl, null);
    }

    public DmsClient(String baseUrl, SharedPreferences sharedPreferences) {
        this.baseUrl = baseUrl;
        this.sharedPreferences = sharedPreferences;
        devCommonDeviceAPI = new CommonDeviceAPI();

        client = new OkHttpClient.Builder()
                .cookieJar(cookieJar)
                .build();
    }

    public void authenticate(String username, String password) throws Exception {
        try {
            URL url = new URL(baseUrl + "/v2/api/Authenticate");

            String requestBody = "{ \"request\": { " +
                    "\"username\":\"" + username + "\"," +
                    "\"password\":\"" + password + "\"" +
                    "} }";

            MediaType mediaType = MediaType.parse("application/json; charset=utf-8");
            RequestBody body = RequestBody.create(mediaType, requestBody);
            Request request = new Request.Builder()
                    .url(url)
                    .post(body)
                    .build();

            Response response = client.newCall(request).execute();
            if (!response.isSuccessful()) {
                Logger.e("", "Error authenticating to DMS");
            }
        } catch (Exception e) {
            Logger.e("", "Error authenticating to DMS: " + e.getMessage());
        }
    }

    public String generateSignedCertificate(
            String csr,
            String type)
            throws Exception {
        URL url = new URL(baseUrl + "/v2/api/GenerateSignedDeviceCertificate");

        String serialNo = devCommonDeviceAPI.getSerialNumber();

        String requestBody = "{ \"request\": {" +
                "\"serialNum\":\"" + serialNo + "\"," +
                "\"devicePublicKey\":\"" + csr + "\"," +
                "\"keyType\":\"" + type + "\"" +
                "} }";

        MediaType mediaType = MediaType.parse("application/json; charset=utf-8");
        RequestBody body = RequestBody.create(mediaType, requestBody);
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();

        Response response = client.newCall(request).execute();
        if (!response.isSuccessful()) {
            throw new Exception("DMS certificate request failed with HTTP " + response.code());
        }

        if (response.body() == null) {
            throw new Exception("DMS certificate response body is empty");
        }

        JSONObject jsonObject = new JSONObject(response.body().string());
        JSONArray errors = jsonObject.optJSONArray("errors");
        if (errors != null && errors.length() > 0) {
            throw new Exception("DMS certificate request returned errors: " + errors);
        }

        String certificate = jsonObject.optString("response", "");
        if (certificate.isEmpty()) {
            throw new Exception("DMS certificate response is missing");
        }

        return certificate;
    }

    public void sendTokenToDmsServer(String token) {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                if (token == null || token.trim().isEmpty()) {
                    Logger.e("", "FCM token is empty, skipping DMS update");
                    return;
                }

                if (sharedPreferences != null) {
                    String cachedToken = sharedPreferences.getString(ClientConstants.FCM_TOKEN, "");
                    if (token.equals(cachedToken)) {
                        Logger.d("", "FCM token already synced with DMS");
                        return;
                    }
                }

                String serialNo = devCommonDeviceAPI.getSerialNumber();
                URL url = new URL(baseUrl + "/api/AddDeviceToken");

                String requestBody = "{ \"request\": {" +
                        "\"serialNumber\":\"" + serialNo + "\"," +
                        "\"token\":\"" + token + "\"" +
                        "} }";

                MediaType mediaType = MediaType.parse("application/json; charset=utf-8");
                RequestBody body = RequestBody.create(mediaType, requestBody);
                Request request = new Request.Builder()
                        .url(url)
                        .post(body)
                        .build();

                Response response = client.newCall(request).execute();

                if (!response.isSuccessful()) {
                    throw new Exception("Error storing token to DMS. HTTP " + response.code());
                }

                if (response.body() == null) {
                    throw new Exception("DMS token response body is empty");
                }

                JSONObject jsonObject = new JSONObject(response.body().string());
                JSONArray errors = jsonObject.optJSONArray("errors");
                if (errors != null && errors.length() > 0) {
                    Logger.e("", "Error storing token to DMS: " + errors);
                } else{
                    if (sharedPreferences != null) {
                        sharedPreferences.edit().putString(ClientConstants.FCM_TOKEN, token).apply();
                    }
                    Logger.d("", "FCM token sent to DMS successfully");
                }
            } catch (Exception e) {
                Logger.e("", "Error storing token to DMS: " + e.getMessage());
            }
        });
    }
}
