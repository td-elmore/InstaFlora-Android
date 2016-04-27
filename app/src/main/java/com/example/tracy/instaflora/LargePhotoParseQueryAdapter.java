package com.example.tracy.instaflora;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.parse.GetDataCallback;
import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseImageView;
import com.parse.ParseQueryAdapter;

/**
 * Created by Tracy on 2/29/2016.
 */
public class LargePhotoParseQueryAdapter extends ParseQueryAdapter<Flora> {


    public LargePhotoParseQueryAdapter(Context context, QueryFactory<Flora> queryFactory) {
        super(context, queryFactory);
    }

    @Override
    public View getItemView(Flora flora, View v, ViewGroup parent) {
        //return super.getItemView(object, v, parent);

        Drawable placeholder = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            placeholder = getContext().getDrawable(R.drawable.waitcircle);
        }

        if (v == null) {
            v = View.inflate(getContext(), R.layout.large_photo_layout, null);
        }

        super.getItemView(flora, v, parent);

        ParseImageView floraImage = (ParseImageView) v.findViewById(R.id.listImage);
        if (placeholder != null) {
            floraImage.setPlaceholder(placeholder);
        }

        ParseFile photoFile = flora.getImageFile();
        if (photoFile != null) {
            floraImage.setParseFile(photoFile);
            floraImage.loadInBackground(new GetDataCallback() {
                @Override
                public void done(byte[] data, ParseException e) {
                    // nothing to do
                }
            });
        }

        String botanical;
        String common;
        String username;
        String description;
        String placeName;

        botanical = flora.getBotanical();
        if (botanical != null && botanical.length() > 0) {
            TextView botanicalTextView = (TextView) v.findViewById(R.id.listBotanical);
            botanicalTextView.setText(botanical);
        }
        common = flora.getCommonName();
        if (common != null && common.length() > 0) {
            TextView commonTextView = (TextView) v.findViewById(R.id.listCommon);
            commonTextView.setText(common);
        }

        username = flora.getOwnerUser();
        if (username != null && username.length() > 0) {
            TextView userTextView = (TextView) v.findViewById(R.id.listUsername);
            userTextView.setText(username);
        }

        description = flora.getDescription();
        if (description != null && description.length() > 0) {
            TextView commentTextView = ((TextView) v.findViewById(R.id.listDescription));
            commentTextView.setText(description);
        }

        placeName = flora.getPlaceName();
        if (placeName != null && placeName.length() > 0) {
            TextView locationTextView = ((TextView) v.findViewById(R.id.listLocation));
            locationTextView.setText(placeName);
        }

        int commentCount = flora.getCommentCount();
        String commentText = "";
        TextView commentCountView = ((TextView) v.findViewById(R.id.listComment));
        if (commentCount == 0) {
            commentText = "No comments, click to add the first one";
        } else if (commentCount == 1) {
            commentText = "1 comment, click to view";
        } else {
            commentText = String.valueOf(commentCount) + " comments, click to view";
        }
        commentCountView.setText(commentText);

        // when the comment text is clicked on, open the comments Activity
        final String objectId = flora.getObjectId();
        commentCountView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getContext(), FloraComments.class);
                intent.putExtra("objectId", objectId);
                getContext().startActivity(intent);
            }
        });

        return v;
    }
}
