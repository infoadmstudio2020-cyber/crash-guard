package com.roni.library.crashguard;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import androidx.core.app.NotificationCompat;

/**
 * Handles system notification alerts when crashes occur in background services or non-UI threads.
 */
public class CrashNotificationManager {

    private static final String CHANNEL_ID = "crashguard_diagnostics_channel";
    private static final int NOTIFICATION_ID = 911911;

    public static void showCrashNotification(Context context, CrashReport report) {
        if (context == null || report == null) return;

        try {
            NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm == null) return;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    context.getString(R.string.crashguard_notification_channel_name),
                    NotificationManager.IMPORTANCE_HIGH
                );
                channel.setDescription(context.getString(R.string.crashguard_notification_channel_desc));
                channel.enableVibration(true);
                nm.createNotificationChannel(channel);
            }

            Intent intent = new Intent(context, DefaultCrashActivity.class);
            intent.putExtra(DefaultCrashActivity.EXTRA_CRASH_REPORT, report);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

            int flags = PendingIntent.FLAG_UPDATE_CURRENT;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                flags |= PendingIntent.FLAG_IMMUTABLE;
            }

            PendingIntent pi = PendingIntent.getActivity(context, 0, intent, flags);

            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.stat_notify_error)
                .setContentTitle(context.getString(R.string.crashguard_notification_title))
                .setContentText(report.getExceptionClass() + ": " + report.getExceptionMessage())
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pi);

            nm.notify(NOTIFICATION_ID, builder.build());
        } catch (Exception ignored) {
            // Notifications may be restricted by permission on Android 13+
        }
    }
}
