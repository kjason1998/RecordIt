package com.example.kevin.recordit.Activity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v4.app.SupportActivity;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
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
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;
import id.zelory.compressor.Compressor;

public class ActivitySetting extends AppCompatActivity {

    private CircleImageView settingUserProfilePicture;
    private TextView settingUserName;
    private TextView settingUserStatus;

    private final static int PICTURE_PICK = 1;
    private StorageReference storageProfilePicturesRef;

    private DatabaseReference getUserDataRefrence;
    private FirebaseAuth mAuth;

    private StorageReference storageThumbImageRefrence;

    Bitmap thumbBitMap = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);

        mAuth = FirebaseAuth.getInstance();
        String online_user_id = mAuth.getCurrentUser().getUid();

        getUserDataRefrence = FirebaseDatabase.getInstance().getReference().child(
                getResources().getString(R.string.database_user)).child(online_user_id);

        storageProfilePicturesRef = FirebaseStorage.getInstance().getReference()
                .child(getResources().getString(R.string.storage_profile_pictures));

        storageThumbImageRefrence = FirebaseStorage.getInstance().getReference()
                .child(getResources().getString(R.string.storage_thumb_image));

        settingUserProfilePicture = findViewById(R.id.setting_user_profile_picture);
        settingUserName = findViewById(R.id.text_username);
        settingUserStatus = findViewById(R.id.text_user_status);

        getUserDataRefrence.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                String name = dataSnapshot
                        .child(getResources().getString(R.string.database_user_name))
                        .getValue().toString();
                String status = dataSnapshot
                        .child(getResources().getString(R.string.database_user_status))
                        .getValue().toString();
                String profilePicture = dataSnapshot
                        .child(getResources().getString(R.string.database_user_profile_picture))
                        .getValue().toString();
                String thumbImage = dataSnapshot
                        .child(getResources().getString(R.string.database_user_thumb_image))
                        .getValue().toString();

                settingUserName.setText(name);
                settingUserStatus.setText(status);
                Log.d("test",profilePicture);
                //load the profile picture, if not set yet use default pict stored in the app.
                Picasso.get().load(profilePicture)
                        .placeholder(R.drawable.default_profile).into(settingUserProfilePicture);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e("MyError_AS","onCancelledDatabase");
            }
        });

        settingUserProfilePicture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent galleryIntent = new Intent();
                galleryIntent.setAction(Intent.ACTION_GET_CONTENT);
                galleryIntent.setType("image/*");
                startActivityForResult(galleryIntent,PICTURE_PICK);
            }
        });

        settingUserStatus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent statusIntent = new Intent(
                        ActivitySetting.this,ActivityStatus.class);
                statusIntent.putExtra(getResources().getString(R.string.old_status)
                        ,settingUserStatus.getText().toString());
                startActivity(statusIntent);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);


        if(requestCode==PICTURE_PICK && resultCode==RESULT_OK && data!=null){

            Uri imageUri =  data.getData();
            // start picker to get image for cropping and then use the image in cropping activity
            CropImage.activity()
                    .setGuidelines(CropImageView.Guidelines.ON)
                    .setAspectRatio(1,1)
                    .start(ActivitySetting.this);
        }
        else if(requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE){

            CropImage.ActivityResult result = CropImage.getActivityResult(data);

            if (resultCode == RESULT_OK) {

                Uri profilePictUri = result.getUri();

                File thumbFilePath = new File(profilePictUri.getPath());

                try{
                    thumbBitMap = new Compressor(this)
                            .setMaxHeight(200).setMaxWidth(200)
                            .setQuality(50).compressToBitmap(thumbFilePath);
                }catch (IOException e){
                    e.printStackTrace();
                }

                String userId = mAuth.getCurrentUser().getUid();

                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                thumbBitMap.compress(Bitmap.CompressFormat.JPEG,50,byteArrayOutputStream);
                final byte[] thumbByte = byteArrayOutputStream.toByteArray();

                final StorageReference userPictureStorageRefrence =
                        storageProfilePicturesRef.child(userId+ getResources().getString(
                                R.string.default_profile_picture_file_type));

                final StorageReference userThumbStorageRefrence =
                        storageThumbImageRefrence.child(userId+ getResources().getString(
                                R.string.default_profile_picture_file_type));

                //putting picture to Firebase_Storage
                userPictureStorageRefrence.putFile(profilePictUri).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onComplete(@NonNull final Task<UploadTask.TaskSnapshot> task) {
                        if(task.isSuccessful()){
                            //get uri
                            userPictureStorageRefrence.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                @Override
                                public void onSuccess(Uri uri) {
                                    Uri downloadUri = uri;
                                    final String downloadProfilePictureLink = downloadUri.toString();

                                    UploadTask uploadTask = userThumbStorageRefrence.putBytes(thumbByte);

                                    uploadTask.addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                                        @Override
                                        public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> thumbTask) {
                                            userThumbStorageRefrence.getDownloadUrl()
                                                    .addOnSuccessListener(new OnSuccessListener<Uri>() {
                                                @Override
                                                public void onSuccess(Uri uri) {
                                                    Uri downloadThumbUri = uri;
                                                    String downloadThumbLink = downloadThumbUri.toString();
                                                    if(task.isSuccessful()){
                                                        Map updateUserData = new HashMap();
                                                        updateUserData.put(getResources().getString
                                                                        (R.string.database_user_profile_picture),
                                                                        downloadProfilePictureLink);
                                                        updateUserData.put(getResources().getString
                                                                        (R.string.database_user_thumb_image),
                                                                downloadThumbLink);

                                                        //put the new data of thumb and profile image uri into database
                                                        getUserDataRefrence.updateChildren(updateUserData)
                                                                .addOnCompleteListener(
                                                                new OnCompleteListener<Void>() {
                                                                    @Override
                                                                    public void onComplete(@NonNull Task<Void> task) {
                                                                        Log.v("Activity setting",
                                                                                "thumb image are succesfully saved in database");
                                                                    }
                                                                }
                                                        );
                                                    }
                                                }
                                            });
                                        }
                                    });
                                }
                            });
                        }
                        else{
                            Toast.makeText(ActivitySetting.this,
                                    "Failed to change profile picture",Toast.LENGTH_LONG);
                        }
                    }
                });
            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                Exception error = result.getError();
            }
        }
    }
}
