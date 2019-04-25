package com.example.kevin.recordit.Adapter;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.kevin.recordit.Model.Message;
import com.example.kevin.recordit.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.MyViewHolder>{
    private List<Message> messageList;
    private FirebaseAuth mAuth;
    private DatabaseReference mRoot;

    //constructor
    public MessageAdapter(List<Message> messageList) {
        this.messageList = messageList;
    }

    // Create new views (invoked by the layout manager)
    @Override
    public MessageAdapter.MyViewHolder onCreateViewHolder(ViewGroup parent,
                                                                        int viewType) {
        // create a new view
        View v = LayoutInflater.from(parent.getContext()).inflate
                (R.layout.message_text_view, parent, false);

        mAuth = FirebaseAuth.getInstance();
        mRoot = FirebaseDatabase.getInstance().getReference().child("users");
        return new MyViewHolder(v);
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(final MyViewHolder holder, int position) {
        Message message = messageList.get(position);

        String fromUserId = message.getFromUserId();
        if(fromUserId.equalsIgnoreCase(mAuth.getCurrentUser().getUid())){
            holder.receivingLayout.setVisibility(View.GONE);
            holder.messageTextViewSending.setText(message.getMessage());

            setProfilePictureChat(mRoot.child(fromUserId),holder.profileImageCircleViewSending);

        }
        else{
            holder.sendingLayout.setVisibility(View.GONE);
            holder.messageTextViewReceiving.setText(message.getMessage());


            setProfilePictureChat(mRoot.child(fromUserId),holder.profileImageCircleViewReceiving);
        }
    }

    private void setProfilePictureChat(DatabaseReference chatUser,
                                       final CircleImageView profileImageHolder) {
        chatUser.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                String profileImageUri = dataSnapshot.child("userThumbImage")
                        .getValue().toString();
                //load the profile picture, if not set yet use default pict stored in the app.
                Picasso.get().load(profileImageUri)
                        .placeholder(R.drawable.default_profile)
                        .into(profileImageHolder);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        int size;
        if(messageList.isEmpty()){
            size = 0;
        }else{
            size = messageList.size();
        }
        return size;
    }

    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder
    public static class MyViewHolder extends RecyclerView.ViewHolder {
        // each data item is just a string in this case
        public LinearLayout receivingLayout;
        public LinearLayout sendingLayout;
        public TextView messageTextViewReceiving;
        public CircleImageView profileImageCircleViewReceiving;
        public TextView messageTextViewSending;
        public CircleImageView profileImageCircleViewSending;
        public MyViewHolder(View v) {
            super(v);
            receivingLayout = v.findViewById(R.id.receiverLinearLayout);
            sendingLayout = v.findViewById(R.id.senderLinearLayout);
            messageTextViewReceiving = v.findViewById(R.id.recyclerViewChatMessageTextReceive);
            profileImageCircleViewReceiving = v.findViewById(R.id.recyclerViewChatProfileImageReceive);
            messageTextViewSending = v.findViewById(R.id.recyclerViewChatMessageTextSending);
            profileImageCircleViewSending = v.findViewById(R.id.recyclerViewChatProfileImageSending);
        }
    }
}
