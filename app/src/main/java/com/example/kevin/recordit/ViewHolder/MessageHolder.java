package com.example.kevin.recordit.ViewHolder;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.example.kevin.recordit.R;

import de.hdodenhof.circleimageview.CircleImageView;

public class MessageHolder extends RecyclerView.ViewHolder {

    // each data item is just a string in this case
    public RelativeLayout background;
    public LinearLayout receivingLayout;
    public LinearLayout sendingLayout;
    public TextView messageTextViewReceiving;
    public CircleImageView profileImageCircleViewReceiving;
    public TextView messageTextViewSending;
    public CircleImageView profileImageCircleViewSending;

    public MessageHolder(View v) {
        super(v);
        background = v.findViewById(R.id.message_background);
        receivingLayout = v.findViewById(R.id.receiverLinearLayout);
        sendingLayout = v.findViewById(R.id.senderLinearLayout);
        messageTextViewReceiving = v.findViewById(R.id.recyclerViewChatMessageTextReceive);
        profileImageCircleViewReceiving = v.findViewById(R.id.recyclerViewChatProfileImageReceive);
        messageTextViewSending = v.findViewById(R.id.recyclerViewChatMessageTextSending);
        profileImageCircleViewSending = v.findViewById(R.id.recyclerViewChatProfileImageSending);
    }
}