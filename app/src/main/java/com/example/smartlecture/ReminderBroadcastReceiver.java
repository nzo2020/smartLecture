package com.example.smartlecture;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

/**
 * Receiver responsible for handling scheduled reminder alarms.
 * When an alarm triggers, this class extracts event data from the intent and
 * displays a system notification to the user.
 * @author Noa Zohar(nz2020@bs.amalnet.k12.il)
 * @version 1.0
 * @since 22.1.2026
 */
public class ReminderBroadcastReceiver extends BroadcastReceiver {

    /**
     * Called when the BroadcastReceiver is receiving an Intent broadcast by the AlarmManager.
     * Extracts reminder details and triggers a high-priority notification if permissions are granted.
     * * @param context The Context in which the receiver is running.
     * @param intent The Intent containing the reminder's title and location.
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        // שליפת נתוני התזכורת שנשלחו יחד עם ה-Intent
        String title = intent.getStringExtra("title");
        String location = intent.getStringExtra("location");

        // הגדרת ערכי ברירת מחדל במידה והנתונים ריקים
        if (title == null) title = "תזכורת למידה!";
        String content = (location != null && !location.isEmpty()) ? "מיקום: " + location : "זמן לביצוע המשימה";


        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, "MyReminderChannel")
                .setSmallIcon(android.R.drawable.ic_popup_reminder) // אייקון השעון המובנה של אנדרואיד
                .setContentTitle(title)                            // כותרת ההתראה (שם המשימה)
                .setContentText(content)                           // תוכן ההתראה (מיקום/תיאור)
                .setPriority(NotificationCompat.PRIORITY_HIGH)     // עדיפות גבוהה כדי שההתראה תצוף למעלה
                .setAutoCancel(true);                              // סגירת ההתראה ברגע שהמשתמש לוחץ עליה

        // השגת מנהל ההתראות של התאימות לאחור (Backwards Compatibility)
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);


        if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED) {

            // הצגת ההתראה בפועל. משתמשים ב-CurrentTime כ-ID ייחודי כדי שהתראות חדשות לא ידרסו קודמות.
            notificationManager.notify((int) System.currentTimeMillis(), builder.build());
        }
    }
}