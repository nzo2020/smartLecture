package com.example.smartlecture;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;
import androidx.core.app.NotificationCompat;

/**
 * Helper class designed to manage and display system notifications.
 * Specifically handles notifications related to the completion of AI-generated lecture summaries.
 * @author Noa Zohar(nz2020@bs.amalnet.k12.il)
 * @version 1.0
 * @since 22.1.2026
 */
public class NotificationHelper {

    /**
     * Creates and displays a high-priority notification to inform the user that a summary is ready.
     * Handles notification channel creation for Android Oreo (API 26) and above.
     * * @param context The application context.
     * @param teacher The name of the lecturer associated with the completed summary.
     */
    public static void showSummaryReadyNotification(Context context, String teacher) {
        // השגת שירות ה-NotificationManager של מערכת האנדרואיד
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        // מזהה ייחודי לערוץ ההתראות (נדרש החל מאנדרואיד 8.0)
        String channelId = "Gemini_Completion_Channel_v2";

        // בדיקה אם גרסת המכשיר היא אנדרואיד 8.0 (Oreo) ומעלה
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            NotificationChannel channel = new NotificationChannel(
                    channelId,
                    "AI Analysis Completion",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("התראות על סיום סיכומי Gemini"); // תיאור הערוץ בהגדרות המכשיר
            channel.enableVibration(true); // הפעלת רטט
            manager.createNotificationChannel(channel); // רישום הערוץ במערכת
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.logo) // הגדרת האייקון הקטן שיופיע בשורת הסטטוס
                .setContentTitle("הסיכום מוכן! ✨") // כותרת ההתראה
                .setContentText("הסיכום להרצאה של " + teacher + " הסתיים בהצלחה.") // תוכן ההתראה
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                // הגדרת ברירת מחדל של סאונד, רטט ואורות
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                // גורם להתראה להיעלם ברגע שהמשתמש לוחץ עליה
                .setAutoCancel(true);

        //הצגת ההתראה בפועל. שימוש ב-System.currentTimeMillis() כ-ID מאפשר להציג מספר התראות במקביל במקום שהתראה חדשה תדרוס את הקודמת

        manager.notify((int) System.currentTimeMillis(), builder.build());
    }
}