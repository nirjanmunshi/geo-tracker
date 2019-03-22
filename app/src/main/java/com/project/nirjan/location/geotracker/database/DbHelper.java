package com.project.nirjan.location.geotracker.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.project.nirjan.location.geotracker.Config;

import java.io.File;

public class DbHelper extends SQLiteOpenHelper {

    // database pulled back to outside for development purpose only.
    private static final String DB_NAME = Config.FOLDER_LOCATION + File.separator + Config.DATABASE_NAME;
    public DbHelper(Context context) {
        super(context, DB_NAME, null, Config.DATABASE_VERSION);
    }

    private static final String CREATE_TEMP_TABLE = "CREATE TABLE if not exists `tbl_temp` ( `id` INTEGER PRIMARY KEY AUTOINCREMENT, `location_data` TEXT, `sub_time` TEXT )";
    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TEMP_TABLE);
    }

    public void createTempTable(){
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL(CREATE_TEMP_TABLE);
    }
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }

    public boolean insertData(ContentValues cv){
        SQLiteDatabase db = this.getWritableDatabase();
        long l = db.insert(Config.TABLE_TEMP,null,cv);
        return l != -1;
    }
}
