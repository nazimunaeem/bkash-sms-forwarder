package com.bankprep.bkashforwarder;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        SharedPreferences prefs =
                context.getSharedPreferences("bkash_forwarder", Context.MODE_PRIVATE);
        if (prefs.getBoolean("enabled", false)) {
            Intent service = new Intent(context, ForwarderService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(service);
            } else {
                context.startService(service);
            }
        }
    }
}
