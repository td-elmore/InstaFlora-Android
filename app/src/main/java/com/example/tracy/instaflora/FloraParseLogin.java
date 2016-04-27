package com.example.tracy.instaflora;


import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;

import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.parse.LogInCallback;
import com.parse.ParseException;
import com.parse.ParseUser;
import com.parse.SignUpCallback;

public class FloraParseLogin extends AppCompatActivity {

    public void onSignup (View view) {

        EditText editUsername = (EditText) findViewById(R.id.editUsername);
        EditText editPassword = (EditText) findViewById(R.id.editPassword);
        String username = editUsername.getText().toString();
        String password = editPassword.getText().toString();

        ParseUser user = new ParseUser();
        if (user == null) {
            MainActivity.instaLog("user is null");
            return;
        }

        user.setUsername(username);
        user.setPassword(password);

        // signup the user assuming their email and password are valid
        if (emailAndPasswordValid(username, password)) {
            user.signUpInBackground(new SignUpCallback() {
                @Override
                public void done(ParseException e) {
                    if (e == null) {
                        MainActivity.instaLog("signin successful");
                        finish();
                    } else {
                        Toast.makeText(getApplicationContext(), "Signup failed (" + e.getMessage() + ")", Toast.LENGTH_LONG).show();
                        e.printStackTrace();
                    }
                }
            });
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_flora_parse_login);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // TODO: get the last known username from system preferences

        TextView loginText = (TextView) findViewById(R.id.textLogIn);
        loginText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EditText editUsername = (EditText) findViewById(R.id.editUsername);
                EditText editPassword = (EditText) findViewById(R.id.editPassword);

                String username = editUsername.getText().toString();
                String password = editPassword.getText().toString();

                if (emailAndPasswordValid(username, password)) {
                    ParseUser.logInInBackground(username, password, new LogInCallback() {
                        @Override
                        public void done(ParseUser user, ParseException e) {
                            if (e == null) {
                                MainActivity.instaLog("login successful");
                                finish();
                            } else {
                                MainActivity.instaLog("login unsuccessful");
                                Toast.makeText(getApplicationContext(), "Login failed, try again or signup (" + e.getMessage() + ")", Toast.LENGTH_LONG).show();
                                e.printStackTrace();
                            }
                        }
                    });
                }
            }
        });

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.hide();

    }
    public boolean emailAndPasswordValid(String username, String password) {

        if (username == null || password == null) {
            Toast.makeText(getApplicationContext(), "Enter username and password please", Toast.LENGTH_LONG).show();
            return false;
        }

        if (username.length() == 0 || password.length() == 0) {
            Toast.makeText(getApplicationContext(), "Enter username and password please", Toast.LENGTH_LONG).show();
            return false;
        }

        return true;
    }
}



