package com.example.smartlecture;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import java.util.List;

/**
 * מחלקה האחראית על ניהול התראות המערכת (AlarmManager).
 * היא מקשרת בין המשימה (Task) לבין ה-BroadcastReceiver שיקפיץ את ההתראה למשתמש.
 */
public class ReminderManager {
    private List<Task> managedTasks;
    private Context context;

    public ReminderManager(Context context, List<Task> managedTasks) {
        this.context = context;
        this.managedTasks = managedTasks;
    }

    /**
     * הוספת תזכורת חדשה למערכת ההתראות של אנדרואיד.
     * @param task האובייקט המכיל את פרטי התזכורת (כותרת, מיקום וזמן).
     */
    public void addTask(Task task) {
        long triggerTime = task.getRemindAt();

        // בדיקה: אם זמן התזכורת כבר עבר, אין טעם לקבוע התראה במערכת.
        if (triggerTime <= System.currentTimeMillis()) {
            return;
        }

        // הוספה לרשימה המנוהלת בזיכרון (אם קיימת)
        if (managedTasks != null) {
            managedTasks.add(task);
        }

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        // יצירת Intent שיפעיל את ה-BroadcastReceiver שלנו
        Intent intent = new Intent(context, ReminderBroadcastReceiver.class);
        intent.putExtra("title", task.getTitle());
        intent.putExtra("location", task.getLocation());

        // שימוש ב-hashCode של ה-ID הייחודי כדי ליצור מזהה ייחודי להתראה (כדי שלא ידרסו אחת את השנייה)
        int requestCode = task.getEventID().hashCode();

        // יצירת ה-PendingIntent
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // קביעת ההתראה במערכת
        if (alarmManager != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                // בגרסאות אנדרואיד חדשות משתמשים ב-ExactAndAllowWhileIdle כדי שההתראה תעבוד גם במצב חיסכון בסוללה
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent);
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent);
            }
        }
    }

    /**
     * ביטול תזכורת קיימת מה-AlarmManager.
     * @param task המשימה שאת ההתראה שלה רוצים לבטל.
     */
    public void cancelReminder(Task task) {
        Intent intent = new Intent(context, ReminderBroadcastReceiver.class);
        int requestCode = task.getEventID().hashCode();

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) {
            alarmManager.cancel(pendingIntent);
        }

        // הסרה מהרשימה המקומית אם היא קיימת
        if (managedTasks != null) {
            managedTasks.remove(task);
        }
    }
}