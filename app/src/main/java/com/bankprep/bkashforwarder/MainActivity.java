package com.bankprep.bkashforwarder;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final int PERM_REQUEST = 100;
    private Switch switchEnable;
    private TextView tvStatus, tvServerUrl, tvSimMode, tvLastForward;
    private ImageView ivStatusIcon;
    private Button btnViewLogs, btnTestSend;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        switchEnable   = findViewById(R.id.switch_enable);
        tvStatus       = findViewById(R.id.tv_status);
        tvServerUrl    = findViewById(R.id.tv_server_url);
        tvSimMode      = findViewById(R.id.tv_sim_mode);
        tvLastForward  = findViewById(R.id.tv_last_forward);
        ivStatusIcon   = findViewById(R.id.iv_status_icon);
        btnViewLogs    = findViewById(R.id.btn_view_logs);
        btnTestSend    = findViewById(R.id.btn_test_send);

        btnViewLogs.setOnClickListener(v ->
                startActivity(new Intent(this, LogActivity.class)));

        btnTestSend.setOnClickListener(v -> sendTestSms());

        switchEnable.setOnCheckedChangeListener((btn, checked) -> {
            SharedPreferences prefs = getPrefs();
            prefs.edit().putBoolean("enabled", checked).apply();
            updateStatusUI(checked);
            if (checked) {
                startForwarderService();
            } else {
                stopService(new Intent(this, ForwarderService.class));
            }
        });

        requestPermissions();
        requestBatteryOptimizationExemption();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshUI();
    }

    private void refreshUI() {
        SharedPreferences prefs = getPrefs();
        boolean enabled = prefs.getBoolean("enabled", false);
        String url      = prefs.getString("server_url", "Not configured");
        String simPref  = prefs.getString("sim_preference", "both");
        String lastFwd  = prefs.getString("last_forward", "Never");

        switchEnable.setChecked(enabled);
        tvServerUrl.setText("Server: " + (url.isEmpty() ? "Not configured" : url));
        tvLastForward.setText("Last forwarded: " + lastFwd);

        String simLabel;
        switch (simPref) {
            case "sim1": simLabel = "SIM 1 only"; break;
            case "sim2": simLabel = "SIM 2 only"; break;
            default:     simLabel = "Both SIMs";  break;
        }
        tvSimMode.setText("Monitor: " + simLabel);
        updateStatusUI(enabled);
    }

    private void updateStatusUI(boolean enabled) {
        if (enabled) {
            tvStatus.setText("● Active – Monitoring bKash SMS");
            tvStatus.setTextColor(getColor(R.color.green));
            ivStatusIcon.setImageResource(R.drawable.ic_active);
        } else {
            tvStatus.setText("● Inactive");
            tvStatus.setTextColor(getColor(R.color.red));
            ivStatusIcon.setImageResource(R.drawable.ic_inactive);
        }
    }

    private void startForwarderService() {
        Intent intent = new Intent(this, ForwarderService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent);
        } else {
            startService(intent);
        }
    }

    private void sendTestSms() {
        String testMsg = "You have received Tk 1.00 from 01729752392. Ref nazim. " +
                "Fee Tk 0.00. Balance Tk 6,908.21. TrxID DDD14QS409 at 13/04/2026 11:17";
        SmsForwarder.forward(this, testMsg, "TEST", result -> runOnUiThread(() -> {
            if (result) {
                Toast.makeText(this, "✓ Test sent successfully!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "✗ Test failed – check server settings", Toast.LENGTH_LONG).show();
            }
        }));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // ── Permissions ──────────────────────────────────────────────────────────

    private void requestPermissions() {
        List<String> needed = new ArrayList<>();
        String[] perms = {
                Manifest.permission.RECEIVE_SMS,
                Manifest.permission.READ_SMS,
                Manifest.permission.READ_PHONE_STATE
        };
        for (String p : perms) {
            if (ContextCompat.checkSelfPermission(this, p) != PackageManager.PERMISSION_GRANTED)
                needed.add(p);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED)
                needed.add(Manifest.permission.POST_NOTIFICATIONS);
        }
        if (!needed.isEmpty()) {
            ActivityCompat.requestPermissions(this,
                    needed.toArray(new String[0]), PERM_REQUEST);
        }
    }

    private void requestBatteryOptimizationExemption() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
            if (pm != null && !pm.isIgnoringBatteryOptimizations(getPackageName())) {
                new AlertDialog.Builder(this)
                        .setTitle("Disable Battery Optimization")
                        .setMessage("To reliably receive SMS in background, please disable battery optimization for this app.")
                        .setPositiveButton("Open Settings", (d, w) -> {
                            Intent i = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                            i.setData(Uri.parse("package:" + getPackageName()));
                            startActivity(i);
                        })
                        .setNegativeButton("Later", null)
                        .show();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int req, @NonNull String[] perms,
                                           @NonNull int[] results) {
        super.onRequestPermissionsResult(req, perms, results);
        for (int r : results) {
            if (r != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this,
                        "Some permissions denied – app may not work correctly",
                        Toast.LENGTH_LONG).show();
                return;
            }
        }
    }

    private SharedPreferences getPrefs() {
        return getSharedPreferences("bkash_forwarder", MODE_PRIVATE);
    }
}
