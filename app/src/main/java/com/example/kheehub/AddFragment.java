package com.example.kheehub;

import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class AddFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_add, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        EditText etName   = view.findViewById(R.id.et_name);
        EditText etFloor  = view.findViewById(R.id.et_floor);
        EditText etLat    = view.findViewById(R.id.et_lat);
        EditText etLng    = view.findViewById(R.id.et_lng);
        EditText etRating = view.findViewById(R.id.et_rating);
        EditText etHours  = view.findViewById(R.id.et_opening_hours);
        SwitchMaterial switchStatus = view.findViewById(R.id.switch_status);
        ChipGroup cgTags  = view.findViewById(R.id.cg_add_tags);
        Button btnAdd     = view.findViewById(R.id.btn_add);

        btnAdd.setOnClickListener(v -> {
            try {
                Toilet t = new Toilet();
                t.name   = etName.getText().toString().trim();
                t.floor  = etFloor.getText().toString().trim();
                t.lat    = Double.parseDouble(etLat.getText().toString());
                t.lng    = Double.parseDouble(etLng.getText().toString());
                t.rating = Double.parseDouble(etRating.getText().toString());
                t.openingHours = etHours.getText().toString().trim();

                // Set status: 1 for available (switch is on), 0 for unavailable
                t.status = switchStatus.isChecked() ? 1 : 0;

                // Loop through the ChipGroup and collect the selected tags
                List<String> selectedTags = new ArrayList<>();
                for (int i = 0; i < cgTags.getChildCount(); i++) {
                    Chip chip = (Chip) cgTags.getChildAt(i);
                    if (chip.isChecked()) {
                        // Convert to lowercase before saving to match database format
                        selectedTags.add(chip.getText().toString().toLowerCase());
                    }
                }
                t.tags = selectedTags;

                // Save to Firestore
                FirebaseFirestore.getInstance().collection("toilets")
                        .document(t.name).set(t)
                        .addOnSuccessListener(unused -> {
                            Toast.makeText(getContext(), "Toilet added successfully!", Toast.LENGTH_SHORT).show();
                            // Optional: Clear the form fields here after successful addition
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(getContext(), "Error adding toilet: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        });

            } catch (NumberFormatException e) {
                // Catch error if user leaves a number field blank and clicks add
                Toast.makeText(getContext(), "Please fill in all number fields correctly.", Toast.LENGTH_SHORT).show();
            }
        });
    }
}