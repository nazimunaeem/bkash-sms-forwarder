package com.bankprep.bkashforwarder;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;

import androidx.core.content.ContextCompat;

import java.util.List;

public class SimHelper {

    private final Context context;

    public SimHelper(Context context) {
        this.context = context;
    }

    /**
     * Returns a display name for the SIM in the given slot (0-based).
     * Returns "Unknown" if unavailable.
     */
    public String getSimName(int slotIndex) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP_MR1) return "SIM " + (slotIndex + 1);
        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_PHONE_STATE)
                != PackageManager.PERMISSION_GRANTED) {
            return "SIM " + (slotIndex + 1);
        }
        try {
            SubscriptionManager sm =
                    (SubscriptionManager) context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE);
            if (sm == null) return "SIM " + (slotIndex + 1);
            List<SubscriptionInfo> list = sm.getActiveSubscriptionInfoList();
            if (list == null || list.isEmpty()) return "Not inserted";
            for (SubscriptionInfo info : list) {
                if (info.getSimSlotIndex() == slotIndex) {
                    String carrier = "";
                    if (info.getCarrierName() != null) carrier = info.getCarrierName().toString();
                    return carrier.isEmpty() ? "SIM " + (slotIndex + 1) : carrier;
                }
            }
        } catch (Exception e) {
            // ignore
        }
        return "Not inserted";
    }
}
