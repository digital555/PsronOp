package com.dji.FPVDemo;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.SurfaceTexture;
import android.os.Bundle;
import android.os.Handler;
import android.os.StrictMode;
import android.util.Log;
import android.view.TextureView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.TextureView.SurfaceTextureListener;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;
import android.widget.EditText;

import java.io.ByteArrayOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Semaphore;

import dji.common.camera.SettingsDefinitions;
import dji.common.camera.SystemState;
import dji.common.error.DJIError;
import dji.common.flightcontroller.LocationCoordinate3D;
import dji.common.product.Model;
import dji.common.util.CommonCallbacks;
import dji.sdk.airlink.LightbridgeLink;
import dji.sdk.base.BaseProduct;
import dji.sdk.camera.Camera;
import dji.sdk.camera.VideoFeeder;
import dji.sdk.codec.DJICodecManager;

import static java.lang.System.out;

public class MainActivity extends Activity implements SurfaceTextureListener,OnClickListener{

    private static final String TAG = MainActivity.class.getName();
    protected VideoFeeder.VideoDataCallback mReceivedVideoDataCallBack = null;

    // Codec for video live view
    protected DJICodecManager mCodecManager = null;

    protected TextureView mVideoSurface = null;
    private Button mMapaBtn, mLbVideo, mVideoMcdBtn, mIpBtn;
    private ToggleButton mModeBtn, onoffBtn;
    private TextView recordingTime;
    private ImageView mImageViewMCD;
    private ImageView mImageViewLay;
    private WebView myWebView;
    private ImageView mDogPose;

    private EditText editTextip;

    private Handler handler;

    public DatagramSocket mDataGramSocketReceive1;
    public DatagramSocket mDataGramSocketReceive2;
    public DatagramSocket mDataGramSocketSend;
    InetAddress addr;

    private final int SERVER_PORT = 1234; //Define the server port
    static CTrollSocket trollSocket = new CTrollSocket();
    static Semaphore sema = new Semaphore(1);
    public byte[] dataMichal= new byte[10];

    boolean flag1 = false;
    boolean flag2 = false;

    public Bitmap bmpFromMcd;
    public Bitmap bmpToMcd;
    public byte[] frameFrom = new byte[60000];
    public byte[] frameToMcd = new byte[60000];

    String[] DogGPS = new String[10];
    String[] DogIMU = new String[10];

    private LightbridgeLink link;
    private Thread Thread1;

    private LocationCoordinate3D Coords;

    //static CTrollBTsimple TrollBT = new CTrollBTsimple();


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (android.os.Build.VERSION.SDK_INT > 9) {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }

        handler = new Handler();

        try {
            mDataGramSocketReceive1 = new DatagramSocket(11004);
            mDataGramSocketReceive1.setReuseAddress(true);
            mDataGramSocketReceive1.setSoTimeout(1500);
            mDataGramSocketReceive2 = new DatagramSocket(11005);
            mDataGramSocketReceive2.setReuseAddress(true);
            mDataGramSocketReceive2.setSoTimeout(1500);
            mDataGramSocketSend = new DatagramSocket();
        } catch (SocketException e) {
            showToast(e.toString() + " " + "sockets");
        }


        initUI();

        trollSocket.Create(SERVER_PORT);
        showToast(trollSocket.dataString);

        myWebView = (WebView) findViewById(R.id.webWiebView);
        WebSettings webSettings = myWebView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        myWebView.setWebViewClient(new WebViewClient());
        myWebView.loadUrl("http://cyberdog.herokuapp.com/operation_map");

        // The callback for receiving the raw H264 video data for camera live view
        mReceivedVideoDataCallBack = new VideoFeeder.VideoDataCallback() {

            @Override
            public void onReceive(byte[] videoBuffer, int size) {
                if (mCodecManager != null) {
                    mCodecManager.sendDataToDecoder(videoBuffer, size);
                }
            }
        };

        Camera camera = FPVDemoApplication.getCameraInstance();

        if (camera != null) {

            camera.setSystemStateCallback(new SystemState.Callback() {
                @Override
                public void onUpdate(SystemState cameraSystemState) {
                    if (null != cameraSystemState) {

                        int recordTime = cameraSystemState.getCurrentVideoRecordingTimeInSeconds();
                        int minutes = (recordTime % 3600) / 60;
                        int seconds = recordTime % 60;

                        final String timeString = String.format("%02d:%02d", minutes, seconds);
                        final boolean isVideoRecording = cameraSystemState.isRecording();

                        MainActivity.this.runOnUiThread(new Runnable() {

                            @Override
                            public void run() {

                                recordingTime.setText(timeString);

                                /*
                                 * Update recordingTime TextView visibility and mRecordBtn's check state
                                 */
                                if (isVideoRecording){
                                    recordingTime.setVisibility(View.VISIBLE);
                                }else
                                {
                                    recordingTime.setVisibility(View.INVISIBLE);
                                }
                            }
                        });
                    }
                }
            });

        }

        link = new LightbridgeLink() {
            @Override
            public boolean isSecondaryVideoOutputSupported() {
                return true;
            }
        };

    }

    protected void onProductChange() {
        initPreviewer();
    }


    public void receive() {

        try {
            if (flag1) {
                        //String text;
                        DatagramPacket p = new DatagramPacket(frameFrom, frameFrom.length);
                        try {
                            //while (true) {  // && counter < 100 TODO
                            // send to server omitted
                            try {                                
                                if (flag2){
                                    mDataGramSocketReceive1.receive(p);
                                } else {
                                    mDataGramSocketReceive2.receive(p);
                                }
                                /*bmpFromMcd = BitmapFactory.decodeByteArray(frameFrom, 0, frameFrom.length);
                                mImageViewMCD.setImageBitmap(bmpFromMcd);*/
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        mImageViewMCD.setImageBitmap(BitmapFactory.decodeByteArray(frameFrom, 0, frameFrom.length));
                                    }
                                });
                                //text = new String(message, 0, p.getLength());
                                // If you're not using an infinite loop:
                                //mDataGramSocketReceive1.close();
                                //showToast(text);
                            } catch (SocketTimeoutException | NullPointerException e) {
                                // no response received after 1 second. continue sending
                                showToast(e.toString() + " " + "recieve3");
                                //}
                            }
                        } catch (Exception e) {
                            showToast(e.toString() + " " + "recieve2");
                            // return "error:" + e.getMessage();
                            //mReceiveTask.publish("error:" + e.getMessage());
                        }
                        // return "out";
            }
        } catch (Exception e){
            showToast(e.toString() + " " + "recieve1");
        }
    }



    public void casVideoSurface(){
        bmpToMcd = Bitmap.createScaledBitmap(mVideoSurface.getBitmap(), 960,
                540, false) ;

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        bmpToMcd.compress(Bitmap.CompressFormat.JPEG, 10, outputStream);
        frameToMcd = outputStream.toByteArray();
        try {
            addr = InetAddress.getByName(editTextip.getText().toString());
            DatagramPacket p = new DatagramPacket(frameToMcd, frameToMcd.length,addr,11000);
            mDataGramSocketSend.send(p);
        } catch (Exception e){
            showToast(e.toString() + " " + "casVideoSurface");
        }


    }

    @Override
    public void onResume() {
        Log.e(TAG, "onResume");
        super.onResume();
        initPreviewer();
        onProductChange();

        if(mVideoSurface == null) {
            Log.e(TAG, "mVideoSurface is null");
        }
    }

    @Override
    public void onPause() {
        Log.e(TAG, "onPause");
        uninitPreviewer();
        super.onPause();
    }

    @Override
    public void onStop() {
        Log.e(TAG, "onStop");
        super.onStop();
    }

    public void onReturn(View view){
        Log.e(TAG, "onReturn");
        this.finish();
    }

    @Override
    protected void onDestroy() {
        Log.e(TAG, "onDestroy");
        uninitPreviewer();
        super.onDestroy();
    }

    private void initUI() {
        // init mVideoSurface
        mVideoSurface = (TextureView)findViewById(R.id.video_previewer_surface);

        recordingTime = (TextView) findViewById(R.id.timer);
        mMapaBtn = (Button) findViewById(R.id.btn_mapa);
        mModeBtn = (ToggleButton) findViewById(R.id.btn_mode);
        mLbVideo = (Button) findViewById(R.id.btn_lb_video);
        mVideoMcdBtn = (Button) findViewById(R.id.btn_video_mcd);
        mIpBtn = (Button) findViewById(R.id.btn_ip);
        mImageViewMCD = (ImageView) findViewById(R.id.imageViewMCD);
        mImageViewMCD.setImageResource(R.drawable.btn_draw_end);
        mImageViewLay = (ImageView) findViewById(R.id.imageViewLay);
        mDogPose = (ImageView) findViewById(R.id.dog_pose);
        mImageViewLay.setImageResource(R.drawable.blak_layer);
        editTextip = (EditText) findViewById(R.id.editTextIp);
        onoffBtn = (ToggleButton) findViewById(R.id.btn_onoff);



        if (null != mVideoSurface) {
            mVideoSurface.setSurfaceTextureListener(this);
        }

        mMapaBtn.setOnClickListener(this);
        mModeBtn.setOnClickListener(this);
        mLbVideo.setOnClickListener(this);
        mVideoMcdBtn.setOnClickListener(this);
        mIpBtn.setOnClickListener(this);
        onoffBtn.setOnClickListener(this);

        recordingTime.setVisibility(View.INVISIBLE);

        mModeBtn.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    //startRecord();
                    link.setBandwidthAllocationForHDMIVideoInputPort(1, null);
                    setmDogPose("IMU a b c 150");
                } else {
                    //stopRecord();
                    link.setBandwidthAllocationForHDMIVideoInputPort(0, null);
                    setmDogPose("IMU a b c 200");
                }
            }
        });

        onoffBtn.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    //startRecord();
                    flag2 = true;
                } else {
                    //stopRecord();
                    flag2 = false;
                }
            }
        });
    }

    private void initPreviewer() {

        BaseProduct product = FPVDemoApplication.getProductInstance();

        if (product == null || !product.isConnected()) {
            showToast(getString(R.string.disconnected));
        } else {
            if (null != mVideoSurface) {
                mVideoSurface.setSurfaceTextureListener(this);
            }
            if (!product.getModel().equals(Model.UNKNOWN_AIRCRAFT)) {
                if (VideoFeeder.getInstance().getVideoFeeds() != null
                        && VideoFeeder.getInstance().getVideoFeeds().size() > 0) {
                    VideoFeeder.getInstance().getVideoFeeds().get(0).setCallback(mReceivedVideoDataCallBack);
                }
            }
        }
    }

    private void uninitPreviewer() {
        Camera camera = FPVDemoApplication.getCameraInstance();
        if (camera != null){
            // Reset the callback
            VideoFeeder.getInstance().getVideoFeeds().get(0).setCallback(null);
        }
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        Log.e(TAG, "onSurfaceTextureAvailable");
        if (mCodecManager == null) {
            mCodecManager = new DJICodecManager(this, surface, width, height);
        }
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        Log.e(TAG, "onSurfaceTextureSizeChanged");
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        Log.e(TAG,"onSurfaceTextureDestroyed");
        if (mCodecManager != null) {
            mCodecManager.cleanSurface();
            mCodecManager = null;
        }

        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
    }

    public void showToast(final String msg) {
        runOnUiThread(new Runnable() {
            public void run() {
                Toast.makeText(MainActivity.this, msg, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onClick(View v) {

        switch (v.getId()) {
            case R.id.btn_mapa:{
                //captureAction();
                myWebView.setVisibility(View.VISIBLE);
                mImageViewMCD.setVisibility(View.INVISIBLE);
                mImageViewLay.setVisibility(View.INVISIBLE);
                flag1 = false;
                break;
            }
            case R.id.btn_lb_video:{
                //switchCameraMode(SettingsDefinitions.CameraMode.SHOOT_PHOTO);
                //mVideoSurface.setVisibility(View.VISIBLE);
                mImageViewMCD.setVisibility(View.INVISIBLE);
                mImageViewLay.setVisibility(View.INVISIBLE);
                myWebView.setVisibility(View.INVISIBLE);
                flag1 = false;
                //link.setBandwidthAllocationForHDMIVideoInputPort(1, null);
                break;
            }
            case R.id.btn_video_mcd:{
                //switchCameraMode(SettingsDefinitions.CameraMode.RECORD_VIDEO);
                //mVideoSurface.setVisibility(View.INVISIBLE);
                mImageViewMCD.setVisibility(View.VISIBLE);
                mImageViewLay.setVisibility(View.VISIBLE);
                editTextip.setVisibility(View.INVISIBLE);
                myWebView.setVisibility(View.INVISIBLE);
                flag1 = true;
                //link.setBandwidthAllocationForHDMIVideoInputPort(0, null);
                try {
                    /*TrollBT.StartBluetooth("Serial");
                    Timer timer1 = new Timer();
                    MyTimerTask timer1_task = new MyTimerTask();
                    timer1.schedule(timer1_task, 2000, 2000);*/
                } catch (Exception e) {
                    showToast(e.toString() + " " + "timery");
                }

                Thread1 = new Thread(new MyRun());
                Thread1.start();

                //receive();
                break;
            }
            case R.id.btn_ip:{
                editTextip.setVisibility(View.VISIBLE);
            }
            default:
                break;
        }
    }

    private void switchCameraMode(SettingsDefinitions.CameraMode cameraMode){

        Camera camera = FPVDemoApplication.getCameraInstance();
        if (camera != null) {
            camera.setMode(cameraMode, new CommonCallbacks.CompletionCallback() {
                @Override
                public void onResult(DJIError error) {

                    if (error == null) {
                        showToast("Switch Camera Mode Succeeded");
                    } else {
                        showToast(error.getDescription());
                    }
                }
            });
        }
    }

    // Method for taking photo
    private void captureAction(){

        final Camera camera = FPVDemoApplication.getCameraInstance();
        if (camera != null) {

            SettingsDefinitions.ShootPhotoMode photoMode = SettingsDefinitions.ShootPhotoMode.SINGLE; // Set the camera capture mode as Single mode
            camera.setShootPhotoMode(photoMode, new CommonCallbacks.CompletionCallback(){
                @Override
                public void onResult(DJIError djiError) {
                    if (null == djiError) {
                        handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                camera.startShootPhoto(new CommonCallbacks.CompletionCallback() {
                                    @Override
                                    public void onResult(DJIError djiError) {
                                        if (djiError == null) {
                                            showToast("take photo: success");
                                        } else {
                                            showToast(djiError.getDescription());
                                        }
                                    }
                                });
                            }
                        }, 2000);
                    }
                }
            });
        }
    }

    // Method for starting recording
    private void startRecord(){

        final Camera camera = FPVDemoApplication.getCameraInstance();
        if (camera != null) {
            camera.startRecordVideo(new CommonCallbacks.CompletionCallback(){
                @Override
                public void onResult(DJIError djiError)
                {
                    if (djiError == null) {
                        showToast("Record video: success");
                    }else {
                        showToast(djiError.getDescription());
                    }
                }
            }); // Execute the startRecordVideo API
        }
    }

    // Method for stopping recording
    private void stopRecord(){

        Camera camera = FPVDemoApplication.getCameraInstance();
        if (camera != null) {
            camera.stopRecordVideo(new CommonCallbacks.CompletionCallback(){

                @Override
                public void onResult(DJIError djiError)
                {
                    if(djiError == null) {
                        showToast("Stop recording: success");
                    }else {
                        showToast(djiError.getDescription());
                    }
                }
            }); // Execute the stopRecordVideo API
        }

    }

    public void setmDogPose(String Dog ){

        DogIMU = Dog.split(" ");

        if (Dog.substring(0, 3).equals("GPS"))
        {
            showToast("GPSIF");
            // 3 i 4 to szerokosc i dlugosc
            DogGPS = Dog.split(" ");

            /*if (DogGPS.length > 4);
                SqlConn(DogGPS[3], DogGPS[4]);*/
        }

        if (Dog.substring(0, 3).equals("IMU"))
        {

            DogIMU = Dog.split(" ");

            if (DogIMU.length > 3)
            {
                int pieseueue = Integer.parseInt(DogIMU[4]);

                if (pieseueue > 185)
                {
                    mDogPose.setImageResource(R.drawable.siedzi);
                }
                else
                {
                    mDogPose.setImageResource(R.drawable.stoi);
                }
            }
            //Console.WriteLine(Dog);
        }
    }



    /*class MyTimerTask extends TimerTask
    {
        public void run()
        {
            showToast(TrollBT.mMessageBT);
        }
    }*/

    public class MyRun implements Runnable {

        @Override
        public void run() {
            while (true) {
                //if(flag2)
                casVideoSurface();

                if(flag1)
                receive();

                try {
                    //Thread.sleep(5);
                } catch (Exception e){

                }
            }
        }
    }
}
