package com.example.kheehub;

import java.util.ArrayList;
import java.util.List;

public class Toilet {
    public String name = "";
    public double lat = 0.0;
    public double lng = 0.0;
    public double rating = 0.0;
    public String floor = "";
    public String openingHours = "";
    public int status = 0;
    public List<String> tags = new ArrayList<>();

    @Override
    public String toString() {
        return name;
    }
//    public String toString() {
//        return "Toilet{name='" + name + "', lat=" + lat + ", lng=" + lng +
//                ", rating=" + rating + ", floor='" + floor + "'}";
//    }
}