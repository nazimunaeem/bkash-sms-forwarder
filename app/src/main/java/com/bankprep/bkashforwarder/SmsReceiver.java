package com.bankprep.bkashforwarder;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.telephony.SmsMessage;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.util.Log;

import java.util.List;

public class SmsReceiver extends BroadcastReceiver {

    private static final String TAG = "BkashSMSReceiver";
    private static final String ACTION = "android.provider.Telephony.SMS_RECEIVED";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!ACTION.equals(intent.getAction())) return;

        SharedPreferences prefs = context.getSharedPreferences("bkash_forwarder", Context.MODE_PRIVATE);
        if (!prefs.getBoolean("enabled", false)) return;

        Bundle bundle = intent.getExtras();
        if (bundle == null) return;

        // ── Determine which SIM received this SMS ──────────────────────────
        int subscriptionId = intent.getIntExtra("subscription", -1);
        if (subscriptionId == -1) {
            subscriptionId = intent.getIntExtra("phone", -1);
        }
        int simSlot = getSimSlotFromSubscriptionId(context, subscriptionId);
        String simLabel = getSimLabel(context, simSlot, subscriptionId);

        // ── Check SIM preference ───────────────────────────────────────────
        String simPref = prefs.getString("sim_preference", "both");
        if ("sim1".equals(simPref) && simSlot != 0) {
            Log.d(TAG, "Ignoring SMS from SIM " + (simSlot + 1) + " (filter: SIM 1 only)");
            return;
        }
        if ("sim2".equals(simPref) && simSlot != 1) {
            Log.d(TAG, "Ignoring SMS from SIM " + (simSlot + 1) + " (filter: SIM 2 only)");
            return;
        }

        // ── Parse SMS messages ─────────────────────────────────────────────
        Object[] pdus = (Object[]) bundle.get("pdus");
        String format = bundle.getString("format");
        if (pdus == null) return;

        StringBuilder fullBody = new StringBuilder();
        String sender = "";

        for (Object pdu : pdus) {
            SmsMessage msg;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                msg = SmsMessage.createFromPdu((byte[]) pdu, format);
            } else {
                msg = SmsMessage.createFromPdu((byte[]) pdu);
            }
            if (msg != null) {
                sender = msg.getDisplayOriginatingAddress();
                fullBody.append(msg.getDisplayMessageBody());
            }
        }

        String body = fullBody.toString().trim();
        Log.d(TAG, "SMS from [" + sender + "] via " + simLabel + ": " + body);

        // ── Filter: only forward bKash messages ───────────────────────────
        String filter = prefs.getString("sender_filter", "bKash").toLowerCase();
        boolean matchesSender = sender != null && sender.toLowerCase().contains(filter);
        boolean matchesBody   = body.toLowerCase().contains("bkash") ||
                                body.toLowerCase().contains("tk ") ||
                                body.toLowerCase().contains("trxid");

        if (!matchesSender && !matchesBody) {
            Log.d(TAG, "Not a bKash SMS, skipping.");
            return;
        }

        // ── Forward to server ─────────────────────────────────────────────
        String finalSender = sender;
        String finalSimLabel = simLabel;
        SmsForwarder.forward(context, body, finalSimLabel, success -> {
            String logEntry = (success ? "[OK] " : "[FAIL] ") +
                    "From: " + finalSender +
                    " | SIM: " + finalSimLabel +
                    " | " + body;
            LogManager.append(context, logEntry);
        });
    }

    /**
     * Maps a subscription ID to a 0-based SIM slot index.
     */
    private int getSimSlotFromSubscriptionId(Context context, int subscriptionId) {
        if (subscriptionId < 0) return 0; // default to SIM 1 if unknown
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                SubscriptionManager sm =
                        (SubscriptionManager) context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE);
                if (sm != null) {
                    List<SubscriptionInfo> list = sm.getActiveSubscriptionInfoList();
                    if (list != null) {
                        for (SubscriptionInfo info : list) {
                            if (info.getSubscriptionId() == subscriptionId) {
                                return info.getSimSlotIndex();
                            }
                        }
                    }
                }
            }
        } catch (SecurityException e) {
            Log.w(TAG, "No permission to read SIM info: " + e.getMessage());
        }
        return subscriptionId; // fallback
    }

    /**
     * Returns a human-readable SIM label (e.g. "SIM 1 (Grameenphone)").
     */
    private String getSimLabel(Context context, int simSlot, int subscriptionId) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                SubscriptionManager sm =
                        (SubscriptionManager) context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE);
                if (sm != null) {
                    List<SubscriptionInfo> list = sm.getActiveSubscriptionInfoList();
                    if (list != null) {
                        for (SubscriptionInfo info : list) {
                            if (info.getSimSlotIndex() == simSlot) {
                                CharSequence name = info.getDisplayName();
                                String carrier = info.getCarrierName() != null ?
                                        info.getCarrierName().toString() : "";
                                return "SIM " + (simSlot + 1) +
                                        (carrier.isEmpty() ? "" : " (" + carrier + ")");
                            }
                        }
                    }
                }
            }
        } catch (SecurityException e) {
            Log.w(TAG, "Cannot read SIM label: " + e.getMessage());
        }
        return "SIM " + (simSlot + 1);
    }
}
