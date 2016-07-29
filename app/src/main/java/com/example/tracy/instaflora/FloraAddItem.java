package com.example.tracy.instaflora;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.parse.ParseACL;
import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseGeoPoint;

import com.parse.ParseUser;
import com.parse.SaveCallback;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class FloraAddItem extends AppCompatActivity implements LocationListener {

    // startActivityForResult() identifier
    static final int GET_PICTURE_FROM_GALLERY = 1;
    static final int GET_COORDS_FROM_MAP = 2;
    static final int TAKE_PICTURE = 3;

    // keeps track of current or chosen latitude and longitude
    // for saving with Flora item information
    static double latFromMap = 0.0;
    static double lngFromMap = 0.0;

    // global location information updated as needed
    LocationManager locationManager;
    String provider;

    // notifies this activity that a location has been
    // chosen by selection on the Map: FloraLocation.class
    static boolean locationChosen = false;

    // path to image file saved to device
    // whenever a picture is taken right now, or retrieved from gallery
    String imageFile = "";

    // activity imgView can be clicked and
    // shows the selected image for the Flora item
    ImageView imgView;

    /*
     * changeLocation()
     *
     * launches the Google Maps activity that allows user
     * to choose a specific location for this item, updates
     * necessary global variables upon return in
     * onActivityResult()
     */
    public void changeLocation() {
        // open a goole map to select a location
        Intent intent = new Intent(this, FloraLocation.class);
        startActivityForResult(intent, GET_COORDS_FROM_MAP);
    }

    /*
     * shareWarning()
     *
     * called when the share checkbox is clicked. Will warn user to
     * only share pictures and locations that are on public property
     *
     */
    public void shareWarning(View view) {
        CheckBox shareCheck = (CheckBox) view;

        if (shareCheck.isChecked()) {
            Toast.makeText(this, "Be sure this plant is not on private property when sharing", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_flora_add_item);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ActionBar ab = getSupportActionBar();
        if (ab != null) {
            ab.setDisplayHomeAsUpEnabled(true);
        }

        // allow user to click apple blossom picture to take a
        // picture directly from this activity screen.
        imgView = (ImageView) findViewById(R.id.imageFlora);
        imgView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dispatchTakePictureIntent();
            }
        });

        // not using the floating action button in this Activity
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.hide();

        initializeActivityGlobals();
    }

    @Override
    protected void onResume() {
        super.onResume();

        initializeLocation();
        locationManager.requestLocationUpdates(provider, 400, 1, this);
    }

    @Override
    protected void onPause() {
        super.onPause();

        locationManager.removeUpdates(this);

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // whichever activity was selected, it was cancelled
        if (resultCode == RESULT_CANCELED) {
            return;
        }


        if (requestCode == GET_COORDS_FROM_MAP && resultCode == RESULT_OK) {

            // manually selecting coordinate from map

            if (data == null) {
                return;
            }

            // if 'Choose Location...' is selected, then the user wants to change
            // it -- whether or not it has been done before.
            locationChosen = false;
            setFloraLocation(data.getDoubleExtra("lat", 0.0), data.getDoubleExtra("lng", 0.0));
            locationChosen = true;
            // no need to update the location if it has already been chosen on the map
            locationManager.removeUpdates(this);

        } else if (requestCode == GET_PICTURE_FROM_GALLERY && resultCode == RESULT_OK) {

            // manually selecting picture from those already saved on device

            if (data == null) {
                return;
            }

            if (data.getData() == null) {
                return;
            }

            Uri selectedImage = data.getData();

            // retrieve the location of the bitmap on disk, this gets an actual path
            // sets the global 'imageFile' for use later at Save time.
            String[] filePathColumn = { MediaStore.Images.Media.DATA };
            Cursor cursor = getContentResolver().query(selectedImage, filePathColumn, null, null, null);
            if (cursor != null) {
                cursor.moveToFirst();
                int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
                String picturePath = cursor.getString(columnIndex);
                cursor.close();

                imageFile = picturePath;
                setImagePic();

            }
        } else if (requestCode == TAKE_PICTURE && resultCode == RESULT_OK) {

            // picture taken from this activity - save it to the device

            // uses imageFile global - set earlier when temp file is created
            setImagePic();
            galleryAddPic(); // allow the image to be picked up from the gallery
        }
    }

    /*
     * setImagePic()
     *
     * shows the image in the view provided to indicate
     * to the user that they have selected/taken a
     * particular picture.
      *
      * scales the image to the imgView window size.
     */
    public void setImagePic() {

        Bitmap bitmap = BitmapFactory.decodeFile(imageFile);

        int imgWidth = bitmap.getWidth();
        int imgHeight = bitmap.getHeight();
        int scaledWidth = imgView.getWidth();
        int scaledHeight = imgView.getHeight();

        if (imgWidth > imgHeight) {
            scaledHeight = (imgView.getWidth() * imgHeight) / imgWidth;
        } else {
            scaledWidth = (imgView.getHeight() * imgWidth) / imgHeight;
        }

        Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, scaledWidth, scaledHeight, true);
        imgView.setImageBitmap(scaledBitmap);

        bitmap.recycle();
    }

    /*
     * galleryAddPic()
     *
     * adds the picture that was taken to the devices gallery
     * so it can be used in other applications.
     */
    private void galleryAddPic() {

        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);

        File f = new File(imageFile);

        Uri contentUri = Uri.fromFile(f);

        mediaScanIntent.setData(contentUri);

        this.sendBroadcast(mediaScanIntent);
    }

    /*
     * createImageFile()
     *
     * creates and image file with a unique name when the user
     * takes a picture.
     *
     * sets the global 'imageFile' to the file name
     */
    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        String imageFileName = "PNG_" + timeStamp + "_";
        File storageDir = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".png",         /* suffix */
                storageDir      /* directory */
        );

        // Save a path for the image to get saved
        imageFile = image.getAbsolutePath();
        MainActivity.instaLog(imageFile);
        return image;
    }

    /*
     * dispatchTakePictureIntent()
     *
     * allows the user to take a picture directly from this activity
     * puts the picture in a temporary file to be loaded later and
     * made accessible to the gallery
     */
    private void dispatchTakePictureIntent() {

        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                MainActivity.instaLog("problem creating the PNG file");
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT,
                        Uri.fromFile(photoFile));
                startActivityForResult(takePictureIntent, TAKE_PICTURE);
            }
        }

    }

    /*
     * initializeActivityGlobals()
     *
     * defaults, as the title says
     */
    public void initializeActivityGlobals() {

        imageFile = "";

        initializeLocation();
    }

    /*
     * initializeLocation()
     *
     * start the location updates
     * retrieve the current location
     * set a default location when an actual location cannot be found
     * initialize the location globals to whatever the current location is
     */
    public void initializeLocation() {

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        if (locationManager != null) {

            provider = locationManager.getBestProvider(new Criteria(), false);

            if (provider != null) {

                double lat, lng;

                Location location = locationManager.getLastKnownLocation(provider);

                if (location == null) {
                    // set to UC Berkeley Botanical Gardens coordinates temporarily
                    // when the current location isn't readily known
                    lat = 37.875612;
                    lng = -122.238690;
                } else {
                    lat = location.getLatitude();
                    lng = location.getLongitude();
                }

                locationManager.requestLocationUpdates(provider, 400, 1, this);
                setFloraLocation(lat, lng);
            }
        }
    }

    /*
     * setFloraLocation()
     *
     * lat = set the global latFromMap to lat or do nothing
     * lng = set the global lngFromMap to lng or do nothing
     *
     * if the location has not been chosen by going to the menu
     * item 'Choose Location..." then the current location is
     * used, set here.
     *
     * Set the text view to reflect the current location
     * 4/2016 - city and state
     */
    public void setFloraLocation(double lat, double lng) {

        if (!locationChosen) {
            latFromMap = lat;
            lngFromMap = lng;
        }
        TextView textView = (TextView) findViewById(R.id.textLocation);
        MainActivity.setLocationDescription(this, textView, lat, lng);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_add_item, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();
        switch (id) {
            case R.id.save_item:
                enterNewFloraItem();
                setResult(RESULT_OK);
                finish();
                return true;
            case R.id.locate:
                changeLocation();
                return true;
            case R.id.gallery:
                Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(intent, GET_PICTURE_FROM_GALLERY);
                return true;
            case android.R.id.home:
                locationChosen = false;
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    /* rotatePicNinety()
     * on some devices the pictures comes in rotated wrong, allow
     * user to rotate the picture manually if needed.
     *
     * this rotate updates the thumbnail very slowly on the MotoG
     */
    public void rotatePicNinety(View view) throws FileNotFoundException {

        if (imgView == null) {
            /* toast? can't rotate an image until one has been chosen, :P */
            return;
        }

        Bitmap bitmapOrg = BitmapFactory.decodeFile(imageFile);

        Matrix matrix = new Matrix();
        matrix.postRotate(90.0f);

        Bitmap rotatedBitmap = Bitmap.createBitmap(bitmapOrg, 0, 0, bitmapOrg.getWidth(), bitmapOrg.getHeight(), matrix, true);

        /* save new bitmap to image file */
        FileOutputStream imgStream = new FileOutputStream(imageFile);
        rotatedBitmap.compress(Bitmap.CompressFormat.PNG, 100, imgStream);
        try {
            imgStream.close();
        } catch (IOException e) {
            e.printStackTrace();
            MainActivity.instaLog(e.getMessage());
        }

        /* change the view of the bitmap on the thumbnail */
        setImagePic();
    }

    /*
     * enterNewFloraItem()
     * does all the work of saving a new item to the Parse 'Flora' database
     * fields and constants are described in the Flora.class
     *
     * if there is no internet connection available when it comes time to
     * save, the action is aborted.
     *
     * if an image has not been selected, the action is aborted.
     *
     * when the action is aborted a Toast() is called to inform the user
     */
    public void enterNewFloraItem() {

        String commonName = "Unknown Common Name";
        String botanicalName = "Unknown Botanical Name";
        String description = "No extra information";
        String placeName = "Unknown Location";

        if (imageFile == null || imageFile.length() == 0) {
            Toast.makeText(getApplicationContext(), "Take a picture by clicking on the apple blossom", Toast.LENGTH_LONG).show();
            return;
        }

        EditText editCommonName = (EditText) findViewById(R.id.editCommon);
        EditText editBotanicalName = (EditText) findViewById(R.id.editBotanical);
        EditText editDescription = (EditText) findViewById(R.id.editDescription);
        TextView locationName = (TextView) findViewById(R.id.textLocation);

        if (editCommonName.length() > 0) {
            commonName = editCommonName.getText().toString();
        }

        if (editBotanicalName.length() > 0) {
            botanicalName = editBotanicalName.getText().toString();
        }

        if (editDescription.length() > 0) {
            description = editDescription.getText().toString();
        }

        if (locationName.length() > 0) {
            placeName = locationName.getText().toString();
        }

        // save flower to Parse
        // inserted username, imageFile, common name, placeName
        // placeName, botanical name & location, ACL, and commentCount
        Flora flora = new Flora();
        flora.setOwnerUser(ParseUser.getCurrentUser().getUsername());
        flora.setBotanical(botanicalName);
        flora.setCommonName(commonName);
        flora.setDescription(description);
        flora.setPlaceName(placeName);
        flora.setCommentCount(0);

        ParseGeoPoint pgp = new ParseGeoPoint(latFromMap, lngFromMap);
        flora.setFloraLocation(pgp);

        // compress the image file into a PNG and put
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        final Bitmap bitmapData = BitmapFactory.decodeFile(imageFile);

        if (bitmapData.getByteCount() < Flora.FLORA_IMAGE_SIZE_PIXELS) {
            bitmapData.compress(Bitmap.CompressFormat.PNG, 100, stream);
        } else {
            int imgWidth = bitmapData.getWidth();
            int imgHeight = bitmapData.getHeight();
            int scaledWidth = Flora.FLORA_IMAGE_SIZE_WIDTH;
            int scaledHeight = Flora.FLORA_IMAGE_SIZE_HEIGHT;
            if (imgWidth > imgHeight) {
                scaledHeight = (Flora.FLORA_IMAGE_SIZE_WIDTH * imgHeight) / imgWidth;
            } else {
                scaledWidth = (Flora.FLORA_IMAGE_SIZE_HEIGHT * imgWidth) / imgHeight;
            }
            Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmapData, scaledWidth, scaledHeight, true);
            scaledBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
        }

        byte[] byteArray = stream.toByteArray();

        ParseFile file = new ParseFile(Flora.FLORA_IMAGE_NAME, byteArray);

        CheckBox buttShare = (CheckBox) findViewById(R.id.checkShare);
        if (buttShare.isChecked()) {
            ParseACL acl = new ParseACL();
            acl.setPublicReadAccess(true);
            acl.setWriteAccess(ParseUser.getCurrentUser(), true);
            flora.setACL(acl);
        } else {
            flora.setACL(new ParseACL(ParseUser.getCurrentUser()));
        }

        // only attempt to save if there is an internet connection
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo ni = cm.getActiveNetworkInfo();
        if ((ni != null) && ni.isConnected()) {
            flora.setImageFile(file);
            flora.saveInBackground(new SaveCallback() {
                @Override
                public void done(ParseException e) {
                    if (e == null) {
                        Toast.makeText(getApplicationContext(), "Save successful! Click Refresh.", Toast.LENGTH_LONG).show();
                        bitmapData.recycle();
                        MainActivity.loadFloraPending = true;
                    } else {
                        Toast.makeText(getApplicationContext(), "Unable to save this plant.", Toast.LENGTH_LONG).show();
                    }
                }
            });
        } else {
            // if no internet connection is detected, inform the user
            Toast.makeText(this, "No internet connection, entry cannot be saved", Toast.LENGTH_LONG).show();
            // TODO: use imageFile, and objectId to save image for later and saveEventually()
        }

        // garbage collect after loading bitmaps etc...
        System.gc();
        // reset the location
        locationChosen = false;
    }

    /*
     * Attempt to keep track of the location of the user to locate the
     * next plant entered into the system.
     */
    @Override
    public void onLocationChanged(Location location) {
            setFloraLocation(location.getLatitude(), location.getLongitude());
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }
}