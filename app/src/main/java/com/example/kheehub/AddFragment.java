package com.example.kheehub;

import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.chip.ChipGroup;

import java.util.List;

public class AddFragment extends Fragment {

    private final ToiletRepository repository = ToiletRepository.getInstance();

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
        RadioButton rbAvailable = view.findViewById(R.id.rb_available);
        ChipGroup cgTags  = view.findViewById(R.id.cg_add_tags);
        Button btnAdd     = view.findViewById(R.id.btn_add);

        btnAdd.setOnClickListener(v -> {
            try {
                Toilet toilet = new Toilet();
                toilet.setName(etName.getText().toString().trim());
                toilet.setFloor(etFloor.getText().toString().trim());
                toilet.setLat(Double.parseDouble(etLat.getText().toString()));
                toilet.setLng(Double.parseDouble(etLng.getText().toString()));
                toilet.setRating(Double.parseDouble(etRating.getText().toString()));
                toilet.setOpeningHours(etHours.getText().toString().trim());
                toilet.setStatus(rbAvailable.isChecked() ? 1 : 0);

                List<String> selectedTags = ChipUtils.getSelectedTags(cgTags);
                toilet.setTags(selectedTags);

                repository.addToilet(
                        toilet,
                        unused -> Toast.makeText(getContext(),
                                getString(R.string.toilet_added),
                                Toast.LENGTH_SHORT).show(),
                        e -> Toast.makeText(getContext(),
                                getString(R.string.toilet_add_error, e.getMessage()),
                                Toast.LENGTH_LONG).show()
                );

            } catch (NumberFormatException e) {
                Toast.makeText(getContext(),
                        getString(R.string.add_number_fields_error),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }
}