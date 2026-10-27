package com.roni.crashguard.demo;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.roni.library.crashguard.CrashGuard;
import com.roni.library.crashguard.CrashReport;

import java.util.ArrayList;
import java.util.List;

/**
 * Activity for exploring and inspecting persisted crash reports stored on device.
 */
public class ReportsHistoryActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private TextView tvEmptyState;
    private ReportsAdapter adapter;
    private final List<CrashReport> reportsList = new ArrayList<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reports_history);

        recyclerView = findViewById(R.id.rv_reports);
        tvEmptyState = findViewById(R.id.tv_empty_state);
        Button btnClearAll = findViewById(R.id.btn_clear_all);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ReportsAdapter(reportsList, new ReportActionListener() {
            @Override
            public void onViewDetails(CrashReport report) {
                showReportDetailsDialog(report);
            }

            @Override
            public void onDelete(CrashReport report) {
                CrashGuard.deleteReport(report.getReportId());
                loadReports();
            }
        });
        recyclerView.setAdapter(adapter);

        btnClearAll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new AlertDialog.Builder(ReportsHistoryActivity.this)
                    .setTitle("Clear All Reports")
                    .setMessage("Are you sure you want to delete all stored crash reports?")
                    .setPositiveButton("Clear", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            CrashGuard.clearReports();
                            loadReports();
                        }
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
            }
        });

        loadReports();
    }

    private void loadReports() {
        reportsList.clear();
        List<CrashReport> stored = CrashGuard.getStoredReports();
        if (stored != null) {
            reportsList.addAll(stored);
        }
        adapter.notifyDataSetChanged();

        if (reportsList.isEmpty()) {
            tvEmptyState.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            tvEmptyState.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }

    private void showReportDetailsDialog(CrashReport report) {
        new AlertDialog.Builder(this)
            .setTitle(report.getExceptionClass())
            .setMessage(report.toFormattedString())
            .setPositiveButton("Copy", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                    if (cm != null) {
                        cm.setPrimaryClip(ClipData.newPlainText("Crash Report", report.toFormattedString()));
                        Toast.makeText(ReportsHistoryActivity.this, "Copied report", Toast.LENGTH_SHORT).show();
                    }
                }
            })
            .setNeutralButton("Share", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    Intent share = new Intent(Intent.ACTION_SEND);
                    share.setType("text/plain");
                    share.putExtra(Intent.EXTRA_TEXT, report.toFormattedString());
                    startActivity(Intent.createChooser(share, "Share Crash Report"));
                }
            })
            .setNegativeButton("Close", null)
            .show();
    }

    interface ReportActionListener {
        void onViewDetails(CrashReport report);
        void onDelete(CrashReport report);
    }

    static class ReportsAdapter extends RecyclerView.Adapter<ReportsAdapter.ViewHolder> {
        private final List<CrashReport> list;
        private final ReportActionListener listener;

        ReportsAdapter(List<CrashReport> list, ReportActionListener listener) {
            this.list = list;
            this.listener = listener;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_report, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            CrashReport item = list.get(position);
            holder.tvException.setText(item.getExceptionClass());
            holder.tvMessage.setText(item.getExceptionMessage());
            holder.tvDate.setText(item.getFormattedDate());
            holder.tvThread.setText(item.getThreadName() + " (thread " + item.getThreadId() + ")");

            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    listener.onViewDetails(item);
                }
            });

            holder.btnDelete.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    listener.onDelete(item);
                }
            });
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvException;
            TextView tvMessage;
            TextView tvDate;
            TextView tvThread;
            Button btnDelete;

            ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvException = itemView.findViewById(R.id.tv_item_exception);
                tvMessage = itemView.findViewById(R.id.tv_item_message);
                tvDate = itemView.findViewById(R.id.tv_item_date);
                tvThread = itemView.findViewById(R.id.tv_item_thread);
                btnDelete = itemView.findViewById(R.id.btn_item_delete);
            }
        }
    }
}
