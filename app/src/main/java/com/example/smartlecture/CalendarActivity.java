package com.example.smartlecture;

import android.content.Intent;
import android.os.Bundle;
import android.provider.CalendarContract;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

public class CalendarActivity extends AppCompatActivity {

    private WebView webViewCalendar;
    private MaterialButton btnAddEvent, btnBackHome;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calendar);

        initViews();
        setupWebView();

        // מאזין לכפתור הוספת אירוע שפותח את הדיאלוג
        btnAddEvent.setOnClickListener(v -> showAddEventDialog());

        btnBackHome.setOnClickListener(v -> finish());
    }

    private void initViews() {
        webViewCalendar = findViewById(R.id.webViewCalendar);
        btnAddEvent = findViewById(R.id.btnAddEvent);
        btnBackHome = findViewById(R.id.btnBackHome);
    }

    private void setupWebView() {
        WebSettings webSettings = webViewCalendar.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webViewCalendar.setWebViewClient(new WebViewClient());

        // טעינת לוח השנה לתצוגה
        webViewCalendar.loadUrl("https://calendar.google.com/calendar/u/0/r");
    }

    private void showAddEventDialog() {
        // 1. ניפוח ה-XML של הדיאלוג (ה-CardView שעיצבת)
        View dialogView = getLayoutInflater().inflate(R.layout.activity_dialog_add_event, null);

        // 2. קישור רכיבי הקלט מתוך הדיאלוג
        EditText etName = dialogView.findViewById(R.id.etEventName);
        EditText etLocation = dialogView.findViewById(R.id.etEventLocation);
        EditText etDescription = dialogView.findViewById(R.id.etEventDescription);

        // 3. יצירת הדיאלוג
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .setPositiveButton("הוסף ליומן", (d, which) -> {
                    String title = etName.getText().toString();

                    if (title.isEmpty()) {
                        Toast.makeText(this, "חובה להזין כותרת", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // שליחת הנתונים לאפליקציית לוח השנה החיצונית
                    sendToExternalCalendar(title, etLocation.getText().toString(), etDescription.getText().toString());
                })
                .setNegativeButton("ביטול", null)
                .create();

        // הגדרת רקע שקוף כדי לראות את הפינות המעוגלות של ה-CardView
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        dialog.show();
    }

    private void sendToExternalCalendar(String title, String location, String description) {
        // יצירת ה-Intent להוספת אירוע
        Intent intent = new Intent(Intent.ACTION_INSERT);
        intent.setData(CalendarContract.Events.CONTENT_URI);

        // הכנסת הנתונים שנקלטו בדיאלוג לתוך ה-Intent
        intent.putExtra(CalendarContract.Events.TITLE, title);
        intent.putExtra(CalendarContract.Events.EVENT_LOCATION, location);
        intent.putExtra(CalendarContract.Events.DESCRIPTION, description);

        // הגדרה כאירוע לכל היום (ניתן להוסיף בחירת זמן בהמשך)
        intent.putExtra(CalendarContract.Events.ALL_DAY, true);

        // בדיקה שיש אפליקציית לוח שנה והפעלה
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivity(intent);
        } else {
            Toast.makeText(this, "לא נמצאה אפליקציית לוח שנה", Toast.LENGTH_SHORT).show();
        }
    }
}