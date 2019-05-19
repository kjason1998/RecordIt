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

/**
 * @author Kevin
 *
 * This the activity for the chatting page,
 * people can send text message, audio message and voice filter (1).
 * There is still a bug to fix:
 *  - messages are acting wierd.
 *  - audio file need to be initilize as
 *      default link audio message first (find a work around!)
 *
 * the Activity chat uses recyclerview, with MessageAdapter and MessageHolder
 * to show the message. Messages are stored in firebaseDatabase (realtime).
 */
public class ActivityChat extends AppCompatActivity {
    private static final String LOG_TAG = "ActivityChat";
    private static final int MAX_CHARS_MESSAGE = 200;

    /////////////////////////////for the audio recording///////////////////
    //temp local file name for storing and retrieving audio file
    private static String TEMP_FILTER_AUDIO_NAME = "tempAudio.pcm";
    private static String TEMP_FILE_AUDIO_NAME = "/audiorecordtest.3gp";
    private static String mFileName = null;

    private StorageReference mStorage;

    private MediaRecorder mRecorder = null;

    // Requesting permission
    public static final int REQUEST_ID_MULTIPLE_PERMISSIONS = 1;
    ////////////////////////////////////////////////////////////////////////

    private String receiverUserId,receiverName;

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
        initListener();

        fetchMessagesFromFirebaseDatabase();
        chatsView = findViewById(R.id.chatActivityRecyclerViewMessages);
        setRecyclerView();

        mFileName = Environment.getExternalStorageDirectory().getAbsolutePath();
        mFileName += TEMP_FILE_AUDIO_NAME;

        //check if permission granted, ask if not
        checkAndRequestPermissions();
    }

    /**
     * Checking if permission is granted or not.
     *  - permission for using microphone : voice message and voice filter message.
     *  - permission to use the storage : this is so that user can hear the audio file.
     *         how the audio work will be explain on later in this code.
     *
     * @return true if all permissions above are granted.
     */
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
     * - Name.
     * - Status, ThumbImage.
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
     * set the everything and initialize
     * anything that need to have listener.
     */
    private void initListener() {
        sendVoiceMessage = findViewById(R.id.chatActivityRecordMessageButton);
        sendChipmunkVoiceMessage = findViewById(R.id.chatActivityChipmunkFilteredRecordMessageButton);
        inputMessage = findViewById(R.id.chatActivityMessageTextView);
        sendMessageBytton = findViewById(R.id.chatActivitySendMessageButton);

        //for normal message
        sendMessageBytton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String messageText = inputMessage.getText().toString();
                //input message check is inside the method sendMessage
                sendMessage(messageText);
            }
        });

        //for voice mesaage
        sendVoiceMessage.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                //WHEN PRESS
                if(motionEvent.getAction() == MotionEvent.ACTION_DOWN){
                    startRecording();
                    Toast.makeText(ActivityChat.this,
                            getResources().getString(R.string.chat_toast_start_record)
                            ,Toast.LENGTH_LONG).show();
                }//WHEN RELEASE
                else if(motionEvent.getAction() == MotionEvent.ACTION_UP){
                    stopRecording();
                    Toast.makeText(ActivityChat.this,
                            getResources().getString(R.string.chat_toast_stop_record)
                            ,Toast.LENGTH_LONG).show();
                }

                return true;
            }
        });

        //for voice message with chipmunk filter.
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
                    Toast.makeText(ActivityChat.this,
                            getResources().getString(R.string.chat_toast_start_record)
                            ,Toast.LENGTH_LONG).show();
                }//WHEN RELEASE
                else if(motionEvent.getAction() == MotionEvent.ACTION_UP){
                    recording = false;
                    Toast.makeText(ActivityChat.this,
                            getResources().getString(R.string.chat_toast_stop_record)
                            ,Toast.LENGTH_LONG).show();
                }

                return true;
            }
        });
    }

    /**
     * start recording normal voice message
     * start when button is push.
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
     * stop when button is released.
     *
     * Also this will put the voice message
     * to the database (Firebase database).
     */
    private void stopRecording() {
        mRecorder.stop();
        mRecorder.release();
        mRecorder = null;

        //Putting message to database.
        putMessageToFirebase(getResources().getString(R.string.default_link_audio_message),
                getResources().getString(R.string.database_message_type_audio));
    }

    /**
     * start the recording using audio recorder - voice message that going to be filter
     * this example have 15000 hz input ( the drop down does not effect the input audio).
     */
    private void startFilterRecord(){

        File file = new File(Environment.getExternalStorageDirectory(), TEMP_FILTER_AUDIO_NAME);

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
            putMessageToFirebase(getResources().getString(R.string.default_link_audio_message),
                    getResources().getString(R.string.database_message_type_chipmunk_audio));

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    /**
     * Uploading audio file to Firebase storage, and also
     * get the download uri to put in firebase database
     * as a message with type audio/ audio filter (chipmunk only for now).
     *
     * @param messageUniqueId - unique id of the message.
     * @param thisOnlineUserId - user id who is sending the message.
     * @param receiverUserId - user id who is receiving the message.
     */
    private void uploadAudio(final String messageUniqueId, final String thisOnlineUserId, final String receiverUserId) {
        final StorageReference filepath = mStorage.child(getResources().getString(R.string.storage_audio)).child(
                messageUniqueId+ getResources().getString(R.string.default_audio_message_file_type));

        final Uri uri = Uri.fromFile(new File(mFileName).getAbsoluteFile());

        filepath.putFile(uri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                Toast.makeText(ActivityChat.this,
                        getResources().getString(R.string.chat_toast_audio),Toast.LENGTH_LONG)
                        .show();
                filepath.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                    public void onSuccess(Uri uri) {
                        Uri downloadUri = uri;
                        String downloadLinkAudio = downloadUri.toString();
                        rootReference
                                .child(getResources().getString(R.string.database_message))
                                .child(thisOnlineUserId)
                                .child(receiverUserId)
                                .child(messageUniqueId)
                                .child(getResources().getString(R.string.database_message_input))
                                .setValue(downloadLinkAudio);
                        rootReference
                                .child(getResources().getString(R.string.database_message))
                                .child(receiverUserId)
                                .child(thisOnlineUserId)
                                .child(messageUniqueId)
                                .child(getResources().getString(R.string.database_message_input))
                                .setValue(downloadLinkAudio);
                        chatAdapter.notifyDataSetChanged();
                    }
                });
            }
        });
    }

    /**
     *
     * Uploading filtered audio file to Firebase storage, and also
     * get the download uri to put in firebase database
     * as a message with type audio/ audio filter (chipmunk only for now).
     *
     * @param messageUniqueId - unique id of the message.
     * @param thisOnlineUserId - user id who is sending the message.
     * @param receiverUserId - user id who is receiving the message.
     */
    private void uploadFilteredAudio(final String messageUniqueId, final String thisOnlineUserId, final String receiverUserId) {
        final StorageReference filepath = mStorage.child("Audio").child(
                messageUniqueId+ getResources().getString(R.string.default_audio_message_file_type));

        File file = new File(Environment.getExternalStorageDirectory(), TEMP_FILTER_AUDIO_NAME);
        final Uri uri = Uri.fromFile(file);

        filepath.putFile(uri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                Toast.makeText(ActivityChat.this,
                        getResources().getString(R.string.chat_toast_audio),Toast.LENGTH_LONG)
                        .show();
                filepath.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                    public void onSuccess(Uri uri) {
                        Uri downloadUri = uri;
                        String downloadLinkAudio = downloadUri.toString();

                        rootReference
                                .child(getResources().getString(R.string.database_message))
                                .child(thisOnlineUserId)
                                .child(receiverUserId)
                                .child(messageUniqueId)
                                .child(getResources().getString(R.string.database_message_input))
                                .setValue(downloadLinkAudio);
                        rootReference
                                .child(getResources().getString(R.string.database_message))
                                .child(receiverUserId)
                                .child(thisOnlineUserId)
                                .child(messageUniqueId)
                                .child(getResources().getString(R.string.database_message_input))
                                .setValue(downloadLinkAudio);
                    }
                });
            }
        });
    }

    /**
     * Send message with normal text.
     *
     * @param messageText - the message that  are going to be send.
     */
    private void sendMessage(String messageText) {
        if(messageText.isEmpty()){
            Toast.makeText(ActivityChat.this,
                    getResources().getString(R.string.chat_activity_err_empty_message),
                    Toast.LENGTH_LONG).show();
        }else if(messageText.length() == MAX_CHARS_MESSAGE){
            Toast.makeText(ActivityChat.this,
                    getResources().getString(R.string.chat_err_message_too_long),
                    Toast.LENGTH_LONG).show();
        }
        else{
            putMessageToFirebase(messageText,
                    getResources().getString(R.string.database_message_type_text));
        }
    }

    /**
     * Put message to Firebase
     * can handle type text and audio
     * @param messageText - string of the message.
     * @param typeMessage - string of the file name audio in Firebase storage.
     */
    private void putMessageToFirebase(String messageText,String typeMessage) {
        String messageSenderPath =
                getResources().getString(R.string.database_message) + "/" +
                        thisOnlineUserId + "/" +receiverUserId;
        String messageReceiverPath =
                getResources().getString(R.string.database_message) + "/" +
                        receiverUserId + "/" + thisOnlineUserId;

        DatabaseReference messageKeyReference = rootReference
                .child(getResources().getString(R.string.database_message))
                .child(thisOnlineUserId)
                .child(receiverUserId)
                .push();

        String messageUniqueId = messageKeyReference.getKey();

        Map messageDetailHashmap = new HashMap();

        messageDetailHashmap.put(getResources().getString(R.string.database_message_read_status),false);
        messageDetailHashmap.put(getResources().getString(R.string.database_message_time),
                ServerValue.TIMESTAMP);

        messageDetailHashmap.put(getResources().getString(R.string.database_message_from),
                thisOnlineUserId);

        if(typeMessage.equalsIgnoreCase(
                getResources().getString(R.string.database_message_type_text)))
        {
            messageDetailHashmap.put(getResources().getString(R.string.database_message_input),messageText);
            messageDetailHashmap.put(getResources().getString(R.string.database_message_type),
                    getResources().getString(R.string.database_message_type_text));
        }else if(typeMessage.equalsIgnoreCase(
                getResources().getString(R.string.database_message_type_chipmunk_audio)))
        {
            uploadFilteredAudio(messageUniqueId,thisOnlineUserId,receiverUserId);
            messageDetailHashmap.put(getResources().getString(R.string.database_message_type),
                    getResources().getString(R.string.database_message_type_chipmunk_audio));
            messageDetailHashmap.put(getResources().getString(R.string.database_message_input),
                    "link file");

        }else
        {
        uploadAudio(messageUniqueId,thisOnlineUserId,receiverUserId);
        messageDetailHashmap.put(getResources().getString(R.string.database_message_type),
                getResources().getString(R.string.database_message_type_audio));
        messageDetailHashmap.put(getResources().getString(R.string.database_message_input),
                "link file");
        }

        Map messageHashmap = new HashMap();

        messageHashmap.put(messageSenderPath + "/" + messageUniqueId, messageDetailHashmap);
        messageHashmap.put(messageReceiverPath + "/" + messageUniqueId, messageDetailHashmap);

        rootReference.updateChildren(messageHashmap, new DatabaseReference.CompletionListener() {
            @Override
            public void onComplete(@Nullable DatabaseError databaseError, @NonNull DatabaseReference databaseReference) {
                if(databaseError != null){
                    Log.e(LOG_TAG,databaseError.getMessage());
                }
                inputMessage.setText(null);
            }
        });
    }


    /**
     * fetching data from firebase to array of message that we use
     * to display the messages in a chat.
     *
     * Bug: acting un-normal sometimes.
     */
    private void fetchMessagesFromFirebaseDatabase() {
        rootReference
                .child(getResources().getString(R.string.database_message))
                .child(thisOnlineUserId)
                .child(receiverUserId)
                .orderByChild(getResources().getString(R.string.database_message_time))
                .addChildEventListener(new ChildEventListener() {
                    @Override
                    public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                        Log.d(LOG_TAG,"onChildAdded");
                        Log.d(LOG_TAG,"onChildChanged before adding: " + messageList);
                        Message dataMessage = dataSnapshot.getValue(Message.class);
                        Log.d(LOG_TAG,"onChildChanged after adding: " + messageList);
                        messageList.add(dataMessage);
                        chatAdapter.notifyDataSetChanged();

                    }

                    @Override
                    public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                        Message dataMessage = dataSnapshot.getValue(Message.class);
                        messageList.set(messageList.size()-1,dataMessage);
                        Log.d(LOG_TAG,"onChildChanged" + dataSnapshot);
                        Log.d(LOG_TAG,"onChildChanged arraylist: " + messageList);
                        chatAdapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {
                        Log.d(LOG_TAG,"onChildRemoved" + dataSnapshot);
                        Log.d(LOG_TAG,"onChildRemoved arraylist: " + messageList);
                        chatAdapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                        Log.d(LOG_TAG,"onChildMoved " + dataSnapshot + s);
                        Log.d(LOG_TAG,"onChildMoved arraylist: " + messageList);
                        chatAdapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
    }

    /**
     * Starting recycler view and connect it to
     * the message adapter.
     */
    private void setRecyclerView() {
        // use a linear layout manager
        chatLinearLayout = new LinearLayoutManager(this);
        chatsView.setLayoutManager(chatLinearLayout);

        // specify an adapter
        chatAdapter = new MessageAdapter(messageList);
        chatsView.setAdapter(chatAdapter);
        chatsView.setItemAnimator(new DefaultItemAnimator());
    }
}
