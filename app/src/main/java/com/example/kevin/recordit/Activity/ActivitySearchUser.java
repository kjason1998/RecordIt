package com.example.kevin.recordit.Activity;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.MultiAutoCompleteTextView;
import android.widget.Toast;

import com.example.kevin.recordit.ViewHolder.UserHolder;
import com.example.kevin.recordit.Model.User;
import com.example.kevin.recordit.R;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.squareup.picasso.Picasso;

/**
 * ActivitySearchUser where people can see
 * everyone else, in order of A-Z,a-z.
 * also user can search name, and click to promt
 * the user's profile page
 *
 * @author Kevin
 */
public class ActivitySearchUser extends AppCompatActivity {

    private Toolbar mToolbar;
    private RecyclerView usersRecyclerView;

    private MultiAutoCompleteTextView searchBar;
    private Button searchButton;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_user);

        mToolbar = findViewById(R.id.search_user_app_bar);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setTitle(getResources().getString(R.string.search_user_app_bar_title));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        usersRecyclerView = findViewById(R.id.recycler_view_users);
        usersRecyclerView.setHasFixedSize(true);
        usersRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        searchBar = findViewById(R.id.search_user_input_box);
        searchButton =  findViewById(R.id.search_user_search_button);

        initListeners();
    }

    @Override
    protected void onStart() {
        super.onStart();
        fetch("");
    }

    /**
     * Initialize all the listener.
     */
    private void initListeners() {
        //automatic search while user are typing.
        searchBar.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchUser();
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
        //search can be done when the button is clicked
        searchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                searchUser();
            }
        });
    }

    /**
     * Searching user using the input message
     * when the input is empty it will shows everything,
     * and promp a message that the input is empty.
     */
    private void searchUser(){
        String usernameSearched = searchBar.getText().toString();
        if(TextUtils.isEmpty(usernameSearched)){
            fetch("");
            Toast.makeText(ActivitySearchUser.this,
                    getResources().getString(R.string.search_user_err_empty),
                    Toast.LENGTH_LONG)
                    .show();
        }else{
            fetch(usernameSearched);
        }
    }

    /**
     * Getting users from the
     * database and put it in the
     * recycler view using firebase adapter
     *
     * @param userName - The input of the username that searched.
     */
    public void fetch(String userName){
        Query query = FirebaseDatabase.getInstance()
                .getReference()
                .child(getResources().getString(R.string.database_user))
                .orderByChild(getResources().getString(R.string.database_user_name))
                .startAt(userName.toUpperCase())
                .endAt(userName.toLowerCase()+"\uf8ff")
                .limitToLast(25);

        FirebaseRecyclerOptions<User> options =
                new FirebaseRecyclerOptions.Builder<User>()
                        .setQuery(query, User.class)
                        .build();

        //adapter build in by FirebaseUI
        FirebaseRecyclerAdapter adapter = new FirebaseRecyclerAdapter<User, UserHolder>(options) {
            @Override
            public UserHolder onCreateViewHolder(ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.card_view_user, parent, false);

                return new UserHolder(view);
            }

            @Override
            protected void onBindViewHolder(UserHolder holder, final int position, User model) {
                holder.userName.setText(model.getUserName());
                holder.userStatus.setText(model.getUserStatus());
                //load the thumb image(compressed image) if no image are set up yet use default image.
                Picasso.get().load(model.getUserThumbImage())
                        .placeholder(R.drawable.blue_profile_picture).into(holder.profilePicture);

                holder.itemView.setOnClickListener(new View.OnClickListener() {
                    @ Override
                    public void onClick(View v) {
                        String userId = getRef(position).getKey();
                        goToProfileActivity(userId);
                    }
                });
            }
        };

        usersRecyclerView.setAdapter(adapter);

        adapter.startListening();
    }

    /**
     * when the card view of a user
     * is click it will move user to
     * the profile activity where user
     * can do adding or receiving
     * friend reqeust.
     *
     * @param userId - the user id of the user that is going to be showed in profile activity.
     */
    private void goToProfileActivity(String userId) {
        Intent profileIntent = new Intent(ActivitySearchUser.this,ActivityProfile.class);
        profileIntent.putExtra(getResources().getString(R.string.extra_user_id),userId);
        startActivity(profileIntent);
    }
}
