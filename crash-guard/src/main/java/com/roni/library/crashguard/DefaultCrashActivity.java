package com.roni.library.crashguard;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Process;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

/**
 * Isolated process Activity providing a robust diagnostic interface when the main app crashes.
 */
public class DefaultCrashActivity extends AppCompatActivity {

    public static final String EXTRA_CRASH_REPORT = "extra_crashguard_report";
    public static final String EXTRA_RESTART_ACTIVITY = "extra_restart_activity";

    private CrashReport crashReport;
    private String restartActivityClassName;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_default_crash);

        Intent intent = getIntent();
        if (intent != null) {
            //noinspection deprecation
            crashReport = (CrashReport) intent.getSerializableExtra(EXTRA_CRASH_REPORT);
            restartActivityClassName = intent.getStringExtra(EXTRA_RESTART_ACTIVITY);
        }

        if (crashReport == null) {
            CrashReportStorage storage = new CrashReportStorage(this, 10);
            crashReport = storage.getLatestReport();
        }

        initViews();
    }

    private void initViews() {
        TextView tvExceptionName = findViewById(R.id.tv_exception_name);
        TextView tvExceptionMessage = findViewById(R.id.tv_exception_message);
        TextView tvStackTrace = findViewById(R.id.tv_stacktrace);
        Button btnCopy = findViewById(R.id.btn_copy);
        Button btnShare = findViewById(R.id.btn_share);
        Button btnRestart = findViewById(R.id.btn_restart);
        Button btnClose = findViewById(R.id.btn_close);

        if (crashReport != null) {
            tvExceptionName.setText(crashReport.getExceptionClass());
            tvExceptionMessage.setText(crashReport.getExceptionMessage());
            tvStackTrace.setText(crashReport.toFormattedString());
        } else {
            tvExceptionName.setText("Crash Report Unavailable");
            tvExceptionMessage.setText("Could not load crash payload.");
            tvStackTrace.setText("No stack trace details available.");
        }

        btnCopy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                copyReportToClipboard();
            }
        });

        btnShare.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                shareCrashReport();
            }
        });

        btnRestart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                restartApplication();
            }
        });

        btnClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                closeApplication();
            }
        });
    }

    private void copyReportToClipboard() {
        if (crashReport == null) return;
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("CrashGuard Report", crashReport.toFormattedString());
        if (clipboard != null) {
            clipboard.setPrimaryClip(clip);
            Toast.makeText(this, R.string.crashguard_copied_to_clipboard, Toast.LENGTH_SHORT).show();
        }
    }

    private void shareCrashReport() {
        if (crashReport == null) return;
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Crash Report: " + crashReport.getExceptionClass());
        shareIntent.putExtra(Intent.EXTRA_TEXT, crashReport.toFormattedString());
        startActivity(Intent.createChooser(shareIntent, getString(R.string.crashguard_btn_share)));
    }

    private void restartApplication() {
        Intent restartIntent = null;
        if (restartActivityClassName != null && !restartActivityClassName.isEmpty()) {
            try {
                Class<?> clazz = Class.forName(restartActivityClassName);
                restartIntent = new Intent(this, clazz);
            } catch (ClassNotFoundException ignored) {}
        }

        if (restartIntent == null) {
            PackageManager pm = getPackageManager();
            restartIntent = pm.getLaunchIntentForPackage(getPackageName());
        }

        if (restartIntent != null) {
            restartIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(restartIntent);
        }

        finish();
        Process.killProcess(Process.myPid());
        System.exit(10);
    }

    private void closeApplication() {
        finish();
        Process.killProcess(Process.myPid());
        System.exit(10);
    }

    @Override
    public void onBackPressed() {
        closeApplication();
    }
}
