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

/**
 * Activity Register where people register
 * using : name, email and paswword
 * with certain rules.
 * name < 2 and > 21
 * email need to be valid
 * password > 7
 *
 * @author Kevin
 */
public class ActivityRegister extends AppCompatActivity {
    private static final int MIN_PASS_LENG = 7;
    private static final int MIN_NAME_LENG = 3;
    private static final int MAX_NAME_LENG = 20;

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

        background = findViewById(R.id.register_background);

        mToolbar = findViewById(R.id.register_tool_bar);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setTitle(getResources().getString(R.string.register_app_bar_title));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        registerName = findViewById(R.id.fillTextName);
        registerEmail =  findViewById(R.id.fillTextEmail);
        registerPassword =  findViewById(R.id.fillTextPassword);

        createAccount = findViewById(R.id.buttonCreateAccount);
        loadingDialog = new ProgressDialog(this);

        initListener();
    }

    /**
     * Initialize all listener.
     */
    private void initListener() {
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

    /**
     * Registering using name email and password
     * with FirebaseAuth
     * @param name
     * @param email
     * @param password
     */
    private void RegisterAccount(final String name, String email, String password) {

        //check the EditText validation
        if(TextUtils.isEmpty(name)){
            Toast.makeText
                    (ActivityRegister.this,
                            getResources().getString(R.string.register_err_name_empty),
                            Toast.LENGTH_LONG).show();
        }
        else if(TextUtils.isEmpty(email)){
            Toast.makeText
                    (ActivityRegister.this,
                            getResources().getString(R.string.register_err_email_empty),
                            Toast.LENGTH_LONG).show();
        }
        else if(TextUtils.isEmpty(password)){
            Toast.makeText
                    (ActivityRegister.this,
                            getResources().getString(R.string.register_err_password_empty),
                            Toast.LENGTH_LONG).show();
        }else if(name.length() < MIN_NAME_LENG){
            Toast.makeText
                    (ActivityRegister.this,
                            getResources().getString(R.string.register_err_name_too_short),
                            Toast.LENGTH_LONG).show();
        }else if(name.length() > MAX_NAME_LENG){
            Toast.makeText
                    (ActivityRegister.this,
                            getResources().getString(R.string.register_err_name_too_long),
                            Toast.LENGTH_LONG).show();
        }
        else if(password.length() < MIN_PASS_LENG){
            Toast.makeText
                    (ActivityRegister.this,
                            getResources().getString(R.string.register_err_password_too_short),
                            Toast.LENGTH_LONG).show();
        }else{
            loadingDialog.setTitle(
                    getResources().getString(R.string.register_loading_dialog_title));
            loadingDialog.setMessage(
                    getResources().getString(R.string.register_loading_dialog_message));
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
                                        .child(getResources().getString(R.string.database_user)).child(currentUserId);
                                mRootRef.child(getResources().getString(R.string.database_user_name))
                                        .setValue(name);
                                mRootRef.child(getResources().getString(R.string.database_user_status))
                                        .setValue(getResources().getString(R.string.default_status));
                                mRootRef.child(getResources().getString(R.string.database_user_profile_picture))
                                        .setValue(getResources().getString(R.string.default_profile_picture));
                                mRootRef.child(getResources().getString(R.string.database_user_thumb_image))
                                        .setValue(getResources().getString(R.string.default_thumb_image));
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
                                        getResources().getString(R.string.register_err_failed),
                                        Toast.LENGTH_LONG).show();
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
