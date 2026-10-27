package com.roni.crashguard.demo;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.switchmaterial.SwitchMaterial;
import com.roni.library.crashguard.Breadcrumb;
import com.roni.library.crashguard.CrashGuard;
import com.roni.library.crashguard.CrashGuardConfig;

import java.io.IOException;

/**
 * Interactive demonstration dashboard for testing CrashGuard features,
 * simulated crashes, breadcrumb logging, and crash inspection.
 */
public class MainActivity extends AppCompatActivity {

    private TextView tvReportsCount;
    private SwitchMaterial switchCustomScreen;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        CrashGuard.addBreadcrumb("MainActivity created", Breadcrumb.Type.NAVIGATION, "ui");

        initViews();
        setupTriggers();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateReportsCount();
    }

    private void initViews() {
        tvReportsCount = findViewById(R.id.tv_reports_count);
        switchCustomScreen = findViewById(R.id.switch_custom_screen);

        switchCustomScreen.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                // Reinstall or reconfigure with custom screen if selected
                CrashGuardConfig config = CrashGuardConfig.newBuilder()
                    .setEnabled(true)
                    .setDebugMode(true)
                    .setShowCrashActivity(true)
                    .setCustomCrashActivityClass(isChecked ? CustomCrashActivity.class : null)
                    .setRestartActivityClass(MainActivity.class)
                    .setMaxStoredReports(30)
                    .build();

                // Update config
                CrashGuard.install(MainActivity.this, config);
                CrashGuard.addBreadcrumb("Toggled custom crash screen: " + isChecked, Breadcrumb.Type.USER_ACTION, "settings");
                Toast.makeText(MainActivity.this, isChecked ? "Custom crash activity activated" : "Default crash activity restored", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateReportsCount() {
        int count = CrashGuard.getStoredReports().size();
        tvReportsCount.setText("Stored Reports: " + count);
    }

    private void setupTriggers() {
        Button btnNpe = findViewById(R.id.btn_trigger_npe);
        Button btnBg = findViewById(R.id.btn_trigger_bg);
        Button btnMath = findViewById(R.id.btn_trigger_math);
        Button btnChained = findViewById(R.id.btn_trigger_chained);
        Button btnHandled = findViewById(R.id.btn_trigger_handled);
        Button btnNavBreadcrumb = findViewById(R.id.btn_add_nav_breadcrumb);
        Button btnNetBreadcrumb = findViewById(R.id.btn_add_net_breadcrumb);
        Button btnViewHistory = findViewById(R.id.btn_view_history);

        // 1. NullPointerException on Main Thread
        btnNpe.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CrashGuard.addBreadcrumb("User tapped Trigger NullPointerException button", Breadcrumb.Type.USER_ACTION, "demo");
                String nullString = null;
                // Intentional crash
                //noinspection ResultOfMethodCallIgnored
                nullString.trim();
            }
        });

        // 2. Background Thread Crash
        btnBg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CrashGuard.addBreadcrumb("User tapped Trigger Background Crash", Breadcrumb.Type.USER_ACTION, "demo");
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Thread.sleep(200);
                        } catch (InterruptedException ignored) {}
                        throw new IllegalStateException("Simulated fatal exception in worker background thread pool");
                    }
                }, "DemoWorkerThread-42").start();
            }
        });

        // 3. Divide by Zero Arithmetic Crash
        btnMath.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CrashGuard.addBreadcrumb("User triggered ArithmeticException division by zero", Breadcrumb.Type.USER_ACTION, "demo");
                int a = 100;
                int b = 0;
                // Intentional crash
                //noinspection NumericOverflow,unused
                int c = a / b;
            }
        });

        // 4. Nested Chained Exception
        btnChained.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CrashGuard.addBreadcrumb("User triggered Chained Nested Exception", Breadcrumb.Type.USER_ACTION, "demo");
                IOException rootCause = new IOException("Connection refused: 127.0.0.1:8080 (socket timeout)");
                IllegalStateException midCause = new IllegalStateException("Service transaction pipeline failed", rootCause);
                throw new RuntimeException("Fatal failure during demo transaction dispatch", midCause);
            }
        });

        // 5. Handled Exception Logging
        btnHandled.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    throw new IllegalArgumentException("Handled validation error: Invalid authentication token header format");
                } catch (Exception e) {
                    CrashGuard.logHandledException(e);
                    Toast.makeText(MainActivity.this, "Logged handled exception to CrashGuard!", Toast.LENGTH_SHORT).show();
                    updateReportsCount();
                }
            }
        });

        // Breadcrumb buttons
        btnNavBreadcrumb.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CrashGuard.addBreadcrumb("Navigated to Settings Tab [page_id: 104]", Breadcrumb.Type.NAVIGATION, "ui");
                Toast.makeText(MainActivity.this, "Navigation breadcrumb recorded", Toast.LENGTH_SHORT).show();
            }
        });

        btnNetBreadcrumb.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CrashGuard.addBreadcrumb("GET /api/v2/user/profile HTTP/1.1 -> 200 OK (142ms)", Breadcrumb.Type.NETWORK, "api");
                Toast.makeText(MainActivity.this, "Network breadcrumb recorded", Toast.LENGTH_SHORT).show();
            }
        });

        // View Reports History
        btnViewHistory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CrashGuard.addBreadcrumb("Opening ReportsHistoryActivity", Breadcrumb.Type.NAVIGATION, "ui");
                Intent intent = new Intent(MainActivity.this, ReportsHistoryActivity.class);
                startActivity(intent);
            }
        });
    }
}
