package com.example.fitnesstracker.auth;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.navigation.fragment.NavHostFragment;

import com.example.fitnesstracker.R;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.firebase.auth.FirebaseAuth;

public class LoginDialogFragment extends BottomSheetDialogFragment {

    public LoginDialogFragment() {
        super(R.layout.dialog_login);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {

        EditText etEmail = view.findViewById(R.id.etEmail);
        EditText etPass = view.findViewById(R.id.etPassword);
        TextView tvErr = view.findViewById(R.id.tvError);
        ProgressBar progress = view.findViewById(R.id.progress);
        Button btn = view.findViewById(R.id.btnDoLogin);

        btn.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            String pass = etPass.getText().toString();

            tvErr.setText("");
            progress.setVisibility(View.VISIBLE);
            btn.setEnabled(false);

            FirebaseAuth.getInstance()
                    .signInWithEmailAndPassword(email, pass)
                    .addOnSuccessListener(r -> {
                        dismiss();
                        NavHostFragment.findNavController(requireParentFragment())
                                .navigate(R.id.action_splashFragment_to_mainFragment);
                    })
                    .addOnFailureListener(e -> {
                        progress.setVisibility(View.GONE);
                        btn.setEnabled(true);
                        tvErr.setText(e.getMessage());
                    });
        });
    }
}
