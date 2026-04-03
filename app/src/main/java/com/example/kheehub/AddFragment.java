package com.example.kheehub;

import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.fragment.app.Fragment;
import com.google.firebase.firestore.FirebaseFirestore;

public class AddFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_add, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        EditText etName   = view.findViewById(R.id.et_name);
        EditText etFloor  = view.findViewById(R.id.et_floor);
        EditText etLat    = view.findViewById(R.id.et_lat);
        EditText etLng    = view.findViewById(R.id.et_lng);
        EditText etRating = view.findViewById(R.id.et_rating);
        Button btnAdd     = view.findViewById(R.id.btn_add);

        btnAdd.setOnClickListener(v -> {
            Toilet t = new Toilet();
            t.name   = etName.getText().toString();
            t.floor  = etFloor.getText().toString();
            t.lat    = Double.parseDouble(etLat.getText().toString());
            t.lng    = Double.parseDouble(etLng.getText().toString());
            t.rating = Double.parseDouble(etRating.getText().toString());

            FirebaseFirestore.getInstance().collection("toilets")
                    .document(t.name).set(t)
                    .addOnSuccessListener(unused ->
                            Toast.makeText(getContext(), "Toilet added!", Toast.LENGTH_SHORT).show());
        });
    }
}