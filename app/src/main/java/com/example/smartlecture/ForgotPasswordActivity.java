package com.example.smartlecture;

import static com.example.smartlecture.FBRef.refAuth;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

public class ForgotPasswordActivity extends AppCompatActivity {

    private EditText eTEmail;
    private TextView tVMsg;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        eTEmail = findViewById(R.id.etForgotEmail);
        tVMsg = findViewById(R.id.tvForgotMsg);
    }

    public void resetPassword(View view) {
        String email = eTEmail.getText().toString().trim();

        if (email.isEmpty()) {
            tVMsg.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
            tVMsg.setText("Please enter your email address");
            return;
        }

        refAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            tVMsg.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
                            tVMsg.setText("Reset link sent! Check your inbox.");

                            Toast.makeText(ForgotPasswordActivity.this, "Check your email", Toast.LENGTH_LONG).show();
                        } else {
                            tVMsg.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
                            tVMsg.setText("Error: " + task.getException().getMessage());
                        }
                    }
                });
    }


    public void finishActivity(View view) {
        finish();
    }
}