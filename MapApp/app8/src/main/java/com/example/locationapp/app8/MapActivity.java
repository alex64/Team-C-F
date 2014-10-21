package com.example.locationapp.app8;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

/**
 * Created by alex64 on 16/10/2014.
 */
public class MapActivity extends FragmentActivity {

    private GoogleMap googleMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.map);

        try {
            // Loading map
            initilizeMap();

        } catch (Exception e) {
            e.printStackTrace();
        }

        Log.d("Message", "Map2");
    }

    private void initilizeMap() {
        if (googleMap == null) {
            googleMap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(
                    R.id.map)).getMap();

            // check if map is created successfully or not
            if (googleMap == null) {
                Toast.makeText(getApplicationContext(),
                        "Sorry! unable to create maps", Toast.LENGTH_SHORT)
                        .show();
            }

            else{
                Intent intent = getIntent();
                Double lat = intent.getDoubleExtra("Lat", 0.0);
                Double lon = intent.getDoubleExtra("Lon", 0.0);
                Log.d("Lat", lat.toString());
                Log.d("Lon", lon.toString());

                // create marker
                MarkerOptions marker = new MarkerOptions().position(new LatLng(lat, lon)).title("Hello Maps ");

// adding marker
                googleMap.addMarker(marker);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        initilizeMap();
    }
}
