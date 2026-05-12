package com.example.smartlecture;

import static com.example.smartlecture.FBRef.refAuth;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

/**
 * Entry point of the application.
 * This activity displays a splash screen and determines the navigation flow
 * based on the user's authentication status and local persistence settings.
 * @author Noa Zohar(nz2020@bs.amalnet.k12.il)
 * @version 1.0
 * @since 22.1.2026
 */
public class SplashActivity extends AppCompatActivity {

    // אובייקט לשמירת נתונים קטנים מקומית על המכשיר (כמו הגדרת "זכור אותי")
    private SharedPreferences sharedPref;

    /**
     * Initializes the activity, sets up full-screen layout (EdgeToEdge),
     * and schedules the user status check after a short delay.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // הפעלת מצב EdgeToEdge המאפשר לתוכן להיפרס על כל המסך (כולל מתחת לסרגל הסטטוס)
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_splash);

        // הגדרת מאזין לשוליים של חלון המערכת (מבטיח שהתוכן לא יוסתר על ידי כפתורי הניווט/סרגל המערכת)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // אתחול ה-SharedPreferences תחת הקובץ "USER_SETTINGS"
        sharedPref = getSharedPreferences("USER_SETTINGS", MODE_PRIVATE);

        /**
         * Delays the transition to the next activity by 2000 milliseconds (2 seconds)
         * to provide visual branding and allow background checks to initialize.
         */
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                checkUserStatus(); // בדיקה לאן לנווט לאחר סיום הצגת מסך הפתיחה
            }
        }, 2000);
    }

    /**
     * Checks if a user is currently logged into Firebase and verifies
     * the "Stay Logged In" preference from local SharedPreferences.
     * Redirects to DashboardActivity or LoginOptionsActivity accordingly.
     */
    private void checkUserStatus() {
        // שליפת הערך הבוליאני המציין אם המשתמש בחר "הישאר מחובר" (ברירת מחדל: false)
        boolean isChecked = sharedPref.getBoolean("stayConnect", false);

        //תנאי הניווט:1. refAuth.getCurrentUser() != null -> האם יש משתמש שמחובר כרגע ב-Firebase.2. isChecked -> האם המשתמש אישר בהגדרות שלו להישאר מחובר.
        if (refAuth.getCurrentUser() != null && isChecked) {
            // אם המשתמש מחובר וביקש להישאר מחובר -> דלג ישר למסך הראשי
            Intent intent = new Intent(SplashActivity.this, DashboardActivity.class);
            startActivity(intent);
        } else {
            // בכל מקרה אחר (לא מחובר או לא ביקש להישאר) -> העבר למסך בחירת התחברות
            Intent intent = new Intent(SplashActivity.this, LoginOptionsActivity.class);
            startActivity(intent);
        }

        // סגירת ה-SplashActivity כדי שלא יהיה ניתן לחזור אליו בלחיצה על כפתור "Back"
        finish();
    }
}