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

public class ActivitySearchUser extends AppCompatActivity {
    private static String ERR_EMPTY_SEARCH_BAR = "Username is empty";

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

        searchBar= (MultiAutoCompleteTextView) findViewById(R.id.search_user_input_box);
        searchButton = (Button) findViewById(R.id.search_user_search_button);

        initListeners();
    }

    private void initListeners() {
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

        searchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                searchUser();
            }
        });
    }

    private void searchUser(){
        String usernameSearched = searchBar.getText().toString();
        if(TextUtils.isEmpty(usernameSearched)){
            Toast.makeText(ActivitySearchUser.this,ERR_EMPTY_SEARCH_BAR,Toast.LENGTH_LONG)
                    .show();
        }else{
            setRecyclerView(usernameSearched.toLowerCase());
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        setRecyclerView("");
    }

    public void setRecyclerView(String userName){
        Query query = FirebaseDatabase.getInstance()
                .getReference()
                .child(getResources().getString(R.string.database_user))
                .orderByChild("userName")
                .startAt(userName)
                .limitToLast(25);

        FirebaseRecyclerOptions<User> options =
                new FirebaseRecyclerOptions.Builder<User>()
                        .setQuery(query, User.class)
                        .build();

        FirebaseRecyclerAdapter adapter = new FirebaseRecyclerAdapter<User, UserHolder>(options) {
            @Override
            public UserHolder onCreateViewHolder(ViewGroup parent, int viewType) {
                // Create a new instance of the ViewHolder, in this case we are using a custom
                // layout called R.layout.message for each item
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

    private void goToProfileActivity(String userId) {
        Intent profileIntent = new Intent(ActivitySearchUser.this,ActivityProfile.class);
        profileIntent.putExtra(getResources().getString(R.string.extra_user_id),userId);
        startActivity(profileIntent);
    }
}
