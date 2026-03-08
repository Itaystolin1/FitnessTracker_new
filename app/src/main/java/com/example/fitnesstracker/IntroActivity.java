package com.example.fitnesstracker;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.fitnesstracker.auth.LoginDialogFragment;
import com.example.fitnesstracker.auth.RegisterDialogFragment;
import com.google.firebase.auth.FirebaseAuth;

public class IntroActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_intro);

        Button btnLogin = findViewById(R.id.btnLogin);
        Button btnRegister = findViewById(R.id.btnRegister);
        Button btnInstructions = findViewById(R.id.btnInstructions); // New Button

        btnLogin.setOnClickListener(v -> {
            new LoginDialogFragment().show(getSupportFragmentManager(), "LoginDialog");
        });

        btnRegister.setOnClickListener(v -> {
            new RegisterDialogFragment().show(getSupportFragmentManager(), "RegisterDialog");
        });

        btnInstructions.setOnClickListener(v -> {
            new com.example.fitnesstracker.auth.InstructionsDialogFragment().show(getSupportFragmentManager(), "InstructionsDialog");
        });
    }
}