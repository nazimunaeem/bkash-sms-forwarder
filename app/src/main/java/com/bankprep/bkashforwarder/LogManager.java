package com.bankprep.bkashforwarder;

import android.content.Context;
import android.content.SharedPreferences;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class LogManager {

    private static final String PREF_KEY = "log_entries";
    private static final int MAX_ENTRIES = 200;
    private static final String DELIMITER = "|||";

    public static void append(Context context, String message) {
        SharedPreferences prefs = context.getSharedPreferences("bkash_log", Context.MODE_PRIVATE);
        String ts = new SimpleDateFormat("dd/MM HH:mm:ss", Locale.getDefault()).format(new Date());
        String entry = ts + " – " + message;

        String existing = prefs.getString(PREF_KEY, "");
        List<String> entries = new ArrayList<>();
        if (!existing.isEmpty()) {
            entries.addAll(Arrays.asList(existing.split("\\|\\|\\|")));
        }
        entries.add(0, entry); // newest first
        if (entries.size() > MAX_ENTRIES) {
            entries = entries.subList(0, MAX_ENTRIES);
        }
        prefs.edit().putString(PREF_KEY, String.join(DELIMITER, entries)).apply();
    }

    public static List<String> getAll(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("bkash_log", Context.MODE_PRIVATE);
        String raw = prefs.getString(PREF_KEY, "");
        if (raw.isEmpty()) return new ArrayList<>();
        return new ArrayList<>(Arrays.asList(raw.split("\\|\\|\\|")));
    }

    public static void clear(Context context) {
        context.getSharedPreferences("bkash_log", Context.MODE_PRIVATE)
                .edit().remove(PREF_KEY).apply();
    }
}
