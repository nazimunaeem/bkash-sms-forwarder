package com.bankprep.bkashforwarder;

import android.os.Bundle;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.util.List;

public class LogActivity extends AppCompatActivity {

    private ListView listView;
    private TextView tvEmpty;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_log);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Forward Log");
        }

        listView = findViewById(R.id.list_log);
        tvEmpty  = findViewById(R.id.tv_empty);
        Button btnClear = findViewById(R.id.btn_clear_log);

        btnClear.setOnClickListener(v -> new AlertDialog.Builder(this)
                .setTitle("Clear Log")
                .setMessage("Delete all log entries?")
                .setPositiveButton("Clear", (d, w) -> {
                    LogManager.clear(this);
                    refreshLog();
                    Toast.makeText(this, "Log cleared", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show());

        refreshLog();
    }

    private void refreshLog() {
        List<String> entries = LogManager.getAll(this);
        if (entries.isEmpty()) {
            listView.setVisibility(android.view.View.GONE);
            tvEmpty.setVisibility(android.view.View.VISIBLE);
        } else {
            tvEmpty.setVisibility(android.view.View.GONE);
            listView.setVisibility(android.view.View.VISIBLE);
            listView.setAdapter(new ArrayAdapter<>(this,
                    android.R.layout.simple_list_item_1, entries));
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) { onBackPressed(); return true; }
        return super.onOptionsItemSelected(item);
    }
}
