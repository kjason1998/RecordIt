package com.example.kevin.recordit.Activity;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
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
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
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

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class ActivityChat extends AppCompatActivity {
    private static final int MAX_CHARS_MESSAGE = 200;
    private static final String ERR_MAX_CHAR = "Maximum char is "+MAX_CHARS_MESSAGE+" chars";
    /////////////////////////////for the audio recording///////////////////
    private static final String LOG_TAG = "AudioRecordTest";
    private static String mFileName = null;
    //temp local file name for storing and retrieving audio file
    private static String tempFilterAudioName = "tempAudio.pcm";

    private StorageReference mStorage;

    private MediaRecorder mRecorder = null;

    // Requesting permission
    public static final int REQUEST_ID_MULTIPLE_PERMISSIONS = 1;
    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;
    private static final int REQUEST_WRITE_EXTERNAL_STORAGE_PERMISSION = 100;
    private String [] permissionsRecordAudio = {Manifest.permission.RECORD_AUDIO};
    private String [] permissionsWriteExternalStorage = {Manifest.permission.WRITE_EXTERNAL_STORAGE};
    ////////////////////////////////////////////////////////////////////////

    private String receiverUserId,receiverName;

    private LinearLayout background;

    private Toolbar toolbar;

    private TextView toolbarReceiverName,toolbarReceiverStatus;
    private CircleImageView toolbarReceiverProfilePicture;

    private RecyclerView chatsView;
    private RecyclerView.LayoutManager chatLinearLayout;
    private MessageAdapter chatAdapter;
    private List<Message> messageList = new ArrayList<Message>();

    //boolean if button is still press - only for filter bcs of how audio record works
    private boolean recording;
    private ImageView sendVoiceMessage;
    private ImageView sendChipmunkVoiceMessage;
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

        checkAndRequestPermissions();
    }


    private  boolean checkAndRequestPermissions() {
        int permissionRecordAudio = ContextCompat.checkSelfPermission(this,
                Manifest.permission.RECORD_AUDIO);
        int permissionWriteExternalStorage = ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE);
        List<String> listPermissionsNeeded = new ArrayList<>();
        if (permissionWriteExternalStorage != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
        if (permissionRecordAudio != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(Manifest.permission.RECORD_AUDIO);
        }
        if (!listPermissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(this, listPermissionsNeeded.toArray(new String[listPermissionsNeeded.size()]),REQUEST_ID_MULTIPLE_PERMISSIONS);
            return false;
        }
        return true;
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
                                .placeholder(R.drawable.blue_profile_picture)
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
        sendChipmunkVoiceMessage = findViewById(R.id.chatActivityChipmunkFilteredRecordMessageButton);
        inputMessage = findViewById(R.id.chatActivityMessageTextView);
        sendMessageBytton = findViewById(R.id.chatActivitySendMessageButton);

        sendMessageBytton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String messageText = inputMessage.getText().toString();
                //input message check is inside the method sendMessage
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

        sendChipmunkVoiceMessage.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                //WHEN PRESS
                if(motionEvent.getAction() == MotionEvent.ACTION_DOWN){
                    Thread recordThread = new Thread(new Runnable(){

                        @Override
                        public void run() {
                            recording = true;
                            startFilterRecord();
                        }

                    });
                    recordThread.start();
                    Toast.makeText(ActivityChat.this,"start recording"
                            ,Toast.LENGTH_LONG).show();
                }//WHEN RELEASE
                else if(motionEvent.getAction() == MotionEvent.ACTION_UP){
                    recording = false;
                    Toast.makeText(ActivityChat.this,"stop recording"
                            ,Toast.LENGTH_LONG).show();
                }

                return true;
            }
        });
    }

    /**
     * start recording normal voice message
     * start when button is push
     */
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

    /**
     * stop recording normal voice message
     * stop when button is released
     */
    private void stopRecording() {
        mRecorder.stop();
        mRecorder.release();
        mRecorder = null;


        putMessageToFirebase("LINK AUDIO FILE",
                getResources().getString(R.string.database_message_type_audio));
        //uploadAudio();
    }

    /**
     * start the recording using audio recorder - voice message that going to be filter
     * this example have 15000 hz input ( the drop down does not effect the input audio)
     */
    private void startFilterRecord(){

        File file = new File(Environment.getExternalStorageDirectory(), tempFilterAudioName);

        try {
            file.createNewFile();

            OutputStream outputStream = new FileOutputStream(file);
            BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(outputStream);
            DataOutputStream dataOutputStream = new DataOutputStream(bufferedOutputStream);

            int minBufferSize = AudioRecord.getMinBufferSize(15000,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT);

            short[] audioData = new short[minBufferSize];

            AudioRecord audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC,
                    15000,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    minBufferSize);

            audioRecord.startRecording();

            while(recording){
                int numberOfShort = audioRecord.read(audioData, 0, minBufferSize);
                for(int i = 0; i < numberOfShort; i++){
                    dataOutputStream.writeShort(audioData[i]);
                }
            }

            audioRecord.stop();
            dataOutputStream.close();
            putMessageToFirebase("LINK AUDIO FILE",
                    getResources().getString(R.string.database_message_type_chipmunk_audio));

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void uploadAudio(final String messageUniqeId, final String thisOnlineUserId, final String receiverUserId) {
        final StorageReference filepath = mStorage.child("Audio").child(
                messageUniqeId+ getResources().getString(R.string.default_audio_message_file_type));

        final Uri uri = Uri.fromFile(new File(mFileName).getAbsoluteFile());

        filepath.putFile(uri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                Toast.makeText(ActivityChat.this,"uploaded succesfully",Toast.LENGTH_LONG)
                        .show();
                filepath.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                    public void onSuccess(Uri uri) {
                        Uri downloadUri = uri;
                        String downloadLinkAudio = downloadUri.toString();
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

    private void uploadFilteredAudio(final String messageUniqeId, final String thisOnlineUserId, final String receiverUserId) {
        final StorageReference filepath = mStorage.child("Audio").child(
                messageUniqeId+ getResources().getString(R.string.default_audio_message_file_type));

        File file = new File(Environment.getExternalStorageDirectory(), tempFilterAudioName);
        final Uri uri = Uri.fromFile(file);

        filepath.putFile(uri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                Toast.makeText(ActivityChat.this,"uploaded succesfully",Toast.LENGTH_LONG)
                        .show();
                filepath.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                    public void onSuccess(Uri uri) {
                        Uri downloadUri = uri;
                        String downloadLinkAudio = downloadUri.toString();

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
        }else if(messageText.length() == MAX_CHARS_MESSAGE){
            Toast.makeText(ActivityChat.this,
                    ERR_MAX_CHAR,
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
        }else if(typeMessage.equalsIgnoreCase(
                getResources().getString(R.string.database_message_type_chipmunk_audio)))
        {
            uploadFilteredAudio(messageUniqeId,thisOnlineUserId,receiverUserId);
            messageDetailHashmap.put(getResources().getString(R.string.database_message_type),
                    getResources().getString(R.string.database_message_type_chipmunk_audio));
            messageDetailHashmap.put(getResources().getString(R.string.database_message_input),
                    "link file");

        }
            else
            {
            uploadAudio(messageUniqeId,thisOnlineUserId,receiverUserId);
            messageDetailHashmap.put(getResources().getString(R.string.database_message_type),
                    getResources().getString(R.string.database_message_type_audio));
            messageDetailHashmap.put(getResources().getString(R.string.database_message_input),
                    "link file");
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

                    }

                    @Override
                    public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                        Message dataMessage = dataSnapshot.getValue(Message.class);
                        messageList.set(messageList.size()-1,dataMessage);
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

        // specify an adapter
        chatAdapter = new MessageAdapter(messageList);
        chatsView.setAdapter(chatAdapter);
        chatsView.setItemAnimator(new DefaultItemAnimator());
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
