package com.example.fitnesstracker.main;
import android.app.Dialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.DialogFragment;

import com.example.fitnesstracker.IntroActivity;
import com.example.fitnesstracker.R;
import com.example.fitnesstracker.util.StepPrefs;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Locale;

public class ProfileDialogFragment extends DialogFragment {

    private ImageView ivDialogProfilePic;
    private ActivityResultLauncher<String> imagePickerLauncher;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Setup the gallery picker
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null && getContext() != null) {
                        // SAVE A PERMANENT COPY INSTEAD OF JUST THE URI
                        String localPath = saveImageLocally(uri);
                        if (localPath != null) {
                            StepPrefs.setProfilePicUri(requireContext(), localPath);
                            ivDialogProfilePic.setImageURI(Uri.fromFile(new java.io.File(localPath)));
                        }
                    }
                });
    }
    // Keep this existing method!
    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        if (dialog.getWindow() != null) {
            // Removes default white box so curved edges show
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
        return dialog;
    }
    private String saveImageLocally(Uri uri) {
        try {
            java.io.InputStream is = requireContext().getContentResolver().openInputStream(uri);
            java.io.File file = new java.io.File(requireContext().getFilesDir(), "profile_avatar.jpg");
            java.io.FileOutputStream fos = new java.io.FileOutputStream(file);
            byte[] buffer = new byte[1024];
            int len;
            while ((len = is.read(buffer)) > 0) fos.write(buffer, 0, len);
            fos.close();
            is.close();
            return file.getAbsolutePath();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    // --- ADD THIS METHOD to force the full width! ---
    @Override
    public void onResume() {
        super.onResume();
        if (getDialog() != null && getDialog().getWindow() != null) {
            // Force the dialog window to fill the full width of the screen!
            getDialog().getWindow().setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
        }
    }
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_profile, container, false);

        ivDialogProfilePic = view.findViewById(R.id.ivDialogProfilePic);
        TextView tvProfileUsername = view.findViewById(R.id.tvProfileUsername);
        TextView tvProfileEmail = view.findViewById(R.id.tvProfileEmail);
        TextView tvProfileWeight = view.findViewById(R.id.tvProfileWeight);
        TextView tvProfileHeight = view.findViewById(R.id.tvProfileHeight);
        Button btnProfileClose = view.findViewById(R.id.btnProfileClose);
        Button btnProfileLogout = view.findViewById(R.id.btnProfileLogout);

        // Load local preferences
        tvProfileWeight.setText(String.format(Locale.US, "%.1f kg", StepPrefs.getWeightKg(requireContext())));
        tvProfileHeight.setText(String.format(Locale.US, "%.1f cm", StepPrefs.getHeightCm(requireContext())));


        // Load existing Profile Pic
        String savedUri = StepPrefs.getProfilePicUri(requireContext());

        // POISON PILL FIX: Delete old expired temporary URIs from previous testing
        if (savedUri.startsWith("content://")) {
            StepPrefs.setProfilePicUri(requireContext(), "");
            savedUri = "";
        }

        if (!savedUri.isEmpty()) {
            java.io.File imgFile = new java.io.File(savedUri);
            if (imgFile.exists()) {
                ivDialogProfilePic.setImageURI(Uri.fromFile(imgFile));
            }
        }

        // Fetch User Data from Firebase
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            tvProfileEmail.setText(user.getEmail());
            DatabaseReference profileRef = FirebaseDatabase.getInstance().getReference("users").child(user.getUid()).child("profile");
            profileRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.hasChild("username")) {
                        tvProfileUsername.setText(snapshot.child("username").getValue(String.class));
                    } else {
                        tvProfileUsername.setText("Legacy User");
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    tvProfileUsername.setText("Legacy User");
                }
            });
        }

        // Click Listeners
        ivDialogProfilePic.setOnClickListener(v -> imagePickerLauncher.launch("image/*"));
        btnProfileClose.setOnClickListener(v -> dismiss());
        btnProfileLogout.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            Intent intent = new Intent(requireContext(), IntroActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });

        return view;
    }
}