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
import android.widget.RelativeLayout;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;

import cafe.adriel.androidaudioconverter.AndroidAudioConverter;
import cafe.adriel.androidaudioconverter.callback.IConvertCallback;
import cafe.adriel.androidaudioconverter.model.AudioFormat;

public class SongListActivity extends AppCompatActivity {

    int CAMERA_REQUEST_CODE = 21234;
    ArrayList<String> songList;
    ArrayList<String> songPathList;
    ArrayAdapter<String> stringArrayAdapter;

    RelativeLayout progress_lyt;

    ListView listview;

    private String TAG = "SongListActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_song_list);

        listview = findViewById(R.id.listview);
        progress_lyt = findViewById(R.id.progress_lyt);
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

                listview.setVisibility(View.GONE);
                progress_lyt.setVisibility(View.VISIBLE);
                String aacfile = songPathList.get(i).replace(".mp3",".aac");
                if (new File(aacfile).exists()){
                    gotoCameraActivity(new File(aacfile), songPathList.get(i));
                }else {
                    convertMp3toAac(songPathList.get(i));
                }
            }
        });
    }
    private void convertMp3toAac(final String audioFile) {
        IConvertCallback iConvertCallback = new IConvertCallback() {
            @Override
            public void onSuccess(File file) {
                gotoCameraActivity(file, audioFile);
                Toast.makeText(SongListActivity.this,file.getAbsolutePath(),Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(Exception e) {
                e.printStackTrace();
            }
        };

        AndroidAudioConverter.with(SongListActivity.this)
                .setFile(new File(audioFile))
                .setFormat(AudioFormat.AAC)
                .setCallback(iConvertCallback)
                .convert();
    }

    private void gotoCameraActivity(File file, String audioFile) {
        Intent intent= new Intent(SongListActivity.this,RecordVideo.class);
        intent.putExtra("audio",audioFile);
        intent.putExtra("aacfile",file.getAbsolutePath());
        startActivity(intent);
        listview.setVisibility(View.VISIBLE);
        progress_lyt.setVisibility(View.GONE);
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
