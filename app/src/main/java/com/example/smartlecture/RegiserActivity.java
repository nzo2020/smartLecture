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

public class RegiserActivity extends AppCompatActivity {

    private EditText eTName, eTEmail, eTPass, eTConfirmPass;
    private TextView tvStatusMsg;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // אתחול הרכיבים - וודאי שה-IDs תואמים ל-XML שלך
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

        // בדיקת תקינות בסיסית
        if (name.isEmpty() || email.isEmpty() || pass.isEmpty() || confirmPass.isEmpty()) {
            tvStatusMsg.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
            tvStatusMsg.setText("Please fill all fields");
            return;
        }

        if (!pass.equals(confirmPass)) {
            tvStatusMsg.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
            tvStatusMsg.setText("Passwords do not match!");
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
                            Log.i("RegisterActivity", "createUserAuth:success");
                            FirebaseUser fbUser = refAuth.getCurrentUser();
                            String uid = fbUser.getUid();

                            // שלב 2: יצירת אובייקט משתמש לשמירה ב-Database
                            // המונים (totalLectures, completedTasks) יתאפסו בבנאי של User
                            User newUser = new User(uid, email, name);

                            // שלב 3: שמירה ב-Realtime Database תחת הענף users
                            refUsers.child(uid).setValue(newUser)
                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> dbTask) {
                                            pd.dismiss();
                                            if (dbTask.isSuccessful()) {
                                                Toast.makeText(RegiserActivity.this, "Registration Complete!", Toast.LENGTH_SHORT).show();

                                                // מעבר למסך הראשי (Dashboard)
                                                Intent intent = new Intent(RegiserActivity.this, DashboardActivity.class);
                                                startActivity(intent);
                                                finish();
                                            } else {
                                                tvStatusMsg.setText("Database Error: " + dbTask.getException().getMessage());
                                            }
                                        }
                                    });
                        } else {
                            pd.dismiss();
                            tvStatusMsg.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
                            handleError(task.getException());
                        }
                    }
                });
    }

    public void goToLogin(View view) {
        Intent intent = new Intent(this, LoginActivity.class);
        startActivity(intent);
        finish();
    }

    private void handleError(Exception exp) {
        if (exp instanceof FirebaseAuthWeakPasswordException) {
            tvStatusMsg.setText("Password too weak (min 6 chars).");
        } else if (exp instanceof FirebaseAuthUserCollisionException) {
            tvStatusMsg.setText("User already exists with this email.");
        } else if (exp instanceof FirebaseAuthInvalidCredentialsException) {
            tvStatusMsg.setText("Malformed email address.");
        } else if (exp instanceof FirebaseNetworkException) {
            tvStatusMsg.setText("Network error. Check connection.");
        } else {
            tvStatusMsg.setText("Error: " + (exp != null ? exp.getMessage() : "Unknown"));
        }
    }
}