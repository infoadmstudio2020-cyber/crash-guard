package com.roni.library.crashguard;

import android.content.Context;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Thread-safe persistent disk storage for crash reports adhering to FIFO retention limits.
 */
public class CrashReportStorage {

    private static final String DIR_NAME = "crashguard_reports";
    private static final String FILE_PREFIX = "crash_";
    private static final String FILE_SUFFIX = ".json";

    private final Context context;
    private final int maxReports;
    private final Object fileLock = new Object();

    public CrashReportStorage(Context context, int maxReports) {
        this.context = context.getApplicationContext();
        this.maxReports = Math.max(1, maxReports);
    }

    private File getReportsDir() {
        File dir = new File(context.getFilesDir(), DIR_NAME);
        if (!dir.exists()) {
            //noinspection ResultOfMethodCallIgnored
            dir.mkdirs();
        }
        return dir;
    }

    public boolean saveReport(CrashReport report) {
        if (report == null || report.getReportId() == null) return false;
        synchronized (fileLock) {
            try {
                File dir = getReportsDir();
                File reportFile = new File(dir, FILE_PREFIX + report.getReportId() + FILE_SUFFIX);
                String jsonStr = report.toJson().toString(2);

                try (FileOutputStream fos = new FileOutputStream(reportFile)) {
                    fos.write(jsonStr.getBytes(StandardCharsets.UTF_8));
                    fos.flush();
                }

                pruneOldReports();
                return true;
            } catch (Exception e) {
                return false;
            }
        }
    }

    public List<CrashReport> getAllReports() {
        synchronized (fileLock) {
            List<CrashReport> reports = new ArrayList<>();
            File dir = getReportsDir();
            File[] files = dir.listFiles((d, name) -> name.startsWith(FILE_PREFIX) && name.endsWith(FILE_SUFFIX));
            if (files == null || files.length == 0) return reports;

            for (File f : files) {
                CrashReport r = readReportFile(f);
                if (r != null) {
                    reports.add(r);
                }
            }

            // Sort newest first
            Collections.sort(reports, new Comparator<CrashReport>() {
                @Override
                public int compare(CrashReport o1, CrashReport o2) {
                    return Long.compare(o2.getTimestamp(), o1.getTimestamp());
                }
            });

            return reports;
        }
    }

    public CrashReport getLatestReport() {
        List<CrashReport> all = getAllReports();
        return all.isEmpty() ? null : all.get(0);
    }

    public boolean deleteReport(String reportId) {
        if (reportId == null) return false;
        synchronized (fileLock) {
            File dir = getReportsDir();
            File file = new File(dir, FILE_PREFIX + reportId + FILE_SUFFIX);
            if (file.exists()) {
                return file.delete();
            }
            return false;
        }
    }

    public void clearAllReports() {
        synchronized (fileLock) {
            File dir = getReportsDir();
            File[] files = dir.listFiles();
            if (files != null) {
                for (File f : files) {
                    //noinspection ResultOfMethodCallIgnored
                    f.delete();
                }
            }
        }
    }

    private CrashReport readReportFile(File file) {
        try (FileInputStream fis = new FileInputStream(file);
             InputStreamReader isr = new InputStreamReader(fis, StandardCharsets.UTF_8);
             BufferedReader reader = new BufferedReader(isr)) {

            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }

            JSONObject json = new JSONObject(sb.toString());
            return CrashReport.fromJson(json);
        } catch (Exception e) {
            return null;
        }
    }

    private void pruneOldReports() {
        File dir = getReportsDir();
        File[] files = dir.listFiles((d, name) -> name.startsWith(FILE_PREFIX) && name.endsWith(FILE_SUFFIX));
        if (files == null || files.length <= maxReports) return;

        // Sort ascending by lastModified (oldest first)
        Arrays.sort(files, Comparator.comparingLong(File::lastModified));

        int toRemove = files.length - maxReports;
        for (int i = 0; i < toRemove; i++) {
            //noinspection ResultOfMethodCallIgnored
            files[i].delete();
        }
    }
}
