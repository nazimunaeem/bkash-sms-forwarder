package com.bankprep.bkashforwarder;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import org.json.JSONObject;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class SmsForwarder {

    private static final String TAG = "SmsForwarder";
    public static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    public interface Callback {
        void onResult(boolean success);
    }

    public static void forward(Context context, String smsBody, String simLabel, Callback callback) {
        SharedPreferences prefs = context.getSharedPreferences("bkash_forwarder", Context.MODE_PRIVATE);
        String serverUrl  = prefs.getString("server_url", "").trim();
        String secretKey  = prefs.getString("secret_key", "").trim();
        boolean verifySSL = prefs.getBoolean("verify_ssl", true);

        if (serverUrl.isEmpty()) {
            Log.w(TAG, "No server URL configured");
            if (callback != null) callback.onResult(false);
            return;
        }

        new Thread(() -> {
            boolean success = false;
            try {
                OkHttpClient client = new OkHttpClient.Builder()
                        .connectTimeout(15, TimeUnit.SECONDS)
                        .readTimeout(15, TimeUnit.SECONDS)
                        .writeTimeout(15, TimeUnit.SECONDS)
                        .build();

                JSONObject payload = new JSONObject();
                payload.put("message", smsBody);
                payload.put("sim", simLabel);
                payload.put("timestamp", System.currentTimeMillis());
                payload.put("source", "bkash_sms_forwarder");

                Request.Builder reqBuilder = new Request.Builder()
                        .url(serverUrl)
                        .post(RequestBody.create(payload.toString(), JSON))
                        .addHeader("Content-Type", "application/json")
                        .addHeader("X-App-Source", "BkashSMSForwarder/1.0");

                if (!secretKey.isEmpty()) {
                    reqBuilder.addHeader("X-Secret-Key", secretKey);
                }

                Request request = reqBuilder.build();
                try (Response response = client.newCall(request).execute()) {
                    success = response.isSuccessful();
                    Log.d(TAG, "Server response: " + response.code());
                }
            } catch (Exception e) {
                Log.e(TAG, "Forward failed: " + e.getMessage());
            }

            // Update last_forward timestamp on success
            if (success) {
                String ts = new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm:ss",
                        java.util.Locale.getDefault()).format(new java.util.Date());
                prefs.edit().putString("last_forward", ts).apply();
            }

            final boolean result = success;
            if (callback != null) callback.onResult(result);
        }).start();
    }
}
