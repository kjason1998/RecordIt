package com.example.kevin.recordit.Activity;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.example.kevin.recordit.R;

/**
 * there will be 2 choice
 * signing in and signing up.
 */
public class ActivityStart extends AppCompatActivity {

    private Button signIn;
    private Button signUp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.start_page);

        signIn = findViewById(R.id.buttonSignIn);
        signUp = findViewById(R.id.buttonSignUp);

        initListener();
    }

    private void initListener() {
        signIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent signingInIntent =
                        new Intent(ActivityStart.this,ActivityLogin.class);
                startActivity(signingInIntent);
            }
        });
        signUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent signingUpIntent =
                        new Intent(ActivityStart.this,ActivityRegister.class);
                startActivity(signingUpIntent);
            }
        });
    }
}
