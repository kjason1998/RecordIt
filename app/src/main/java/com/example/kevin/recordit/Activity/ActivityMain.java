package com.example.kevin.recordit.Activity;

import android.content.Intent;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;

import com.example.kevin.recordit.Adapter.TabsPageAdapter;
import com.example.kevin.recordit.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

/**
 * Activity main is the main page,
 * in there there is friends page
 * which show every single friends that
 * the user have.
 */
public class ActivityMain extends AppCompatActivity {

    private Toolbar mToolbar;
    private FirebaseAuth mAuth;

    private ViewPager myViewPager;
    private TabLayout myTabLayout;
    private TabsPageAdapter myTabsPagerAdapter;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_page);

        //user check , if sign in or not
        mAuth = FirebaseAuth.getInstance();

        //Tabs for main act
        myViewPager = findViewById(R.id.main_tabs_pager);
        myTabsPagerAdapter = new TabsPageAdapter(getSupportFragmentManager());
        myViewPager.setAdapter(myTabsPagerAdapter);

        myTabLayout = findViewById(R.id.main_tabs);
        myTabLayout.setupWithViewPager(myViewPager);

        mToolbar = findViewById(R.id.home_page_toolbar);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setTitle(getResources().getString(R.string.main_app_bar_title));
    }

    @Override
    protected void onStart() {
        super.onStart();

        //get current user in firebase
        FirebaseUser currentUser = mAuth.getCurrentUser();

        /*  disable user to go back to home activity when they press back from start
                activity since people go from welcome act to main and check if the user are
                log in or not and send them here if not. */
        if(currentUser == null){
            LogOutUser();
        }
    }

    /**
     * This method is for logging out when called
     */
    private void LogOutUser() {
        Intent startPageIntent =
                new Intent(ActivityMain.this,ActivityStart.class);
        //make sure people can not go back in again
        startPageIntent.addFlags
                (Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(startPageIntent);
        finish();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        getMenuInflater().inflate(R.menu.main_menu, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);

        if(item.getItemId() == R.id.main_logout_button){
            mAuth.signOut();

            LogOutUser();
        }

        if(item.getItemId() == R.id.main_account_setting_button){
            Intent settingPageIntent =
                    new Intent(ActivityMain.this,ActivitySetting.class);
            startActivity(settingPageIntent);
        }

        if(item.getItemId() == R.id.main_user_list_button){
            Intent allUsersIntent =
                    new Intent(ActivityMain.this,ActivitySearchUser.class);
            startActivity(allUsersIntent);
        }

        return true;
    }
}
