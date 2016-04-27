package com.example.tracy.instaflora;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.parse.GetCallback;
import com.parse.ParseACL;
import com.parse.ParseException;
import com.parse.ParseGeoPoint;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;


import java.util.Date;

public class FloraDetails extends AppCompatActivity {

    boolean canEdit;
    double lat;
    double lng;

    // objectId is used to retrieve object once initialized
    String objectId;

    // menu items are hidden if user does not own the Flora Item selected
    Menu detailsMenu = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_flora_details);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ActionBar ab = getSupportActionBar();
        if (ab != null) {
            ab.setDisplayHomeAsUpEnabled(true);
        }

        if (!initializeFields()) {
            setResult(RESULT_CANCELED);
            finish();
        }

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // when clicking here navigate to the plant
                MainActivity.instaLog("Floating Navigation Button Clicked");
                // directly from the guide on the following web page
                // https://developers.google.com/maps/documentation/android-api/intents
                if (lat != 0.0 && lng != 0.0) {
                    Uri gmmIntentUri = Uri.parse("google.navigation:q=" + String.valueOf(lat) + "," + String.valueOf(lng));
                    Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
                    mapIntent.setPackage("com.google.android.apps.maps");
                    if (mapIntent.resolveActivity(getPackageManager()) != null) {
                        startActivity(mapIntent);
                    }
                } else {
                    Toast.makeText(getApplicationContext(), "Sorry, the location saved for this plant is invalid", Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    /*
     * saveNewInfo()
     *
     * whether or not user has changed any info, if they select save
     * in the menu bar, the information will be retrieved from the user
     * interface and the flora item with the global objectId will be
     * saved to Parse.
     *
     */
    public void saveNewInfo() {

        // button will not show if the user is not allowed to edit item

        EditText editCommon = (EditText) findViewById(R.id.editCommonName);
        final String common = editCommon.getText().toString();
        EditText editBotanical = (EditText) findViewById(R.id.editBotanicalName);
        final String botanical = editBotanical.getText().toString();
        EditText editDescription = (EditText) findViewById(R.id.editDescription);
        final String description = editDescription.getText().toString();
        CheckBox sharedBox = (CheckBox) findViewById(R.id.checkShare);
        final boolean shared = sharedBox.isChecked();

        ParseQuery<Flora> query = ParseQuery.getQuery("Flora");

        query.getInBackground(objectId, new GetCallback<Flora>() {
            @Override
            public void done(Flora flora, ParseException e) {
                if (e == null) {
                    MainActivity.instaLog("object found with Object ID: " + flora.getObjectId());
                    flora.setCommonName(common);
                    flora.setBotanical(botanical);
                    flora.setDescription(description);

                    if (shared) {
                        ParseACL acl = new ParseACL();
                        acl.setPublicReadAccess(true);
                        acl.setWriteAccess(ParseUser.getCurrentUser(), true);
                        flora.setACL(acl);
                    } else {
                        flora.setACL(new ParseACL(ParseUser.getCurrentUser()));
                    }

                    // this will update the local datastore if the object is there
                    flora.saveInBackground();
                } else {
                    MainActivity.instaLog("object not found" + e.getMessage());
                }
            }
        });

        finish();
    }

    /*
     * initializeFields()
     *
     * checks if the objectID is there and if so, gets the flora item to show to the user
     *
     * if the user owns this item, changes all UI items to allow user to modify/delete/save
     * this item. Does not allow user to change image or location of this item.
     */
    public boolean initializeFields() {

        Intent intent = getIntent();
        objectId = intent.getStringExtra("objectId");

        lat = 0.0;
        lng = 0.0;

        // set to true if the user owns the item
        canEdit = false;

        if (objectId != null) {
            ParseQuery<Flora> pquery = ParseQuery.getQuery("Flora");
            pquery.whereEqualTo("objectId", objectId);

            pquery.getFirstInBackground(new GetCallback<Flora>() {
                @Override
                public void done(Flora flora, ParseException e) {
                    if (e == null) {
                        // save to globals for when user clicks to navigate to item
                        ParseGeoPoint point = flora.getLocation();
                        lat = point.getLatitude();
                        lng = point.getLongitude();

                        // set up editable items
                        String common = flora.getCommonName();
                        String botanical = flora.getBotanical();
                        String description = flora.getDescription();
                        Date createdDate = flora.getCreatedAt();

                        TextView commonText = (TextView) findViewById(R.id.textCommonName);
                        TextView botanicalText = (TextView) findViewById(R.id.textBotanicalName);
                        TextView descriptionText = (TextView) findViewById(R.id.textDescription);
                        CheckBox checkShare = (CheckBox) findViewById(R.id.checkShare);

                        // if this user owns this item, then allow editing
                        ParseACL acl = flora.getACL();
                        if (acl.getWriteAccess(ParseUser.getCurrentUser())) {
                            canEdit = true;
                            commonText.setVisibility(View.INVISIBLE);
                            botanicalText.setVisibility(View.INVISIBLE);
                            descriptionText.setVisibility(View.INVISIBLE);
                            EditText commonEdit = (EditText) findViewById(R.id.editCommonName);
                            commonEdit.setVisibility(View.VISIBLE);
                            commonEdit.setText(common);
                            EditText botanicalEdit = (EditText) findViewById(R.id.editBotanicalName);
                            botanicalEdit.setVisibility(View.VISIBLE);
                            botanicalEdit.setText(botanical);
                            EditText descriptionEdit = (EditText) findViewById(R.id.editDescription);
                            descriptionEdit.setVisibility(View.VISIBLE);
                            descriptionEdit.setText(description);

                            if (acl.getPublicReadAccess()) {
                                checkShare.setChecked(true);
                            } else {
                                checkShare.setChecked(false);
                            }

                        } else {
                            if (detailsMenu != null) {
                                detailsMenu.clear();
                            }
                            checkShare.setVisibility(View.INVISIBLE);

                            descriptionText.setText(description);
                            botanicalText.setText(botanical);
                            commonText.setText(common);
                        }

                        TextView ownerText = (TextView) findViewById(R.id.textOwner);
                        ownerText.setText(flora.getOwnerUser());

                        TextView dateText = (TextView) findViewById(R.id.textCreatedDate);
                        dateText.setText(createdDate.toString());

                        TextView locationView = (TextView) findViewById(R.id.textLocation);
                        locationView.setText(flora.getPlaceName());
                    }
                }
            });
            return true;
        } else {
            Toast.makeText(this, "Details cannot be retrieved for this item", Toast.LENGTH_LONG).show();
            return false;
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();

        switch(id){
            case R.id.save_flora:
                saveNewInfo();
                setResult(RESULT_OK);
                finish();
                return true;
            case R.id.delete_flora:
                ParseQuery<ParseObject> query = ParseQuery.getQuery("Flora");
                query.getInBackground(objectId, new GetCallback<ParseObject>() {
                    @Override
                    public void done(ParseObject object, ParseException e) {
                        if (e == null) {
                            object.deleteInBackground();
                        }
                    }
                });
                setResult(RESULT_OK);
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_details, menu);
        detailsMenu = menu;
        return true;
    }

}

