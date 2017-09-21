package com.dji.FPVDemo;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.SurfaceTexture;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.StrictMode;
import android.support.annotation.FloatRange;
import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.Size;
import android.support.v4.app.ActivityCompat;
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
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Semaphore;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

import dji.common.camera.SettingsDefinitions;
import dji.common.camera.SystemState;
import dji.common.error.DJIError;
import dji.common.flightcontroller.ConnectionFailSafeBehavior;
import dji.common.flightcontroller.ControlGimbalBehavior;
import dji.common.flightcontroller.ControlMode;
import dji.common.flightcontroller.FlightControllerState;
import dji.common.flightcontroller.FlightOrientationMode;
import dji.common.flightcontroller.IOStateOnBoard;
import dji.common.flightcontroller.LocationCoordinate3D;
import dji.common.flightcontroller.PowerStateOnBoard;
import dji.common.flightcontroller.RCSwitchFlightMode;
import dji.common.flightcontroller.imu.IMUState;
import dji.common.flightcontroller.virtualstick.FlightControlData;
import dji.common.model.LocationCoordinate2D;
import dji.common.product.Model;
import dji.common.util.CommonCallbacks;
import dji.sdk.airlink.LightbridgeLink;
import dji.sdk.base.BaseProduct;
import dji.sdk.camera.Camera;
import dji.sdk.camera.VideoFeeder;
import dji.sdk.codec.DJICodecManager;
import dji.sdk.flightcontroller.FlightController;
import dji.sdk.products.Aircraft;
import dji.sdk.sdkmanager.DJISDKManager;


import static java.lang.System.out;

public class MainActivity extends Activity implements SurfaceTextureListener, OnClickListener {

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
    private WebView backgroudWebview;
    private ImageView mDogPose;

    private EditText editTextip;

    private Handler handler;

    public DatagramSocket mDataGramSocketReceiveVideo1;
    public DatagramSocket mDataGramSocketReceiveVideo2;
    public DatagramSocket mDataGramSocketReceiveIMUData;

    public DatagramSocket mDataGramSocketSendLbVideo;
    public DatagramSocket mDataGramSocketSendGPSData;
    public InetAddress addr;

    private OnMessageReceived mMessageListener = null;

    private BufferedReader mBufferIn;


    private final int SERVER_PORT = 1234; //Define the server port
    //static CTrollSocket trollSocket = new CTrollSocket();
    static Semaphore sema = new Semaphore(1);
    public byte[] dataMichal = new byte[10];

    boolean flag1 = false;
    boolean flag2 = false;

    public Bitmap bmpFromMcd;
    public Bitmap bmpToMcd;
    public byte[] frameFrom = new byte[60000];
    public byte[] frameToMcd = new byte[60000];
    public byte[] GPSBatch = new byte[60000];
    public byte[] IMUBatch = new byte[60000];

    int lbVideoPortSend = 11000;

    String[] DogGPS = new String[10];
    String[] DogIMU = new String[10];

    private LightbridgeLink link;

    private Thread Thread1;
    private Thread Thread2;
    private Thread Thread3;
    private Thread Thread4;
    private Thread Thread5;

    private FlightController mFlightController;

    private double droneLocationLat = 181, droneLocationLng = 181, droneLocationAtt = 181;
    private String strLat = "", strLng = "";

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
            mDataGramSocketReceiveVideo1 = new DatagramSocket(11004);
            mDataGramSocketReceiveVideo1.setReuseAddress(true);
            mDataGramSocketReceiveVideo1.setSoTimeout(1500);
            mDataGramSocketReceiveVideo2 = new DatagramSocket(11005);
            mDataGramSocketReceiveVideo2.setReuseAddress(true);
            mDataGramSocketReceiveVideo2.setSoTimeout(1500);
            mDataGramSocketReceiveIMUData = new DatagramSocket(11007);
            mDataGramSocketReceiveIMUData.setReuseAddress(true);
            mDataGramSocketReceiveIMUData.setSoTimeout(1500);
            mDataGramSocketSendLbVideo = new DatagramSocket();
            mDataGramSocketSendGPSData = new DatagramSocket();
        } catch (SocketException e) {
            showToast(e.toString() + " " + "sockets");
        }


        initUI();

        /*trollSocket.Create(SERVER_PORT);
        showToast(trollSocket.dataString);*/

        myWebView = (WebView) findViewById(R.id.webWiebView);
        backgroudWebview = (WebView) findViewById(R.id.BGview);
        WebSettings webSettings1 = myWebView.getSettings();
        WebSettings webSettings2 = backgroudWebview.getSettings();
        webSettings1.setJavaScriptEnabled(true);
        webSettings2.setJavaScriptEnabled(true);
        myWebView.setWebViewClient(new WebViewClient());
        backgroudWebview.setWebViewClient(new WebViewClient());
        myWebView.loadUrl("http://cyberdog.herokuapp.com/operation_map?is_menu=0");

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
                                if (isVideoRecording) {
                                    recordingTime.setVisibility(View.VISIBLE);
                                } else {
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

        LocationManager mLocationManager = (LocationManager)
                getSystemService(Context.LOCATION_SERVICE);
        LocationListener mLocationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                location.getLatitude();
                location.getLongitude();

                showToast(String.valueOf(location.getLatitude()) + " " + String.valueOf(location.getLongitude()));

                strLat = String.valueOf(location.getLatitude());
                strLng = String.valueOf(location.getLongitude());

                backgroudWebview.post(new Runnable() {
                    @Override
                    public void run() {
                        backgroudWebview.loadUrl("http://cyberdog.herokuapp.com/api/add_drone_trajectory?latitude="+strLat+"&longitude="+strLng+"&dog_id=21");
                    }
                });

                sendGPSData(location.getLatitude(), location.getLongitude());
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {
                showToast(provider + " StatusChanged");
            }

            @Override
            public void onProviderEnabled(String provider) {
                showToast(provider + " onProviderEnabled");
            }

            @Override
            public void onProviderDisabled(String provider) {
                showToast(provider + " onProviderDisabled");
            }
        };

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, mLocationListener);
    }


    private void initFlightController() {

        BaseProduct product = FPVDemoApplication.getProductInstance();
            if (product != null && product.isConnected()) {
                if (product instanceof Aircraft) {
                    mFlightController = ((Aircraft) product).getFlightController();
                }
            }
            if (mFlightController != null) {

                    mFlightController.setStateCallback(
                            new FlightControllerState.Callback() {
                                @Override
                                public void onUpdate(FlightControllerState djiFlightControllerCurrentState) {
                                    try {
                                    /*droneLocationLat = djiFlightControllerCurrentState.getAircraftLocation().getLatitude();
                                    droneLocationLng = djiFlightControllerCurrentState.getAircraftLocation().getLongitude();
                                    droneLocationAtt = djiFlightControllerCurrentState.getAircraftLocation().getAltitude();
                                    sendGPSData(droneLocationLat, droneLocationLng);*/


                                        showToast(String.valueOf(djiFlightControllerCurrentState.getAircraftLocation().getLatitude()) + " ; " +
                                                String.valueOf(djiFlightControllerCurrentState.getAircraftLocation().getLongitude()) + " ; " +
                                                String.valueOf(djiFlightControllerCurrentState.getAircraftLocation().getAltitude()));


                                    /*droneLocationLat = 0;
                                    droneLocationLng = 0;
                                    droneLocationAtt = 0;*/

                                    /*backgroudWebview.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            if(droneLocationLat > 0) {
                                                backgroudWebview.loadUrl("http://cyberdog.herokuapp.com/api/add_drone_trajectory?latitude=" + String.valueOf(droneLocationLat) + "&longitude=" +
                                                        String.valueOf(droneLocationLng) + "&drone_id=28");
                                            }
                                        }
                                    });*/


                                        //updateDroneLocation();
                                    } catch (Exception e) {
                                        showToast(e.toString());
                                    }
                                }
                            });
                }

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
                                if (!flag2){
                                    mDataGramSocketReceiveVideo1.receive(p);
                                } else {
                                    mDataGramSocketReceiveVideo2.receive(p);
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
                                //mDataGramSocketReceiveVideo1.close();
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
        //bmpToMcd = Bitmap.createScaledBitmap(mVideoSurface.getBitmap(), 960, 540, false) ;
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Bitmap.createScaledBitmap(mVideoSurface.getBitmap(), 480,
                270, false).compress(Bitmap.CompressFormat.JPEG, 10, outputStream);
        frameToMcd = outputStream.toByteArray();
        try {
            addr = InetAddress.getByName(editTextip.getText().toString());
            DatagramPacket p = new DatagramPacket(frameToMcd, frameToMcd.length,addr,lbVideoPortSend);
            mDataGramSocketSendLbVideo.send(p);
        } catch (Exception e){
            showToast(e.toString() + " " + "casVideoSurface");
        }
    }


    public void sendGPSData (double lat, double lng){

        GPSBatch = (lat + " " + lng).getBytes();

        try {
            addr = InetAddress.getByName(editTextip.getText().toString());
            DatagramPacket p = new DatagramPacket(GPSBatch, GPSBatch.length,addr,11001);
            mDataGramSocketSendGPSData.send(p);
        } catch (Exception e){
            showToast(e.toString() + " " + "sendGPSData");
        }
    }

    public void receiveIMUData(){
        try {
            DatagramPacket p = new DatagramPacket(IMUBatch, IMUBatch.length);
            mDataGramSocketReceiveIMUData.receive(p);
            setmDogPose(new String(IMUBatch, "US-ASCII"));
        } catch (Exception e){
            showToast(e.toString() + " " + "receiveIMUData");
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
        Thread1.stop();
        Thread2.stop();
        Thread3.stop();
        Thread4.stop();
        Thread5.stop();
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
                    //startRecord()
                    showToast(String.valueOf(link.isSecondaryVideoOutputSupported()));
                    link.setBandwidthAllocationForHDMIVideoInputPort(1, null);
                    setmDogPose("IMU a b c 150");
                } else {
                    //stopRecord();
                    showToast(String.valueOf(link.isSecondaryVideoOutputSupported()));
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
                    showToast("Video Feeder size: " + String.valueOf(VideoFeeder.getInstance().getVideoFeeds().size()));
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
                //initFlightController();
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
                Thread4 = new Thread(new casVideoSurfaceTCP());
                Thread4.start();
                Thread5 = new Thread(new droneLocSender());
                //Thread5.start();

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

                Thread1 = new Thread(new MyRun1());
                //Thread1.start();
                Thread2 = new Thread(new MyRun2());
                //Thread2.start();
                Thread3 = new Thread(new receiveTCP());
                //Thread3.start();

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

        showToast(Dog + "setmDogPose");

        if (Dog.substring(0, 3).equals("GPS"))
        {
            showToast("GPSIF");
            // 3 i 4 to szerokosc i dlugosc
            DogGPS = Dog.split("\t");

            /*if (DogGPS.length > 4);
                SqlConn(DogGPS[3], DogGPS[4]);*/
        }

        if (Dog.substring(0, 3).equals("IMU"))
        {
            DogIMU = Dog.split("\t");

            if (DogIMU.length > 2)
            {
                showToast(DogIMU[3]);
                int pieseueue = Integer.parseInt(DogIMU[3]);


                //showToast(DogIMU[0] + " " + DogIMU[1]);

                if (pieseueue > 40)
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

    public class MyRun1 implements Runnable {

        @Override
        public void run() {
            while (true) {

                try {
                    casVideoSurface();
                } catch (Exception e){
                    showToast(e.toString() + "try casVideoSurface");
                }

                try {
                    //initFlightController();

                } catch (Exception e){
                    showToast(e.toString() + " try initFlightController");
                }

                try {
                    Thread1.sleep(50);
                    //showToast("awake");
                } catch (Exception e){
                    showToast(e.toString() + "try sleep");
                }

            }
        }
    }

    public class MyRun2 implements Runnable {

        @Override
        public void run() {
            while (true) {
                try {
                    //receiveIMUData();
                    //receive();
                } catch (Exception e) {
                    showToast(e.toString() + " try receiveIMU");
                }

                try {
                    Thread.sleep(25);
                } catch (Exception e){

                }
            }
        }
    }

    public class receiveTCP implements Runnable {

        @Override
        public void run() {
            try {
                System.out.println("S: Connecting...");

                //create a server socket. A server socket waits for requests to come in over the network.
                //ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
                Socket socket1 = new Socket(editTextip.getText().toString(), SERVER_PORT);

                //create client socket... the method accept() listens for a connection to be made to this socket and accepts it.
                //Socket client = serverSocket.accept();
                System.out.println("S: Receiving...");

                try {

                    //sends the message to the client
                    //PrintStream out = new PrintStream(socket1.getOutputStream(), true);

                    //read the message received from client
                    BufferedReader in = new BufferedReader(new InputStreamReader(socket1.getInputStream()));

                    //in this while we wait to receive messages from client (it's an infinite loop)
                    //this while it's like a listener for messages
                    while (true) {
                        frameFrom = in.readLine().getBytes();
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                mImageViewMCD.setImageBitmap(BitmapFactory.decodeByteArray(frameFrom, 0, frameFrom.length));
                            }
                        });
                        showToast(String.valueOf("TCP message length " + frameFrom.length));
                    }

                } catch (Exception e) {
                    System.out.println("S: Error");
                    e.printStackTrace();
                } finally {
                    socket1.close();
                    System.out.println("S: Done.");
                }

            } catch (Exception e) {
                System.out.println("S: Error");
                e.printStackTrace();
            }
        }
    }

    public class casVideoSurfaceTCP implements Runnable {
        @Override
        public void run() {
            try {
                System.out.println("S: Connecting...");

                //create a server socket. A server socket waits for requests to come in over the network.
                //ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
                Socket socket2 = new Socket(editTextip.getText().toString(), 23233);

                //create client socket... the method accept() listens for a connection to be made to this socket and accepts it.
                //Socket client = serverSocket.accept();
                System.out.println("S: Receiving...");

                try {

                    //sends the message to the client
                    PrintStream out = new PrintStream(socket2.getOutputStream(), true);
                    OutputStream os = socket2.getOutputStream();

                    //read the message received from client
                    BufferedReader in = new BufferedReader(new InputStreamReader(socket2.getInputStream()));

                    //in this while we wait to receive messages from client (it's an infinite loop)
                    //this while it's like a listener for messages
                    byte[] buffer = new byte[4];
                    int byteCount;

                    ByteBuffer bb = ByteBuffer.allocate(4);
                    bb.order(ByteOrder.LITTLE_ENDIAN);
                    while (true) {

                        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                        Bitmap.createScaledBitmap(mVideoSurface.getBitmap(), 480,
                                270, false).compress(Bitmap.CompressFormat.JPEG, 10, outputStream);
                        frameToMcd = outputStream.toByteArray();
                        byteCount = frameToMcd.length;
                        buffer[0] = (byte) (byteCount >> 24);
                        buffer[1] = (byte) ( (byteCount << 8) >> 24);
                        buffer[2] = (byte) ( (byteCount << 16) >> 24);
                        buffer[3] = (byte) ( (byteCount << 24) >> 24);
                        os.write(buffer);
                        os.write(frameToMcd, 0, frameToMcd.length);
                        showToast(frameToMcd.length + " " + ByteBuffer.allocate(4).putInt(frameToMcd.length).array()[0] +
                                " " + ByteBuffer.allocate(4).putInt(frameToMcd.length).array()[1] +
                                " " + ByteBuffer.allocate(4).putInt(frameToMcd.length).array()[2] +
                                " " + ByteBuffer.allocate(4).putInt(frameToMcd.length).array()[3]);
                        os.flush();
                    }

                } catch (Exception e) {
                    System.out.println("S: Error");
                    e.printStackTrace();
                } finally {
                    socket2.close();
                    System.out.println("S: Done.");
                }

            } catch (Exception e) {
                System.out.println("S: Error");
                e.printStackTrace();
            }
        }
    }

    public class droneLocSender implements Runnable{
        @Override
        public void run() {
            while (true) {
                try {
                    initFlightController();
                    Thread.sleep(5000);
                } catch (Exception e) {
                    showToast(e.toString() + " droneLocSender");
                }
            }
        }
    }

    public interface OnMessageReceived {
        public void messageReceived(String message);
    }
}
