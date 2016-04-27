package com.example.tracy.instaflora;

import com.parse.ParseClassName;
import com.parse.ParseFile;
import com.parse.ParseGeoPoint;
import com.parse.ParseObject;

/**
 * Created by Tracy on 3/4/2016.
 * extends ParseObject to prevent silly errors that arise when
 * using raw strings everywhere -- as in the column headers
 */
@ParseClassName("Flora")
public class Flora extends ParseObject {

    static final int FLORA_IMAGE_SIZE_PIXELS = 9000;
    static final int FLORA_IMAGE_SIZE_WIDTH = 300;
    static final int FLORA_IMAGE_SIZE_HEIGHT = 300;
    static final String FLORA_IMAGE_NAME = "image.png";

    public Flora() {
        // default constructor required
    }

    public void setLocation (ParseGeoPoint geoPoint) {

        put("location", geoPoint);

    }

    public ParseGeoPoint getLocation() {

        return getParseGeoPoint("location");

    }

    public void setPlaceName(String placeName) {
        put("place", placeName);
    }

    public String getPlaceName() {
        return getString("place");
    }

    public void setCommentCount(int commentCount) {
        put("commentCount", commentCount);
    }

    public int getCommentCount() {
        return getInt("commentCount");
    }

    public void setCommonName(String common) {
        put("common", common);
    }

    public String getCommonName() {
        return getString("common");
    }

    public void setBotanical(String botanical) {
        put("botanical", botanical);
    }

    public String getBotanical() {
        return getString("botanical");
    }

    public void setDescription(String description) {
        put("description", description);
    }

    public String getDescription() {
        return getString("description");
    }

    public void setOwnerUser(String username) {
        put("username", username);
    }

    public String getOwnerUser() {
        return getString("username");
    }

    public void setImageFile(ParseFile file) {
        put("image", file);
    }

    public ParseFile getImageFile() {
        return getParseFile("image");
    }

    public void setFloraLocation(ParseGeoPoint pt) {
        put("location", pt);
    }

    public ParseGeoPoint getFloraLocation() {
        return getParseGeoPoint("location");
    }
}
