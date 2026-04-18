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

    public ReminderManager(Context context, List<Task> managedTasks) {
        this.context = context;
        this.managedTasks = managedTasks;
    }

    public void addTask(Task task) {
        if (managedTasks != null) {
            managedTasks.add(task);
        }

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, ReminderBroadcastReceiver.class);

        // שליחת הנתונים ל-Receiver
        intent.putExtra("title", task.getTitle());
        intent.putExtra("location", task.getLocation());

        int requestCode = task.getEventID().hashCode();

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        if (alarmManager != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, task.getReminder(), pendingIntent);
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, task.getReminder(), pendingIntent);
            }
        }
    }
}