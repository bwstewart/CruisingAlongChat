package com.example.ben_s.cruisingalongchat;

import com.firebase.client.Firebase;

// Initialise Firebase before usage so app won't crash
public class ChatApplication extends android.app.Application {
    @Override
    public void onCreate() {
        super.onCreate();
        Firebase.setAndroidContext(this);
    }
}
