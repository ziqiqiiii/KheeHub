package com.example.kheehub;

import android.content.Context;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.*;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Button;
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

import android.widget.ImageButton;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.switchmaterial.SwitchMaterial;


import java.util.ArrayList;
import java.util.List;
import java.util.Calendar;

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

    // NEW Filter variables
    private List<Marker> allMarkers = new ArrayList<>();
    private boolean isFilterOpenNow = false;
    private List<String> currentSelectedTags = new ArrayList<>();
    private Toilet currentToilet;

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
        AutoCompleteTextView etSearch = view.findViewById(R.id.et_search);

        // Set up the adapter after toilets load (we'll call setupSearchAdapter later)
        etSearch.setOnItemClickListener((parent, v1, position, id) -> {
            Toilet selected = (Toilet) parent.getItemAtPosition(position);
            if (selected != null) {
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(
                        new LatLng(selected.lat, selected.lng), 18f));
                updateBottomSheetUI(selected, null);
            }
            InputMethodManager imm = (InputMethodManager) requireActivity()
                    .getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) imm.hideSoftInputFromWindow(etSearch.getWindowToken(), 0);
        });

        // Keep the IME search action as fallback
        etSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                String query = etSearch.getText().toString().trim().toLowerCase();
                if (!query.isEmpty()) searchForToilet(query);
                InputMethodManager imm = (InputMethodManager) requireActivity()
                        .getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                etSearch.dismissDropDown();
                return true;
            }
            return false;
        });
        Button btnEdit = view.findViewById(R.id.btn_edit_toilet);
        btnEdit.setOnClickListener(v -> {
            if (currentToilet != null) {
                showEditDialog(currentToilet);
            }
        });

        ImageButton btnFilter = view.findViewById(R.id.btn_filter);
        if (btnFilter != null) {
            btnFilter.setOnClickListener(v -> showFilterDialog());
        }
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
                //Log.d("DEBUGGG1", t.toString());
                boolean matchesTags = true;
                for (String requiredTag : currentSelectedTags) {
                    if (t.tags == null || !t.tags.contains(requiredTag)) {
                        matchesTags = false;
                        break;
                    }
                }
                //Log.d("DEBUGGG2", "Status: " + t.status);
                boolean matchesOpen = true;
                //if (isFilterOpenNow) {
                    if (t.status == 0 || !isCurrentlyOpen(t.openingHours)) {
                        matchesOpen = false;
                        //Log.d("DEBUGGG3", t.toString());
                        continue; // try
                    }
                //}
//                Log.d("DEBUGGG3", t.toString());
                if (!matchesTags || !matchesOpen) {
                    continue;
                }
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
            else{
                Toast.makeText(requireContext(), "No nearby toilets match your active filters.", Toast.LENGTH_LONG).show();
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

                    allMarkers.clear(); // Clear the list before adding
                    for (Toilet t : allToilets) {
                        // Determine color based on isCurrentlyOpen
                        float markerColor = isCurrentlyOpen(t.openingHours) ?
                                BitmapDescriptorFactory.HUE_RED :  // Open = Red
                                BitmapDescriptorFactory.HUE_ROSE;  // Closed = Light Red

                        Marker marker = mMap.addMarker(new MarkerOptions()
                                .position(new LatLng(t.lat, t.lng))
                                .title(t.name)
                                .icon(BitmapDescriptorFactory.defaultMarker(markerColor)));

                        if (marker != null) {
                            marker.setTag(t);
                            marker.setAlpha(isCurrentlyOpen(t.openingHours) ? 1.0f : 0.25f);
                            allMarkers.add(marker); // Save the marker for filtering!
                        }
                    }

                    if (!allToilets.isEmpty()) {
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                                new LatLng(allToilets.get(0).lat, allToilets.get(0).lng), 13f));
                    }
                    if (getView() != null) {
                        AutoCompleteTextView etSearch = getView().findViewById(R.id.et_search);
                        ToiletSearchAdapter adapter = new ToiletSearchAdapter(requireContext(), allToilets);
                        etSearch.setAdapter(adapter);
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
        currentToilet = toilet;
        tvToiletName.setText(toilet.name);

        // 1. Floor, Rating & Distance
        String detailsStr = "Floor: " + toilet.floor + " | Rating: " + toilet.rating;

        // If we have the distance (from the Find Nearest button), add it to the end!
        if (distanceInMeters != null) {
            detailsStr += (distanceInMeters < 1000) ?
                    String.format(" | %.0f m away", distanceInMeters) :
                    String.format(" | %.1f km away", distanceInMeters / 1000);
        }

        tvToiletDetails.setText(detailsStr);

        // 2. Status (1 = Available, 0 = Not Available)
        if (toilet.status == 1 && isCurrentlyOpen(toilet.openingHours)) {
            tvStatus.setText("Open");
            tvStatus.setTextColor(0xFF388E3C); // Green text
            tvStatus.setBackgroundColor(0xFFE8F5E9); // Light Green box
        }
        else if (toilet.status == 1 && !isCurrentlyOpen(toilet.openingHours)) {
            tvStatus.setText("Close");
            tvStatus.setTextColor(0xFFF57C00); // Orange text
            tvStatus.setBackgroundColor(0xFFFFE0B2); // Light Orange box
        }
        else {
            tvStatus.setText("Under Construction");
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
    private void showFilterDialog() {
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        // Note: Make sure you created dialog_filter.xml in your layout folder!
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_filter, null);
        dialog.setContentView(dialogView);

        SwitchMaterial switchOpenNow = dialogView.findViewById(R.id.switch_open_now);
        ChipGroup cgFilterTags = dialogView.findViewById(R.id.cg_filter_tags);
        Button btnApply = dialogView.findViewById(R.id.btn_apply_filter);

        // Restore previous selections when opening the dialog
        switchOpenNow.setChecked(isFilterOpenNow);
        if (cgFilterTags != null) {
            for (int i = 0; i < cgFilterTags.getChildCount(); i++) {
                Chip chip = (Chip) cgFilterTags.getChildAt(i);
                if (currentSelectedTags.contains(chip.getText().toString().toLowerCase())) {
                    chip.setChecked(true);
                }
            }
        }

        btnApply.setOnClickListener(v -> {
            // Save the states
            isFilterOpenNow = switchOpenNow.isChecked();
            currentSelectedTags.clear();

            if (cgFilterTags != null) {
                for (int i = 0; i < cgFilterTags.getChildCount(); i++) {
                    Chip chip = (Chip) cgFilterTags.getChildAt(i);
                    if (chip.isChecked()) {
                        currentSelectedTags.add(chip.getText().toString().toLowerCase());
                    }
                }
            }

            // Apply logic and close
            applyFiltersToMap();
            dialog.dismiss();
        });

        dialog.show();
    }

    // Loops through all map markers and hides the ones that don't match
    private void applyFiltersToMap() {
        int visibleCount = 0;

        for (Marker marker : allMarkers) {
            Toilet t = (Toilet) marker.getTag();
            if (t == null) continue;

            // 1. Check Tags (Toilet must contain ALL selected tags)
            boolean matchesTags = true;
            for (String requiredTag : currentSelectedTags) {
                if (t.tags == null || !t.tags.contains(requiredTag)) {
                    matchesTags = false;
                    break;
                }
            }

            // 2. Check Open Status and Time
            boolean matchesOpen = true;
            if (isFilterOpenNow) {
                // To be open now, status must be 1 AND current time must be within opening hours
                if (t.status != 1 || !isCurrentlyOpen(t.openingHours)) {
                    matchesOpen = false;
                }
            }

            // Update marker visibility
            boolean shouldBeVisible = matchesTags && matchesOpen;
            marker.setVisible(shouldBeVisible);

            if (shouldBeVisible) visibleCount++;
        }

        Toast.makeText(getContext(), "Found " + visibleCount + " matching toilets", Toast.LENGTH_SHORT).show();
    }

    // Checks if the current phone time is within the toilet's operating hours string
    private boolean isCurrentlyOpen(String hours) {
        if (hours == null || hours.isEmpty()) return false;

        // Handle 24-hour edge cases easily
        if (hours.equals("24.00-23.59") || hours.equals("24:00-23:59") || hours.equals("00:00-23:59") || hours.equalsIgnoreCase("24 hours")) {
            return true;
        }

        try {
            String[] parts = hours.split("-");
            if (parts.length != 2) return false;

            Calendar calendar = Calendar.getInstance();
            int currentHour = calendar.get(Calendar.HOUR_OF_DAY);
            int currentMinute = calendar.get(Calendar.MINUTE);
            int currentTimeInMinutes = (currentHour * 60) + currentMinute;

            String[] openParts = parts[0].split(":");
            int openTimeInMinutes = (Integer.parseInt(openParts[0].trim()) * 60) + Integer.parseInt(openParts[1].trim());

            String[] closeParts = parts[1].split(":");
            int closeTimeInMinutes = (Integer.parseInt(closeParts[0].trim()) * 60) + Integer.parseInt(closeParts[1].trim());

            if (openTimeInMinutes <= closeTimeInMinutes) {
                return currentTimeInMinutes >= openTimeInMinutes && currentTimeInMinutes <= closeTimeInMinutes;
            } else {
                // Handles hours that cross midnight (e.g., 22:00 - 06:00)
                return currentTimeInMinutes >= openTimeInMinutes || currentTimeInMinutes <= closeTimeInMinutes;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false; // If the string is formatted wrong in Firestore, assume closed
        }
    }
    private void showEditDialog(Toilet toilet) {
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_edit_toilet, null);
        dialog.setContentView(dialogView);

        // Find views
        EditText editName = dialogView.findViewById(R.id.edit_name);
        EditText editFloor = dialogView.findViewById(R.id.edit_floor);
        EditText editRating = dialogView.findViewById(R.id.edit_rating);
        EditText editOpeningHours = dialogView.findViewById(R.id.edit_opening_hours);
        RadioGroup rgStatus = dialogView.findViewById(R.id.rg_status);
        RadioButton rbAvailable = dialogView.findViewById(R.id.rb_available);
        RadioButton rbUnavailable = dialogView.findViewById(R.id.rb_unavailable);
        ChipGroup cgEditTags = dialogView.findViewById(R.id.cg_edit_tags);
        Button btnSave = dialogView.findViewById(R.id.btn_save);

        // Pre-fill current values
        editName.setText(toilet.name);
        editFloor.setText(toilet.floor);
        editRating.setText(String.valueOf(toilet.rating));
        editOpeningHours.setText(toilet.openingHours);

        if (toilet.status == 1) {
            rbAvailable.setChecked(true);
        } else {
            rbUnavailable.setChecked(true);
        }

        // Pre-check tags
        if (toilet.tags != null) {
            for (int i = 0; i < cgEditTags.getChildCount(); i++) {
                Chip chip = (Chip) cgEditTags.getChildAt(i);
                if (toilet.tags.contains(chip.getText().toString().toLowerCase())) {
                    chip.setChecked(true);
                }
            }
        }

        // Save button
        btnSave.setOnClickListener(v -> {
            String newName = editName.getText().toString().trim();
            String newFloor = editFloor.getText().toString().trim();
            String newRating = editRating.getText().toString().trim();
            String newHours = editOpeningHours.getText().toString().trim();
            int newStatus = rbAvailable.isChecked() ? 1 : 0;

            // Collect selected tags
            List<String> newTags = new ArrayList<>();
            for (int i = 0; i < cgEditTags.getChildCount(); i++) {
                Chip chip = (Chip) cgEditTags.getChildAt(i);
                if (chip.isChecked()) {
                    newTags.add(chip.getText().toString().toLowerCase());
                }
            }

            // Validate
            if (newName.isEmpty() || newFloor.isEmpty() || newRating.isEmpty()) {
                Toast.makeText(requireContext(), "Please fill in all fields.", Toast.LENGTH_SHORT).show();
                return;
            }

            // Update Firestore
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            db.collection("toilets")
                    .whereEqualTo("name", toilet.name)
                    .whereEqualTo("lat", toilet.lat)
                    .whereEqualTo("lng", toilet.lng)
                    .get()
                    .addOnSuccessListener(querySnapshot -> {
                        if (!querySnapshot.isEmpty()) {
                            String docId = querySnapshot.getDocuments().get(0).getId();

                            db.collection("toilets").document(docId)
                                    .update(
                                            "name", newName,
                                            "floor", newFloor,
                                            "rating", Double.parseDouble(newRating),
                                            "openingHours", newHours,
                                            "status", newStatus,
                                            "tags", newTags
                                    )
                                    .addOnSuccessListener(unused -> {
                                        Toast.makeText(requireContext(), "Updated successfully!", Toast.LENGTH_SHORT).show();

                                        // Update local data
                                        toilet.name = newName;
                                        toilet.floor = newFloor;
                                        toilet.rating = Double.parseDouble(newRating);
                                        toilet.openingHours = newHours;
                                        toilet.status = newStatus;
                                        toilet.tags = newTags;

                                        // Refresh bottom sheet and marker
                                        updateBottomSheetUI(toilet, null);
                                        refreshMarker(toilet);

                                        dialog.dismiss();
                                    })
                                    .addOnFailureListener(e -> {
                                        Toast.makeText(requireContext(), "Update failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                    });
                        }
                    });
        });

        dialog.show();
    }
    private void refreshMarker(Toilet toilet) {
        for (Marker marker : allMarkers) {
            Toilet t = (Toilet) marker.getTag();
            if (t != null && t.lat == toilet.lat && t.lng == toilet.lng) {
                marker.setTitle(toilet.name);
                marker.setTag(toilet);

                float markerColor = isCurrentlyOpen(toilet.openingHours) ?
                        BitmapDescriptorFactory.HUE_RED :
                        BitmapDescriptorFactory.HUE_ROSE;
                marker.setIcon(BitmapDescriptorFactory.defaultMarker(markerColor));
                marker.setAlpha(isCurrentlyOpen(toilet.openingHours) ? 1.0f : 0.25f);
                break;
            }
        }
    }

}