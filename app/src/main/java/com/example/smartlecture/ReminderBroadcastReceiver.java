package com.example.smartlecture;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;


public class ReminderBroadcastReceiver extends BroadcastReceiver {

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