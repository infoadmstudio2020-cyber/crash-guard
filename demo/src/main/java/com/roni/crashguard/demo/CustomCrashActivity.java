package com.roni.crashguard.demo;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Process;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.roni.library.crashguard.CrashReport;
import com.roni.library.crashguard.CrashReportStorage;
import com.roni.library.crashguard.DefaultCrashActivity;

/**
 * Example of a custom branded recovery screen running in an isolated process (:crash_guard_process).
 */
public class CustomCrashActivity extends AppCompatActivity {

    private CrashReport report;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_custom_crash);

        Intent intent = getIntent();
        if (intent != null) {
            //noinspection deprecation
            report = (CrashReport) intent.getSerializableExtra(DefaultCrashActivity.EXTRA_CRASH_REPORT);
        }

        if (report == null) {
            CrashReportStorage storage = new CrashReportStorage(this, 10);
            report = storage.getLatestReport();
        }

        TextView tvError = findViewById(R.id.tv_custom_error);
        TextView tvDetails = findViewById(R.id.tv_custom_details);
        Button btnCopy = findViewById(R.id.btn_custom_copy);
        Button btnRestart = findViewById(R.id.btn_custom_restart);

        if (report != null) {
            tvError.setText(report.getExceptionClass() + "\n" + report.getExceptionMessage());
            tvDetails.setText(report.toFormattedString());
        }

        btnCopy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (report != null) {
                    ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                    if (cm != null) {
                        cm.setPrimaryClip(ClipData.newPlainText("Crash Diagnostics", report.toFormattedString()));
                        Toast.makeText(CustomCrashActivity.this, "Copied to clipboard!", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });

        btnRestart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent restart = new Intent(CustomCrashActivity.this, MainActivity.class);
                restart.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(restart);
                finish();
                Process.killProcess(Process.myPid());
                System.exit(10);
            }
        });
    }

    @Override
    public void onBackPressed() {
        finish();
        Process.killProcess(Process.myPid());
        System.exit(10);
    }
}
