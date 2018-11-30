package com.winterfell.arun.videomaker;

import android.media.MediaPlayer;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import java.io.IOException;

public class SaveActivity extends AppCompatActivity {

    private String TAG = "SaveActivity";
    private String recordedFile;
    private String audioFile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_save);

        recordedFile = getIntent().getStringExtra("outputfile");
        audioFile = getIntent().getStringExtra("audio");

        Log.d(TAG, "onCreate: "+recordedFile);
        muxing();
    }

    private void muxing(){

    }
}
