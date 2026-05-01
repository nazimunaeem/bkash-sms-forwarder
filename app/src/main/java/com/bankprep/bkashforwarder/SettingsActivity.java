package com.bankprep.bkashforwarder;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

public class SettingsActivity extends AppCompatActivity {

    private EditText etServerUrl, etSecretKey, etSenderFilter;
    private RadioGroup rgSimPref;
    private RadioButton rbBoth, rbSim1, rbSim2;
    private Switch switchVerifySSL;
    private TextView tvSim1Name, tvSim2Name;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Settings");
        }

        etServerUrl    = findViewById(R.id.et_server_url);
        etSecretKey    = findViewById(R.id.et_secret_key);
        etSenderFilter = findViewById(R.id.et_sender_filter);
        rgSimPref      = findViewById(R.id.rg_sim_pref);
        rbBoth         = findViewById(R.id.rb_both);
        rbSim1         = findViewById(R.id.rb_sim1);
        rbSim2         = findViewById(R.id.rb_sim2);
        switchVerifySSL= findViewById(R.id.switch_verify_ssl);
        tvSim1Name     = findViewById(R.id.tv_sim1_name);
        tvSim2Name     = findViewById(R.id.tv_sim2_name);

        Button btnSave = findViewById(R.id.btn_save);
        Button btnTestConn = findViewById(R.id.btn_test_connection);

        loadSimInfo();
        loadSettings();

        btnSave.setOnClickListener(v -> saveSettings());
        btnTestConn.setOnClickListener(v -> testConnection());
    }

    private void loadSimInfo() {
        SimHelper helper = new SimHelper(this);
        String sim1 = helper.getSimName(0);
        String sim2 = helper.getSimName(1);
        tvSim1Name.setText("SIM 1: " + sim1);
        tvSim2Name.setText("SIM 2: " + sim2);

        // Update radio button labels
        rbSim1.setText("SIM 1 only (" + sim1 + ")");
        rbSim2.setText("SIM 2 only (" + sim2 + ")");
    }

    private void loadSettings() {
        SharedPreferences prefs = getPrefs();
        etServerUrl.setText(prefs.getString("server_url", ""));
        etSecretKey.setText(prefs.getString("secret_key", ""));
        etSenderFilter.setText(prefs.getString("sender_filter", "bKash"));
        switchVerifySSL.setChecked(prefs.getBoolean("verify_ssl", true));

        String simPref = prefs.getString("sim_preference", "both");
        switch (simPref) {
            case "sim1": rbSim1.setChecked(true); break;
            case "sim2": rbSim2.setChecked(true); break;
            default:     rbBoth.setChecked(true);  break;
        }
    }

    private void saveSettings() {
        String url    = etServerUrl.getText().toString().trim();
        String key    = etSecretKey.getText().toString().trim();
        String filter = etSenderFilter.getText().toString().trim();

        if (url.isEmpty()) {
            etServerUrl.setError("Server URL is required");
            return;
        }
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            etServerUrl.setError("URL must start with http:// or https://");
            return;
        }

        String simPref;
        int checked = rgSimPref.getCheckedRadioButtonId();
        if (checked == R.id.rb_sim1)      simPref = "sim1";
        else if (checked == R.id.rb_sim2) simPref = "sim2";
        else                               simPref = "both";

        getPrefs().edit()
                .putString("server_url", url)
                .putString("secret_key", key)
                .putString("sender_filter", filter.isEmpty() ? "bKash" : filter)
                .putString("sim_preference", simPref)
                .putBoolean("verify_ssl", switchVerifySSL.isChecked())
                .apply();

        Toast.makeText(this, "✓ Settings saved", Toast.LENGTH_SHORT).show();
        finish();
    }

    private void testConnection() {
        String url = etServerUrl.getText().toString().trim();
        if (url.isEmpty()) {
            Toast.makeText(this, "Enter server URL first", Toast.LENGTH_SHORT).show();
            return;
        }
        Toast.makeText(this, "Testing connection...", Toast.LENGTH_SHORT).show();

        String testMsg = "You have received Tk 1.00 from 01729752392. Ref nazim. " +
                "Fee Tk 0.00. Balance Tk 6,908.21. TrxID DDD14QS409 at 13/04/2026 11:17";

        // Temporarily save url for test
        String savedUrl  = getPrefs().getString("server_url", "");
        String savedKey  = getPrefs().getString("secret_key", "");
        getPrefs().edit()
                .putString("server_url", url)
                .putString("secret_key", etSecretKey.getText().toString().trim())
                .apply();

        SmsForwarder.forward(this, testMsg, "TEST-SIM", result -> runOnUiThread(() -> {
            if (result) {
                Toast.makeText(this, "✓ Connection successful!", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "✗ Connection failed – check URL/server", Toast.LENGTH_LONG).show();
            }
        }));
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) { onBackPressed(); return true; }
        return super.onOptionsItemSelected(item);
    }

    private SharedPreferences getPrefs() {
        return getSharedPreferences("bkash_forwarder", MODE_PRIVATE);
    }
}
