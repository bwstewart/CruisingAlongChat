package com.example.ben_s.cruisingalongchat;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.database.DataSetObserver;
import android.os.Bundle;

import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;
import java.util.Random;

public class MainActivity extends ListActivity {

    private static final String FIREBASE_URL = "https://project-2242616374814194626.firebaseio.com";

    private String mUsername;
    private String mChannel;
    private Firebase mFirebaseRef;
    private ValueEventListener mConnectedListener;
    private ChatListAdapter mChatListAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Make sure we have a mUsername
        setupUsername();
        setupChannel();

        setTitle("Chatting as " + mUsername);

        // Setup our Firebase mFirebaseRef
        mFirebaseRef = new Firebase(FIREBASE_URL).child(mChannel);


        // Setup our input methods. Enter key on the keyboard or pushing the send button
        EditText inputText = (EditText) findViewById(R.id.messageInput);
        inputText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if (actionId == EditorInfo.IME_NULL && keyEvent.getAction() == KeyEvent.ACTION_DOWN) {
                    sendMessage();
                }
                return true;
            }
        });

        findViewById(R.id.sendButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendMessage();
            }
        });

        findViewById(R.id.channelButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final String[] Channels = {"CruiseChat", "CruiseChat2", "CruiseChat3"};
                final AlertDialog.Builder alertBuilder = new AlertDialog.Builder(MainActivity.this);

                alertBuilder.setTitle("Select Channel");
                alertBuilder.setCancelable(true).setItems(Channels, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        changeChannel(Channels[which]);
                        dialog.dismiss();
                    }
                });

                AlertDialog dialogChangeUsername = alertBuilder.create();
                dialogChangeUsername.show();
            }
        });

        findViewById(R.id.menuButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                AlertDialog.Builder alertBuilder = new AlertDialog.Builder(MainActivity.this);
                final EditText txtUsername = new EditText(MainActivity.this);

                alertBuilder.setTitle("Set Username");
                alertBuilder.setMessage("Username:");
                alertBuilder.setView(txtUsername);
                alertBuilder.setPositiveButton("Ok", new DialogInterface.OnClickListener(){

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if(txtUsername.getText().length() > 3) {
                            changeUsername(txtUsername.getText().toString());
                        }
                    }
                });

                alertBuilder.setCancelable(true).setNegativeButton("Cancel", new DialogInterface.OnClickListener(){

                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                });

                AlertDialog dialogChangeUsername = alertBuilder.create();
                dialogChangeUsername.show();
            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        // Setup our view and list adapter. Ensure it scrolls to the bottom as data changes
        final ListView listView = getListView();
        // Tell our list adapter that we only want 50 messages at a time
        mChatListAdapter = new ChatListAdapter(mFirebaseRef.limit(50), this, R.layout.chat_message, mUsername);
        listView.setAdapter(mChatListAdapter);
        mChatListAdapter.registerDataSetObserver(new DataSetObserver() {
            @Override
            public void onChanged() {
                super.onChanged();
                listView.setSelection(mChatListAdapter.getCount() - 1);
            }
        });

        // Finally, a little indication of connection status
        mConnectedListener = mFirebaseRef.getRoot().child(".info/connected").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                boolean connected = (Boolean) dataSnapshot.getValue();
                if (connected) {
                    Toast.makeText(MainActivity.this, "Connected to Firebase", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(MainActivity.this, "Disconnected from Firebase", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {
                // No-op
            }
        });
    }

    @Override
    public void onStop() {
        super.onStop();
        mFirebaseRef.getRoot().child(".info/connected").removeEventListener(mConnectedListener);
        mChatListAdapter.cleanup();
    }

    private void setupUsername() {
        SharedPreferences prefs = getApplication().getSharedPreferences("ChatPrefs", 0);
        mUsername = prefs.getString("username", null);
        if (mUsername == null) {
            Random r = new Random();
            // Assign a random user name if we don't have one saved.
            mUsername = "Cruiser " + r.nextInt(100);
            prefs.edit().putString("username", mUsername).commit();
        }
    }

    private void setupChannel() {
        SharedPreferences prefs = getApplication().getSharedPreferences("ChatPrefs", 0);
        mChannel = prefs.getString("channel", null);
        if (mChannel == null) {
            mChannel = "CruiseChat";
            prefs.edit().putString("channel", mChannel).commit();
            TextView bannerText = (TextView) findViewById(R.id.textChannel);
            bannerText.setText(mChannel);
        }

    }
    public void changeUsername(String s) {
        SharedPreferences prefs = getApplication().getSharedPreferences("ChatPrefs", 0);
        mUsername = s;
        mChatListAdapter.setmUsername(mUsername);
        prefs.edit().putString("username", mUsername).commit();
    }

    public void changeChannel(String s) {
        SharedPreferences prefs = getApplication().getSharedPreferences("ChatPrefs", 0);
        mChannel = s;
        prefs.edit().putString("channel", mChannel).commit();
        mFirebaseRef = new Firebase(FIREBASE_URL).child(mChannel);
        TextView bannerText = (TextView) findViewById(R.id.textChannel);
        bannerText.setText(mChannel);
        onStart();
    }

    private void sendMessage() {
        EditText inputText = (EditText) findViewById(R.id.messageInput);
        String input = inputText.getText().toString();
        if (!input.equals("")) {
            // Create our 'model', a Chat object
            Chat chat = new Chat(input, mUsername);
            // Create a new, auto-generated child of that chat location, and save our chat data there
            mFirebaseRef.push().setValue(chat);
            inputText.setText("");
        }
    }
}