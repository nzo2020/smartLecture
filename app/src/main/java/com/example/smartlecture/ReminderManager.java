package com.example.smartlecture;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import java.util.List;

/**
 * Manager class responsible for scheduling and canceling system-wide reminders.
 * It interfaces with the Android AlarmManager to ensure notifications are triggered
 * even if the application is not actively running.
 * @author Noa Zohar(nz2020@bs.amalnet.k12.il)
 * @version 1.0
 * @since 22.1.2026
 */
public class ReminderManager {
    /** Local list to track tasks currently managed in memory */
    private List<Task> managedTasks; // רשימה המנהלת את המשימות בזיכרון האפליקציה
    /** Context used to access system services */
    private Context context;        // הקשר האפליקציה הנדרש לגישה לשירותי מערכת

    /**
     * Constructs a ReminderManager with a specific context and task list.
     * @param context The application context.
     * @param managedTasks The list of tasks to be managed.
     */
    public ReminderManager(Context context, List<Task> managedTasks) {
        this.context = context;
        this.managedTasks = managedTasks;
    }

    /**
     * Schedules a new system alarm for the given task.
     * Uses unique request codes based on task IDs to prevent overlapping alarms.
     * @param task The task object containing title, location, and trigger time.
     */
    public void addTask(Task task) {
        long triggerTime = task.getRemindAt(); // שליפת זמן היעד לביצוע התזכורת

        // בדיקה: אם זמן התזכורת כבר עבר, אין טעם לקבוע התראה במערכת.
        if (triggerTime <= System.currentTimeMillis()) {
            return;
        }

        // הוספה לרשימה המנוהלת בזיכרון (אם קיימת) כדי לעקוב אחרי המשימות הפעילות
        if (managedTasks != null) {
            managedTasks.add(task);
        }

        // השגת שירות ה-AlarmManager של אנדרואיד
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        //יצירת Intent שיפעיל את ה-BroadcastReceiver שלנו.אנחנו מצרפים את הכותרת והמיקום כדי שה-Receiver יוכל להציג אותם בהתראה.

        Intent intent = new Intent(context, ReminderBroadcastReceiver.class);
        intent.putExtra("title", task.getTitle());
        intent.putExtra("location", task.getLocation());

        //requestCode: מזהה ייחודי עבור ה-PendingIntent.שימוש ב-hashCode של ה-ID הייחודי מבטיח שאם יש כמה תזכורות, הן לא ידרסו אחת את השנייה (אלא אם ה-ID זהה).
        int requestCode = task.getEventID().hashCode();

        //יצירת ה-PendingIntent:זהו "ייפוי כוח" שאנחנו נותנים למערכת ההפעלה להריץ את ה-Intent שלנו בשמנו בעתיד. FLAG_IMMUTABLE: דרישת אבטחה בגרסאות אנדרואיד חדשות.
        //
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // קביעת ההתראה במערכת הפעלה
        if (alarmManager != null) {
            //בדיקת גרסת מערכת:החל מגרסה 6 (Marshmallow), המערכת נכנסת למצב Doze (חיסכון בסוללה).setExactAndAllowWhileIdle מבטיח שהתזכורת תקפוץ גם אם המכשיר "ישן".
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent);
            } else {
                // לגרסאות ישנות יותר
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent);
            }
        }
    }

    /**
     * Cancels an existing alarm from the system and removes it from the local list.
     * @param task The task for which the reminder should be canceled.
     */
    //ביטול תזכורת קיימת מה-AlarmManager.משתמש באותו requestCode כדי למצוא את ה-PendingIntent הרלוונטי ולבטל אותו.
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
            alarmManager.cancel(pendingIntent); // ביטול הרישום במערכת ההפעלה
        }

        // הסרה מהרשימה המקומית בזיכרון
        if (managedTasks != null) {
            managedTasks.remove(task);
        }
    }
}