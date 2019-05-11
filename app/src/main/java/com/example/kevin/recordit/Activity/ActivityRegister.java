package com.example.kevin.recordit.Activity;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.example.kevin.recordit.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;

public class ActivityRegister extends AppCompatActivity {
    private static final int MIN_PASS_LENG = 7;
    private static final int MIN_NAME_LENG = 3;
    private static final int MAX_NAME_LENG = 20;

    private static final String ERR_INPUT_EMPTY = "One or more field are empty";
    private static final String ERR_PASS_MIN = "Password need to be at least " + MIN_PASS_LENG
            + " characters";
    private static final String ERR_NAME_MIN = "Name need to be at least " + MIN_NAME_LENG
            + " characters";
    private static final String ERR_NAME_MAX = "Maximum character for name is exceeded, max " + MAX_NAME_LENG
            + " characters";

    private FirebaseAuth mAuth;
    private DatabaseReference mRootRef;

    private LinearLayout background;

    private Toolbar mToolbar;
    private ProgressDialog loadingDialog;

    private EditText registerName;
    private EditText registerEmail;
    private EditText registerPassword;

    private Button createAccount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.register_page);

        mAuth = FirebaseAuth.getInstance();

        background = (LinearLayout) findViewById(R.id.register_background);

        mToolbar = (Toolbar) findViewById(R.id.register_tool_bar);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setTitle("Sign up");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        registerName = (EditText) findViewById(R.id.fillTextName);
        registerEmail = (EditText) findViewById(R.id.fillTextEmail);
        registerPassword= (EditText) findViewById(R.id.fillTextPassword);

        createAccount = (Button) findViewById(R.id.buttonCreateAccount);
        loadingDialog = new ProgressDialog(this);

        createAccount.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String name = registerName.getText().toString();
                String email = registerEmail.getText().toString();
                String password = registerPassword.getText().toString();

                RegisterAccount(name,email,password);
            }
        });
        background.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                hideKeyboard(ActivityRegister.this);
            }
        });
    }

    //using firebase create account
    private void RegisterAccount(final String name, String email, String password) {

        //check the EditText validation
        if(TextUtils.isEmpty(name)){
            Toast.makeText
                    (ActivityRegister.this,ERR_INPUT_EMPTY,Toast.LENGTH_LONG).show();
        }
        else if(TextUtils.isEmpty(email)){
            Toast.makeText
                    (ActivityRegister.this,ERR_INPUT_EMPTY,Toast.LENGTH_LONG).show();
        }
        else if(TextUtils.isEmpty(password)){
            Toast.makeText
                    (ActivityRegister.this,ERR_INPUT_EMPTY,Toast.LENGTH_LONG).show();
        }else if(name.length() < MIN_NAME_LENG){
            Toast.makeText
                    (ActivityRegister.this,ERR_NAME_MIN,Toast.LENGTH_LONG).show();
        }else if(name.length() > MAX_NAME_LENG){
            Toast.makeText
                    (ActivityRegister.this,ERR_NAME_MAX,Toast.LENGTH_LONG).show();
        }
        else if(password.length() < MIN_PASS_LENG){
            Toast.makeText
                    (ActivityRegister.this,ERR_PASS_MIN,Toast.LENGTH_LONG).show();
        }else{
            loadingDialog.setTitle("Creating new account");
            loadingDialog.setMessage("Please wait until account created");
            loadingDialog.show();

            mAuth.createUserWithEmailAndPassword(email,password)
                    .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            //if the mAuth method is successful
                            if(task.isSuccessful()){
                                String currentUserId = mAuth.getCurrentUser().getUid();
                                //store the reference from the firebase
                                mRootRef = FirebaseDatabase.getInstance().getReference()
                                        .child("users").child(currentUserId);
                                mRootRef.child("userName").setValue(name);
                                mRootRef.child("userStatus").setValue("Online");
                                mRootRef.child("userImage").setValue("default_Profile");
                                mRootRef.child("userThumbImage").setValue("default_image");
                                FirebaseInstanceId.getInstance().getInstanceId().addOnSuccessListener(new OnSuccessListener<InstanceIdResult>() {
                                    @Override
                                    public void onSuccess(InstanceIdResult instanceIdResult) {
                                        String deviceToken = instanceIdResult.getToken();

                                        mRootRef.child(getResources().getString(R.string.database_user_token))
                                                .setValue(deviceToken).addOnSuccessListener(new OnSuccessListener<Void>() {
                                            @Override
                                            public void onSuccess(Void aVoid) {
                                                Intent mainIntent =
                                                        new Intent(ActivityRegister.this,
                                                                ActivityMain.class);
                                                //cant go back - only cant if log out
                                                mainIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|
                                                        Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                                startActivity(mainIntent);
                                                finish();
                                            }
                                        });
                                    }
                                });

                            }
                            else{
                                Toast.makeText(ActivityRegister.this,
                                        "Make sure to have valid Email address " +
                                                task.toString(),Toast.LENGTH_LONG).show();
                            }
                            loadingDialog.dismiss();
                        }
                    });
        }
    }

    public static void hideKeyboard(Activity activity) {
        InputMethodManager imm = (InputMethodManager) activity.getSystemService(Activity.INPUT_METHOD_SERVICE);
        //Find the currently focused view, so we can grab the correct window token from it.
        View view = activity.getCurrentFocus();
        //If no view currently has focus, create a new one, just so we can grab a window token from it
        if (view == null) {
            view = new View(activity);
        }
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }
}
