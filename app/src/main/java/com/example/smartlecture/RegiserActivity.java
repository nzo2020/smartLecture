package com.example.smartlecture;

import static com.example.smartlecture.FBRef.refAuth;
import static com.example.smartlecture.FBRef.refUsers;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;

public class RegiserActivity extends AppCompatActivity {

    // רכיבי קלט וטקסט מה-XML
    private EditText eTName, eTEmail, eTPass, eTConfirmPass;
    private TextView tvStatusMsg;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // אתחול הרכיבים לפי ה-ID שלהם
        eTName = findViewById(R.id.etName);
        eTEmail = findViewById(R.id.etEmail);
        eTPass = findViewById(R.id.etPassword);
        eTConfirmPass = findViewById(R.id.etConfirmPassword);
        tvStatusMsg = findViewById(R.id.tvStatusMsg);
    }


    public void createUser(View view) {
        String name = eTName.getText().toString().trim();
        String email = eTEmail.getText().toString().trim();
        String pass = eTPass.getText().toString().trim();
        String confirmPass = eTConfirmPass.getText().toString().trim();

        // 1. בדיקה שכל השדות מלאים
        if (name.isEmpty() || email.isEmpty() || pass.isEmpty() || confirmPass.isEmpty()) {
            setStatusError("Please fill all fields");
            return;
        }

        // 2. בדיקה שהסיסמאות תואמות
        if (!pass.equals(confirmPass)) {
            setStatusError("Passwords do not match!");
            return;
        }

        // 3. בדיקה שהסיסמה מספיק ארוכה (דרישת מינימום של Firebase היא 6 תווים)
        if (pass.length() < 6) {
            setStatusError("Password must be at least 6 characters");
            return;
        }

        // הצגת חלונית המתנה בזמן התקשורת עם השרת
        ProgressDialog pd = new ProgressDialog(this);
        pd.setTitle("Connecting");
        pd.setMessage("Creating user and profile...");
        pd.setCancelable(false); // המשתמש לא יכול לסגור את החלונית בלחיצה בחוץ
        pd.show();


        refAuth.createUserWithEmailAndPassword(email, pass)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // המשתמש נוצר בהצלחה
                            FirebaseUser fbUser = refAuth.getCurrentUser();
                            if (fbUser != null) {
                                String uid = fbUser.getUid(); // ה-ID הייחודי שגוגל הצמידה למשתמש


                                UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                        .setDisplayName(name)
                                        .build();

                                fbUser.updateProfile(profileUpdates).addOnCompleteListener(profileTask -> {

                                    saveUserToDatabase(uid, email, name, pd);
                                });
                            }
                        } else {
                            // אם הרישום נכשל (למשל המשתמש כבר קיים)
                            pd.dismiss();
                            handleError(task.getException());
                        }
                    }
                });
    }


    private void saveUserToDatabase(String uid, String email, String name, ProgressDialog pd) {
        // יצירת מופע חדש של מחלקת User (שמכילה UID, אימייל ושם)
        User newUser = new User(uid, email, name);

        // שמירה בנתיב users/UID
        refUsers.child(uid).setValue(newUser)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> dbTask) {
                        pd.dismiss(); // סגירת חלונית ההמתנה
                        if (dbTask.isSuccessful()) {
                            Toast.makeText(RegiserActivity.this, "Welcome " + name + "!", Toast.LENGTH_SHORT).show();

                            // מעבר למסך הראשי וניקוי המחסנית (כדי שלא יהיה ניתן לחזור אחורה למסך הרישום)
                            Intent intent = new Intent(RegiserActivity.this, DashboardActivity.class);
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                            finish();
                        } else {
                            setStatusError("Database Error: " + dbTask.getException().getMessage());
                        }
                    }
                });
    }


    private void setStatusError(String msg) {
        tvStatusMsg.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
        tvStatusMsg.setText(msg);
    }


    public void goToLogin(View view) {
        Intent intent = new Intent(this, LoginActivity.class);
        startActivity(intent);
        finish();
    }


    private void handleError(Exception exp) {
        tvStatusMsg.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
        if (exp instanceof FirebaseAuthWeakPasswordException) {
            tvStatusMsg.setText("Password too weak.");
        } else if (exp instanceof FirebaseAuthUserCollisionException) {
            tvStatusMsg.setText("Email already registered.");
        } else if (exp instanceof FirebaseAuthInvalidCredentialsException) {
            tvStatusMsg.setText("Invalid email format.");
        } else if (exp instanceof FirebaseNetworkException) {
            tvStatusMsg.setText("No internet connection.");
        } else {
            tvStatusMsg.setText("Error: " + (exp != null ? exp.getMessage() : "Unknown error"));
        }
    }
}