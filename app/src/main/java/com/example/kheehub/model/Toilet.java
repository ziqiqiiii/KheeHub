package com.example.kheehub.model;

import com.google.android.gms.maps.model.LatLng;

public class Toilet {
    private String id;
    private String name;
    private double lat;
    private double lng;

    public Toilet() {} // Required for Firestore

    public Toilet(String id, String name, double lat, double lng) {
        this.id = id;
        this.name = name;
        this.lat = lat;
        this.lng = lng;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public double getLat() { return lat; }
    public double getLng() { return lng; }
    
    public LatLng getLatLng() {
        return new LatLng(lat, lng);
    }
}
