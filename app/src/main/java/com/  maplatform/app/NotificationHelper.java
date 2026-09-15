package com.maplatform.app;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import java.util.Calendar;
import java.util.concurrent.TimeUnit;

public class NotificationHelper {

    public static final String CHANNEL_ID = "ma_daily_reminder";
    public static final String CHANNEL_NAME = "تذكير التدريب اليومي";
    public static final String WORK_NAME = "ma_daily_work";

    private static final int NOTIF_ID = 2001;

    /** إنشاء قناة الإشعارات */
    public static void createChannel(Context ctx) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager nm =
                (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm == null) return;
            NotificationChannel existing = nm.getNotificationChannel(CHANNEL_ID);
            if (existing != null) return;

            NotificationChannel ch = new NotificationChannel(
                    CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT);
            ch.setDescription("تذكير يومي بمواصلة التدريب والتأهيل المهني");
            ch.enableVibration(true);
            ch.setShowBadge(true);
            nm.createNotificationChannel(ch);
        }
    }

    /** إظهار إشعار فوري */
    public static void showNow(Context ctx) {
        createChannel(ctx);
        NotificationManager nm =
            (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null) return;

        Intent intent = new Intent(ctx, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) flags |= PendingIntent.FLAG_IMMUTABLE;
        PendingIntent pi = PendingIntent.getActivity(ctx, 0, intent, flags);

        NotificationCompat.Builder b = new NotificationCompat.Builder(ctx, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("منصة M.A للتدريب والتأهيل")
                .setContentText("حان وقت جلسة التدريب اليومية — استمر في التقدم!")
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText("حان وقت جلسة التدريب اليومية. حتى 15 دقيقة يومياً تحقق فرقاً كبيراً في التأهيل المهني."))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .setContentIntent(pi)
                .setColor(0xFF1E3A8A);

        try { nm.notify(NOTIF_ID, b.build()); } catch (Exception ignored) {}
    }

    /** جدولة تذكير يومي (بعد 20 ساعة مثلاً) */
    public static void scheduleDailyReminder(Context ctx) {
        try {
            // حساب وقت التشغيل الأول (بعد ساعة من الآن، ثم كل 24 ساعة)
            long initialDelay = calculateInitialDelay();
            PeriodicWorkRequest req =
                new PeriodicWorkRequest.Builder(
                        ReminderWorker.class,
                        24, TimeUnit.HOURS,
                        15, TimeUnit.MINUTES)
                    .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
                    .build();

            WorkManager.getInstance(ctx).enqueueUniquePeriodicWork(
                    WORK_NAME,
                    ExistingPeriodicWorkPolicy.KEEP,
                    req);
        } catch (Exception ignored) {}
    }

    public static void cancelDailyReminder(Context ctx) {
        try {
            WorkManager.getInstance(ctx).cancelUniqueWork(WORK_NAME);
        } catch (Exception ignored) {}
    }

    private static long calculateInitialDelay() {
        // تذكير الساعة 8 مساءً
        Calendar now = Calendar.getInstance();
        Calendar target = Calendar.getInstance();
        target.set(Calendar.HOUR_OF_DAY, 20);
        target.set(Calendar.MINUTE, 0);
        target.set(Calendar.SECOND, 0);
        if (target.before(now)) target.add(Calendar.DAY_OF_MONTH, 1);
        return target.getTimeInMillis() - now.getTimeInMillis();
    }
}
