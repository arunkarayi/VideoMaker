package com.winterfell.arun.videomaker;

import android.Manifest;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.ArrayList;

public class SongListActivity extends AppCompatActivity {

    int CAMERA_REQUEST_CODE = 21234;
    ArrayList<String> songList;
    ArrayList<String> songPathList;
    ArrayAdapter<String> stringArrayAdapter;

    ListView listview;

    private String TAG = "SongListActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_song_list);

        listview = findViewById(R.id.listview);
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkPermissions();
    }

    private void checkPermissions() {
        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED ||
                checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
            requestPermissions(new String[]{Manifest.permission.CAMERA,Manifest.permission.WRITE_EXTERNAL_STORAGE}, CAMERA_REQUEST_CODE);
        }else {
            createMusicList();
        }
    }

    private void createMusicList() {
        // 2 arraylist to save time writing an adapter
        songList = new ArrayList<>();
        songPathList = new ArrayList<>();
        getMusic();
        stringArrayAdapter = new ArrayAdapter<>(this,android.R.layout.simple_list_item_1,songList);
        listview.setAdapter(stringArrayAdapter);

        listview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Intent intent= new Intent(SongListActivity.this,RecordVideo.class);
                intent.putExtra("audio",songPathList.get(i));
                startActivity(intent);
            }
        });
    }

    private void getMusic() {
        ContentResolver contentResolver = getContentResolver();
        Uri songuri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        Cursor songCursor = contentResolver.query(songuri,null,null,null,null);
        if (songCursor != null && songCursor.moveToFirst()){
            int name = songCursor.getColumnIndex(MediaStore.Audio.Media.DISPLAY_NAME);
            int data = songCursor.getColumnIndex(MediaStore.Audio.Media.DATA);
            do {
                songList.add(songCursor.getString(name));
                songPathList.add(songCursor.getString(data));
                Log.d(TAG, "getMusic: "+songCursor.getString(data));
            }while (songCursor.moveToNext());
        }
    }
}
