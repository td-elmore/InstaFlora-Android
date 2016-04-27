package com.example.tracy.instaflora;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import android.location.Address;
import android.location.Criteria;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;

import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;

import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.parse.DeleteCallback;
import com.parse.FindCallback;
import com.parse.Parse;
import com.parse.ParseException;
import com.parse.ParseGeoPoint;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseQueryAdapter;
import com.parse.ParseUser;
import com.parse.SaveCallback;

import java.io.IOException;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    // preference settings used here to define main listview contents
    static final int RANGE_ALLFLORA = 0;
    static final int RANGE_ONEMILE = 1;
    static final int RANGE_FIVEMILE = 2;
    static final int RANGE_TENMILE = 3;

    static int rangeMiles = 1;
    static boolean allFlora = false;
    static boolean preferencesInitialized = false;
    static boolean privateOnly = false;

    // set in FloraAddItem Activity when save is finished
    static boolean loadFloraPending = false;
    static boolean showFloraPending = true;

    // result codes from outside activities
    static final int RESULT_FLORA_SETTINGS = 4;
    static final int RESULT_FLORA_ADD = 6;

    // ParseAdapter that contains all that is needed for the List View
    static boolean parseInitialized = false;
    ParseQueryAdapter pqAdapter = null;

    // Current location to keep updated if allFlora = false.
    LocationManager locationManager;
    String provider;
    double currentLat;
    double currentLng;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        initializeParse();
        initializePreferences();
        updateLocation();

        // Floating Action Button Adds a new Flora Entry
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                System.gc();
                Intent intent = new Intent(getApplicationContext(), FloraAddItem.class);
                startActivityForResult(intent, RESULT_FLORA_ADD);

            }
        });

        // Parse is working, go to login screen if needed
        if (ParseUser.getCurrentUser() == null) {
            // bring up an activity to have the user log in

            Intent i = new Intent(this, FloraParseLogin.class);
            startActivity(i);
            loadFloraPending = true;

        } else {
            showCurrentFlora();
            loadFloraData();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        updateLocation();
        if (loadFloraPending) {
            loadFloraData();
        }
        showCurrentFlora();
    }

    /*
     * loadFloraData()
     *
     * sets up the data store on startup
     * and after the settings or location have changed.
     */
    public void loadFloraData() {

        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo ni = cm.getActiveNetworkInfo();

        if ((ni != null) && ni.isConnected()) {

            // first delete any items that may already be in the local datastore
            // this information is now old if you are calling loadFloraData()
            ParseObject.unpinAllInBackground(new DeleteCallback() {
                @Override
                public void done(ParseException e) {

                    if (e != null) {
                        instaLog("unpin failed " + e.getMessage());
                        return;
                    }
                    // when that is done, use the current query to fill the local datastore and
                    // refresh the adapter view
                    ParseQuery<Flora> query = ParseQuery.getQuery("Flora");

                    if (!allFlora) {
                        query.whereWithinMiles("location", new ParseGeoPoint(currentLat, currentLng), rangeMiles);
                    } else if (privateOnly) {
                        query.whereEqualTo("username", ParseUser.getCurrentUser().getUsername());
                    }

                    query.findInBackground(new FindCallback<Flora>() {
                        @Override
                        public void done(List<Flora> floras, ParseException e) {
                            if (e != null) {
                                instaLog("cannot load floras: "+ e.getMessage());
                                return;
                            }

                            ParseObject.pinAllInBackground(floras, new SaveCallback() {
                                public void done(ParseException e) {
                                    if (e == null) {
                                        if (!isFinishing()) {
                                            pqAdapter.loadObjects();
                                        }
                                        MainActivity.instaLog("pinAllInBackground() success");
                                    } else {
                                        MainActivity.instaLog("Error pinning list items " + e.getMessage());
                                    }
                                }
                            });
                        }
                    });
                    showFloraPending = true;
                }
            });

        } else {
            Toast.makeText(this, "No internet access. Click REFRESH when connection is available", Toast.LENGTH_LONG).show();
            // if there is not internet activity, just load what is currently in the local datastore
            if (pqAdapter != null) {
                pqAdapter.loadObjects();
            }
            // when the user refreshes, this tries again
            loadFloraPending = true;
        }
    }

    /*
     * updateLocation()
     *
     * if the user has chosen to get only those flora within a certain area
     * around them, then the current location is needed.
     */
    public void updateLocation() {

        if (!allFlora) {
            locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            provider = locationManager.getBestProvider(new Criteria(), true);

            Location location = locationManager.getLastKnownLocation(provider);
            if (location == null) {
                // dummy location to use for emulator or other stubborn devices
                // set to UCBerkeley Botanical Gardens
                currentLat = 37.875612;
                currentLng = -122.238690;
            } else {
                currentLat = location.getLatitude();
                currentLng = location.getLongitude();
            }
        } else {
            currentLat = 0.0;
            currentLng = 0.0;
        }
    }

    /*
     * Used in FloraAddItem to show a description of the location based on the
     * longitude and latitude in the TextView provided. Ensures that the description
      * is uniform throughout the App.
      * TODO: put this in another thread
     */
    static public void setLocationDescription(Context context, TextView textView, double lat, double lng) {

        String addressText = "";
        Geocoder geocoder = new Geocoder(context);

        try {
            List<Address> listAddresses = geocoder.getFromLocation(lat, lng, 1);

            if (listAddresses != null && listAddresses.size() > 0) {

                Address currentAddress = listAddresses.get(0);
                if (currentAddress!= null) {
                    if (currentAddress.getLocality() != null &&
                            currentAddress.getCountryName() != null){
                        addressText = currentAddress.getLocality() + ", " +
                                currentAddress.getCountryName();
                    } else if (currentAddress.getCountryName() != null) {
                        addressText = currentAddress.getCountryName();
                    }
                } else {
                    addressText = "Unnamed address (lat: " + lat + " lng: " + lng + ")";
                }
                textView.setText(addressText);
            } else {
                textView.setText(addressText);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    /*
     * showCurrentFlora() is called any time an update to the ListView is needed
     * it retrieves entries from the local data store which is filled at startup
     * and when settings are changed
     */
    public void showCurrentFlora() {

        ListView listView = (ListView) findViewById(R.id.listView);

        // if pqAdapter is already initialized and the data in the
        // data store has not changed, then refresh the data
        if (pqAdapter != null && !showFloraPending) {
            pqAdapter.loadObjects();
            return;
        }

        pqAdapter = new LargePhotoParseQueryAdapter(this, new ParseQueryAdapter.QueryFactory<Flora>() {
            @Override
            public ParseQuery<Flora> create() {

                ParseQuery<Flora> query = ParseQuery.getQuery("Flora");

                query.addDescendingOrder("updatedAt");
                query.fromLocalDatastore();

                return query;
            }
        });

        listView.setAdapter(pqAdapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                ParseObject obj = pqAdapter.getItem(position);
                if (obj != null) {

                    String objectId = obj.getObjectId();
                    Intent intent = new Intent(view.getContext(), FloraDetails.class);
                    intent.putExtra("objectId", objectId);
                    startActivity(intent);

                } else {
                    instaLog("obj is null");
                }

            }
        });

        showFloraPending = false;
    }

    public void initializePreferences () {

        if (!preferencesInitialized) {
            SharedPreferences sharedPreferences;

            sharedPreferences = this.getSharedPreferences("com.example.tracy.instaflora", Context.MODE_PRIVATE);
            if (sharedPreferences != null) {
                privateOnly = sharedPreferences.getBoolean("privateOnly", false);

                switch (sharedPreferences.getInt("range", RANGE_ALLFLORA)) {
                    case RANGE_ONEMILE:
                        rangeMiles = 1;
                        break;
                    case RANGE_FIVEMILE:
                        rangeMiles = 5;
                        break;
                    case RANGE_TENMILE:
                        rangeMiles = 10;
                        break;
                    default:
                        allFlora = true;
                        break;
                }
            } else {
                // default settings
                rangeMiles = 0;
                allFlora = true;
                privateOnly = false;
            }
            preferencesInitialized = true;
        }
    }

    public void initializeParse() {

        if (!parseInitialized) {
            ParseObject.registerSubclass(Flora.class);
            Parse.enableLocalDatastore(this);
            Parse.initialize(this);
            parseInitialized = true;
        }
    }

    static public void instaLog(String log) {

        //Log.i("instalog", log);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (resultCode == RESULT_CANCELED){
            return;
        }

        if (requestCode == RESULT_FLORA_SETTINGS) {
            updateLocation();
            loadFloraData();
            showCurrentFlora();
        } else if (requestCode == RESULT_FLORA_ADD) {
            // the newly saved item might not be done updating
            // yet which causes errors, so let the user
            // refresh the list once they get the Toast()
            // that the save is done.
        }


    }

        @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();

        switch (id) {
            case R.id.logout:
                ParseUser.logOut();
                Intent loginIntent = new Intent(this, FloraParseLogin.class);
                startActivity(loginIntent);
                return true;
            case R.id.refresh:
                if (loadFloraPending) {
                    loadFloraData();
                }
                showCurrentFlora();
                return true;
            case R.id.settings:
                Intent settingsIntent = new Intent(this, FloraSettings.class);
                startActivityForResult(settingsIntent, RESULT_FLORA_SETTINGS);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

}
