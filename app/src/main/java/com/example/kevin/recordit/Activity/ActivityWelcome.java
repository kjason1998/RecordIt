package com.example.kevin.recordit.Activity;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.example.kevin.recordit.R;

/**
 * Loading screen for the application
 * will show the app logo.
 */
public class ActivityWelcome extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.welcome_page);

        Thread thread = new Thread(){
            @Override
            public void run() {
                try{
                    sleep(1000);
                }
                catch (Exception e){
                    e.printStackTrace();
                }
                finally {
                    Intent mainIntent =
                            new Intent(ActivityWelcome.this,ActivityMain.class );
                    startActivity(mainIntent);
                }
            }
        };
        thread.start();
    }

    @Override
    protected void onPause() {
        super.onPause();

        finish();
    }
}
