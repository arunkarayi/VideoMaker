package com.winterfell.arun.videomaker;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.CamcorderProfile;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;

public class RecordVideo extends AppCompatActivity{

    private int CAMERA_REQUEST_CODE = 23451;
    private static int NOT_REDORDING = 0;
    private static int RECORDING = 1;
    private static int RECORDING_PAUSED = 2;
    private int RECORDING_STATE = 0;

    private String TAG = "RecordActivity";
    String outputfile;
    String audioPath;
    private Uri uri;
    private TextureView cameraView;
    private Button record_btn;
    private Button save_btn;
    private CameraManager cameraManager;
    private MediaRecorder recorder;
    private String cameraId;
    private Size size;
    private CameraDevice cameraDevice;
    private HandlerThread handlerThread;
    private Handler backgroundHandler;
    private MediaRecorder mediaRecorder;
    CaptureRequest.Builder captureRequestBuilder;
    CameraCaptureSession previewSession;
    MediaPlayer mediaPlayer;
    CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            cameraDevice= camera;
            startCameraPreview();
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {

        }

        @Override
        public void onError(@NonNull CameraDevice camera, int i) {

        }
    };

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_record_video);

        recorder = new MediaRecorder();

        cameraView =  findViewById(R.id.cameraview);
        record_btn = findViewById(R.id.record_btn);
        save_btn = findViewById(R.id.save_btn);

        audioPath = getIntent().getStringExtra("audio");

//        uri = Uri.parse("android.resources://com.winterfell.arun.videomaker/raw/bgmusic.mp3");
//        int resID = getResources().getIdentifier("bgmusic","raw","com.winterfell.arun.videomaker");
//        mediaPlayer = MediaPlayer.create(this,resID);
        mediaPlayer = new MediaPlayer();
        try {
            mediaPlayer.setDataSource(audioPath);
            mediaPlayer.prepare();
        } catch (IOException e) {
            e.printStackTrace();
        }

        record_btn.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if (motionEvent.getAction() == MotionEvent.ACTION_DOWN){
                    if (RECORDING_STATE == RECORDING_PAUSED)
                        resumeRecording();
                    else if (RECORDING_STATE == NOT_REDORDING)
                        startRecording();
                }else if (motionEvent.getAction() == MotionEvent.ACTION_UP){
                    if (RECORDING_STATE == RECORDING)
                        pauseRecording();
//                    stopRecording();
                }
                return true;
            }
        });

        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mediaPlayer) {
                stopRecording();
            }
        });

        save_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                stopRecording();
            }
        });

    }

    private void setupCamera() {
        cameraManager = (CameraManager) getSystemService(CAMERA_SERVICE);
        try {
            String[] cameraIds = cameraManager.getCameraIdList();
            for (String cameraId: cameraIds) {
                CameraCharacteristics cameraCharacteristics = cameraManager.getCameraCharacteristics(cameraId);
                if (cameraCharacteristics.get(CameraCharacteristics.LENS_FACING) == CameraMetadata.LENS_FACING_BACK){
                    this.cameraId = cameraId;
                    StreamConfigurationMap streamConfigurationMap = cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                    size = streamConfigurationMap.getOutputSizes(SurfaceTexture.class)[0];
                }
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void openCamera(){
        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED ||
                checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
            requestPermissions(new String[]{Manifest.permission.CAMERA,Manifest.permission.WRITE_EXTERNAL_STORAGE}, CAMERA_REQUEST_CODE);
        }else {
            try {
                cameraManager.openCamera(cameraId,stateCallback,backgroundHandler);
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }
    }

    private void openBackgroundHandler(){
        handlerThread = new HandlerThread("camera");
        handlerThread.start();
        backgroundHandler = new Handler(handlerThread.getLooper());
    }

    private void closeBackgroundHandler(){
        handlerThread.quit();
        handlerThread = null;
        backgroundHandler = null;
    }

    private void startCameraPreview() {
        closePreviewSession();
        createCaptureSession();
    }

    private void closePreviewSession(){
        if (previewSession != null){
            previewSession.close();
            previewSession = null;
        }
    }

    private void createCaptureSession() {
        SurfaceTexture surfaceTexture = cameraView.getSurfaceTexture();
        surfaceTexture.setDefaultBufferSize(size.getWidth(), size.getHeight());
        final Surface surface= new Surface(surfaceTexture);

        try {
            cameraDevice.createCaptureSession(Collections.singletonList(surface), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                    try {
                        previewSession = cameraCaptureSession;
                        captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                        captureRequestBuilder.addTarget(surface);
                        previewSession.setRepeatingRequest(captureRequestBuilder.build(), null, backgroundHandler);

                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {

                }
            }, backgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void pauseRecording() {
        mediaPlayer.pause();
        RECORDING_STATE = RECORDING_PAUSED;
        try {
            mediaRecorder.pause();
        }catch (Exception e){
            startCameraPreview();
            e.printStackTrace();
        }
    }

    private void startRecording() {
        setMediaRecorder();
//        if (mediaPlayer != null) {
            mediaPlayer.start();
            RECORDING_STATE = RECORDING;
//        }
    }

    private void resumeRecording() {
        int length = mediaPlayer.getCurrentPosition();
        mediaPlayer.start();
        mediaPlayer.seekTo(length);
        RECORDING_STATE = RECORDING;
        mediaRecorder.resume();
    }

    private void stopRecording() {
        try {
            mediaRecorder.stop();
            mediaRecorder.reset();
//            startCameraPreview();
        }catch (Exception e){
            e.printStackTrace();
        }

        mediaPlayer.reset();

        Intent intent = new Intent(RecordVideo.this,SaveActivity.class);
        intent.putExtra("outputfile",outputfile);
        intent.putExtra("audio",audioPath);
        intent.putExtra("aacfile",getIntent().getStringExtra("aacfile"));
        startActivity(intent);
        finish();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        Log.d(TAG, "onRestart: ");
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "onStart: ");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume: ");
        cameraView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int i, int i1) {
                openBackgroundHandler();
                setupCamera();
                openCamera();
            }

            @Override
            public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int i, int i1) {

            }

            @Override
            public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
                return false;
            }

            @Override
            public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {

            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause: ");
        closeBackgroundHandler();
        closePreviewSession();

        if (cameraDevice != null){
            cameraDevice.close();
            cameraDevice = null;
        }

        if (mediaRecorder != null) {
            mediaRecorder.release();
            mediaRecorder = null;
        }

        mediaPlayer.pause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "onStop: ");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy: ");
    }

    private void setMediaRecorder(){
        mediaRecorder = new MediaRecorder();
        mediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        outputfile = getOutputFilePath();
        mediaRecorder.setOutputFile(outputfile);
        Log.d(TAG, "setMediaRecorder: "+outputfile);
        mediaRecorder.setVideoSize(size.getWidth(), size.getHeight());
        mediaRecorder.setVideoEncodingBitRate(5000000);
        mediaRecorder.setVideoFrameRate(30);
        mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
        mediaRecorder.setOrientationHint(90);
        try {
            mediaRecorder.prepare();
            mediaRecorder.start();

        SurfaceTexture surfaceTexture = cameraView.getSurfaceTexture();
        surfaceTexture.setDefaultBufferSize(size.getWidth(), size.getHeight());
        Surface previewSurface = new Surface(surfaceTexture);
        Surface recordSurface = mediaRecorder.getSurface();
        captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);
        captureRequestBuilder.addTarget(previewSurface);
        captureRequestBuilder.addTarget(recordSurface);
        cameraDevice.createCaptureSession(Arrays.asList(previewSurface, recordSurface), new CameraCaptureSession.StateCallback() {
            @Override
            public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                try {
                    cameraCaptureSession.setRepeatingRequest(captureRequestBuilder.build(),null,null);
                } catch (CameraAccessException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {

            }
        }, null);
        } catch (CameraAccessException | IOException e) {
            e.printStackTrace();
        }
    }

    private String getOutputFilePath(){
        File dir = Environment.getExternalStorageDirectory();
        File file = new File(dir,"VideoMaker");
        if (!file.exists()) {
            file.mkdir();
        }
        return file.getAbsolutePath()+"/"+System.currentTimeMillis()+".mp4";
    }
}
