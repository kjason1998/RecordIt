package com.example.kevin.recordit.ViewHolder;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

import com.example.kevin.recordit.R;

import de.hdodenhof.circleimageview.CircleImageView;

public class FriendsHolder extends  RecyclerView.ViewHolder{

    public TextView userName,userStatus;
    public CircleImageView profilePicture;

    public FriendsHolder(View itemView) {
        super(itemView);
        userName = itemView.findViewById(R.id.card_view_username);
        userStatus = itemView.findViewById(R.id.card_view_status);
        profilePicture = itemView.findViewById(R.id.card_view_profile_picture);
    }
}
