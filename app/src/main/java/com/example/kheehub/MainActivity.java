package com.example.kheehub;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private ConstraintLayout mapContainer;
    private View listContainer;
    private View profileContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize Containers
        mapContainer = findViewById(R.id.map_container);
        listContainer = findViewById(R.id.list_container);
        profileContainer = findViewById(R.id.profile_container);

        // Initialize Map
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        setupUI();
    }

    private void setupUI() {
        // Bottom Navigation Switching
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            
            // Reset visibilities
            mapContainer.setVisibility(View.GONE);
            listContainer.setVisibility(View.GONE);
            profileContainer.setVisibility(View.GONE);

            if (itemId == R.id.nav_map) {
                mapContainer.setVisibility(View.VISIBLE);
                return true;
            } else if (itemId == R.id.nav_list) {
                listContainer.setVisibility(View.VISIBLE);
                return true;
            } else if (itemId == R.id.nav_profile) {
                profileContainer.setVisibility(View.VISIBLE);
                return true;
            }
            return false;
        });

        // FAB: Toggle Satellite/Normal View
        FloatingActionButton fabLayers = findViewById(R.id.fab_layers);
        fabLayers.setOnClickListener(v -> {
            if (mMap != null) {
                int type = mMap.getMapType();
                if (type == GoogleMap.MAP_TYPE_NORMAL) {
                    mMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
                } else {
                    mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
                }
            }
        });

        // Custom Zoom Controls
        findViewById(R.id.btn_zoom_in).setOnClickListener(v -> {
            if (mMap != null) mMap.animateCamera(CameraUpdateFactory.zoomIn());
        });
        findViewById(R.id.btn_zoom_out).setOnClickListener(v -> {
            if (mMap != null) mMap.animateCamera(CameraUpdateFactory.zoomOut());
        });

        // Find Nearest Button
        findViewById(R.id.btn_find_nearest).setOnClickListener(v -> {
            Toast.makeText(this, "Searching for nearby toilets...", Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;

        // UI Settings
        mMap.getUiSettings().setZoomControlsEnabled(false); // Hide default ones
        mMap.getUiSettings().setZoomGesturesEnabled(true);

        // Default position
        LatLng defaultLocation = new LatLng(1.3521, 103.8198);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 12));

        loadToiletMarkers();
    }

    private void loadToiletMarkers() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("toilets")
                .get()
                .addOnSuccessListener(result -> {
                    for (DocumentSnapshot document : result) {
                        Double lat = document.getDouble("lat");
                        Double lng = document.getDouble("lng");
                        String name = document.getString("name");
                        if (lat != null && lng != null) {
                            LatLng pos = new LatLng(lat, lng);
                            mMap.addMarker(new MarkerOptions().position(pos).title(name));
                        }
                    }
                });
    }
}
