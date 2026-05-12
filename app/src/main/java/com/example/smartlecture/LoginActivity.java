package com.example.smartlecture;

import static com.example.smartlecture.FBRef.refAuth; // שימוש בקישור ל-FirebaseAuth מהמחלקה המרכזית

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;

/**
 * Activity responsible for user authentication and login.
 * Manages user sessions using Firebase Auth and local preferences for persistent login.
 * @author Noa Zohar(nz2020@bs.amalnet.k12.il)
 * @version 1.0
 * @since 22.1.2026
 */
public class LoginActivity extends AppCompatActivity {

    /** Input fields for user credentials */
    private EditText eTEmail, eTPass;
    /** TextView to display status or error messages to the user */
    private TextView tVMsg;
    /** SharedPreferences to store the "stay connected" preference */
    private SharedPreferences sharedPref; // רכיב לשמירת נתונים קטנים במכשיר (כמו מצב "זכור אותי")

    /**
     * Initializes the activity and binds the UI components.
     * @param savedInstanceState Bundle containing the activity's previously saved state.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // אתחול הרכיבים על ידי קישורם ל-ID ב-XML
        eTEmail = findViewById(R.id.etUsername);
        eTPass = findViewById(R.id.etPassword);
        tVMsg = findViewById(R.id.tvMsg);

        // יצירת/פתיחת קובץ הגדרות מקומי בשם "USER_SETTINGS"
        sharedPref = getSharedPreferences("USER_SETTINGS", MODE_PRIVATE);
    }

    /**
     * Checks if a user is already logged in and has requested to stay connected.
     * If true, navigates automatically to the Dashboard.
     */
    @Override
    protected void onStart() {
        super.onStart();
        //בדיקה אוטומטית בעת עליית המסך: אם קיים משתמש מחובר ב-Firebase והמשתמש בחר בעבר "להישאר מחובר", נעבור ישירות ל-Dashboard בלי לבקש פרטים שוב.

        boolean isChecked = sharedPref.getBoolean("stayConnect", false);
        if (refAuth.getCurrentUser() != null && isChecked) {
            goToDashboard();
        }
    }

    /**
     * Validates input and attempts to sign in the user via Firebase.
     * Displays a progress dialog during the network request.
     * @param view The view that triggered the login attempt.
     */
    public void loginUser(View view) {
        String email = eTEmail.getText().toString().trim();
        String pass = eTPass.getText().toString().trim();

        // בדיקה בסיסית שהשדות אינם ריקים
        if (email.isEmpty() || pass.isEmpty()) {
            tVMsg.setText("Please fill all fields");
            return;
        }

        // יצירת חלונית המתנה (ProgressDialog) כדי שהמשתמש ידע שהפעולה מתבצעת
        ProgressDialog pd = new ProgressDialog(this);
        pd.setMessage("Logging in...");
        pd.setCancelable(false); // מניעת סגירה של החלונית בלחיצה מחוץ לה
        pd.show();

        // שליחת בקשת התחברות ל-Firebase Authentication
        refAuth.signInWithEmailAndPassword(email, pass)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        pd.dismiss(); // סגירת חלונית ההמתנה בסיום הפעולה

                        if (task.isSuccessful()) {
                            // אם ההתחברות הצליחה: נשמור במכשיר שהמשתמש ביקש להישאר מחובר
                            sharedPref.edit().putBoolean("stayConnect", true).apply();
                            goToDashboard();
                        } else {
                            // אם נכשלה: נשלח את השגיאה לטיפול ייעודי
                            handleLoginErrors(task.getException());
                        }
                    }
                });
    }

    /**
     * Analyzes authentication exceptions and provides descriptive error messages in the UI.
     * @param exp The exception caught during the login attempt.
     */
    private void handleLoginErrors(Exception exp) {
        if (exp instanceof FirebaseAuthInvalidUserException) {
            // מקרה שבו האימייל לא קיים במערכת
            tVMsg.setText("No account found with this email.");
        } else if (exp instanceof FirebaseAuthInvalidCredentialsException) {
            // מקרה של סיסמה שגויה או אימייל לא תקין
            tVMsg.setText("Wrong password or email.");
        } else if (exp instanceof FirebaseNetworkException) {
            // בעיית חיבור לאינטרנט
            tVMsg.setText("Check your internet connection.");
        } else {
            // שגיאה כללית אחרת
            tVMsg.setText("Error: " + exp.getMessage());
        }
    }

    /**
     * Navigates to the DashboardActivity and finishes the current login screen.
     */
    private void goToDashboard() {
        Intent intent = new Intent(this, DashboardActivity.class);
        startActivity(intent);
        finish(); // השמדת ה-Activity הנוכחי כדי שלא יהיה ניתן לחזור אליו בלחיצה על Back
    }

    /**
     * Navigates to the registration screen.
     * @param view The view that triggered the navigation.
     */
    public void goToRegister(View view) {
        startActivity(new Intent(this, RegiserActivity.class));
    }

    /**
     * Navigates to the forgot password screen.
     * @param view The view that triggered the navigation.
     */
    public void goToForgotPass(View view) {
        startActivity(new Intent(this, ForgotPasswordActivity.class));
    }
}