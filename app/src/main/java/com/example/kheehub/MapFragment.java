package com.example.kheehub;

import android.os.Bundle;
import android.view.*;
import android.widget.LinearLayout;     //bottom-sheet
import android.widget.TextView;         //bottom-sheet
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;    //bottom-sheet
import androidx.fragment.app.Fragment;

import com.google.android.gms.maps.*;
import com.google.android.gms.maps.model.*;
import com.google.android.material.bottomsheet.BottomSheetBehavior;     //bottom-sheet
import com.google.firebase.firestore.FirebaseFirestore;


import java.util.List;

public class MapFragment extends Fragment implements OnMapReadyCallback {

    private GoogleMap mMap;


    // bottom-sheet
    // 1. Declare Bottom Sheet and UI Variables
    private BottomSheetBehavior<LinearLayout> bottomSheetBehavior;
    private TextView tvToiletName;
    private TextView tvToiletDetails;
    // bottom-sheet

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_map, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        SupportMapFragment mapFragment = (SupportMapFragment)
                getChildFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }


        // bottom-sheet
        // 2. Initialize UI elements
        LinearLayout bottomSheetLayout = view.findViewById(R.id.bottom_sheet_layout);
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheetLayout);

        // Start with the bottom sheet completely hidden
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);

        tvToiletName = view.findViewById(R.id.tv_toilet_name);
        tvToiletDetails = view.findViewById(R.id.tv_toilet_details);
        // bottom-sheet
    }

    // bottom-sheet
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        FirebaseFirestore.getInstance().collection("toilets").get()
                .addOnSuccessListener(result -> {
                    List<Toilet> toilets = result.toObjects(Toilet.class);
                    for (Toilet t : toilets) {
                        // Create the marker
                        Marker marker = mMap.addMarker(new MarkerOptions()
                                .position(new LatLng(t.lat, t.lng))
                                .title(t.name));

                        // 3. Store the Toilet object inside the marker itself!
                        if (marker != null) {
                            marker.setTag(t);
                        }
                    }

                    if (!toilets.isEmpty()) {
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                                new LatLng(toilets.get(0).lat, toilets.get(0).lng), 13f));
                    }
                });

        // 4. Handle clicks on the Map Pins
        mMap.setOnMarkerClickListener(marker -> {
            // Retrieve the Toilet object we stored in the tag
            Toilet clickedToilet = (Toilet) marker.getTag();

            if (clickedToilet != null) {
                // Update the text in the bottom sheet
                tvToiletName.setText(clickedToilet.name);
                tvToiletDetails.setText("Floor: " + clickedToilet.floor + " | Rating: " + clickedToilet.rating);

                // Slide the bottom sheet up
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
            }
            return true; // Return true to indicate we consumed the click event
        });

        // 5. Hide the bottom sheet if the user clicks anywhere else on the map
        mMap.setOnMapClickListener(latLng -> {
            if (bottomSheetBehavior.getState() != BottomSheetBehavior.STATE_HIDDEN) {
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
            }
        });
    }
    // bottom-sheet
}