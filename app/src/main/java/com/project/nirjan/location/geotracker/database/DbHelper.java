package com.project.nirjan.location.geotracker.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.project.nirjan.location.geotracker.Config;

import java.io.File;

public class DbHelper extends SQLiteOpenHelper {

    private static final String DB_NAME = Config.FOLDER_LOCATION + File.separator + Config.DATABASE_NAME;
    public DbHelper(Context context) {
        super(context, DB_NAME, null, Config.DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {

    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }
}
