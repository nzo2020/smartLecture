package com.example.smartlecture;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import java.util.List;

public class ReminderManager {
    private List<Task> managedTasks;
    private Context context;

    // בנאי לקבלת הקשר (Context) לצורך עבודה עם מערכת ההתראות
    public ReminderManager(Context context, List<Task> managedTasks) {
        this.context = context;
        this.managedTasks = managedTasks;
    }

    public void addTask(Task task) {
        // 1. לוגיקה להוספת Task לרשימה המנוהלת
        if (managedTasks != null) {
            managedTasks.add(task);
        }

        // 2. לוגיקה להגדרת AlarmManager
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        // יצירת Intent שיפעיל BroadcastReceiver בעת הגעת הזמן
        Intent intent = new Intent(context, ReminderBroadcastReceiver.class);
        intent.putExtra("title", task.getTitle());
        intent.putExtra("location", task.getLocation());
        intent.putExtra("eventID", task.getEventID());

        // יצירת PendingIntent - מזהה ייחודי לפי ה-HashCode של ה-ID של המשימה
        int requestCode = task.getEventID().hashCode();
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // הגדרת ההתראה בזמן המדויק (remindAt)
        if (alarmManager != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                // מאפשר התראה גם כשהמכשיר במצב שינה (Doze mode)
                alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        task.getRemindAt(),
                        pendingIntent
                );
            } else {
                alarmManager.setExact(
                        AlarmManager.RTC_WAKEUP,
                        task.getRemindAt(),
                        pendingIntent
                );
            }
        }
    }
}