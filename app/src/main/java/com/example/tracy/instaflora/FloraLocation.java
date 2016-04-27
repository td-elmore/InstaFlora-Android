package com.example.tracy.instaflora;

import android.content.Context;
import android.content.Intent;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;

public class FloraLocation extends FragmentActivity implements OnMapReadyCallback, GoogleMap.OnMapClickListener, LocationListener {

    private GoogleMap mMap;
    LocationManager locationManager;
    String provider;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_flora_location);

        // get the location manager to access the current location
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        provider = locationManager.getBestProvider(new Criteria(), false);
        MainActivity.instaLog("Best provider today is: " + provider);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        Location location;
        LatLng mapLocation;

        locationManager.requestLocationUpdates(provider, 400, 1, this);
        location = locationManager.getLastKnownLocation(provider);

        if (location != null) {
            mapLocation = new LatLng(location.getLatitude(), location.getLongitude());
        } else {
            // if can't get location, set to UC Berkeley Botanical Gardens
            mapLocation = new LatLng(37.875612, -122.238690);
        }

        //mMap.addMarker(new MarkerOptions().position(mapLocation).title("Flora Location"));
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(mapLocation, 16));
        mMap.setOnMapClickListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        locationManager.requestLocationUpdates(provider, 400, 1, this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        locationManager.removeUpdates(this);
    }

    @Override
    public void onMapClick(LatLng latLng) {
        MainActivity.instaLog("Map has been clicked " + latLng.toString());
        Intent data = new Intent();
        data.putExtra("lat", latLng.latitude);
        data.putExtra("lng", latLng.longitude);
        setResult(RESULT_OK, data);
        finish();
    }

    @Override
    public void onLocationChanged(Location location) {
        MainActivity.instaLog("onLocationChanged()");
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        MainActivity.instaLog("onStatusChanged()");
    }

    @Override
    public void onProviderEnabled(String provider) {
        MainActivity.instaLog("onProviderEnabled()");
    }

    @Override
    public void onProviderDisabled(String provider) {
        MainActivity.instaLog("onProviderDisabled()");
    }
}
