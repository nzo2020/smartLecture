package com.example.smartlecture;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;


import com.google.android.material.button.MaterialButton;


public class LoginOptionsActivity extends AppCompatActivity {

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