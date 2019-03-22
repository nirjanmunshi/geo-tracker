package com.project.nirjan.location.geotracker;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.location.LocationManager;
import android.os.Build;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.project.nirjan.location.geotracker.database.DbHelper;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.text.DecimalFormat;
import java.util.Objects;

import static com.project.nirjan.location.geotracker.Config.*;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    LocationService myService;
    static boolean status;
    LocationManager locationManager;
   // static TextView dist, time, speed;
    //Button start, pause, stop;
    static long startTime, endTime;
    //ImageView image;
    static ProgressDialog pDialog;
    static int p = 0;

    BroadcastReceiver receiver;
    TextView tvLatitude, tvLongitude, tvAccuracy;
    TextView tvCurrentSpeed, tvMaxSpeed,tvAvgSpeed;
    TextView tvTimeTaken;
    TextView tvDistanceCovered;
    Button btnStart, btnStop;
    double max_speed = 0.0;
    String location_data = null;
    DbHelper db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initialization();

        // add permission check
        File f = new File(Config.FOLDER_LOCATION);
        if (!f.exists())
            f.mkdir();

        db = new DbHelper(MainActivity.this);
        db.createTempTable();

        btnStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                    //The method below checks if Location is enabled on device or not. If not, then an alert dialog box appears with option
                    //to enable gps.
                    checkGps();
                    locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

                    if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                        Toast.makeText(MainActivity.this, "GPS not enabled", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    //Here, the Location Service gets bound and the GPS Speedometer gets Active.
                    if (!status)
                        bindService();
                    pDialog = new ProgressDialog(MainActivity.this);
                    pDialog.setIndeterminate(true);
                    pDialog.setCancelable(false);
                    pDialog.setMessage("Getting Location...");
                    pDialog.show();

                    btnStart.setEnabled(false);
                    btnStop.setEnabled(true);

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        startForegroundService(new Intent(MainActivity.this,LocationService.class));
                    } else
                        startService(new Intent(MainActivity.this,LocationService.class));

                    registerReceiver(receiver,new IntentFilter(getString(R.string.broadcast_key)));


            }
        });

        btnStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (status)
                    unbindService();
                btnStop.setEnabled(false);
                btnStart.setEnabled(true);
                p = 0;
                if (receiver != null)
                    unregisterReceiver(receiver);
                stopService(new Intent(MainActivity.this,LocationService.class));
            }
        });

    }

    private void initialization() {
        tvLatitude = findViewById(R.id.tvLatitude);
        tvLongitude = findViewById(R.id.tvLongitude);
        tvAccuracy = findViewById(R.id.tvAccuracy);
        tvCurrentSpeed = findViewById(R.id.tvCurrentSpeed);
        tvAvgSpeed = findViewById(R.id.tvAvgSpeed);
        tvTimeTaken = findViewById(R.id.tvTimeTaken);
        btnStart = findViewById(R.id.btnStart);
        btnStop = findViewById(R.id.btnStop);
        tvDistanceCovered = findViewById(R.id.tvDistanceCovered);
        tvMaxSpeed = findViewById(R.id.tvMaxSpeed);
    }


    //This method leads you to the alert dialog box.
    void checkGps() {
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER))
            showGPSDisabledAlertToUser();
    }

    //This method configures the Alert Dialog box.
    private void showGPSDisabledAlertToUser() {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setMessage("Enable GPS to use application")
                .setCancelable(false)
                .setPositiveButton("Enable GPS",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                Intent callGPSSettingIntent = new Intent(
                                        android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                                startActivity(callGPSSettingIntent);
                            }
                        });
        alertDialogBuilder.setNegativeButton("Cancel",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });
        AlertDialog alert = alertDialogBuilder.create();
        alert.show();
    }

    private ServiceConnection sc = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            LocationService.LocalBinder binder = (LocationService.LocalBinder) service;
            myService = binder.getService();
            status = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            status = false;
        }
    };

    void bindService() {
        if (status)
            return;
        Intent i = new Intent(getApplicationContext(), LocationService.class);
        bindService(i, sc, BIND_AUTO_CREATE);
        status = true;
        startTime = System.currentTimeMillis();
    }

    void unbindService() {
        if (!status)
            return;
        Intent i = new Intent(getApplicationContext(), LocationService.class);
        unbindService(sc);
        status = false;
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (receiver == null){
            receiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    location_data = Objects.requireNonNull(intent.getExtras()).getString(LOCATION_DATA);
                    updateUI(location_data);
                    Log.d(TAG, "onReceive: "+location_data);
                }
            };
        }
        registerReceiver(receiver,new IntentFilter(getString(R.string.broadcast_key)));


    }

    private void updateUI(String location_data) {
        double latitude = 0.0, longitude = 0.0, accuracy = 0.0, current_speed = 0.0, avg_speed = 0.0,distance = 0.0;
        long totalTime = 0;

        //double speed = intent.getExtras().getDouble(CURRENT_SPEED,0.0);
        //double avg_speed = intent.getExtras().getDouble(AVG_SPEED,0.0);
        //totalTime = intent.getExtras().getLong(TRAVEL_TIME,0);
        //double distance = intent.getExtras().getDouble(DISTANCE,0);
        try {
            JSONObject jo = new JSONObject(location_data);
            latitude = jo.getDouble(LATITUDE);
            longitude = jo.getDouble(LONGITUDE);
            accuracy = jo.getDouble(ACCURACY);
            current_speed = jo.getDouble(CURRENT_SPEED);
            avg_speed = jo.getDouble(AVG_SPEED);
            totalTime = jo.getLong(TRAVEL_TIME);
            distance = jo.getDouble(DISTANCE);

        } catch (JSONException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error in location data", Toast.LENGTH_SHORT).show();
        }

        // calculating max speed
        if (current_speed > max_speed)
            max_speed = current_speed;

        tvLatitude.setText(String.valueOf(latitude));
        tvLongitude.setText(String.valueOf(longitude));
        tvAccuracy.setText(String.valueOf(accuracy).concat(" m."));
        tvCurrentSpeed.setText(String.valueOf(new DecimalFormat("#.###").format(current_speed)));
        tvAvgSpeed.setText(String.valueOf(new DecimalFormat("#.###").format(avg_speed)));
        tvTimeTaken.setText(String.valueOf(totalTime));
        tvDistanceCovered.setText(String.valueOf(new DecimalFormat("#.###").format(distance)));
        tvMaxSpeed.setText(String.valueOf(new DecimalFormat("#.###").format(max_speed)));
    }

    @Override
    protected void onStart() {
        super.onStart();
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (status)
            unbindService();
    }

    @Override
    public void onBackPressed() {
        if (!status)
            super.onBackPressed();
        else
            moveTaskToBack(true);
    }

}
