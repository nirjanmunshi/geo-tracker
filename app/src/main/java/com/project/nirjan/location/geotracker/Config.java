package com.project.nirjan.location.geotracker;

import android.os.Environment;

import java.io.File;

public class Config {

    static final String LATITUDE = "latitude";
    static final String LONGITUDE = "longitude";
    static final String ACCURACY = "accuracy";
    static final String PROVIDER = "provider";
    static final String MAX_SPEED = "max-speed";
    static final String AVG_SPEED= "avg-speed";
    static final String CURRENT_SPEED = "current-speed";
    static final String DISTANCE = "distance";
    static final String TRAVEL_TIME= "travel-time";
    static final String LOCATION_DATA= "location-data";

    // database information
    public static final String DATABASE_NAME = "GeoTracker.db";
    public static final int DATABASE_VERSION = 1;
    public static final String FOLDER_LOCATION = Environment.getExternalStorageDirectory() + File.separator + "GeoTracker";

}
