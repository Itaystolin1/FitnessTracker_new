package com.example.fitnesstracker.auth;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.example.fitnesstracker.R;
import com.google.firebase.auth.FirebaseAuth;

public class SplashFragment extends Fragment {

    public SplashFragment() {
        super(R.layout.fragment_splash);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            NavHostFragment.findNavController(this)
                    .navigate(R.id.action_splashFragment_to_mainFragment);
            return;
        }

        Button btnLogin = view.findViewById(R.id.btnLogin);
        Button btnRegister = view.findViewById(R.id.btnRegister);

        btnLogin.setOnClickListener(v ->
                new LoginDialogFragment().show(getParentFragmentManager(), "login"));

        btnRegister.setOnClickListener(v ->
                new RegisterDialogFragment().show(getParentFragmentManager(), "register"));
    }
}
