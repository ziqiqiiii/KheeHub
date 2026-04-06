package com.example.kheehub;

import android.content.Context;
import android.os.Bundle;
import android.view.*;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.gms.maps.*;
import com.google.android.gms.maps.model.*;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.firebase.firestore.FirebaseFirestore;

import android.Manifest;
import android.content.pm.PackageManager;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

public class MapFragment extends Fragment implements OnMapReadyCallback {

    private GoogleMap mMap;

    private BottomSheetBehavior<LinearLayout> bottomSheetBehavior;
    private TextView tvToiletName;
    private TextView tvToiletDetails;
    private List<Toilet> allToilets = new ArrayList<>();
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;

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

        LinearLayout bottomSheetLayout = view.findViewById(R.id.bottom_sheet_layout);
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheetLayout);

        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);

        tvToiletName = view.findViewById(R.id.tv_toilet_name);
        tvToiletDetails = view.findViewById(R.id.tv_toilet_details);

        // 2. Setup the Search Bar
        EditText etSearch = view.findViewById(R.id.et_search);
        etSearch.setOnEditorActionListener((v, actionId, event) -> {
            // Trigger when the user presses the search button on their keyboard
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                String query = etSearch.getText().toString().trim().toLowerCase();

                if (!query.isEmpty()) {
                    searchForToilet(query);
                }

                // Hide the keyboard after searching so the user can see the map
                InputMethodManager imm = (InputMethodManager) requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                }

                return true;
            }
            return false;
        });
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            // Permission is already granted! Turn on the blue dot.
            mMap.setMyLocationEnabled(true);
        } else {
            // Permission is missing. Ask the user for it.
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        }

        FirebaseFirestore.getInstance().collection("toilets").get()
                .addOnSuccessListener(result -> {
                    List<Toilet> downloadedToilets = result.toObjects(Toilet.class);

                    allToilets.clear();
                    allToilets.addAll(downloadedToilets);

                    for (Toilet t : allToilets) {
                        Marker marker = mMap.addMarker(new MarkerOptions()
                                .position(new LatLng(t.lat, t.lng))
                                .title(t.name));

                        if (marker != null) {
                            marker.setTag(t);
                        }
                    }

                    if (!allToilets.isEmpty()) {
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                                new LatLng(allToilets.get(0).lat, allToilets.get(0).lng), 13f));
                    }
                });

        mMap.setOnMarkerClickListener(marker -> {
            Toilet clickedToilet = (Toilet) marker.getTag();

            if (clickedToilet != null) {
                tvToiletName.setText(clickedToilet.name);
                tvToiletDetails.setText("Floor: " + clickedToilet.floor + " | Rating: " + clickedToilet.rating);

                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
            }
            return true;
        });

        mMap.setOnMapClickListener(latLng -> {
            if (bottomSheetBehavior.getState() != BottomSheetBehavior.STATE_HIDDEN) {
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            // If request is cancelled, the result arrays are empty.
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // The user clicked "Allow"! Turn on the location layer.
                // We have to check permission again here briefly to satisfy Android Studio's security warnings
                if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    mMap.setMyLocationEnabled(true);
                }
            } else {
                // The user clicked "Deny".
                Toast.makeText(getContext(), "Location permission is needed to show your position.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void searchForToilet(String query) {
        boolean found = false;

        for (Toilet t : allToilets) {
            // Check if the search query matches the toilet's name or floor
            if (t.name.toLowerCase().contains(query) || t.floor.toLowerCase().contains(query)) {

                // Move and zoom the camera to the found toilet
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(t.lat, t.lng), 18f));

                // Open the bottom sheet automatically to show the found toilet's details
                tvToiletName.setText(t.name);
                tvToiletDetails.setText("Floor: " + t.floor + " | Rating: " + t.rating);
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);

                found = true;
                break; // Stop searching after finding the first match
            }
        }

        // Let the user know if their search didn't match anything
        if (!found) {
            Toast.makeText(getContext(), "No toilet found matching that search.", Toast.LENGTH_SHORT).show();
        }
    }
}