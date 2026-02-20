package com.example.fitnesstracker.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;

import com.example.fitnesstracker.MainActivity;
import com.example.fitnesstracker.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class LoginDialogFragment extends DialogFragment {

    private FirebaseAuth mAuth;
    private EditText etEmail, etPassword;
    private Button btnLogin;

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
        etPassword = view.findViewById(R.id.etPassword);
        btnLogin = view.findViewById(R.id.btnLogin);

        btnLogin.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
                Toast.makeText(getContext(), "Please fill in all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            mAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(requireActivity(), task -> {
                        if (task.isSuccessful()) {
                            FirebaseUser user = mAuth.getCurrentUser();
                            if (user != null) {
                                Toast.makeText(getContext(), "Login Successful", Toast.LENGTH_SHORT).show();
                                dismiss();

                                // THE CRASH FIX: Safe Navigation Fallback
                                try {
                                    if (getParentFragment() != null) {
                                        NavHostFragment.findNavController(getParentFragment())
                                                .navigate(R.id.action_splashFragment_to_mainFragment);
                                    } else {
                                        Navigation.findNavController(requireActivity(), R.id.nav_host_fragment)
                                                .navigate(R.id.action_splashFragment_to_mainFragment);
                                    }
                                } catch (Exception e) {
                                    // Foolproof fallback: just restart the Main Activity
                                    Intent intent = new Intent(requireActivity(), MainActivity.class);
                                    startActivity(intent);
                                    requireActivity().finish();
                                }
                            }
                        } else {
                            Toast.makeText(getContext(), "Authentication failed.", Toast.LENGTH_SHORT).show();
                        }
                    });
        });
    }
}