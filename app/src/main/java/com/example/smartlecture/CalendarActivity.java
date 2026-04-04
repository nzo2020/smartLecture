package com.example.smartlecture;

import android.content.Intent;
import android.os.Bundle;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

public class CalendarActivity extends AppCompatActivity {

    private WebView webViewCalendar;
    private MaterialButton btnAddEvent, btnBackHome;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calendar); // וודאי שזה שם ה-XML שלך

        initViews();
        setupWebView();

        // כפתור הוספת אירוע - פותח את הדיאלוג או מסך הוספת תזכורת
        btnAddEvent.setOnClickListener(v -> {
            Intent intent = new Intent(this, AddReminderActivity.class);
            startActivity(intent);
        });

        // חזרה לדאשבורד
        btnBackHome.setOnClickListener(v -> finish());
    }

    private void initViews() {
        webViewCalendar = findViewById(R.id.webViewCalendar);
        btnAddEvent = findViewById(R.id.btnAddEvent);
        btnBackHome = findViewById(R.id.btnBackHome);
    }

    private void setupWebView() {
        WebSettings webSettings = webViewCalendar.getSettings();

        // הגדרות קריטיות להצגת לוחות שנה דינמיים
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true); // מאפשר שמירת נתונים מקומית של האתר
        webSettings.setBuiltInZoomControls(true);
        webSettings.setDisplayZoomControls(false);

        // גורם לקישורים להיפתח בתוך ה-WebView ולא בדפדפן החיצוני
        webViewCalendar.setWebViewClient(new WebViewClient());

        // כאן את יכולה לשים לינק ללוח שנה ציבורי או ל-Google Calendar של המשתמש
        // לצורך הדוגמה, נשתמש בלוח שנה גנרי של Google
        String calendarUrl = "https://calendar.google.com/calendar/u/0/r";

        webViewCalendar.loadUrl(calendarUrl);

        Toast.makeText(this, "Loading Calendar...", Toast.LENGTH_SHORT).show();
    }

    // מאפשר חזרה אחורה בתוך ה-WebView בעזרת כפתור ה-Back של הטלפון
    @Override
    public void onBackPressed() {
        if (webViewCalendar.canGoBack()) {
            webViewCalendar.goBack();
        } else {
            super.onBackPressed();
        }
    }
}