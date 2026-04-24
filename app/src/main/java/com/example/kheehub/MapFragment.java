package com.example.kheehub;

import android.content.Context;
import android.location.Location;
import android.os.Bundle;
import android.view.*;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Button;
import android.widget.ImageView;
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

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;

    private GoogleMap mMap;
    private BottomSheetBehavior<LinearLayout> bottomSheetBehavior;
    private TextView tvToiletName;
    private TextView tvToiletDetails;
    private TextView tvStatus;
    private TextView tvOpeningHours;
    private ChipGroup cgTags;
    private final List<Toilet> allToilets = new ArrayList<>();
    private FusedLocationProviderClient fusedLocationClient;
    private final List<Marker> allMarkers = new ArrayList<>();
    private boolean isFilterOpenNow = false;
    private final List<String> currentSelectedTags = new ArrayList<>();
    private Toilet currentToilet;

    private final ToiletRepository repository = ToiletRepository.getInstance();

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

        tvToiletName    = view.findViewById(R.id.tv_toilet_name);
        tvToiletDetails = view.findViewById(R.id.tv_toilet_reviews);
        tvStatus        = view.findViewById(R.id.tv_status);
        tvOpeningHours  = view.findViewById(R.id.tv_opening_hours);
        cgTags          = view.findViewById(R.id.cg_tags);

        ExtendedFloatingActionButton btnNearest = view.findViewById(R.id.btn_nearest);
        btnNearest.setOnClickListener(v -> findNearestToilet());

        AutoCompleteTextView etSearch = view.findViewById(R.id.et_search);

        etSearch.setOnItemClickListener((parent, v1, position, id) -> {
            Toilet selected = (Toilet) parent.getItemAtPosition(position);
            if (selected != null) {
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(
                        new LatLng(selected.getLat(), selected.getLng()), 18f));
                updateBottomSheetUI(selected, null);
            }
            InputMethodManager imm = (InputMethodManager) requireActivity()
                    .getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) imm.hideSoftInputFromWindow(etSearch.getWindowToken(), 0);
        });

        ImageView btnSearchIcon = view.findViewById(R.id.btn_search_icon);
        if (btnSearchIcon != null) {
            btnSearchIcon.setOnClickListener(v -> executeSearch(etSearch));
        }

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

    private void executeSearch(EditText etSearch) {
        String query = etSearch.getText().toString().trim().toLowerCase();
        if (!query.isEmpty()) {
            searchForToilet(query);
        }
        InputMethodManager imm = (InputMethodManager) requireActivity()
                .getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(etSearch.getWindowToken(), 0);
        }
    }

    private void findNearestToilet() {
        if (ActivityCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
            return;
        }

        if (allToilets.isEmpty()) {
            Toast.makeText(requireContext(),
                    getString(R.string.no_toilets_loaded), Toast.LENGTH_SHORT).show();
            return;
        }

        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            if (location == null) {
                Toast.makeText(requireContext(),
                        getString(R.string.no_location), Toast.LENGTH_SHORT).show();
                return;
            }

            Toilet nearest = null;
            float minDistance = Float.MAX_VALUE;

            for (Toilet t : allToilets) {
                boolean matchesTags = true;
                for (String requiredTag : currentSelectedTags) {
                    if (t.getTags() == null || !t.getTags().contains(requiredTag)) {
                        matchesTags = false;
                        break;
                    }
                }

                if (!t.isAvailable() || !isCurrentlyOpen(t.getOpeningHours())) {
                    continue;
                }

                if (!matchesTags) {
                    continue;
                }

                float[] results = new float[1];
                Location.distanceBetween(
                        location.getLatitude(), location.getLongitude(),
                        t.getLat(), t.getLng(), results);

                if (results[0] < minDistance) {
                    minDistance = results[0];
                    nearest = t;
                }
            }

            if (nearest != null) {
                LatLng pos = new LatLng(nearest.getLat(), nearest.getLng());
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(pos, 18f));
                updateBottomSheetUI(nearest, minDistance);
            } else {
                Toast.makeText(requireContext(),
                        getString(R.string.no_matching_nearby), Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        if (ContextCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
        } else {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        }

        repository.getAllToilets(
                downloadedToilets -> {
                    allToilets.clear();
                    allToilets.addAll(downloadedToilets);

                    allMarkers.clear();
                    for (Toilet t : allToilets) {
                        float markerColor = isCurrentlyOpen(t.getOpeningHours()) ?
                                BitmapDescriptorFactory.HUE_RED :
                                BitmapDescriptorFactory.HUE_ROSE;

                        Marker marker = mMap.addMarker(new MarkerOptions()
                                .position(new LatLng(t.getLat(), t.getLng()))
                                .title(t.getName())
                                .icon(BitmapDescriptorFactory.defaultMarker(markerColor)));

                        if (marker != null) {
                            marker.setTag(t);
                            marker.setAlpha(isCurrentlyOpen(t.getOpeningHours()) ? 1.0f : 0.25f);
                            allMarkers.add(marker);
                        }
                    }

                    if (!allToilets.isEmpty()) {
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                                new LatLng(allToilets.get(0).getLat(), allToilets.get(0).getLng()), 13f));
                    }

                    if (getView() != null) {
                        AutoCompleteTextView etSearch = getView().findViewById(R.id.et_search);
                        ToiletSearchAdapter adapter = new ToiletSearchAdapter(requireContext(), allToilets);
                        etSearch.setAdapter(adapter);
                    }
                },
                e -> Toast.makeText(requireContext(),
                        getString(R.string.load_toilets_failed, e.getMessage()),
                        Toast.LENGTH_LONG).show()
        );

        mMap.setOnMarkerClickListener(marker -> {
            Toilet clickedToilet = (Toilet) marker.getTag();
            if (clickedToilet != null) {
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
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (ContextCompat.checkSelfPermission(requireContext(),
                        Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    mMap.setMyLocationEnabled(true);
                }
            } else {
                Toast.makeText(getContext(),
                        getString(R.string.no_location_permission), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void searchForToilet(String query) {
        boolean found = false;

        for (Toilet t : allToilets) {
            if (t.getName().toLowerCase().contains(query) || t.getFloor().toLowerCase().contains(query)) {
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(
                        new LatLng(t.getLat(), t.getLng()), 18f));
                updateBottomSheetUI(t, null);
                found = true;
                break;
            }
        }
        if (!found) {
            Toast.makeText(getContext(),
                    getString(R.string.no_toilet_found), Toast.LENGTH_SHORT).show();
        }
    }

    private void updateBottomSheetUI(Toilet toilet, Float distanceInMeters) {
        currentToilet = toilet;
        tvToiletName.setText(toilet.getName());

        String detailsStr = "Floor: " + toilet.getFloor() + " | Rating: " + toilet.getRating();
        if (distanceInMeters != null) {
            detailsStr += (distanceInMeters < 1000) ?
                    String.format(" | %.0f m away", distanceInMeters) :
                    String.format(" | %.1f km away", distanceInMeters / 1000);
        }
        tvToiletDetails.setText(detailsStr);

        Context ctx = requireContext();
        if (toilet.isAvailable() && isCurrentlyOpen(toilet.getOpeningHours())) {
            tvStatus.setText(R.string.status_open);
            tvStatus.setTextColor(ContextCompat.getColor(ctx, R.color.status_open_text));
            tvStatus.setBackgroundColor(ContextCompat.getColor(ctx, R.color.status_open_bg));
        } else if (toilet.isAvailable()) {
            tvStatus.setText(R.string.status_closed);
            tvStatus.setTextColor(ContextCompat.getColor(ctx, R.color.status_closed_text));
            tvStatus.setBackgroundColor(ContextCompat.getColor(ctx, R.color.status_closed_bg));
        } else {
            tvStatus.setText(R.string.status_unavailable);
            tvStatus.setTextColor(ContextCompat.getColor(ctx, R.color.status_unavailable_text));
            tvStatus.setBackgroundColor(ContextCompat.getColor(ctx, R.color.status_unavailable_bg));
        }

        if (toilet.getOpeningHours() != null && !toilet.getOpeningHours().isEmpty()) {
            tvOpeningHours.setText(toilet.getOpeningHours());
            tvOpeningHours.setVisibility(View.VISIBLE);
        } else {
            tvOpeningHours.setVisibility(View.GONE);
        }

        cgTags.removeAllViews();
        if (toilet.getTags() != null && !toilet.getTags().isEmpty()) {
            for (String tag : toilet.getTags()) {
                Chip chip = new Chip(requireContext());
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
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_filter, null);
        dialog.setContentView(dialogView);

        SwitchMaterial switchOpenNow = dialogView.findViewById(R.id.switch_open_now);
        ChipGroup cgFilterTags = dialogView.findViewById(R.id.cg_filter_tags);
        Button btnApply = dialogView.findViewById(R.id.btn_apply_filter);

        switchOpenNow.setChecked(isFilterOpenNow);
        if (cgFilterTags != null) {
            ChipUtils.restoreSelectedTags(cgFilterTags, currentSelectedTags);
        }

        btnApply.setOnClickListener(v -> {
            isFilterOpenNow = switchOpenNow.isChecked();
            currentSelectedTags.clear();
            if (cgFilterTags != null) {
                currentSelectedTags.addAll(ChipUtils.getSelectedTags(cgFilterTags));
            }
            applyFiltersToMap();
            dialog.dismiss();
        });

        dialog.show();
    }

    private void applyFiltersToMap() {
        int visibleCount = 0;

        for (Marker marker : allMarkers) {
            Toilet t = (Toilet) marker.getTag();
            if (t == null) continue;

            boolean matchesTags = true;
            for (String requiredTag : currentSelectedTags) {
                if (t.getTags() == null || !t.getTags().contains(requiredTag)) {
                    matchesTags = false;
                    break;
                }
            }

            boolean matchesOpen = true;
            if (isFilterOpenNow) {
                if (!t.isAvailable() || !isCurrentlyOpen(t.getOpeningHours())) {
                    matchesOpen = false;
                }
            }

            boolean shouldBeVisible = matchesTags && matchesOpen;
            marker.setVisible(shouldBeVisible);
            if (shouldBeVisible) visibleCount++;
        }

        Toast.makeText(getContext(),
                getString(R.string.toilets_matching_count, visibleCount),
                Toast.LENGTH_SHORT).show();
    }

    private boolean isCurrentlyOpen(String hours) {
        if (hours == null || hours.isEmpty()) return false;

        if (hours.equals("24.00-23.59") || hours.equals("24:00-23:59")
                || hours.equals("00:00-23:59") || hours.equals("00.00-23.59")
                || hours.equalsIgnoreCase("24 hours")) {
            return true;
        }

        try {
            String[] parts = hours.split("-");
            if (parts.length != 2) return false;

            Calendar calendar = Calendar.getInstance();
            int currentHour = calendar.get(Calendar.HOUR_OF_DAY);
            int currentMinute = calendar.get(Calendar.MINUTE);
            int currentTimeInMinutes = (currentHour * 60) + currentMinute;

            String[] openParts = parts[0].split("[:.]");
            int openTimeInMinutes = (Integer.parseInt(openParts[0].trim()) * 60)
                    + Integer.parseInt(openParts[1].trim());

            String[] closeParts = parts[1].split("[:.]");
            int closeTimeInMinutes = (Integer.parseInt(closeParts[0].trim()) * 60)
                    + Integer.parseInt(closeParts[1].trim());

            if (openTimeInMinutes <= closeTimeInMinutes) {
                return currentTimeInMinutes >= openTimeInMinutes
                        && currentTimeInMinutes <= closeTimeInMinutes;
            } else {
                return currentTimeInMinutes >= openTimeInMinutes
                        || currentTimeInMinutes <= closeTimeInMinutes;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private void showEditDialog(Toilet toilet) {
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_edit_toilet, null);
        dialog.setContentView(dialogView);

        EditText editName         = dialogView.findViewById(R.id.edit_name);
        EditText editFloor        = dialogView.findViewById(R.id.edit_floor);
        EditText editRating       = dialogView.findViewById(R.id.edit_rating);
        EditText editOpeningHours = dialogView.findViewById(R.id.edit_opening_hours);
        RadioButton rbAvailable   = dialogView.findViewById(R.id.rb_available);
        RadioButton rbUnavailable = dialogView.findViewById(R.id.rb_unavailable);
        ChipGroup cgEditTags      = dialogView.findViewById(R.id.cg_edit_tags);
        Button btnSave            = dialogView.findViewById(R.id.btn_save);

        editName.setText(toilet.getName());
        editFloor.setText(toilet.getFloor());
        editRating.setText(String.valueOf(toilet.getRating()));
        editOpeningHours.setText(toilet.getOpeningHours());

        if (toilet.isAvailable()) {
            rbAvailable.setChecked(true);
        } else {
            rbUnavailable.setChecked(true);
        }

        ChipUtils.restoreSelectedTags(cgEditTags, toilet.getTags());

        btnSave.setOnClickListener(v -> {
            String newName   = editName.getText().toString().trim();
            String newFloor  = editFloor.getText().toString().trim();
            String newRating = editRating.getText().toString().trim();
            String newHours  = editOpeningHours.getText().toString().trim();
            int newStatus    = rbAvailable.isChecked() ? 1 : 0;

            List<String> newTags = ChipUtils.getSelectedTags(cgEditTags);

            if (newName.isEmpty() || newFloor.isEmpty() || newRating.isEmpty()) {
                Toast.makeText(requireContext(),
                        getString(R.string.edit_missing_fields), Toast.LENGTH_SHORT).show();
                return;
            }

            repository.updateToilet(
                    toilet,
                    newName, newFloor, Double.parseDouble(newRating),
                    newHours, newStatus, newTags,
                    unused -> {
                        Toast.makeText(requireContext(),
                                getString(R.string.update_success), Toast.LENGTH_SHORT).show();

                        toilet.setName(newName);
                        toilet.setFloor(newFloor);
                        toilet.setRating(Double.parseDouble(newRating));
                        toilet.setOpeningHours(newHours);
                        toilet.setStatus(newStatus);
                        toilet.setTags(newTags);

                        updateBottomSheetUI(toilet, null);
                        refreshMarker(toilet);
                        dialog.dismiss();
                    },
                    e -> Toast.makeText(requireContext(),
                            getString(R.string.update_failed, e.getMessage()),
                            Toast.LENGTH_SHORT).show()
            );
        });

        dialog.show();
    }

    private void refreshMarker(Toilet toilet) {
        for (Marker marker : allMarkers) {
            Toilet t = (Toilet) marker.getTag();
            if (t != null && t.getLat() == toilet.getLat() && t.getLng() == toilet.getLng()) {
                marker.setTitle(toilet.getName());
                marker.setTag(toilet);

                float markerColor = isCurrentlyOpen(toilet.getOpeningHours()) ?
                        BitmapDescriptorFactory.HUE_RED :
                        BitmapDescriptorFactory.HUE_ROSE;
                marker.setIcon(BitmapDescriptorFactory.defaultMarker(markerColor));
                marker.setAlpha(isCurrentlyOpen(toilet.getOpeningHours()) ? 1.0f : 0.25f);
                break;
            }
        }
    }
}