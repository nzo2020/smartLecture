package com.example.smartlecture;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;


import com.google.android.material.button.MaterialButton;

/**
 * Entry point activity that provides the user with options to either log in or register.
 * This screen serves as the initial gateway for unauthenticated users.
 * @author Noa Zohar(nz2020@bs.amalnet.k12.il)
 * @version 1.0
 * @since 22.1.2026
 */
public class LoginOptionsActivity extends AppCompatActivity {

    /**
     * Initializes the activity, sets up the full-screen (EdgeToEdge) display,
     * and configures navigation listeners for login and registration.
     * @param savedInstanceState Bundle containing the activity's previously saved state.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // EdgeToEdge: מאפשר לממשק המשתמש לנצל את כל שטח המסך, כולל האזורים שמתחת לסורגי המערכת (StatusBar/NavigationBar).
        EdgeToEdge.enable(this);

        // קביעת עיצוב המסך לפי קובץ ה-XML המתאים
        setContentView(R.layout.activity_login_options);

        // אתחול כפתורי ה-Material Design באמצעות ה-ID שלהם מה-XML
        MaterialButton btnLogin = findViewById(R.id.btnGoogleLogin);
        MaterialButton btnRegister = findViewById(R.id.btnRegister);

        // הגדרת מאזין (Listener) ללחיצה על כפתור ההתחברות
        btnLogin.setOnClickListener(new View.OnClickListener() {
            /**
             * Navigates to the LoginActivity and closes the current activity.
             * @param v The view that was clicked.
             */
            @Override
            public void onClick(View v) {
                // יצירת Intent למעבר ממסך האפשרויות למסך ההתחברות (LoginActivity)
                Intent intent = new Intent(LoginOptionsActivity.this, LoginActivity.class);
                startActivity(intent);

                // finish(): סוגר את המסך הנוכחי כדי שהמשתמש לא יוכל לחזור אליו בלחיצה על כפתור ה-Back
                finish();
            }
        });

        // הגדרת מאזין (Listener) ללחיצה על כפתור ההרשמה
        btnRegister.setOnClickListener(new View.OnClickListener() {
            /**
             * Navigates to the RegisterActivity, allowing the user to return if needed.
             * @param v The view that was clicked.
             */
            @Override
            public void onClick(View v) {
                // יצירת Intent למעבר למסך ההרשמה (RegiserActivity)
                Intent intent = new Intent(LoginOptionsActivity.this, RegiserActivity.class);
                startActivity(intent);
                // כאן לא שמנו finish() כדי לאפשר למשתמש לחזור אחורה אם התחרט ורצה להתחבר במקום להירשם
            }
        });
    }
}