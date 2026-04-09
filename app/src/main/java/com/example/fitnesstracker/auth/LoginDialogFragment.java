package com.example.fitnesstracker.auth;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.example.fitnesstracker.MainActivity;
import com.example.fitnesstracker.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;

public class LoginDialogFragment extends DialogFragment {

    private FirebaseAuth mAuth;
    private EditText etEmail, etPass;
    private Button btn;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_login, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mAuth = FirebaseAuth.getInstance();

        // Ensure these IDs match your dialog_login.xml
        etEmail = view.findViewById(R.id.etEmail);
        etPass = view.findViewById(R.id.etPassword);
        btn = view.findViewById(R.id.btnLogin);

        btn.setOnClickListener(v -> {
            String input = etEmail.getText().toString().trim().toLowerCase(); // This could be an email OR a username!
            String pass = etPass.getText().toString();

            if (input.isEmpty() || pass.isEmpty()) {
                Toast.makeText(requireContext(), "Please enter your credentials", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!input.contains("@")) {
                // They typed a username! Look it up in our phonebook.
                FirebaseDatabase.getInstance().getReference("usernames").child(input)
                        .get().addOnSuccessListener(snapshot -> {
                            if (snapshot.exists() && snapshot.getValue() != null) {
                                String actualEmail = snapshot.getValue(String.class);
                                loginWithFirebase(actualEmail, pass);
                            } else {
                                Toast.makeText(requireContext(), "Username not found", Toast.LENGTH_SHORT).show();
                            }
                        });
            } else {
                // They typed an email!
                loginWithFirebase(input, pass);
            }
        });
    }

    private void loginWithFirebase(String email, String pass) {
        FirebaseAuth.getInstance()
                .signInWithEmailAndPassword(email, pass)
                .addOnSuccessListener(r -> {
                    dismiss();
                    startActivity(new Intent(requireActivity(), MainActivity.class));
                    requireActivity().finish();
                })
                .addOnFailureListener(e -> {
                    // Show Firebase errors (like wrong password) as a popup!
                    Toast.makeText(requireContext(), e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    @Override
    public void onStart() {
        super.onStart();
        // This makes the default white pop-up box completely invisible!
        if (getDialog() != null && getDialog().getWindow() != null) {
            getDialog().getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            // Makes the pop-up wide enough so your text isn't cramped
            getDialog().getWindow().setLayout(android.view.ViewGroup.LayoutParams.MATCH_PARENT, android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }
}