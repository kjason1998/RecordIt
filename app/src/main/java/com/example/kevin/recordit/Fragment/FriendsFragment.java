package com.example.kevin.recordit.Fragment;
import android.content.Intent;
import android.support.annotation.NonNull;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.kevin.recordit.Activity.ActivityChat;
import com.example.kevin.recordit.Activity.ActivityProfile;
import com.example.kevin.recordit.ViewHolder.FriendsHolder;
import com.example.kevin.recordit.Model.User;
import com.example.kevin.recordit.R;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;


/**
 * A simple {@link Fragment} subclass.
 */
public class FriendsFragment extends Fragment {
    private RecyclerView myFriendListView;

    private DatabaseReference userRefrence;
    private FirebaseAuth mAuth;

    private View myMainView;

    String onlineUserId;

    public FriendsFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        myMainView = inflater.inflate(R.layout.fragment_friends, container, false);

        myFriendListView = (RecyclerView) myMainView.findViewById(R.id.friendListRecyclerView);
        if (myFriendListView != null) {
            ViewGroup parent = (ViewGroup) myFriendListView.getParent();
            if (parent != null) {
                parent.removeAllViews();
            }
        }
        myFriendListView.setLayoutManager(new LinearLayoutManager(getContext()));

        mAuth = FirebaseAuth.getInstance();
        onlineUserId = mAuth.getCurrentUser().getUid();

        userRefrence = FirebaseDatabase.getInstance().getReference()
                .child(getResources().getString(R.string.database_user));

        return myFriendListView;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (myFriendListView != null) {
            ViewGroup parent = (ViewGroup) myFriendListView.getParent();
            if (parent != null) {
                parent.removeAllViews();
            }
        }
    }

    @Override
    public void onStart() {
        super.onStart();

        setRecyclerView();
    }

    public void setRecyclerView(){
        Query query = FirebaseDatabase.getInstance()
                .getReference()
                .child(getResources().getString(R.string.database_friend_list))
                .child(onlineUserId)
                .orderByValue()
                .limitToLast(50);

        FirebaseRecyclerOptions<User> options =
                new FirebaseRecyclerOptions.Builder<User>()
                        .setQuery(query, User.class)
                        .build();

        FirebaseRecyclerAdapter adapter = new FirebaseRecyclerAdapter<User, FriendsHolder>(options) {
            @Override
            public FriendsHolder onCreateViewHolder(ViewGroup parent, int viewType) {
                // Create a new instance of the ViewHolder, in this case we are using a custom
                // layout called R.layout.message for each item
                View view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.card_view_user, parent, false);

                return new FriendsHolder(view);
            }

            @Override
            protected void onBindViewHolder(final FriendsHolder holder, final int position, User model) {
                final String userId = getRef(position).getKey();
                userRefrence.child(userId).addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        final String userNameData = dataSnapshot.child(getResources().getString(
                                R.string.database_user_name)).getValue().toString();

                        final String userStatusData = dataSnapshot.child(getResources().getString(
                                R.string.database_user_name)).getValue().toString();
                        holder.userName.setText(userNameData);
                        holder.userStatus.setText(userStatusData);
                        //load the thumb image(compressed image) if no image are set up yet use default image.
                        Picasso.get().load(dataSnapshot.child(getResources().getString(
                                R.string.database_user_profile_picture)).getValue().toString())
                                .placeholder(R.drawable.default_profile).into(holder.profilePicture);
                        holder.profilePicture.setOnClickListener(new View.OnClickListener() {
                            @ Override
                            public void onClick(View v) {
                                //either takes user to chat or profile
                                goToProfileActivity(userId);
                            }
                        });
                        holder.itemView.setOnClickListener(new View.OnClickListener() {
                            @ Override
                            public void onClick(View v) {
                                //either takes user to chat or profile
                                goToChatActivity(userId,userNameData);
                            }
                        });
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
            }
        };

        myFriendListView.setAdapter(adapter);

        adapter.startListening();
    }

    private void goToProfileActivity(String userId) {
        Intent profileIntent = new Intent(getContext(),ActivityProfile.class);
        profileIntent.putExtra(getResources().getString(R.string.extra_user_id),userId);
        startActivity(profileIntent);
    }

    private void goToChatActivity(String userId,String userNameData) {
        Intent profileIntent = new Intent(getContext(),ActivityChat.class);
        profileIntent.putExtra(getResources().getString(R.string.extra_user_id),userId);
        profileIntent.putExtra(getResources().getString(R.string.extra_user_name),userNameData);
        startActivity(profileIntent);
    }
}
