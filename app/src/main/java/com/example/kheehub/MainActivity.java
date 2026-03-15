package com.example.kheehub;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;

import com.example.kheehub.model.Toilet;
import com.example.kheehub.viewmodel.MapViewModel;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.List;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private ConstraintLayout mapContainer;
    private View listContainer;
    private View profileContainer;
    private MapViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        
        viewModel = new ViewModelProvider(this).get(MapViewModel.class);

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
        observeViewModel();
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
        fabLayers.setOnClickListener(v -> viewModel.toggleMapType());

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

    private void observeViewModel() {
        viewModel.getMapType().observe(this, type -> {
            if (mMap != null) {
                mMap.setMapType(type);
            }
        });

        viewModel.getToilets().observe(this, this::updateMarkers);
    }

    private void updateMarkers(List<Toilet> toilets) {
        if (mMap == null || toilets == null) return;
        mMap.clear();
        for (Toilet toilet : toilets) {
            mMap.addMarker(new MarkerOptions()
                    .position(new LatLng(toilet.getLat(), toilet.getLng()))
                    .title(toilet.getName()));
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;

        // UI Settings
        mMap.getUiSettings().setZoomControlsEnabled(false); 
        mMap.getUiSettings().setZoomGesturesEnabled(true);

        // Default position
        LatLng defaultLocation = new LatLng(1.3521, 103.8198);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 12));

        // Sync initial map type and markers
        if (viewModel.getMapType().getValue() != null) {
            mMap.setMapType(viewModel.getMapType().getValue());
        }
        updateMarkers(viewModel.getToilets().getValue());
    }
}
