package com.example.tracy.instaflora;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.RadioButton;
import android.widget.TextView;

import com.parse.ParseUser;

// sorry about handling the radio buttons this way, but I did not know about RadioGroups
// TODO use the more standard radio groups in case there is an advantage with that.

public class FloraSettings extends AppCompatActivity {

    int range = MainActivity.RANGE_ALLFLORA;
    SharedPreferences sharedPreferences;
    CheckBox privateBox;

    RadioButton onemile;
    RadioButton fivemile;
    RadioButton tenmile;
    RadioButton allflora;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_flora_settings);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // back button, whoo
        ActionBar ab = getSupportActionBar();
        if (ab != null) {
            ab.setDisplayHomeAsUpEnabled(true);
        }

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.hide();

        // retrieve the sharedPreferences pointer to save at the end
        sharedPreferences = this.getSharedPreferences("com.example.tracy.instaflora", Context.MODE_PRIVATE);
        privateBox = (CheckBox) findViewById(R.id.checkPrivateOnly);

        String loggedInAs;
        loggedInAs = "You are logged in as: " + ParseUser.getCurrentUser().getUsername();
        TextView usernameText = (TextView) findViewById(R.id.textLoggedUsername);
        usernameText.setText(loggedInAs);

        onemile = (RadioButton) findViewById(R.id.radioOneMile);
        fivemile = (RadioButton) findViewById(R.id.radioFiveMiles);
        tenmile = (RadioButton) findViewById(R.id.radioTenMiles);
        allflora = (RadioButton) findViewById(R.id.radioAllFlora);

        if (!MainActivity.allFlora) {
            switch (MainActivity.rangeMiles) {
                case 5:
                    fivemile.setChecked(true);
                    fivemileClick(fivemile);
                    break;
                case 1:
                    onemile.setChecked(true);
                    onemileClick(onemile);
                    break;
                case 10:
                    tenmile.setChecked(true);
                    tenmileClick(tenmile);
                    break;
                default:
                    allflora.setChecked(true);
                    allfloraClick(allflora);
                    break;
            }
        } else {
            allflora.setChecked(true);
            allfloraClick(allflora);
        }

        privateBox.setChecked(MainActivity.privateOnly);
    }

    public void onemileClick(View view) {
        if (!privateBox.isChecked()) {
            fivemile.setChecked(false);
            tenmile.setChecked(false);
            allflora.setChecked(false);
            range = MainActivity.RANGE_ONEMILE;
        } else {
            allfloraClick(view);
        }
    }

    public void fivemileClick(View view) {
        if (!privateBox.isChecked()) {
            onemile.setChecked(false);
            tenmile.setChecked(false);
            allflora.setChecked(false);
            range = MainActivity.RANGE_FIVEMILE;
        } else {
            allfloraClick(view);
        }
    }

    public void tenmileClick(View view) {
        if (!privateBox.isChecked()) {
            fivemile.setChecked(false);
            onemile.setChecked(false);
            allflora.setChecked(false);
            range = MainActivity.RANGE_TENMILE;
        } else {
            allfloraClick(view);
        }
    }

    public void allfloraClick(View view) {
        fivemile.setChecked(false);
        tenmile.setChecked(false);
        onemile.setChecked(false);
        range = MainActivity.RANGE_ALLFLORA;
    }

    public void privateonlyClick(View view) {

        if (privateBox.isChecked()) {
            allflora.setChecked(true);
            allfloraClick(view);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_settings, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();

        switch(id){
            case R.id.save_settings:
                setSettingsGlobals();
                if (sharedPreferences != null) {
                    sharedPreferences.edit().putInt("range", range).apply();
                    sharedPreferences.edit().putBoolean("privateOnly", privateBox.isChecked()).apply();
                }
                setResult(RESULT_OK);
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void setSettingsGlobals() {

        MainActivity.allFlora = false;

        switch (range) {
            case MainActivity.RANGE_ONEMILE:
                MainActivity.rangeMiles = 1;
                break;
            case MainActivity.RANGE_FIVEMILE:
                MainActivity.rangeMiles = 5;
                break;
            case MainActivity.RANGE_TENMILE:
                MainActivity.rangeMiles = 10;
                break;
            case MainActivity.RANGE_ALLFLORA:
                MainActivity.allFlora = true;
                break;
        }
        MainActivity.privateOnly = privateBox.isChecked();
    }

}
