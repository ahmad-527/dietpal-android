package com.example.dietpal.tracker;

import android.Manifest;
import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import java.util.Calendar;

/** User-opted local reminders; no exact-alarm privilege or network service. */
public final class ReminderScheduler {
  public static final String CHANNEL = "dietpal_reminders";
  public static final String[] TYPES = {"meal", "water", "workout"};
  private final Context context;
  private final SharedPreferences settings;

  public ReminderScheduler(Context context) {
    this.context = context.getApplicationContext();
    settings = this.context.getSharedPreferences("dietpal_reminders", Context.MODE_PRIVATE);
  }

  public void channel() {
    NotificationManager manager =
        (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
    manager.createNotificationChannel(
        new NotificationChannel(
            CHANNEL, "DietPal reminders", NotificationManager.IMPORTANCE_DEFAULT));
  }

  public boolean notificationsAllowed() {
    channel();
    if (Build.VERSION.SDK_INT >= 33
        && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED) return false;
    NotificationChannel channel =
        ((NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE))
            .getNotificationChannel(CHANNEL);
    return NotificationManagerCompat.from(context).areNotificationsEnabled()
        && channel.getImportance() != NotificationManager.IMPORTANCE_NONE;
  }

  public boolean enabled(String type) {
    validate(type);
    return settings.getBoolean(type + "_enabled", false);
  }

  public int hour(String type) {
    validate(type);
    return settings.getInt(
        type + "_hour", type.equals("meal") ? 19 : type.equals("water") ? 12 : 18);
  }

  public int minute(String type) {
    validate(type);
    return settings.getInt(type + "_minute", 0);
  }

  public void set(String type, boolean enabled, int hour, int minute) {
    validate(type);
    if (hour < 0 || hour > 23 || minute < 0 || minute > 59)
      throw new IllegalArgumentException("Invalid time");
    settings
        .edit()
        .putBoolean(type + "_enabled", enabled)
        .putInt(type + "_hour", hour)
        .putInt(type + "_minute", minute)
        .apply();
    schedule(type);
  }

  public void schedule(String type) {
    validate(type);
    AlarmManager alarms = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
    int id = 1001 + java.util.Arrays.asList(TYPES).indexOf(type);
    Intent intent =
        new Intent(context, ReminderReceiver.class)
            .setAction("com.example.dietpal.REMINDER")
            .putExtra("type", type);
    PendingIntent pending =
        PendingIntent.getBroadcast(
            context, id, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    alarms.cancel(pending);
    if (!enabled(type) || !notificationsAllowed()) return;
    Calendar now = Calendar.getInstance(), next = (Calendar) now.clone();
    next.set(Calendar.HOUR_OF_DAY, hour(type));
    next.set(Calendar.MINUTE, minute(type));
    next.set(Calendar.SECOND, 0);
    next.set(Calendar.MILLISECOND, 0);
    if (!next.after(now)) next.add(Calendar.DATE, 1);
    alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next.getTimeInMillis(), pending);
  }

  public void refresh() {
    for (String type : TYPES) schedule(type);
  }

  public java.util.Map<String, ?> export() {
    return settings.getAll();
  }

  public void clear() {
    for (String type : TYPES) set(type, false, hour(type), minute(type));
    settings.edit().clear().apply();
    NotificationManagerCompat.from(context).cancelAll();
  }

  private void validate(String type) {
    if (!java.util.Arrays.asList(TYPES).contains(type))
      throw new IllegalArgumentException("Invalid reminder type");
  }
}
