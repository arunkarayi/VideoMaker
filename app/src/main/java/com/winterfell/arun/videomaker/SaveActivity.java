package com.winterfell.arun.videomaker;

import android.content.res.AssetFileDescriptor;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.MediaController;
import android.widget.Toast;
import android.widget.VideoView;

import com.github.hiteshsondhi88.libffmpeg.FFmpeg;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

import cafe.adriel.androidaudioconverter.AndroidAudioConverter;
import cafe.adriel.androidaudioconverter.callback.IConvertCallback;
import cafe.adriel.androidaudioconverter.model.AudioFormat;

public class SaveActivity extends AppCompatActivity {

    private String TAG = "SaveActivity";
    private String recordedFile;
//    private String audioFile;
    private String aacFile;
    private String outputFile;

    private VideoView videoView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_save);

        recordedFile = getIntent().getStringExtra("outputfile");
        aacFile = getIntent().getStringExtra("aacfile");
//        audioFile = getIntent().getStringExtra("audio");

        videoView = findViewById(R.id.videoview);

//        convertMp3toAac();
        muxing();
    }

//    private void convertMp3toAac() {
//        IConvertCallback iConvertCallback = new IConvertCallback() {
//            @Override
//            public void onSuccess(File file) {
//                Toast.makeText(SaveActivity.this,file.getAbsolutePath(),Toast.LENGTH_SHORT);
//                Log.d(TAG, "onSuccess: "+file.getAbsolutePath());
//                aacFile = file;
////                muxing();
//            }
//
//            @Override
//            public void onFailure(Exception e) {
//                e.printStackTrace();
//            }
//        };
//
//        AndroidAudioConverter.with(SaveActivity.this)
//                .setFile(new File(audioFile))
//                .setFormat(AudioFormat.AAC)
//                .setCallback(iConvertCallback)
//                .convert();
//    }

    private void muxing() {

        try {

            File file = new File(Environment.getExternalStorageDirectory()+File.separator+"VideoMaker" + File.separator +"outputFile.mp4");
            file.createNewFile();
            outputFile = file.getAbsolutePath();
            Log.d(TAG, "muxing: "+outputFile);

            MediaExtractor videoExtractor = new MediaExtractor();
            videoExtractor.setDataSource(recordedFile);

            MediaExtractor audioExtractor = new MediaExtractor();
            audioExtractor.setDataSource(aacFile);

            Log.d(TAG, "Video Extractor Track Count " + videoExtractor.getTrackCount());
            Log.d(TAG, "Audio Extractor Track Count " + audioExtractor.getTrackCount());

            MediaMuxer muxer = new MediaMuxer(outputFile, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
            muxer.setOrientationHint(90);

            videoExtractor.selectTrack(0);
            MediaFormat videoFormat = videoExtractor.getTrackFormat(0);
            int videoTrack = muxer.addTrack(videoFormat);

            audioExtractor.selectTrack(0);
            MediaFormat audioFormat = audioExtractor.getTrackFormat(0);
            int audioTrack = muxer.addTrack(audioFormat);

            Log.d(TAG, "Video Format " + videoFormat.toString());
            Log.d(TAG, "Audio Format " + audioFormat.toString());

            boolean sawEOS = false;
            int frameCount = 0;
            int offset = 100;
            int sampleSize = 256 * 1024;
            ByteBuffer videoBuf = ByteBuffer.allocate(sampleSize);
            ByteBuffer audioBuf = ByteBuffer.allocate(sampleSize);
            MediaCodec.BufferInfo videoBufferInfo = new MediaCodec.BufferInfo();
            MediaCodec.BufferInfo audioBufferInfo = new MediaCodec.BufferInfo();


            videoExtractor.seekTo(0, MediaExtractor.SEEK_TO_CLOSEST_SYNC);
            audioExtractor.seekTo(0, MediaExtractor.SEEK_TO_CLOSEST_SYNC);

            muxer.start();

            while (!sawEOS) {
                videoBufferInfo.offset = offset;
                videoBufferInfo.size = videoExtractor.readSampleData(videoBuf, offset);


                if (videoBufferInfo.size < 0 || audioBufferInfo.size < 0) {
                    Log.d(TAG, "saw input EOS.");
                    sawEOS = true;
                    videoBufferInfo.size = 0;

                } else {
                    videoBufferInfo.presentationTimeUs = videoExtractor.getSampleTime();
                    videoBufferInfo.flags = videoExtractor.getSampleFlags();
                    muxer.writeSampleData(videoTrack, videoBuf, videoBufferInfo);
                    videoExtractor.advance();


                    frameCount++;
                    Log.d(TAG, "Frame (" + frameCount + ") Video PresentationTimeUs:" + videoBufferInfo.presentationTimeUs + " Flags:" + videoBufferInfo.flags + " Size(KB) " + videoBufferInfo.size / 1024);
                    Log.d(TAG, "Frame (" + frameCount + ") Audio PresentationTimeUs:" + audioBufferInfo.presentationTimeUs + " Flags:" + audioBufferInfo.flags + " Size(KB) " + audioBufferInfo.size / 1024);

                }
            }

//            Toast.makeText(getApplicationContext(), "saweos frame:" + frameCount, Toast.LENGTH_SHORT).show();


            boolean sawEOS2 = false;
            int frameCount2 = 0;
            while (!sawEOS2) {
                frameCount2++;

                audioBufferInfo.offset = offset;
                audioBufferInfo.size = audioExtractor.readSampleData(audioBuf, offset);

                Log.d(TAG, "muxing: "+frameCount+" - "+frameCount2);

                if (frameCount2 > frameCount+10) {
                    Log.d(TAG, "saw input EOS.");
                    sawEOS2 = true;
                    audioBufferInfo.size = 0;
                } else {
                    audioBufferInfo.presentationTimeUs = audioExtractor.getSampleTime();
                    audioBufferInfo.flags = audioExtractor.getSampleFlags();
                    muxer.writeSampleData(audioTrack, audioBuf, audioBufferInfo);
                    audioExtractor.advance();


                    Log.d(TAG, "Frame (" + frameCount + ") Video PresentationTimeUs:" + videoBufferInfo.presentationTimeUs + " Flags:" + videoBufferInfo.flags + " Size(KB) " + videoBufferInfo.size / 1024);
                    Log.d(TAG, "Frame (" + frameCount + ") Audio PresentationTimeUs:" + audioBufferInfo.presentationTimeUs + " Flags:" + audioBufferInfo.flags + " Size(KB) " + audioBufferInfo.size / 1024);

                }
            }

//            Toast.makeText(getApplicationContext(), "saweos2 frame:" + frameCount2, Toast.LENGTH_SHORT).show();

            muxer.stop();
            muxer.release();
            videoPlayback();
//            new File(recordedFile).delete();
//            new File(aacFile).delete(); // delete temporary aac file created

        } catch (IOException e) {
            Log.d(TAG, "Mixer Error 1 " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            Log.d(TAG, "Mixer Error 2 " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void videoPlayback() {
        videoView.setVideoPath(outputFile);
        videoView.requestFocus();
        videoView.start();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }
}
