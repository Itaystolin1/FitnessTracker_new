package com.example.fitnesstracker.auth;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.navigation.fragment.NavHostFragment;

import com.example.fitnesstracker.R;
import com.example.fitnesstracker.util.StepPrefs;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;

public class RegisterDialogFragment extends BottomSheetDialogFragment {

    public RegisterDialogFragment() {
        super(R.layout.dialog_register);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        EditText etEmail  = view.findViewById(R.id.etEmail);
        EditText etPass   = view.findViewById(R.id.etPassword);
        EditText etHeight = view.findViewById(R.id.etHeight);
        EditText etWeight = view.findViewById(R.id.etWeight);
        Spinner spGender  = view.findViewById(R.id.spGender);

        TextView tvErr = view.findViewById(R.id.tvError);
        Button btn     = view.findViewById(R.id.btnDoRegister);

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
            try {
                String email = etEmail.getText().toString().trim();
                String pass  = etPass.getText().toString();

                float height = Float.parseFloat(etHeight.getText().toString());
                float weight = Float.parseFloat(etWeight.getText().toString());
                String gender = spGender.getSelectedItem().toString();

                if (email.isEmpty() || pass.isEmpty()) {
                    tvErr.setText("Email and password required");
                    return;
                }

                if (height < 100 || height > 250) {
                    tvErr.setText("Invalid height");
                    return;
                }

                if (weight < 30 || weight > 300) {
                    tvErr.setText("Invalid weight");
                    return;
                }

                FirebaseAuth.getInstance()
                        .createUserWithEmailAndPassword(email, pass)
                        .addOnSuccessListener(r -> {
                            String uid = FirebaseAuth.getInstance()
                                    .getCurrentUser()
                                    .getUid();

                            // Firebase profile
                            Map<String, Object> user = new HashMap<>();
                            user.put("email", email);
                            user.put("heightCm", height);
                            user.put("weightKg", weight);
                            user.put("gender", gender);
                            user.put("createdAt", System.currentTimeMillis());

                            FirebaseDatabase.getInstance().getReference()
                                    .child("users")
                                    .child(uid)
                                    .child("profile")
                                    .setValue(user);

                            // Local cache
                            StepPrefs.saveProfile(
                                    requireContext(),
                                    height,
                                    weight,
                                    gender
                            );

                            dismiss();
                            NavHostFragment
                                    .findNavController(requireParentFragment())
                                    .navigate(
                                            R.id.action_splashFragment_to_mainFragment
                                    );
                        })
                        .addOnFailureListener(e ->
                                tvErr.setText(e.getMessage())
                        );

            } catch (Exception e) {
                tvErr.setText("Please fill all fields correctly");
            }
        });
    }
}
