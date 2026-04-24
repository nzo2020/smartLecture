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

    private EditText eTName, eTEmail, eTPass, eTConfirmPass;
    private TextView tvStatusMsg;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // אתחול הרכיבים
        eTName = findViewById(R.id.etName);
        eTEmail = findViewById(R.id.etEmail);
        eTPass = findViewById(R.id.etPassword);
        eTConfirmPass = findViewById(R.id.etConfirmPassword);
        tvStatusMsg = findViewById(R.id.tvStatusMsg);
    }

    /**
     * פונקציה ליצירת משתמש חדש
     */
    public void createUser(View view) {
        String name = eTName.getText().toString().trim();
        String email = eTEmail.getText().toString().trim();
        String pass = eTPass.getText().toString().trim();
        String confirmPass = eTConfirmPass.getText().toString().trim();

        // בדיקות תקינות קלט
        if (name.isEmpty() || email.isEmpty() || pass.isEmpty() || confirmPass.isEmpty()) {
            setStatusError("Please fill all fields");
            return;
        }

        if (!pass.equals(confirmPass)) {
            setStatusError("Passwords do not match!");
            return;
        }

        if (pass.length() < 6) {
            setStatusError("Password must be at least 6 characters");
            return;
        }

        ProgressDialog pd = new ProgressDialog(this);
        pd.setTitle("Connecting");
        pd.setMessage("Creating user and profile...");
        pd.setCancelable(false);
        pd.show();

        // שלב 1: יצירת משתמש ב-Firebase Authentication
        refAuth.createUserWithEmailAndPassword(email, pass)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            FirebaseUser fbUser = refAuth.getCurrentUser();
                            if (fbUser != null) {
                                String uid = fbUser.getUid();

                                // שלב 2: עדכון ה-DisplayName ב-Firebase Auth (חשוב ל-RecordingService)
                                UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                        .setDisplayName(name)
                                        .build();

                                fbUser.updateProfile(profileUpdates).addOnCompleteListener(profileTask -> {
                                    // שלב 3: יצירת אובייקט משתמש ושמירה ב-Realtime Database
                                    saveUserToDatabase(uid, email, name, pd);
                                });
                            }
                        } else {
                            pd.dismiss();
                            handleError(task.getException());
                        }
                    }
                });
    }

    /**
     * שמירת נתוני המשתמש ב-Database
     */
    private void saveUserToDatabase(String uid, String email, String name, ProgressDialog pd) {
        User newUser = new User(uid, email, name);

        refUsers.child(uid).setValue(newUser)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> dbTask) {
                        pd.dismiss();
                        if (dbTask.isSuccessful()) {
                            Toast.makeText(RegiserActivity.this, "Welcome " + name + "!", Toast.LENGTH_SHORT).show();

                            // מעבר לדאשבורד וסגירת מסך הרישום
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

    /**
     * טיפול בשגיאות נפוצות של Firebase Auth
     */
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