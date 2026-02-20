package com.munis.familyotp.network;

import android.util.Log;

import org.json.JSONObject;

import java.io.IOException;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class SheetService {

    private static final String TAG = "SheetService";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private final OkHttpClient client;
    private final String sheetUrl;

    public SheetService(String sheetUrl) {
        this.sheetUrl = sheetUrl;
        this.client = new OkHttpClient.Builder()
                .followRedirects(true)
                .followSslRedirects(true)
                .build();
    }

    public boolean postOtp(String sender, String userName, String encryptedMessage) {
        try {
            JSONObject json = new JSONObject();
            json.put("sender", sender);
            json.put("userName", userName);
            json.put("encryptedMessage", encryptedMessage);

            RequestBody body = RequestBody.create(json.toString(), JSON);
            Request request = new Request.Builder()
                    .url(sheetUrl)
                    .post(body)
                    .build();

            Response response = client.newCall(request).execute();
            String responseBody = response.body() != null ? response.body().string() : "";
            Log.e(TAG, "POST Response: " + response.code() + " - " + responseBody);
            return response.isSuccessful();
        } catch (Exception e) {
            Log.e(TAG, "POST failed", e);
            return false;
        }
    }

    public String fetchLastOtp() {
        try {
            Request request = new Request.Builder()
                    .url(sheetUrl)
                    .get()
                    .build();

            Response response = client.newCall(request).execute();
            if (response.isSuccessful() && response.body() != null) {
                String responseBody = response.body().string();
                Log.e(TAG, "GET Response: " + responseBody);
                JSONObject json = new JSONObject(responseBody);
                if ("ok".equals(json.optString("status"))) {
                    return json.optString("encryptedMessage", "");
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "GET failed", e);
        }
        return null;
    }
}
