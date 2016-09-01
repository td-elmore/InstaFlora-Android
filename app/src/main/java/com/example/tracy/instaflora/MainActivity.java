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
import android.os.Handler;
import android.support.design.widget.FloatingActionButton;

import android.support.v4.widget.SwipeRefreshLayout;
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
import com.parse.ParseFile;
import com.parse.ParseGeoPoint;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseQueryAdapter;
import com.parse.ParseUser;
import com.parse.SaveCallback;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

public class MainActivity extends AppCompatActivity implements SwipeRefreshLayout.OnRefreshListener {

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
    static boolean imagesPending = false;

    // result codes from outside activities
    static final int RESULT_FLORA_SETTINGS = 4;
    static final int RESULT_FLORA_ADD = 6;

    // ParseAdapter that contains all that is needed for the List View
    static boolean parseInitialized = false;
    ParseQueryAdapter pqAdapter = null;

    // Current location to keep updated if allFlora = false.
    LocationManager locationManager;
    String provider;
    double currentLat = 0.0;
    double currentLng = 0.0;

    // use standard swipe to refresh the listview
    SwipeRefreshLayout swipeLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        swipeLayout = (SwipeRefreshLayout) findViewById(R.id.swipe_container);
        swipeLayout.setOnRefreshListener(this);
        // color scheme needed?

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

        } else {
            loadFloraData();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        updateLocation();
        loadFloraData();

    }

    static public boolean isNetworkConnected() {
        Runtime runtime = Runtime.getRuntime();
        try {

            Process ipProcess = runtime.exec("/system/bin/ping -c 1 8.8.8.8");
            int     exitValue = ipProcess.waitFor();
            return (exitValue == 0);

        } catch (IOException e)          { e.printStackTrace(); }
        catch (InterruptedException e) { e.printStackTrace(); }

        return false;
        /* -- was using, but will give true also when internet is available,
         * but not connected.
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo ni = cm.getActiveNetworkInfo();

        if ((ni != null) && ni.isConnected())
         */
    }

    /*
     * loadFloraData()
     *
     * sets up the data store on startup
     * and after the settings or location have changed, this
     * is the main refresh function and handles the logic
     * for what happens when the network is connected and
     * when the connection has come back up.
     */
    public void loadFloraData() {

        if (isNetworkConnected()) {

            if (imagesPending) {
                loadMissingImages();
            } else {
                refreshFloraList(false);
            }

        } else {
            Toast.makeText(this, "No internet connection is available.", Toast.LENGTH_LONG).show();
            // if there is no internet activity,
            // show in the list view what is currently in the local datastore
            showCurrentFlora();
        }
    }

    /*
     * pinDataStoreFloras()
     *
     * boolean fromDatastore, true to get items saved in the cache,
     * or false if a network connection is available to get the
     * current data from the server
     *
     */
    public void pinFloras(boolean fromDataStore) {
        // use the current query to fill the local datastore and
        // refresh the adapter view
        ParseQuery<Flora> query = ParseQuery.getQuery("Flora");

        if (!allFlora) {
            instaLog("pinDataStoreFloras: !allFlora, rangeMiles= " + rangeMiles);
            query.whereWithinMiles("location", new ParseGeoPoint(currentLat, currentLng), rangeMiles);
        } else if (privateOnly) {
            instaLog("pinDataStoreFloras: privateOnly, username = " + ParseUser.getCurrentUser().getUsername());
            query.whereEqualTo("username", ParseUser.getCurrentUser().getUsername());
        }

        if (fromDataStore) {
            instaLog("pinFloras: localDatastore()");
            query.fromLocalDatastore().findInBackground(new FindCallback<Flora>() {
                @Override
                public void done(List<Flora> floras, ParseException e) {
                    if (e != null) {
                        instaLog("pinFloras: cannot load floras: " + e.getMessage());
                        return;
                    }
                    // pin the newly queried items.
                    ParseObject.pinAllInBackground(floras, new SaveCallback() {
                        public void done(ParseException e) {
                            if (e == null) {
                                MainActivity.instaLog("pinFloras: pinAllInBackground() success");
                                showCurrentFlora();
                            } else {
                                MainActivity.instaLog("pinFloras: Error pinning list items " + e.getMessage());
                            }
                        }
                    });
                }
            });
        } else {
            query.findInBackground(new FindCallback<Flora>() {
                @Override
                public void done(List<Flora> floras, ParseException e) {
                    if (e != null) {
                        instaLog("pinFloras: cannot load floras: " + e.getMessage());
                        return;
                    }
                    // pin the newly queried items.
                    ParseObject.pinAllInBackground(floras, new SaveCallback() {
                        public void done(ParseException e) {
                            if (e == null) {
                                MainActivity.instaLog("pinFloras: pinAllInBackground() success");
                                showCurrentFlora();
                            } else {
                                MainActivity.instaLog("pinFloras: Error pinning list items " + e.getMessage());
                            }
                        }
                    });
                }
            });
        }
    }


    /*
     * refreshFloraList()
     *
     * unpins all the items in the cached list and refills the cache
     * using the current query (from Settings)
     * -- called only if there is a reliable internet connection
     */
    void refreshFloraList(final boolean fromLocalDatastore) {
        // first delete items in the local datastore
        // this information is now old
        ParseObject.unpinAllInBackground(new DeleteCallback() {
            @Override
            public void done(ParseException e) {

                if (e != null) {
                    instaLog("refreshFloraList: unpin failed " + e.getMessage());
                    return;
                }

                pinFloras(fromLocalDatastore);
            }
        });

    }

    /*
     * loadMissingImages()
     *
     * retrieves images that have been saved when FloraAddItem() didn't have
     * a reliable internet connection.
     * -- called only when there is a reliable internet connection!!!
     */
    void loadMissingImages() {

        ParseQuery<Flora> query = ParseQuery.getQuery("Flora");

        // using the current query, get all pinned data
        if (!allFlora) {
            query.whereWithinMiles("location", new ParseGeoPoint(currentLat, currentLng), rangeMiles);
        } else if (privateOnly) {
            query.whereEqualTo("username", ParseUser.getCurrentUser().getUsername());
        }

        query.findInBackground(new FindCallback<Flora>() {
            @Override
            public void done(List<Flora> floras, ParseException e) {
                if (e != null) {
                    instaLog("loadFloraData: cannot load floras: " + e.getMessage());
                    return;
                }
                // check to see if any are missing their ParseFile (png image)
                for (Flora flora : floras) {
                    if (flora.getImageFile() == null) {
                        String deviceImgName = flora.getDeviceImgName();
                        if (deviceImgName.length() > 0) {
                            instaLog("loadMissingImages: " + deviceImgName);

                            final File imgFile = new File(getApplicationContext().getFilesDir(), deviceImgName);
                            ParseFile newParseFile = new ParseFile(imgFile);

                            flora.deleteDeviceImgName();
                            flora.setImageFile(newParseFile);
                            flora.saveInBackground(new SaveCallback() {
                                @Override
                                public void done(ParseException e) {
                                    if (e == null) {
                                        instaLog("loadMissingImages: success, saving flora image.");
                                        imgFile.delete();
                                    } else {
                                        instaLog("loadMissingImages: unsuccessful, " + e.getMessage());
                                    }
                                    refreshFloraList(false);
                                }
                            });
                        }
                    }
                }
                imagesPending = false;
                turnOffImagesPending();
            }
        });
    }

    public void turnOffImagesPending() {
        SharedPreferences sharedPreferences;

        sharedPreferences = this.getSharedPreferences("com.tzlandscapedesign.instaflora", Context.MODE_PRIVATE);
        sharedPreferences.edit().putBoolean("imagesPending", false).apply();
    }

    /*
     * updateLocation()
     *
     * if the user has chosen to get only those flora within a certain area
     * around them, then the current location is needed.
     */
    public void updateLocation() {

        SharedPreferences sharedPreferences;

        sharedPreferences = this.getSharedPreferences("com.tzlandscapedesign.instaflora", Context.MODE_PRIVATE);

        // if the gps system is delayed or not working for some reason
        // this sets the default to the UC Berkeley Botanical Garden, Berkeley, CA USA
        // hopefully, though, the most recent gps location has been saved
        long lat = Double.doubleToLongBits(37.875612);
        long lng = Double.doubleToLongBits(-122.238690);
        lng = sharedPreferences.getLong("longitude", lng);
        lat = sharedPreferences.getLong("latitude", lat);
        currentLng = Double.longBitsToDouble(lng);
        currentLat = Double.longBitsToDouble(lat);

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        provider = locationManager.getBestProvider(new Criteria(), true);
        instaLog("updateLocation: provider: " + provider);

        Location location = locationManager.getLastKnownLocation(provider);
        if (location != null) {
            instaLog("updateLocation: success getting location!");
            currentLat = location.getLatitude();
            currentLng = location.getLongitude();

            // save the current location for use later
            lat = Double.doubleToLongBits(currentLat);
            lng = Double.doubleToLongBits(currentLng);
            sharedPreferences.edit().putLong("longitude", lng).apply();
            sharedPreferences.edit().putLong("latitude", lat).apply();
        }
    }


    /*
     * Used in FloraAddItem to show a description of the location based on the
     * longitude and latitude in the TextView provided. Ensures that the description
      * is uniform throughout the App.
     */
    static public boolean setLocationDescription(Context context, TextView textView, double lat, double lng) {

        if (!isNetworkConnected()) {
            return false;
        }

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
            return false;
        }
        return true;
    }

    public void updateListView(){
        instaLog("updateListView()");
        if (pqAdapter != null) {
            pqAdapter.loadObjects();
        }
    }

    /*
     * showCurrentFlora() is called to initialize the pqAdapter
     */
    public void showCurrentFlora() {

        instaLog("showCurrentFlora()");
        ListView listView = (ListView) findViewById(R.id.listView);

        pqAdapter = new LargePhotoParseQueryAdapter(this, new ParseQueryAdapter.QueryFactory<Flora>() {
            @Override
            public ParseQuery<Flora> create() {

                ParseQuery<Flora> query = ParseQuery.getQuery("Flora");

                query.fromLocalDatastore()
                        .orderByDescending("deviceImgName")
                        .addDescendingOrder("updatedAt");

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
                    instaLog("showCurrentFlora(): obj is null");
                }

            }
        });
    }

    public void initializePreferences () {

        if (!preferencesInitialized) {
            SharedPreferences sharedPreferences;

            sharedPreferences = this.getSharedPreferences("com.tzlandscapedesign.instaflora", Context.MODE_PRIVATE);
            if (sharedPreferences != null) {

                imagesPending = sharedPreferences.getBoolean("imagesPending", false);
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
        instaLog("initializePreferences: imagesPending: " + imagesPending);
    }

    public void initializeParse() {

        if (!parseInitialized) {
            ParseObject.registerSubclass(Flora.class);
            //Parse.enableLocalDatastore(this);
            // originally used in simplest form api.parse.com
            // can no longer use their servers 5/4/2016
            //Parse.initialize(this);
            // api.parse.com APP_ID: 3OS3hHGLLZlOkiawSIDOTYGsva5eF6tQIfJ8Lua5
            // api.parse.com CLIENT_KEY: H1LsY3nV4kMlPsNu5AoZxubRo76MWPG3vreO80eH
            Parse.initialize(new Parse.Configuration.Builder(this)
                            .enableLocalDataStore()
                            .server("http://instaflora.herokuapp.com/parse/")
                            .build()
            );
            parseInitialized = true;
        }
    }

    static public void instaLog(String log) {

        Log.i("instalog", log);

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
            case R.id.settings:
                Intent settingsIntent = new Intent(this, FloraSettings.class);
                startActivityForResult(settingsIntent, RESULT_FLORA_SETTINGS);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onRefresh() {
        new Handler().postDelayed(new Runnable() {
            @Override public void run() {
                swipeLayout.setRefreshing(false);
                loadFloraData();
            }
        }, 5000);
    }
}
