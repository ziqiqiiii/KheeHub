package com.example.kheehub;

import android.content.Context;
import android.location.Location;
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

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.*;
import com.google.android.gms.maps.model.*;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
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
    private TextView tvStatus;
    private TextView tvOpeningHours;
    private ChipGroup cgTags;
    private List<Toilet> allToilets = new ArrayList<>();
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private FusedLocationProviderClient fusedLocationClient;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_map, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());

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

        // Initialize new UI elements
        tvStatus = view.findViewById(R.id.tv_status);
        tvOpeningHours = view.findViewById(R.id.tv_opening_hours);
        cgTags = view.findViewById(R.id.cg_tags);

        // Find Nearest button
        ExtendedFloatingActionButton btnNearest = view.findViewById(R.id.btn_nearest);
        btnNearest.setOnClickListener(v -> findNearestToilet());

        // Search Bar
        EditText etSearch = view.findViewById(R.id.et_search);
        etSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                String query = etSearch.getText().toString().trim().toLowerCase();

                if (!query.isEmpty()) {
                    searchForToilet(query);
                }

                InputMethodManager imm = (InputMethodManager) requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                }

                return true;
            }
            return false;
        });
    }

    private void findNearestToilet() {
        if (ActivityCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
            return;
        }

        if (allToilets.isEmpty()) {
            Toast.makeText(requireContext(), "No toilets loaded yet.", Toast.LENGTH_SHORT).show();
            return;
        }

        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            if (location == null) {
                Toast.makeText(requireContext(), "Unable to get your location.", Toast.LENGTH_SHORT).show();
                return;
            }

            Toilet nearest = null;
            float minDistance = Float.MAX_VALUE;

            for (Toilet t : allToilets) {
                float[] results = new float[1];
                Location.distanceBetween(
                        location.getLatitude(), location.getLongitude(),
                        t.lat, t.lng, results);

                if (results[0] < minDistance) {
                    minDistance = results[0];
                    nearest = t;
                }
            }

            if (nearest != null) {
                LatLng pos = new LatLng(nearest.lat, nearest.lng);
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(pos, 18f));

                // Use the new helper method!
                updateBottomSheetUI(nearest, minDistance);
            }
        });
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
        } else {
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
                // Use the new helper method!
                updateBottomSheetUI(clickedToilet, null);
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
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    mMap.setMyLocationEnabled(true);
                }
            } else {
                Toast.makeText(getContext(), "Location permission is needed to show your position.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void searchForToilet(String query) {
        boolean found = false;

        for (Toilet t : allToilets) {
            if (t.name.toLowerCase().contains(query) || t.floor.toLowerCase().contains(query)) {

                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(t.lat, t.lng), 18f));

                // Use the new helper method!
                updateBottomSheetUI(t, null);

                found = true;
                break;
            }
        }

        if (!found) {
            Toast.makeText(getContext(), "No toilet found matching that search.", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateBottomSheetUI(Toilet toilet, Float distanceInMeters) {
        tvToiletName.setText(toilet.name);

        // 1. Distance & Floor
        String detailsStr = "Floor: " + toilet.floor;
        if (distanceInMeters != null) {
            detailsStr += (distanceInMeters < 1000) ?
                    String.format(" | %.0f m away", distanceInMeters) :
                    String.format(" | %.1f km away", distanceInMeters / 1000);
        } else {
            detailsStr += " | Rating: " + toilet.rating;
        }
        tvToiletDetails.setText(detailsStr);

        // 2. Status (1 = Available, 0 = Not Available)
        if (toilet.status == 1) {
            tvStatus.setText("Available");
            tvStatus.setTextColor(0xFF388E3C); // Green text
            tvStatus.setBackgroundColor(0xFFE8F5E9); // Light Green box
        } else {
            tvStatus.setText("Not Available");
            tvStatus.setTextColor(0xFFD32F2F); // Red text
            tvStatus.setBackgroundColor(0xFFFFEBEE); // Light Red box
        }

        // 3. Opening Hours
        if (toilet.openingHours != null && !toilet.openingHours.isEmpty()) {
            tvOpeningHours.setText(toilet.openingHours);
            tvOpeningHours.setVisibility(View.VISIBLE);
        } else {
            tvOpeningHours.setVisibility(View.GONE);
        }

        // 4. Tags
        cgTags.removeAllViews();
        if (toilet.tags != null && !toilet.tags.isEmpty()) {
            for (String tag : toilet.tags) {
                Chip chip = new Chip(requireContext());
                // Capitalize the first letter for a cleaner look
                String displayTag = tag.substring(0, 1).toUpperCase() + tag.substring(1);
                chip.setText(displayTag);
                chip.setCheckable(false);
                chip.setClickable(false);
                cgTags.addView(chip);
            }
            cgTags.setVisibility(View.VISIBLE);
        } else {
            cgTags.setVisibility(View.GONE);
        }

        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
    }
}