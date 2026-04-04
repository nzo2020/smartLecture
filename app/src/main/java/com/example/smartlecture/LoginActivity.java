package com.example.smartlecture;

import static com.example.smartlecture.FBRef.refAuth;

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

public class LoginActivity extends AppCompatActivity {

    private EditText eTEmail, eTPass;
    private TextView tVMsg;
    private SharedPreferences sharedPref;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // שימי לב: IDs תואמים בדיוק ל-XML ששלחת
        eTEmail = findViewById(R.id.etUsername);
        eTPass = findViewById(R.id.etPassword);
        tVMsg = findViewById(R.id.tvMsg);

        sharedPref = getSharedPreferences("USER_SETTINGS", MODE_PRIVATE);
    }

    @Override
    protected void onStart() {
        super.onStart();
        // בדיקה אם המשתמש כבר מחובר
        boolean isChecked = sharedPref.getBoolean("stayConnect", false);
        if (refAuth.getCurrentUser() != null && isChecked) {
            goToDashboard();
        }
    }

    public void loginUser(View view) {
        String email = eTEmail.getText().toString().trim();
        String pass = eTPass.getText().toString().trim();

        if (email.isEmpty() || pass.isEmpty()) {
            tVMsg.setText("Please fill all fields");
            return;
        }

        ProgressDialog pd = new ProgressDialog(this);
        pd.setMessage("Logging in...");
        pd.setCancelable(false);
        pd.show();

        refAuth.signInWithEmailAndPassword(email, pass)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        pd.dismiss();
                        if (task.isSuccessful()) {
                            // שומרים שהמשתמש מחובר
                            sharedPref.edit().putBoolean("stayConnect", true).apply();
                            goToDashboard();
                        } else {
                            handleLoginErrors(task.getException());
                        }
                    }
                });
    }

    private void handleLoginErrors(Exception exp) {
        if (exp instanceof FirebaseAuthInvalidUserException) {
            tVMsg.setText("No account found with this email.");
        } else if (exp instanceof FirebaseAuthInvalidCredentialsException) {
            tVMsg.setText("Wrong password or email.");
        } else if (exp instanceof FirebaseNetworkException) {
            tVMsg.setText("Check your internet connection.");
        } else {
            tVMsg.setText("Error: " + exp.getMessage());
        }
    }

    private void goToDashboard() {
        Intent intent = new Intent(this, DashboardActivity.class);
        startActivity(intent);
        finish();
    }

    public void goToRegister(View view) {
        startActivity(new Intent(this, RegiserActivity.class));
    }

    public void goToForgotPass(View view) {
        startActivity(new Intent(this, ForgotPasswordActivity.class));
    }
}