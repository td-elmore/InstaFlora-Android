package com.example.tracy.instaflora;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ListView;

import com.parse.GetCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseQueryAdapter;
import com.parse.ParseUser;
import com.parse.SaveCallback;

public class FloraComments extends AppCompatActivity {

    String objectId;
    CommentParseQueryAdapter pqAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_flora_comments);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ActionBar ab = getSupportActionBar();
        if (ab != null) {
            ab.setDisplayHomeAsUpEnabled(true);
        }

        Intent intent = getIntent();
        objectId = intent.getStringExtra("objectId");
        if (objectId == null) {
            MainActivity.instaLog("problem with the objectId = null");
            finish();
        }

        initializeCommentList();

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                EditText editComment = (EditText) findViewById(R.id.editAddComment);
                String comment = editComment.getText().toString();
                if (comment.length() > 0) {
                    ParseObject obj = new ParseObject("Comments");
                    obj.put("floraId", objectId);
                    obj.put("username", ParseUser.getCurrentUser().getUsername());
                    obj.put("comment", comment);
                    obj.saveInBackground(new SaveCallback() {
                        @Override
                        public void done(ParseException e) {
                            pqAdapter.loadObjects();
                        }
                    });
                    editComment.setText("");
                }
            }
        });
    }

    public void initializeCommentList() {

        pqAdapter = new CommentParseQueryAdapter(this, new ParseQueryAdapter.QueryFactory<ParseObject>() {
            @Override
            public ParseQuery<ParseObject> create() {
                ParseQuery<ParseObject> query = new ParseQuery<ParseObject>("Comments");
                query.whereEqualTo("floraId", objectId);
                return query;
            }
        });

        ListView listView = (ListView) findViewById(R.id.listComments);
        listView.setAdapter(pqAdapter);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();

        if (id == android.R.id.home) {
            final int commentCount = pqAdapter.getCount();

            ParseQuery<Flora> query = ParseQuery.getQuery("Flora");
            query.whereEqualTo("objectId", objectId);
            query.getFirstInBackground(new GetCallback<Flora>() {
                @Override
                public void done(Flora object, ParseException e) {
                    if (e == null) {
                        object.setCommentCount(commentCount);
                        object.saveInBackground();
                    }
                }
            });
        }

        return super.onOptionsItemSelected(item);
    }
}
