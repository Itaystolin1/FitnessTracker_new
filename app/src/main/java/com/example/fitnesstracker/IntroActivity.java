package com.example.fitnesstracker;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;
import com.example.fitnesstracker.auth.LoginDialogFragment;
import com.example.fitnesstracker.auth.RegisterDialogFragment;
import com.google.firebase.auth.FirebaseAuth;

public class IntroActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Check if user is already logged in
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_intro);

        Button btnLogin = findViewById(R.id.btnLogin);
        Button btnRegister = findViewById(R.id.btnRegister);

        btnLogin.setOnClickListener(v -> {
            new LoginDialogFragment().show(getSupportFragmentManager(), "LoginDialog");
        });

        btnRegister.setOnClickListener(v -> {
            new RegisterDialogFragment().show(getSupportFragmentManager(), "RegisterDialog");
        });
    }
}