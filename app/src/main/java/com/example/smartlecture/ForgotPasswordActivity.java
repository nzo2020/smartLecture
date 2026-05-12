package com.example.smartlecture;

import static com.example.smartlecture.FBRef.refAuth; // ייבוא סטטי של אובייקט ה-FirebaseAuth ממחלקת FBRef שיצרנו

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

/**
 * Activity for handling password reset requests through Firebase Authentication.
 * Allows users to request a reset link via their registered email address.
 * @author Noa Zohar(nz2020@bs.amalnet.k12.il)
 * @version 1.0
 * @since 22.1.2026
 */
public class ForgotPasswordActivity extends AppCompatActivity {

    /** Input field for the user's email address */
    private EditText eTEmail;
    /** TextView to display error or success messages to the user */
    private TextView tVMsg;

    /**
     * Initializes the activity and connects the UI components to their XML definitions.
     * @param savedInstanceState Bundle containing the activity's previously saved state.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        // קישור המשתנים לרכיבים ב-XML באמצעות ה-ID שלהם
        eTEmail = findViewById(R.id.etForgotEmail);
        tVMsg = findViewById(R.id.tvForgotMsg);
    }

    /**
     * Attempts to send a password reset email using Firebase Auth.
     * Validates that the email field is not empty before proceeding.
     * @param view The button view that triggered the reset request.
     */
    public void resetPassword(View view) {
        // שליפת כתובת האימייל מהשדה והסרת רווחים מיותרים (trim)
        String email = eTEmail.getText().toString().trim();

        // בדיקת תקינות (Validation) - וודוא שהשדה לא ריק לפני הפנייה ל-Firebase
        if (email.isEmpty()) {
            tVMsg.setTextColor(getResources().getColor(android.R.color.holo_red_dark)); // שינוי צבע הטקסט לאדום (שגיאה)
            tVMsg.setText("Please enter your email address");
            return;
        }


        refAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        // בדיקה האם הפעולה הצליחה מול שרתי Firebase
                        if (task.isSuccessful()) {
                            // הודעה חיובית למשתמש במידה והמייל נשלח
                            tVMsg.setTextColor(getResources().getColor(android.R.color.holo_green_dark)); // צבע ירוק (הצלחה)
                            tVMsg.setText("Reset link sent! Check your inbox.");

                            Toast.makeText(ForgotPasswordActivity.this, "Check your email", Toast.LENGTH_LONG).show();
                        } else {
                            // במקרה של שגיאה (למשל: אימייל לא רשום או פורמט לא תקין)
                            tVMsg.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
                            // שליפת הודעת השגיאה המדויקת מה-Exception של Firebase והצגתה למשתמש
                            tVMsg.setText("Error: " + task.getException().getMessage());
                        }
                    }
                });
    }

    /**
     * Closes the current activity and returns to the previous screen.
     * @param view The view that triggered the finish action.
     */
    public void finishActivity(View view) {
        finish();
    }
}