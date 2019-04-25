package com.example.kevin.recordit.Activity;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.kevin.recordit.Adapter.MessageAdapter;
import com.example.kevin.recordit.Model.Message;
import com.example.kevin.recordit.R;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class ActivityChat extends AppCompatActivity {
    /////////////////////////////for the audio recording///////////////////
    private static final String LOG_TAG = "AudioRecordTest";
    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;
    private static final int REQUEST_WRITE_EXTERNAL_STORAGE_PERMISSION = 100;
    private static String mFileName = null;

    private StorageReference mStorage;

    private MediaRecorder mRecorder = null;

    // Requesting permission
    private boolean permissionToRecordAccepted = false;
    private boolean permissionToWriteExternalStorage = false;
    private String [] permissionsRecordAudio = {Manifest.permission.RECORD_AUDIO};
    private String [] permissionsWriteExternalStorage = {Manifest.permission.WRITE_EXTERNAL_STORAGE};
    ////////////////////////////////////////////////////////////////////////

    private String receiverUserId,receiverName;

    private Toolbar toolbar;

    private TextView toolbarReceiverName,toolbarReceiverStatus;
    private CircleImageView toolbarReceiverProfilePicture;

    private RecyclerView chatsView;
    private RecyclerView.LayoutManager chatLinearLayout;
    private RecyclerView.Adapter chatAdapter;
    private List<Message> messageList = new ArrayList<Message>();

    private ImageView sendVoiceMessage;
    private EditText inputMessage;
    private ImageView sendMessageBytton;

    private DatabaseReference rootReference;

    private FirebaseAuth mAuth;
    private String thisOnlineUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        mStorage = FirebaseStorage.getInstance().getReference();
        rootReference = FirebaseDatabase.getInstance().getReference();

        mAuth = FirebaseAuth.getInstance();
        thisOnlineUserId = mAuth.getCurrentUser().getUid();

        Intent intent = getIntent();
        receiverName =
                intent.getExtras().get(getResources().getString(R.string.extra_user_name)).toString();

        receiverUserId =
                intent.getExtras().get(getResources().getString(R.string.extra_user_id)).toString();

        setToolbar();
        setButtonsAndInputMessage();

        fetchMessagesFromFirebaseDatabase();
        chatsView = findViewById(R.id.chatActivityRecyclerViewMessages);
        setRecyclerView();

        mFileName = Environment.getExternalStorageDirectory().getAbsolutePath();
        mFileName += "/audiorecordtest.3gp";

        if (ContextCompat.checkSelfPermission(ActivityChat.this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, permissionsRecordAudio, REQUEST_RECORD_AUDIO_PERMISSION);
        }

        if (ContextCompat.checkSelfPermission(ActivityChat.this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, permissionsWriteExternalStorage, REQUEST_WRITE_EXTERNAL_STORAGE_PERMISSION);
        }
    }

    /**
     * initialize the tool bar
     * - Name
     * - Status, ThumbImage
     */
    private void setToolbar() {
        toolbar = findViewById(R.id.chat_app_bar);
        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setDisplayShowCustomEnabled(true);

        LayoutInflater layoutInflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View actionBarView = layoutInflater.inflate(R.layout.chat_custom_bar, null);

        actionBar.setCustomView(actionBarView);

        toolbarReceiverStatus = findViewById(R.id.chatBarUserStatusDisplay);
        toolbarReceiverName = findViewById(R.id.chatBarUserNameDisplay);
        toolbarReceiverProfilePicture = findViewById(R.id.chatBarUserProfileImageDisplay);

        toolbarReceiverName.setText(receiverName);

        rootReference
                .child(getResources().getString(R.string.database_user))
                .child(receiverUserId)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        final String status = dataSnapshot
                                .child(getResources().getString(R.string.database_user_status))
                                .getValue().toString();
                        final String thumbProfileImage = dataSnapshot
                                .child(getResources().getString(R.string.database_user_thumb_image))
                                .getValue().toString();

                        //change when you want to set offline functions
                        Picasso.get()
                                .load(thumbProfileImage)
                                .placeholder(R.drawable.default_profile)
                                .into(toolbarReceiverProfilePicture);
                        toolbarReceiverStatus.setText(status);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
    }

    /**
     * set the send message button to send
     * also get message from the input message.
     * initialize select photo
     */
    private void setButtonsAndInputMessage() {
        sendVoiceMessage = findViewById(R.id.chatActivityRecordMessageButton);
        inputMessage = findViewById(R.id.chatActivityMessageTextView);
        sendMessageBytton = findViewById(R.id.chatActivitySendMessageButton);

        sendMessageBytton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String messageText = inputMessage.getText().toString();
                sendMessage(messageText);
            }
        });

        sendVoiceMessage.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                //WHEN PRESS
                if(motionEvent.getAction() == MotionEvent.ACTION_DOWN){
                    startRecording();
                    Toast.makeText(ActivityChat.this,"start recording"
                            ,Toast.LENGTH_LONG).show();
                }//WHEN RELEASE
                else if(motionEvent.getAction() == MotionEvent.ACTION_UP){
                    stopRecording();
                    Toast.makeText(ActivityChat.this,"stop recording"
                            ,Toast.LENGTH_LONG).show();
                }

                return true;
            }
        });
    }

    private void startRecording() {
        mRecorder = new MediaRecorder();
        mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        mRecorder.setOutputFile(mFileName);
        mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);

        try {
            mRecorder.prepare();
        } catch (IOException e) {
            Log.e(LOG_TAG, "prepare() failed");
        }

        mRecorder.start();
    }

    private void stopRecording() {
        mRecorder.stop();
        mRecorder.release();
        mRecorder = null;


        putMessageToFirebase("LINK AUDIO FILE",
                getResources().getString(R.string.database_message_type_audio));
        //uploadAudio();
    }

    private void uploadAudio(final String messageUniqeId, final String thisOnlineUserId, final String receiverUserId) {
        final StorageReference filepath = mStorage.child("Audio").child(
                messageUniqeId+ getResources().getString(R.string.default_audio_message_file_type));

        final Uri uri = Uri.fromFile(new File(mFileName).getAbsoluteFile());

        Log.d(LOG_TAG,uri.toString());
        filepath.putFile(uri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                Toast.makeText(ActivityChat.this,"uploaded succesfully",Toast.LENGTH_LONG)
                        .show();
                filepath.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                    public void onSuccess(Uri uri) {
                        Uri downloadUri = uri;
                        Log.d(LOG_TAG,downloadUri.toString());
                        String downloadLinkAudio = downloadUri.toString();
                        Log.d(LOG_TAG,"downloadLink:"+downloadLinkAudio);
                        rootReference
                                .child(getResources().getString(R.string.database_message))
                                .child(thisOnlineUserId)
                                .child(receiverUserId)
                                .child(messageUniqeId)
                                .child(getResources().getString(R.string.database_message_input))
                                .setValue(downloadLinkAudio);
                        rootReference
                                .child(getResources().getString(R.string.database_message))
                                .child(receiverUserId)
                                .child(thisOnlineUserId)
                                .child(messageUniqeId)
                                .child(getResources().getString(R.string.database_message_input))
                                .setValue(downloadLinkAudio);
                    }
                });
            }
        });
    }

    private void sendMessage(String messageText) {
        if(messageText.isEmpty()){
            Toast.makeText(ActivityChat.this,
                    getResources().getString(R.string.chat_activity_err_empty_message),
                    Toast.LENGTH_LONG).show();
        }
        else{
            putMessageToFirebase(messageText,
                    getResources().getString(R.string.database_message_type_text));
        }
    }

    /**
     * put message to firebase
     * can handle type text and audio
     * @param messageText - > string of the message
     * @param typeMessage - > string of the file name audio in firebase storage
     */
    private void putMessageToFirebase(String messageText,String typeMessage) {
        String messageSenderPath =
                getResources().getString(R.string.database_message) + "/" +
                        thisOnlineUserId + "/" +receiverUserId;
        String messageReceiverPath =
                getResources().getString(R.string.database_message) + "/" +
                        receiverUserId + "/" + thisOnlineUserId;

        DatabaseReference messageKeyRefrence = rootReference
                .child(getResources().getString(R.string.database_message))
                .child(thisOnlineUserId)
                .child(receiverUserId)
                .push();
        String messageUniqeId = messageKeyRefrence.getKey();

        Map messageDetailHashmap = new HashMap();

        messageDetailHashmap.put(getResources().getString(R.string.database_message_read_status),false);
        messageDetailHashmap.put(getResources().getString(R.string.database_message_time),
                ServerValue.TIMESTAMP);



        messageDetailHashmap.put(getResources().getString(R.string.database_message_from),
                thisOnlineUserId);

        if(typeMessage.equalsIgnoreCase(
                getResources().getString(R.string.database_message_type_text))){
            messageDetailHashmap.put(getResources().getString(R.string.database_message_input),messageText);
            messageDetailHashmap.put(getResources().getString(R.string.database_message_type),
                    getResources().getString(R.string.database_message_type_text));
        }else{
            uploadAudio(messageUniqeId,thisOnlineUserId,receiverUserId);
            messageDetailHashmap.put(getResources().getString(R.string.database_message_type),
                    getResources().getString(R.string.database_message_type_audio));
            messageDetailHashmap.put(getResources().getString(R.string.database_message_input),
                    "link file");

            Log.d(LOG_TAG,"test2");
        }

        Map messageHashmap = new HashMap();

        messageHashmap.put(messageSenderPath + "/" + messageUniqeId, messageDetailHashmap);
        messageHashmap.put(messageReceiverPath + "/" + messageUniqeId, messageDetailHashmap);

        rootReference.updateChildren(messageHashmap, new DatabaseReference.CompletionListener() {
            @Override
            public void onComplete(@Nullable DatabaseError databaseError, @NonNull DatabaseReference databaseReference) {
                if(databaseError != null){
                    Log.e("ActivityChat update children error",databaseError.getMessage().toString());
                }

                inputMessage.setText(null);
            }
        });
    }

    private void fetchMessagesFromFirebaseDatabase() {
        rootReference
                .child(getResources().getString(R.string.database_message))
                .child(thisOnlineUserId)
                .child(receiverUserId)
                .addChildEventListener(new ChildEventListener() {
                    @Override
                    public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                        Message dataMessage = dataSnapshot.getValue(Message.class);

                        messageList.add(dataMessage);

                        chatAdapter.notifyDataSetChanged();
                        Log.d("ActivityChat,onChildAdded", messageList.toString());
                    }

                    @Override
                    public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                        Message dataMessage = dataSnapshot.getValue(Message.class);
                        //edit the newly changed data - for now it is special only for audio file
                        messageList.set(messageList.size()-1,dataMessage);
                        Log.d("ActivityChat,onChildChanged", dataMessage.getMessage());
                        Log.d("ActivityChat,onChildChanged", messageList.toString());
                        chatAdapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {

                    }

                    @Override
                    public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
    }

    private void setRecyclerView() {
        // use a linear layout manager
        chatLinearLayout = new LinearLayoutManager(this);
        chatsView.setLayoutManager(chatLinearLayout);

        // specify an adapter (see also next example)
        chatAdapter = new MessageAdapter(messageList);
        chatsView.setAdapter(chatAdapter);
        chatsView.setItemAnimator(new DefaultItemAnimator());
    }
}
