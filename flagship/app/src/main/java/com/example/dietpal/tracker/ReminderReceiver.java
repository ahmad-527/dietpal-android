package com.example.dietpal.tracker;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import com.example.dietpal.FlagshipActivity;

public final class ReminderReceiver extends BroadcastReceiver {
  @Override
  public void onReceive(Context context, Intent intent) {
    ReminderScheduler scheduler = new ReminderScheduler(context);
    if (!"com.example.dietpal.REMINDER".equals(intent.getAction())) {
      scheduler.refresh();
      return;
    }
    String type = intent.getStringExtra("type");
    if (!java.util.Arrays.asList(ReminderScheduler.TYPES).contains(type)) return;
    if (scheduler.enabled(type) && scheduler.notificationsAllowed()) {
      String title =
          type.equals("meal")
              ? "Your food diary"
              : type.equals("water") ? "A moment for water" : "Your workout reminder";
      String text =
          type.equals("meal")
              ? "Take a moment to log your meals."
              : type.equals("water")
                  ? "Your water log is ready when you are."
                  : "Make time for the movement you planned.";
      Intent open =
          new Intent(
                  context, type.equals("workout") ? WorkoutsActivity.class : FlagshipActivity.class)
              .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
      int id = 1001 + java.util.Arrays.asList(ReminderScheduler.TYPES).indexOf(type);
      PendingIntent content =
          PendingIntent.getActivity(
              context, id, open, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
      android.app.Notification notification =
          new NotificationCompat.Builder(context, ReminderScheduler.CHANNEL)
              .setSmallIcon(android.R.drawable.ic_popup_reminder)
              .setContentTitle(title)
              .setContentText(text)
              .setContentIntent(content)
              .setAutoCancel(true)
              .build();
      try {
        NotificationManagerCompat.from(context).notify(id, notification);
      } catch (SecurityException permissionChanged) {
        /* The user can revoke permission at any time. */
      }
    }
    scheduler.schedule(type);
  }
}
