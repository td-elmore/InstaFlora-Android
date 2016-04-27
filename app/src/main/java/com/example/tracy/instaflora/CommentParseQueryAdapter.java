package com.example.tracy.instaflora;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.parse.ParseObject;
import com.parse.ParseQueryAdapter;

/**
 * Created by Tracy on 3/11/2016.
 */
public class CommentParseQueryAdapter extends ParseQueryAdapter<ParseObject> {


    public CommentParseQueryAdapter(Context context, QueryFactory<ParseObject> queryFactory) {
        super(context, queryFactory);
    }


    @Override
    public View getItemView(ParseObject object, View v, ViewGroup parent) {


        if (v == null) {
            v = View.inflate(getContext(), R.layout.comment_layout, null);
        }

        String username = object.getString("username");
        String comment = object.getString("comment");

        if (username != null && username.length() > 0) {

            TextView textUsername = (TextView) v.findViewById(R.id.commentUsername);
            textUsername.setText(username);

        }

        if (comment != null && comment.length() > 0) {

            TextView textComment = (TextView) v.findViewById(R.id.commentComment);
            textComment.setText(comment);

        }

        super.getItemView(object, v, parent);

        return v;
    }
}
