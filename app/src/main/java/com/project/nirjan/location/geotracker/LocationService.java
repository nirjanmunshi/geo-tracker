package com.project.nirjan.location.geotracker;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.location.Location;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.project.nirjan.location.geotracker.database.DbHelper;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.concurrent.TimeUnit;

public class LocationService extends Service implements LocationListener, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private static final String TAG = "LocationService";
    private static final long INTERVAL = 1000 * 2;
    private static final long FASTEST_INTERVAL = 1000 * 1;
    LocationRequest mLocationRequest;
    GoogleApiClient mGoogleApiClient;
    Location mCurrentLocation, lStart, lEnd;
    static double distance = 0;
    double speed, max_speed = 0.0, avg_speed = 0.0;
    private final IBinder mBinder = new LocalBinder();
    float [] a;
    JSONObject jo = null;
    DbHelper db;

    @Override
    public IBinder onBind(Intent intent) {
        createLocationRequest();
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
        mGoogleApiClient.connect();
        return mBinder;
    }

    @SuppressLint("RestrictedApi")
    private void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(INTERVAL);
        mLocationRequest.setFastestInterval(FASTEST_INTERVAL);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }


    @SuppressLint("MissingPermission")
    @Override
    public void onConnected(@Nullable Bundle bundle) {
        try{
            LocationServices.FusedLocationApi.requestLocationUpdates(
                    mGoogleApiClient,mLocationRequest,this
            );
        } catch (SecurityException e){
            Toast.makeText(this, "Security Exception", Toast.LENGTH_SHORT).show();
        }

    }

    protected void stopLocationUpdates() {
        LocationServices.FusedLocationApi.removeLocationUpdates(
                mGoogleApiClient, this);
        distance = 0;
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onLocationChanged(Location location) {
        MainActivity.pDialog.dismiss();
        mCurrentLocation = location;
        if (lStart == null) {
            lStart = mCurrentLocation;
            lEnd = mCurrentLocation;
        } else
            lEnd = mCurrentLocation;

        speed = location.getSpeed() * 18 / 5;
        distance = (lStart.distanceTo(lEnd) / 1000.00);


        MainActivity.endTime = System.currentTimeMillis();
        long travelTime = MainActivity.endTime - MainActivity.startTime;
        travelTime = TimeUnit.MILLISECONDS.toMinutes(travelTime);

        if (speed > max_speed)
            max_speed = speed;

        if (travelTime != 0.0)
            avg_speed = (distance * 60)/travelTime;

        HashMap<Object,Object> hashMap = new HashMap<>();
        Log.d(TAG, "onAccuracyChanged: "+location.getAccuracy());

        hashMap.put(Config.LATITUDE,location.getLatitude());
        hashMap.put(Config.LONGITUDE,location.getLongitude());
        hashMap.put(Config.ACCURACY,location.getAccuracy());
        hashMap.put(Config.PROVIDER,location.getProvider());
        hashMap.put(Config.CURRENT_SPEED,speed);
        hashMap.put(Config.MAX_SPEED,max_speed);
        hashMap.put(Config.AVG_SPEED,avg_speed);
        hashMap.put(Config.DISTANCE,distance);
        hashMap.put(Config.TRAVEL_TIME,travelTime);


        jo = new JSONObject(hashMap);
        Log.d(TAG, "onLocationChanged: "+jo.toString());

        Intent i = new Intent(getApplicationContext().getString(R.string.broadcast_key));
        i.putExtra(Config.LOCATION_DATA,jo.toString());
        i.putExtra(Config.CURRENT_SPEED,speed);
        i.putExtra(Config.MAX_SPEED,max_speed);
        i.putExtra(Config.AVG_SPEED,avg_speed);
        i.putExtra(Config.DISTANCE,distance);
        i.putExtra(Config.TRAVEL_TIME,travelTime);

        ContentValues cv = new ContentValues();
        cv.put(Config.LOCATION,jo.toString());

        sendBroadcast(i);
    }

    public class LocalBinder extends Binder {
        public LocationService getService() {
            return LocationService.this;
        }
    }

    @Override
    public boolean onUnbind(Intent intent) {
        stopLocationUpdates();
        if (mGoogleApiClient.isConnected())
            mGoogleApiClient.disconnect();
        lStart = null;
        lEnd = null;
        distance = 0;
        return super.onUnbind(intent);
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            startMyOwnForeground();
            db = new DbHelper(getApplicationContext());
        } else{
            startForeground(1, new Notification());
        }
        return START_STICKY;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void startMyOwnForeground(){
        String NOTIFICATION_CHANNEL_ID = "com.project.nirjan.location.geotracker";
        String channelName = "channel2019";
        NotificationChannel chan = new NotificationChannel(NOTIFICATION_CHANNEL_ID, channelName, NotificationManager.IMPORTANCE_NONE);
        chan.setLightColor(Color.BLUE);
        chan.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        assert manager != null;
        manager.createNotificationChannel(chan);
        String s = "Getting location. Please wait ";
        if (jo != null){
            try {
                s = getString(
                        R.string.notification_text,
                        jo.getDouble(Config.LATITUDE),
                        jo.getDouble(Config.LONGITUDE),
                        jo.getDouble(Config.ACCURACY)

                );
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);


        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID);
        Notification notification = notificationBuilder.setOngoing(true)
                .setSmallIcon(R.drawable.ic_launcher_background)
                .setContentTitle("App is running in background")
                .setPriority(NotificationManager.IMPORTANCE_MIN)
                .setCategory(Notification.CATEGORY_SERVICE)
                .setContentText(s)
                .setContentIntent(pendingIntent)
                .build();

//        Intent notificationIntent = new Intent(getApplicationContext(),MainActivity.class);
//        notification.contentIntent = PendingIntent.getActivity(this.getApplicationContext(), 0, notificationIntent, 0);
        startForeground(2, notification);
    }
}
