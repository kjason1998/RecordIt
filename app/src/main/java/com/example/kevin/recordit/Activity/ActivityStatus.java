package com.example.kevin.recordit.Activity;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.example.kevin.recordit.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class ActivityStatus extends AppCompatActivity {
    private Toolbar mToolbar;
    private Button updateStatusButtom;
    private EditText statusInput;

    private DatabaseReference statusDatabaseRefrence;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_status);

        mToolbar = findViewById(R.id.status_app_bar);
        updateStatusButtom = findViewById(R.id.statusButton);
        statusInput = (EditText) findViewById(R.id.statusEditText);

        String oldStatus = getIntent().getStringExtra(getResources().getString(R.string.old_status));

        statusInput.setHint(oldStatus);

        mAuth = FirebaseAuth.getInstance();
        String onlineUId = mAuth.getCurrentUser().getUid();
        statusDatabaseRefrence = FirebaseDatabase.getInstance().getReference()
                .child(getResources().getString(R.string.database_user))
                .child(onlineUId).child(getResources().getString(R.string.database_user_status));

        setSupportActionBar(mToolbar);
        getSupportActionBar().setTitle(getResources().getString(R.string.status_app_bar_title));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        updateStatusButtom.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String newStatus = statusInput.getText().toString();

                changeUserStatus(newStatus);
            }
        });
    }

    private void changeUserStatus(String newStatus) {
        if(newStatus.isEmpty()){
            Toast.makeText(ActivityStatus.this,getResources().getString(
                    R.string.status_err_empty),Toast.LENGTH_LONG).show();
        }
        else if(newStatus.length()>getResources().getInteger(R.integer.status_max_chars)){
            Toast.makeText(ActivityStatus.this,getResources().getString(
                    R.string.status_err_over),Toast.LENGTH_LONG).show();
        }
        else if(newStatus.length()<getResources().getInteger(R.integer.status_min_chars)){
            Toast.makeText(ActivityStatus.this,getResources().getString(
                    R.string.status_err_below),Toast.LENGTH_LONG).show();
        }
        else{
            statusDatabaseRefrence.setValue(newStatus).addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
                    Intent goBackSetting =
                            new Intent(ActivityStatus.this,ActivitySetting.class);
                    goBackSetting.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(goBackSetting);
                    Toast.makeText(ActivityStatus.this,
                            getResources().getString(R.string.status_update_success),
                            Toast.LENGTH_LONG).show();
                }
            });
        }
    }
}
