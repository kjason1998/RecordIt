package com.example.kevin.recordit.Activity;

import android.content.Intent;
import android.preference.PreferenceActivity;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.example.kevin.recordit.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;

import de.hdodenhof.circleimageview.CircleImageView;

public class ActivityProfile extends AppCompatActivity {
    private Toolbar mToolbar;

    private String RELATION_CURRENT_USER;

    private TextView userName,userStatus;
    private CircleImageView profileImage;
    private Button acceptFriendRequest,declineFriendRequest;
    private FloatingActionButton addFriendButton;

    private FirebaseAuth mAuth;

    private DatabaseReference getUserProfileDataReference;
    private DatabaseReference getCurrentUserDataReference;
    //new reference that store friend request of user's
    private DatabaseReference friendRequestReference;
    //new reference that store friend request of user's
    private DatabaseReference friendListReference;
    //new reference that store notification
    private DatabaseReference notificationsReference;

    private String currentOnlineUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        mToolbar = findViewById(R.id.profile_app_bar);
        setMToolbar();

        RELATION_CURRENT_USER = getResources().getString(R.string.relation_no_relation);

        profileImage = findViewById(R.id.profile_picture);

        userName = findViewById(R.id.profile_username);
        userStatus = findViewById(R.id.profile_user_status);

        acceptFriendRequest = findViewById(R.id.profile_accept_friend_button);
        declineFriendRequest = findViewById(R.id.profile_decline_friend_button);

        addFriendButton = findViewById(R.id.profile_send_friend_request_fbs);

        //create new database friend request
        friendRequestReference = FirebaseDatabase.getInstance().getReference()
                .child(getResources().getString(R.string.database_friend_request));

        //create new database friend request
        friendListReference = FirebaseDatabase.getInstance().getReference()
                .child(getResources().getString(R.string.database_friend_list));

        //create new database friend request
        notificationsReference = FirebaseDatabase.getInstance().getReference()
                .child(getResources().getString(R.string.database_notification));

        //getting the user that are currently using the app
        mAuth = FirebaseAuth.getInstance();
        currentOnlineUserId = mAuth.getCurrentUser().getUid();
        getCurrentUserDataReference = FirebaseDatabase.getInstance().getReference()
                .child(getResources().getString(R.string.database_user))
                .child(currentOnlineUserId);

        //getting the user that are clicked in the all user list
        getUserProfileDataReference = FirebaseDatabase.getInstance().getReference()
                .child(getResources().getString(R.string.database_user))
                .child(getIntent().getExtras().get(getResources().
                        getString(R.string.extra_user_id)).toString());

        //set the profile picture,name and status of the viewed user's profile page
        setProfileInfo(getUserProfileDataReference);

        //set the buttons send friend request, accept and decline the
        //friend request of the viewed user.
        setButton(getCurrentUserDataReference,getUserProfileDataReference);

    }

    private void setMToolbar() {
        setSupportActionBar(mToolbar);
        getSupportActionBar().setTitle(getResources().getString(R.string.profile_app_bar_title));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    private void setButton(final DatabaseReference getCurrentUserDataReference,
                           final DatabaseReference getUserProfileDataReference) {


        //make sure user cant add them self by set the add button gone
        //when viewing own profile page
        if(getCurrentUserDataReference.equals(getUserProfileDataReference)){
            addFriendButton.setVisibility(View.GONE);
        }

        
        friendRequestReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.hasChild(getCurrentUserDataReference.getKey())){
                    friendRequestReference.child(getCurrentUserDataReference.getKey())
                            .addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                    if(dataSnapshot.hasChild(getUserProfileDataReference.getKey())){
                                        String friendRequestType = dataSnapshot.child(getUserProfileDataReference.getKey())
                                                .child(getResources().getString(R.string.database_friend_request_progress))
                                                .getValue().toString();
                                        if(friendRequestType.equalsIgnoreCase(
                                                getResources().getString(R.string.database_friend_request_sent))){
                                            RELATION_CURRENT_USER =
                                                    getResources().getString(R.string.database_friend_request_sent);
                                        }else if(friendRequestType.equalsIgnoreCase(
                                                getResources().getString(R.string.database_friend_request_receive))) {

                                            RELATION_CURRENT_USER =
                                                    getResources().getString(R.string.database_friend_request_receive);
                                        }

                                        // if user's profile page that are being viewed
                                        // are sending friend request to current user than they can accept or decline.
                                        if(RELATION_CURRENT_USER.equalsIgnoreCase
                                                (getResources().getString(R.string.database_friend_request_receive))){
                                            setAcceptDeclineButton();
                                            addFriendButton.setVisibility(View.GONE);
                                        }
                                    }
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError databaseError) {

                                }
                            });
                }
                else{
                    // if there is no request than the possibilities are,it is
                    // already friends and did unfriend.
                    // or not friend and have not sent any request
                    // check if current user is friend with the user that the profile page are being view
                    // if yes than relation current user are set to friend.
                    friendListReference.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            if(dataSnapshot.hasChild(getCurrentUserDataReference.getKey())){
                                friendListReference.child(getCurrentUserDataReference.getKey())
                                        .addListenerForSingleValueEvent(new ValueEventListener() {
                                            @Override
                                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                                if(dataSnapshot.hasChild(getUserProfileDataReference.getKey())){
                                                    RELATION_CURRENT_USER =
                                                            getResources().getString(R.string.relation_friend);
                                                }
                                            }

                                            @Override
                                            public void onCancelled(@NonNull DatabaseError databaseError) {

                                            }
                                        });

                            }
                            /*else{
                                RELATION_CURRENT_USER =
                                        getResources().getString(R.string.relation_no_relation);
                            }*/
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {

                        }
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        addFriendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //so that the user cant click it too fast, until the user add
                //process are finish the button cant be click.
                addFriendButton.setEnabled(false);

                if(RELATION_CURRENT_USER.equalsIgnoreCase
                        (getResources().getString(R.string.relation_no_relation))){
                    sendFriendRequestTo();
                }
                else if(RELATION_CURRENT_USER.equalsIgnoreCase
                        (getResources().getString(R.string.database_friend_request_sent))){
                    removeFriendRequest();
                }
                else if(RELATION_CURRENT_USER.equalsIgnoreCase
                        (getResources().getString(R.string.relation_friend))){
                    unfriend();
                }
            }
        });
    }

    //un friend a friend
    private void unfriend() {
        friendListReference.child(getCurrentUserDataReference.getKey())
                .child(getUserProfileDataReference.getKey())
                .removeValue()
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if(task.isSuccessful()){
                            friendListReference.child(getUserProfileDataReference.getKey())
                                    .child(getCurrentUserDataReference.getKey())
                                    .removeValue()
                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            Toast.makeText(ActivityProfile.this,
                                                getResources().getString(R.string.profile_toast_unfriend),
                                                Toast.LENGTH_LONG)
                                                .show();
                                            addFriendButton.setEnabled(true);
                                            RELATION_CURRENT_USER =
                                                    getResources().getString(R.string.relation_no_relation);
                                        }
                                    });
                        }
                    }
                });
    }

    // setting accept and decline button
    private void setAcceptDeclineButton() {
        acceptFriendRequest.setVisibility(View.VISIBLE);
        declineFriendRequest.setVisibility(View.VISIBLE);
        
        acceptFriendRequest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                acceptFriendRequest();
            }
        });
        declineFriendRequest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                removeFriendRequest();
            }
        });
    }

    //accepting friend request
    private void acceptFriendRequest() {
        Calendar date = Calendar.getInstance();
        SimpleDateFormat currentDateFormat = new SimpleDateFormat
                (getResources().getString(R.string.default_date_format));
        final String currentDate = currentDateFormat.format(date.getTime());

        friendListReference.child(getCurrentUserDataReference.getKey())
                .child(getUserProfileDataReference.getKey())
                .child(getResources().getString(R.string.database_friend_list_date))
                .setValue(currentDate)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        friendListReference.child(getUserProfileDataReference.getKey())
                            .child(getCurrentUserDataReference.getKey())
                                .child(getResources().getString(R.string.database_friend_list_date))
                                .setValue(currentDate).addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                friendRequestReference
                                        .child(getCurrentUserDataReference.getKey())
                                        .child(getUserProfileDataReference.getKey())
                                        .removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Void> task) {
                                        if(task.isSuccessful()){

                                            friendRequestReference
                                                    .child(getUserProfileDataReference.getKey())
                                                    .child(getCurrentUserDataReference.getKey())
                                                    .removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
                                                @Override
                                                public void onComplete(@NonNull Task<Void> task) {
                                                    if(task.isSuccessful()){
                                                        Toast.makeText(ActivityProfile.this,
                                                                getResources().getString(R.string.profile_toast_accepting),
                                                                Toast.LENGTH_LONG)
                                                                .show();
                                                        RELATION_CURRENT_USER =
                                                                getResources().getString(R.string.relation_friend);
                                                        refreshActivity();
                                                    }
                                                }
                                            });
                                        }
                                    }
                                });
                            }
                        });
                    }
                });
    }

    //cancel friend request if friend request is being sent
    private void removeFriendRequest() {
        //removing sending request from the database
        friendRequestReference
                .child(getCurrentUserDataReference.getKey())
                .child(getUserProfileDataReference.getKey())
                .removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if(task.isSuccessful()){
                    //removing receiving request from the database
                    friendRequestReference
                            .child(getUserProfileDataReference.getKey())
                            .child(getCurrentUserDataReference.getKey())
                            .removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if(task.isSuccessful()){
                                Toast.makeText(ActivityProfile.this,
                                    getResources().getString(R.string.profile_toast_canceling_adding),
                                    Toast.LENGTH_LONG)
                                    .show();
                                addFriendButton.setEnabled(true);
                                RELATION_CURRENT_USER =
                                        getResources().getString(R.string.relation_no_relation);
                                refreshActivity();
                            }
                        }
                    });
                }
            }
        });
    }

    //to add friend from current user to other user
    private void sendFriendRequestTo() {
        //putting sending request to database
        friendRequestReference
                .child(getCurrentUserDataReference.getKey())
                .child(getUserProfileDataReference.getKey())
                .child(getResources().getString(R.string.database_friend_request_progress))
                .setValue(getResources().getString(R.string.database_friend_request_sent))
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if(task.isSuccessful()){
                            //putting receiving request to database
                            friendRequestReference
                                    .child(getUserProfileDataReference.getKey())
                                    .child(getCurrentUserDataReference.getKey())
                                    .child(getResources().getString(R.string.database_friend_request_progress))
                                    .setValue(getResources().getString(R.string.database_friend_request_receive))
                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            if(task.isSuccessful()){
                                                //putting the request to the database
                                                HashMap<String,String> notificationDetail =
                                                        new HashMap<String,String>();
                                                notificationDetail.put
                                                        (getResources().getString(R.string.database_notification_from),
                                                                getCurrentUserDataReference.getKey());
                                                notificationDetail.put
                                                        (getResources().getString(R.string.database_notification_type),
                                                                getResources().getString(R.string.database_notification_type_request));

                                                notificationsReference.child(getUserProfileDataReference.getKey())
                                                        .push().setValue(notificationDetail)
                                                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                            @Override
                                                            public void onComplete(@NonNull Task<Void> task) {
                                                                if(task.isSuccessful()){
                                                                    addFriendButton.setEnabled(true);
                                                                    RELATION_CURRENT_USER =
                                                                            getResources().getString(
                                                                                    R.string.relation_sending_friendRequest);
                                                                    Toast.makeText(ActivityProfile.this,
                                                                            getResources().getString(R.string.profile_toast_adding),
                                                                                    Toast.LENGTH_LONG)
                                                                            .show();
                                                                    refreshActivity();
                                                                }
                                                            }
                                                        });
                                            }
                                        }
                                    });
                        }
                    }
                });
    }

    // setting up all the user's profile information that are being viewed.
    private void setProfileInfo(DatabaseReference getUserProfileDataReference) {
        getUserProfileDataReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                String name = dataSnapshot.child(getResources().getString
                        (R.string.database_user_name))
                        .getValue().toString();
                String status = dataSnapshot.child(getResources().getString
                        (R.string.database_user_status))
                        .getValue().toString();
                String profileImageUri = dataSnapshot.child(getResources().getString
                        (R.string.database_user_profile_picture))
                        .getValue().toString();

                userName.setText(name);
                userStatus.setText(status);
                //load the profile picture, if not set yet use default pict stored in the app.
                Picasso.get().load(profileImageUri)
                        .placeholder(R.drawable.blue_profile_picture).into(profileImage);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    //reload the activity profile activity
    private void refreshActivity() {
        finish();
        startActivity(getIntent());
    }
}
