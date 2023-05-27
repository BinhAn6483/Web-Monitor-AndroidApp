package com.example.monitoringlocation;

import com.google.gson.annotations.SerializedName;


public class LocationData {
    @SerializedName("person_id")
    private String token;

    @SerializedName("time")
    private String time;

    @SerializedName("latitude")
    private double latitude;

    @SerializedName("longitude")
    private double longitude;

    public LocationData(String token, String time, double latitude, double longitude) {
        this.token = token;
        this.time = time;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }
}
