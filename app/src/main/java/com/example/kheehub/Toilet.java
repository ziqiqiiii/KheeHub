//package com.example.kheehub;
//
//import java.util.ArrayList;
//import java.util.List;
//
//public class Toilet {
//    public String name = "";
//    public double lat = 0.0;
//    public double lng = 0.0;
//    public double rating = 0.0;
//    public String floor = "";
//    public String openingHours = "";
//    public int status = 0;
//    public List<String> tags = new ArrayList<>();
//
//    @Override
//    public String toString() {
//        return name;
//    }
//}

package com.example.kheehub;

import java.util.ArrayList;
import java.util.List;

public class Toilet {

    // CHANGED: All fields are now private — information hiding applied.
    // Firestore still works because it uses the public getters/setters below.
    private String name = "";
    private double lat = 0.0;
    private double lng = 0.0;
    private double rating = 0.0;
    private String floor = "";
    private String openingHours = "";
    private int status = 0;
    private List<String> tags = new ArrayList<>();

    // Required by Firestore for deserialization
    public Toilet() {}

    // --- Getters ---
    public String getName() { return name; }
    public double getLat() { return lat; }
    public double getLng() { return lng; }
    public double getRating() { return rating; }
    public String getFloor() { return floor; }
    public String getOpeningHours() { return openingHours; }
    public int getStatus() { return status; }
    public List<String> getTags() { return tags; }

    // --- Setters (required by Firestore for deserialization) ---
    public void setName(String name) { this.name = name; }
    public void setLat(double lat) { this.lat = lat; }
    public void setLng(double lng) { this.lng = lng; }
    public void setRating(double rating) { this.rating = rating; }
    public void setFloor(String floor) { this.floor = floor; }
    public void setOpeningHours(String openingHours) { this.openingHours = openingHours; }
    public void setStatus(int status) { this.status = status; }
    public void setTags(List<String> tags) { this.tags = tags; }

    // ADDED: Meaningful semantic method — Tell, Don't Ask principle.
    // Callers ask "is this toilet available?" not "is status == 1?"
    public boolean isAvailable() {
        return status == 1;
    }

    @Override
    public String toString() {
        return name;
    }
}