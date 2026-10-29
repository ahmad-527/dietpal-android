package com.example.dietpal;

import static org.junit.Assert.*;
import static org.robolectric.Shadows.shadowOf;

import android.Manifest;
import android.app.AlarmManager;
import android.content.Context;
import com.example.dietpal.tracker.ReminderScheduler;
import java.util.Calendar;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowAlarmManager;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class ReminderSchedulingTest {
  @Test
  public void optInSchedulesFutureLocalTimeAndOffCancelsIt() {
    Context context = RuntimeEnvironment.getApplication();
    shadowOf(RuntimeEnvironment.getApplication())
        .grantPermissions(Manifest.permission.POST_NOTIFICATIONS);
    ReminderScheduler scheduler = new ReminderScheduler(context);
    scheduler.clear();
    assertTrue(scheduler.notificationsAllowed());
    ShadowAlarmManager alarms =
        shadowOf((AlarmManager) context.getSystemService(Context.ALARM_SERVICE));
    long before = System.currentTimeMillis();
    scheduler.set("water", true, 14, 25);
    assertEquals(1, alarms.getScheduledAlarms().size());
    ShadowAlarmManager.ScheduledAlarm alarm = alarms.getScheduledAlarms().get(0);
    assertTrue(alarm.isAllowWhileIdle());
    assertEquals(AlarmManager.RTC_WAKEUP, alarm.getType());
    assertTrue(alarm.getTriggerAtMs() > before);
    assertTrue(alarm.getTriggerAtMs() - before <= 25 * 60 * 60 * 1000L);
    Calendar next = Calendar.getInstance();
    next.setTimeInMillis(alarm.getTriggerAtMs());
    assertEquals(14, next.get(Calendar.HOUR_OF_DAY));
    assertEquals(25, next.get(Calendar.MINUTE));
    scheduler.refresh();
    assertEquals(1, alarms.getScheduledAlarms().size());
    scheduler.set("water", false, 14, 25);
    assertTrue(alarms.getScheduledAlarms().isEmpty());
  }
}
