package com.example.smartlecture;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;
import androidx.core.app.NotificationCompat;

public class NotificationHelper {
    public static void showSummaryReadyNotification(Context context, String teacher) {
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        String channelId = "Gemini_Completion_Channel_v2";

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // הקפדה על IMPORTANCE_HIGH - זה מה שגורם לזה לקפוץ מלמעלה
            NotificationChannel channel = new NotificationChannel(
                    channelId,
                    "AI Analysis Completion",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("התראות על סיום סיכומי Gemini");
            channel.enableVibration(true);
            manager.createNotificationChannel(channel);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.logo) // ודאי שיש לך קובץ logo ב-drawable
                .setContentTitle("הסיכום מוכן! ✨")
                .setContentText("הסיכום להרצאה של " + teacher + " הסתיים בהצלחה.")
                // הגדרת PRIORITY_HIGH חיונית למכשירים ישנים יותר
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                // הוספתDefaults גורמת לזה להשמיע סאונד ולרטוט כמו ב-BroadcastReceiver שלך
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setAutoCancel(true);

        // שימוש ב-ID ייחודי (כמו שעשית ב-Broadcast עם המילישניות)
        manager.notify((int) System.currentTimeMillis(), builder.build());
    }
}