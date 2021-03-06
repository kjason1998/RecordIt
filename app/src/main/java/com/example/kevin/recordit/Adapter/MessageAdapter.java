package com.example.kevin.recordit.Adapter;


import android.app.Activity;
import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import com.example.kevin.recordit.Model.Message;
import com.example.kevin.recordit.R;
import com.example.kevin.recordit.ViewHolder.MessageHolder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;
import java.util.concurrent.ExecutionException;

import de.hdodenhof.circleimageview.CircleImageView;

public class MessageAdapter extends RecyclerView.Adapter<MessageHolder> implements MediaPlayer.OnPreparedListener{

    private static String tempFilterAudioName = "tempAudio.3gp";

    private List<Message> messageList;
    private FirebaseAuth mAuth;
    private DatabaseReference mRoot;
    private MediaPlayer mPlayer = null;
    private Context context = null;

    //constructor
    public MessageAdapter(List<Message> messageList) {
        this.messageList = messageList;
    }

    // Create new views (invoked by the layout manager)
    @Override
    public MessageHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        context = parent.getContext();

        mAuth = FirebaseAuth.getInstance();
        mRoot = FirebaseDatabase.getInstance().getReference()
                .child(context.getResources().getString(R.string.database_user));

        //new view
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.message_text_view, parent, false);

        return new MessageHolder(view);
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(final MessageHolder holder, int position) {
        final Message message = messageList.get(position);

        String fromUserId = message.getFromUserId();
        String messageType = message.getType();

        if(fromUserId.equalsIgnoreCase(mAuth.getCurrentUser().getUid())){
            holder.receivingLayout.setVisibility(View.GONE);
            holder.messageTextViewSending.setText(message.getMessage());
            setProfilePictureChat(mRoot.child(fromUserId),holder.profileImageCircleViewSending);
            if(messageType.equalsIgnoreCase(
                    context.getResources().getString(R.string.database_message_type_audio)))
            {//MSG TYPE = audio

                holder.messageTextViewSending
                        .setText(context.getResources().getString(R.string.chat_audio_chat_view_message));
                holder.messageTextViewSending.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mPlayer = new MediaPlayer();
                        mPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                        fetchAudioUrlFromFirebase(message.getMessage());
                    }
                });
            }
            else if(messageType.equalsIgnoreCase(
                    context.getResources().getString(R.string.database_message_type_chipmunk_audio)))
            {//MSG TYPE = chipmunk audio
                holder.messageTextViewSending
                        .setText(context.getResources().getString(R.string.chat_audio_chat_view_message));
                holder.messageTextViewSending.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        try {
                            fetchFilteredAudioFileFromFirebase(message.getMessage());
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        } catch (ExecutionException e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
            else{//MSG TYPE = text

            }

        }
        else{
            holder.sendingLayout.setVisibility(View.GONE);
            holder.messageTextViewReceiving.setText(message.getMessage());
            setProfilePictureChat(mRoot.child(fromUserId),holder.profileImageCircleViewReceiving);
            if(messageType.equalsIgnoreCase(
                    context.getResources().getString(R.string.database_message_type_audio)))
            {//MSG TYPE = audio
                holder.messageTextViewReceiving
                        .setText(context.getResources().getString(R.string.chat_audio_chat_view_message));
                holder.messageTextViewReceiving.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mPlayer = new MediaPlayer();
                        mPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                        fetchAudioUrlFromFirebase(message.getMessage());
                    }
                });
            }
            else if(messageType.equalsIgnoreCase(
                    context.getResources().getString(R.string.database_message_type_chipmunk_audio)))
            {//MSG TYPE = chipmunk audio
                holder.messageTextViewReceiving
                        .setText(context.getResources().getString(R.string.chat_audio_chat_view_message));
                holder.messageTextViewReceiving.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        try {
                            fetchFilteredAudioFileFromFirebase(message.getMessage());
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        } catch (ExecutionException e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
            else
                {//MSG TYPE = text
                //do something to text if needed
            }
        }
    }


    private void setProfilePictureChat(DatabaseReference chatUser,
                                       final CircleImageView profileImageHolder) {
        chatUser.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                String profileImageUri = dataSnapshot
                        .child(context.getResources().getString(R.string.database_user_thumb_image))
                        .getValue().toString();
                //load the profile picture, if not set yet use default pict stored in the app.
                Picasso.get().load(profileImageUri)
                        .placeholder(R.drawable.blue_profile_picture)
                        .into(profileImageHolder);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        int size;
        if(messageList.isEmpty()){
            size = 0;
        }else{
            size = messageList.size();
        }
        return size;
    }

    private void fetchAudioUrlFromFirebase(final String url) {
        try {
            mPlayer.setDataSource(url);
            // wait for media player to get prepare
            mPlayer.setOnPreparedListener(this);
            mPlayer.prepareAsync();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void fetchFilteredAudioFileFromFirebase(final String url) throws ExecutionException, InterruptedException {
        new DownloadFileFromURL().execute(url);

        File localFile = new File( Environment
                .getExternalStorageDirectory() + "/"
                + tempFilterAudioName);

        //waiting to get the audio file, so that we know that not the OLD audio that
        //are going to be played but the new one that are just been downloaded
        String str_result= new DownloadFileFromURL().execute().get();
        playFilteredAudio(localFile);
    }

    private void playFilteredAudio(File file) {
        int shortSizeInBytes = Short.SIZE/Byte.SIZE;

        int bufferSizeInBytes = (int)(file.length()/shortSizeInBytes);
        short[] audioData = new short[bufferSizeInBytes];

        try {
            InputStream inputStream = new FileInputStream(file);
            BufferedInputStream bufferedInputStream = new BufferedInputStream(inputStream);
            DataInputStream dataInputStream = new DataInputStream(bufferedInputStream);

            int i = 0;
            while(dataInputStream.available() > 0){
                audioData[i] = dataInputStream.readShort();
                i++;
            }

            dataInputStream.close();

            AudioTrack audioTrack = new AudioTrack(
                    AudioManager.STREAM_MUSIC,
                    30000,
                    AudioFormat.CHANNEL_CONFIGURATION_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    bufferSizeInBytes,
                    AudioTrack.MODE_STREAM);

            audioTrack.play();
            audioTrack.write(audioData, 0, bufferSizeInBytes);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    class DownloadFileFromURL extends AsyncTask<String, String, String> {
        @Override
        protected String doInBackground(String... f_url) {
            int count;
            try {
                URL url = new URL(f_url[0]);
                URLConnection conection = url.openConnection();
                conection.connect();

                // this will be useful so that you can show a tipical 0-100%
                // progress bar
                int lenghtOfFile = conection.getContentLength();

                // download the file
                InputStream input = new BufferedInputStream(url.openStream(),
                        8192);

                // Output stream
                OutputStream output = new FileOutputStream(Environment
                        .getExternalStorageDirectory() + "/"
                        + tempFilterAudioName);

                byte data[] = new byte[1024];

                long total = 0;

                while ((count = input.read(data)) != -1) {
                    total += count;
                    // publishing the progress....
                    // After this onProgressUpdate will be called
                    publishProgress("" + (int) ((total * 100) / lenghtOfFile));

                    // writing data to file
                    output.write(data, 0, count);
                }

                // flushing output
                output.flush();

                // closing streams
                output.close();
                input.close();

            } catch (Exception e) {
                Log.e("Download Error: ", e.getMessage());
            }

            return null;
        }
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        mp.start();
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
