package com.example.fitnesstracker.auth;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.example.fitnesstracker.MainActivity;
import com.example.fitnesstracker.R;
import com.example.fitnesstracker.util.StepPrefs;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;

public class RegisterDialogFragment extends DialogFragment {

    public RegisterDialogFragment() {
        super(R.layout.dialog_register);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        EditText etEmail = view.findViewById(R.id.etEmail);
        EditText etPass = view.findViewById(R.id.etPassword);
        EditText etHeight = view.findViewById(R.id.etHeight);
        EditText etWeight = view.findViewById(R.id.etWeight);
        Spinner spGender = view.findViewById(R.id.spGender);

        TextView tvErr = view.findViewById(R.id.tvError);
        Button btn = view.findViewById(R.id.btnDoRegister);

        // ===== Gender spinner setup =====
        ArrayAdapter<CharSequence> genderAdapter =
                ArrayAdapter.createFromResource(
                        requireContext(),
                        R.array.gender_options,
                        android.R.layout.simple_spinner_item
                );
        genderAdapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
        );
        spGender.setAdapter(genderAdapter);

        // ===== Register click =====
        btn.setOnClickListener(v -> {
            EditText etUser = view.findViewById(R.id.etUsername);
            String username = etUser.getText().toString().trim().toLowerCase();
            String email = etEmail.getText().toString().trim();
            String pass = etPass.getText().toString();

            if (username.isEmpty() || email.isEmpty() || pass.isEmpty()) {
                tvErr.setText("All fields required");
                return;
            }
            if (!username.matches("[a-zA-Z0-9_]+")) {
                tvErr.setText("Username can only use letters, numbers, and underscores");
                return;
            }

            // 1. Check if the username is already taken in the database
            FirebaseDatabase.getInstance().getReference("usernames").child(username)
                    .get().addOnCompleteListener(task -> {
                        if (task.isSuccessful() && task.getResult().exists()) {
                            tvErr.setText("Username is already taken!");
                        } else {
                            // 2. If it is available, register them!
                            registerUser(username, email, pass, etHeight, etWeight, spGender, tvErr);
                        }
                    });
        });
    }
    private void registerUser(String username, String email, String pass, EditText etHeight, EditText etWeight, Spinner spGender, TextView tvErr) {
        float height = Float.parseFloat(etHeight.getText().toString());
        float weight = Float.parseFloat(etWeight.getText().toString());
        String gender = spGender.getSelectedItem().toString();

        FirebaseAuth.getInstance()
                .createUserWithEmailAndPassword(email, pass)
                .addOnSuccessListener(r -> {
                    String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

                    // Save to their personal profile
                    java.util.Map<String, Object> user = new java.util.HashMap<>();
                    user.put("username", username);
                    user.put("email", email);
                    user.put("heightCm", height);
                    user.put("weightKg", weight);
                    user.put("gender", gender);
                    FirebaseDatabase.getInstance().getReference("users").child(uid).child("profile").setValue(user);

                    // Save to the public "Phonebook" so they can log in later!
                    FirebaseDatabase.getInstance().getReference("usernames").child(username).setValue(email);

                    StepPrefs.saveProfile(requireContext(), height, weight, gender);
                    dismiss();
                    startActivity(new Intent(requireActivity(), MainActivity.class));
                    requireActivity().finish();
                })
                .addOnFailureListener(e -> tvErr.setText(e.getMessage()));
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