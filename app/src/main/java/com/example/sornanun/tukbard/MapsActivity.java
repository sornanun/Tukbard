package com.example.sornanun.tukbard;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import dmax.dialog.SpotsDialog;


public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private GoogleMap mMap;
    private GoogleApiClient mGoogleApiClient;
    private Location mLastLocation;
    private Double latitude;
    private Double longitude;
    private ImageButton changeViewBTN;
    private int DistanceForLook;
    private int TimeForUpdateLocation;
    SpotsDialog dialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        changeViewBTN = (ImageButton) findViewById(R.id.changeView_btn);
        changeViewBTN.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showMapTypeSelectorDialog();
            }
        });

        // Create an instance of GoogleAPIClient.
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }

        getPreferencesFromLocal();
    }

    private void getPreferencesFromLocal() {
        // get value was setting from local data
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        DistanceForLook = Integer.valueOf(settings.getString("distance_for_look", "1"));
        TimeForUpdateLocation = Integer.valueOf(settings.getString("time_for_update_location", "10"));
        // Convert data
        DistanceForLook = DistanceForLook * 1000; // 1000 meter is 1 kilometer
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        if (mLastLocation != null) {
            latitude = mLastLocation.getLatitude();
            longitude = mLastLocation.getLongitude();

            // set default map focus to last know location
            CameraUpdate point = CameraUpdateFactory.newLatLngZoom(new LatLng(latitude, longitude), 14.0f);
            // moves camera to coordinates
            mMap.moveCamera(point);
            // animates camera to coordinates
            mMap.animateCamera(point);
        } else {
            // set default map focus to bangkok
            CameraUpdate point = CameraUpdateFactory.newLatLngZoom(new LatLng(13.751263, 100.504173), 7.0f);
            // moves camera to coordinates
            mMap.moveCamera(point);
            // animates camera to coordinates
            mMap.animateCamera(point);
        }
        mMap.getUiSettings().setZoomControlsEnabled(true);

        // Process of this app
        stepProcess();
    }

    @Override
    public void onConnectionSuspended(int i) {
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
    }

    protected void onStart() {
        mGoogleApiClient.connect();
        super.onStart();
    }

    protected void onStop() {
        mGoogleApiClient.disconnect();
        super.onStop();
    }

    @Override
    public void onMapReady(GoogleMap map) {
        mMap = map;
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
        } else {
            // Show rationale and request permission.
        }
    }

    //////------------------ Custom methods ---------------------

    private static final ScheduledExecutorService worker =
            Executors.newSingleThreadScheduledExecutor();

    // this boolean firstGet create for check get data from parse is first or not
    public void timeToGetDataFromParse() {
        dialog.setMessage("กำลังดึงข้อมูลตำแหน่งพระสงฆ์...");
        // next time to get data from parse is in this loop
        getPreferencesFromLocal(); // get data application setting that user just set
        getMonkLocationFromParse(); // get data from parse when this method is called
        Log.d("Preference", "Distance for look : " + DistanceForLook + " Time : " + TimeForUpdateLocation);

        // check this activity is opening.
        if (mGoogleApiClient.isConnected()) {
            Runnable task = new Runnable() {
                public void run() {
                    timeToGetDataFromParse();
                }
            };
            worker.schedule(task, TimeForUpdateLocation, TimeUnit.SECONDS);
        }
    }

    public void stepProcess() {

        if (isNetworkAvailable(this.getApplicationContext())) {
            if (checkLocationEnabled() == true) {
                dialog = new SpotsDialog(MapsActivity.this,"กรุณารอสักครู่...");
                dialog.show();
                if(canGetLocation()){
                    setCurrentLocation();
                }
                else{
                    dialog.setMessage("กำลังระบุตำแหน่งปัจจุบัน...");
                    while(canGetLocation() == false){
                        try {
                            Thread.sleep(1000);    //1000 milliseconds is one second.
                        } catch(InterruptedException ex) {
                            Thread.currentThread().interrupt();
                        }
                    }
                    setCurrentLocation();
                }
                timeToGetDataFromParse();
            } else {
                final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                final String action = Settings.ACTION_LOCATION_SOURCE_SETTINGS;
                final String message = "แอพพลิเคชั่นต้องการเปิดการระบุตำแหน่งบนอุปกรณ์ของคุณ \nคุณต้องการเปิด GPS หรือไม่";
                builder.setMessage(message)
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface d, int id) {
                                //// Open setting
                                startActivity(new Intent(action));
                                //// check permission before get location
                                if (ContextCompat.checkSelfPermission(MapsActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                                    mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
                                    if (mLastLocation != null)
                                        stepProcess(); // if can get location will go to next process
                                }
                                d.dismiss(); // close alert dialog
                            }
                        })
                        .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface d, int id) {
                                d.cancel();
                            }
                        });
                builder.create().show();
            }
        } else {
            new AlertDialog.Builder(this)
                    .setTitle("ตรวจสอบการเชื่อมต่อ")
                    .setMessage("ต้องการเชื่อมต่ออินเทอร์เน็ต โปรดตรวจสอบการเชื่อมต่ออินเทอร์เน็ตเพื่อค้นหาสถานที่ของพระสงฆ์")
                    .setPositiveButton("รับทราบ", null).show();
        }
    }

    public boolean isNetworkAvailable(final Context context) {
        final ConnectivityManager connectivityManager = ((ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE));
        return connectivityManager.getActiveNetworkInfo() != null && connectivityManager.getActiveNetworkInfo().isConnected();
    }

    public void getMonkLocationFromParse() {
        ParseQuery<ParseObject> query = ParseQuery.getQuery("Monk_Location");
        query.findInBackground(new FindCallback<ParseObject>() {
            public void done(List<ParseObject> monk_location_list, ParseException e) {
                if (e == null) {
                    // remove monk marker if over limit distance
                    selectOnlyDistance(monk_location_list);
                } else {
                    Log.d("Monk_Location", "Error: " + e.getMessage());
                }
            }
        });
    }

    public void selectOnlyDistance(List<ParseObject> monk_location_list) {
        int len = monk_location_list.size();
        if (len > 0) {
            Location your_location = new Location("YourLocation");
            your_location.setLatitude(latitude);
            your_location.setLongitude(longitude);

            for (int i = 0; i < len; i++) {
                ParseObject p = monk_location_list.get(i);
                Double MonkLat = Double.valueOf(p.getString("lat"));
                Double MonkLong = Double.valueOf(p.getString("long"));
                String Address = p.getString("address");

                Location monk_location = new Location("MonkLocation");
                monk_location.setLatitude(MonkLat);
                monk_location.setLongitude(MonkLong);

                double distance = your_location.distanceTo(monk_location);
                if (distance <= DistanceForLook) {
                    setMarker(MonkLat, MonkLong, Address);
                }
                dialog.dismiss();
            }
        } else {
            new AlertDialog.Builder(this).setTitle("ขออภัย")
                    .setMessage("ไม่พบสถานที่ของพระสงฆ์ที่บิณฑบาตในระยะ " + (DistanceForLook / 1000) + " กิโลเมตร")
                    .setPositiveButton("รับทราบ", null).show();
        }
    }

    public void setMarker(Double MonkLat, Double MonkLong, String Detail) {
        // add marker on map
        mMap.addMarker(new MarkerOptions().position(new LatLng(MonkLat, MonkLong)).title(Detail).icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_map_marker)));
        Log.d("Near Monk ", "lat :" + MonkLat + " long : " + MonkLong + " address : " + Detail);
    }

    public boolean checkLocationEnabled() {
        LocationManager mlocManager = (LocationManager) getBaseContext().getSystemService(Context.LOCATION_SERVICE);
        boolean enabled = mlocManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        return enabled;
    }

    public boolean canGetLocation() {
        boolean connectionEnabled = false;
        //// check permission before get location
        if (ContextCompat.checkSelfPermission(MapsActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
            if (mLastLocation != null) {
                connectionEnabled = true;
            }
        }
        return connectionEnabled;
    }

    public void setCurrentLocation() {
        latitude = mLastLocation.getLatitude();
        longitude = mLastLocation.getLongitude();

        LatLng currentLocation = new LatLng(latitude, longitude);

        // set zoom level
        float zoomLevel = 14.0f; // default
        if (DistanceForLook == 1000) zoomLevel = 15.0f;
        else if (DistanceForLook == 2000) zoomLevel = 14.2f;
        else if (DistanceForLook == 3000) zoomLevel = 13.6f;
        else if (DistanceForLook == 4000) zoomLevel = 13.0f;
        else if (DistanceForLook == 5000) zoomLevel = 12.5f;
        else if (DistanceForLook == 10000) zoomLevel = 11.2f;

        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, zoomLevel));
    }

    private static final CharSequence[] MAP_TYPE_ITEMS = {"เส้นทาง", "ดาวเทียม", "ผสม"};

    private void showMapTypeSelectorDialog() {
        // Prepare the dialog by setting up a Builder.
        final String fDialogTitle = "เลือกรูปแบบแผนที่";
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(fDialogTitle);

        // Find the current map type to pre-check the item representing the current state.
        int checkItem = mMap.getMapType() - 1;

        // Add an OnClickListener to the dialog, so that the selection will be handled.
        builder.setSingleChoiceItems(
                MAP_TYPE_ITEMS,
                checkItem,
                new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int item) {
                        // Locally create a finalised object.

                        // Perform an action depending on which item was selected.
                        switch (item) {
                            case 1:
                                mMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
                                break;
                            case 2:
                                mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
                                break;
                            default:
                                mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
                        }
                        dialog.dismiss();
                    }
                }
        );

        // Build the dialog and show it.
        AlertDialog fMapTypeDialog = builder.create();
        fMapTypeDialog.setCanceledOnTouchOutside(true);
        fMapTypeDialog.show();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
        overridePendingTransition(R.anim.trans_right_in, R.anim.trans_right_out);
    }
}
