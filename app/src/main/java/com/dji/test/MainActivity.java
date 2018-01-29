package com.dji.test;

import android.Manifest;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.amap.api.maps2d.AMap;
import com.amap.api.maps2d.AMap.OnMapClickListener;
import com.amap.api.maps2d.CameraUpdate;
import com.amap.api.maps2d.CameraUpdateFactory;
import com.amap.api.maps2d.MapView;
import com.amap.api.maps2d.model.BitmapDescriptorFactory;
import com.amap.api.maps2d.model.LatLng;
import com.amap.api.maps2d.model.Marker;
import com.amap.api.maps2d.model.MarkerOptions;

import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.location.AMapLocationListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import dji.common.camera.CameraParamRangeManager;
import dji.common.camera.SettingsDefinitions;
import dji.common.flightcontroller.FlightControllerState;
import dji.common.mission.hotpoint.HotpointHeading;
import dji.common.mission.hotpoint.HotpointMission;
import dji.common.mission.hotpoint.HotpointStartPoint;
import dji.common.model.LocationCoordinate2D;
import dji.common.useraccount.UserAccountState;
import dji.common.util.CommonCallbacks;
import dji.sdk.base.BaseProduct;
import dji.sdk.flightcontroller.FlightController;
import dji.common.error.DJIError;
import dji.common.gimbal.Attitude;
import dji.common.gimbal.Rotation;
import dji.sdk.mission.hotpoint.HotpointMissionOperator;
import dji.sdk.mission.timeline.actions.MissionAction;
import dji.sdk.products.Aircraft;
import dji.sdk.sdkmanager.DJISDKManager;
import dji.sdk.useraccount.UserAccountManager;

import dji.sdk.camera.Capabilities;
import dji.sdk.camera.Camera;
import dji.sdk.camera.PlaybackManager;

import dji.sdk.mission.MissionControl;
import dji.sdk.mission.Triggerable;
import dji.sdk.mission.timeline.TimelineElement;
import dji.sdk.mission.timeline.TimelineEvent;
import dji.sdk.mission.timeline.Mission;
import dji.sdk.mission.timeline.actions.MissionAction;
import dji.sdk.mission.timeline.actions.GimbalAttitudeAction;
import dji.sdk.mission.timeline.actions.GoHomeAction;
import dji.sdk.mission.timeline.actions.GoToAction;
import dji.sdk.mission.timeline.actions.HotpointAction;
import dji.sdk.mission.timeline.actions.RecordVideoAction;
import dji.sdk.mission.timeline.actions.ShootPhotoAction;
import dji.sdk.mission.timeline.actions.TakeOffAction;
import dji.sdk.mission.timeline.triggers.AircraftLandedTrigger;
import dji.sdk.mission.timeline.triggers.BatteryPowerLevelTrigger;
import dji.sdk.mission.timeline.triggers.Trigger;
import dji.sdk.mission.timeline.triggers.TriggerEvent;
import dji.sdk.mission.timeline.triggers.WaypointReachedTrigger;

public class MainActivity extends FragmentActivity implements View.OnClickListener, OnMapClickListener {

    protected static final String TAG = "MainActivity";

    private MissionControl missionControl;
    private MapView mapView;
    private AMap aMap;

    private Button back;
    private Button locate, add, clear,config;
    private Button start,pause,stop,factor,capture;

    private boolean isAdd = false;
    private boolean isPause = false;
    private boolean isFactor = false;

    private double droneLocationLat = 181, droneLocationLng = 181;
    private double HotPointLat = 181, HotPointLng = 181;
    private final Map<Integer, Marker> mMarkers = new ConcurrentHashMap<Integer, Marker>();
    private Marker droneMarker = null;

    private float altitude = 0.0f;
    private float mSpeed = 0.0f;
    private float radius = 0.0f;
    private int number = 0;

    private FlightController mFlightController;
    private HotpointMissionOperator instance;

    @Override
    protected void onResume(){
        super.onResume();
        initFlightController();
    }

    @Override
    protected void onPause(){
        super.onPause();
    }

    @Override
    protected void onDestroy(){
        unregisterReceiver(mReceiver);
        super.onDestroy();
    }

    /**
     * @Description : RETURN Button RESPONSE FUNCTION
     */
    public void onReturn(View view){
        Log.d(TAG, "onReturn");
        this.finish();
    }

    private void setResultToToast(final String string){
        MainActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MainActivity.this, string, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void initUI() {
        back = (Button) findViewById(R.id.back);
        locate = (Button) findViewById(R.id.locate);
        add = (Button) findViewById(R.id.add);
        clear = (Button) findViewById(R.id.clear);
        config = (Button) findViewById(R.id.config);
        start = (Button) findViewById(R.id.start);
        pause = (Button) findViewById(R.id.pause);
        stop = (Button) findViewById(R.id.stop);
        factor = (Button) findViewById(R.id.factor);
        capture = (Button) findViewById(R.id.capture);

        locate.setOnClickListener(this);
        back.setOnClickListener(this);
        add.setOnClickListener(this);
        clear.setOnClickListener(this);
        config.setOnClickListener(this);
        start.setOnClickListener(this);
        stop.setOnClickListener(this);
        pause.setOnClickListener(this);
        factor.setOnClickListener(this);
        capture.setOnClickListener(this);

    }

    private void initMapView() {

        if (aMap == null) {
            aMap = mapView.getMap();
            aMap.setOnMapClickListener(this);// add the listener for click for amap object
        }

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // When the compile and target version is higher than 22, please request the
        // following permissions at runtime to ensure the
        // SDK work well.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.VIBRATE,
                            Manifest.permission.INTERNET, Manifest.permission.ACCESS_WIFI_STATE,
                            Manifest.permission.WAKE_LOCK, Manifest.permission.ACCESS_COARSE_LOCATION,
                            Manifest.permission.ACCESS_NETWORK_STATE, Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.CHANGE_WIFI_STATE, Manifest.permission.MOUNT_UNMOUNT_FILESYSTEMS,
                            Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.SYSTEM_ALERT_WINDOW,
                            Manifest.permission.READ_PHONE_STATE,
                    }
                    , 1);
        }

        setContentView(R.layout.activity_main);

        IntentFilter filter = new IntentFilter();
        filter.addAction(DemoApplication.FLAG_CONNECTION_CHANGE);
        registerReceiver(mReceiver, filter);

        mapView = (MapView) findViewById(R.id.map);
        mapView.onCreate(savedInstanceState);

        initMapView();
        initUI();

    }

    protected BroadcastReceiver mReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            onProductConnectionChange();
        }
    };

    private void onProductConnectionChange()
    {
        initFlightController();
        loginAccount();
    }

    private void loginAccount(){

        UserAccountManager.getInstance().logIntoDJIUserAccount(this,
                new CommonCallbacks.CompletionCallbackWith<UserAccountState>() {
                    @Override
                    public void onSuccess(final UserAccountState userAccountState) {
                        Log.e(TAG, "登录成功");
                    }
                    @Override
                    public void onFailure(DJIError error) {
                        setResultToToast("登录失败:"
                                + error.getDescription());
                    }
                });
    }

    private void initFlightController() {

        BaseProduct product = DemoApplication.getProductInstance();
        if (product != null && product.isConnected()) {
            if (product instanceof Aircraft) {
                mFlightController = ((Aircraft) product).getFlightController();
            }
        }

        if (mFlightController != null) {

            mFlightController.setStateCallback(
                    new FlightControllerState.Callback() {
                        @Override
                        public void onUpdate(FlightControllerState
                                                     djiFlightControllerCurrentState) {
                            droneLocationLat = djiFlightControllerCurrentState.getAircraftLocation().getLatitude();
                            droneLocationLng = djiFlightControllerCurrentState.getAircraftLocation().getLongitude();
                            updateDroneLocation();
                        }
                    });

        }
    }

    public HotpointMissionOperator getHotpointMissionOperator() {
        if (instance == null) {
            instance = DJISDKManager.getInstance().getMissionControl().getHotpointMissionOperator();
        }
        return instance;
    }

    @Override
    public void onMapClick(LatLng point) {
        if (isAdd == true){
            markHotpoint(point);
            HotPointLat=point.latitude;
            HotPointLng=point.longitude;
        }
    }

    public static boolean checkGpsCoordination(double latitude, double longitude) {
        return (latitude > -90 && latitude < 90 && longitude > -180 && longitude < 180) && (latitude != 0f && longitude != 0f);
    }

    // Update the drone location based on states from MCU.
    private void updateDroneLocation(){

        LatLng pos = new LatLng(droneLocationLat, droneLocationLng);
        //Create MarkerOptions object
        final MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(pos);
        markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.aircraft));

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (droneMarker != null) {
                    droneMarker.remove();
                }

                if (checkGpsCoordination(droneLocationLat, droneLocationLng)) {
                    droneMarker = aMap.addMarker(markerOptions);
                }
            }
        });
    }

    private void markHotpoint(LatLng point){
        //Create MarkerOptions object
        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(point);
        markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE));
        Marker marker = aMap.addMarker(markerOptions);
        mMarkers.put(mMarkers.size(), marker);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.back:{
                Intent intent = new Intent(this,DefaultLayoutActivity.class);
                startActivity(intent);
                this.finish();
                break;
            }
            case R.id.locate:{
                updateDroneLocation();
                cameraUpdate(); // Locate the drone's place
                break;
            }
            case R.id.add:{
                enableDisableAdd();
                break;
            }
            case R.id.clear: {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        aMap.clear();
                    }

                });
                updateDroneLocation();
                break;
            }
            case R.id.config:{
                showSettingDialog();
                break;
            }
            case R.id.start:{
                startHotpointMission();
                break;
            }
            case R.id.pause:{
                pauseandresume();
                break;
            }
            case R.id.stop:{
                stopHotpointMission();
                break;
            }
            case R.id.factor:{
                setFactor();
                break;
            }
            case R.id.capture:{
                captureAction();
                break;
            }
            default:
                break;
        }
    }



    private void cameraUpdate(){
        LatLng pos = new LatLng(droneLocationLat, droneLocationLng);
        float zoomlevel = (float) 18.0;
        CameraUpdate cu = CameraUpdateFactory.newLatLngZoom(pos, zoomlevel);
        aMap.moveCamera(cu);

    }

    private void enableDisableAdd(){
        if (isAdd == false) {
            isAdd = true;
            add.setText("结束添加");
        }else{
            isAdd = false;
            add.setText("添加");
        }
    }

    private void pauseandresume()
    {
        if(isPause == false){
            isPause = true;
            pause.setText("恢复");
            pauseHotpointMission();
        }else{
            isPause = false;
            pause.setText("暂停");
            resumeHotpointMission();
        }
    }

    private void setFactor()
    {
        if(isFactor == false){
            isFactor = true;
            stop.setText("远景");
            Factor2Action();
        }else{
            isFactor = false;
            stop.setText("近景");
            Factor1Action();
        }
    }

    private void showSettingDialog(){
        LinearLayout hotPointSettings = (LinearLayout)getLayoutInflater().inflate(R.layout.dialog_hotpointsetting, null);

        final TextView wpAltitude_TV = (TextView) hotPointSettings.findViewById(R.id.altitude);
        final TextView wpRadius_TV = (TextView) hotPointSettings.findViewById(R.id.radius);
        final TextView wpNumber_TV = (TextView) hotPointSettings.findViewById(R.id.number);
        RadioGroup speed_RG = (RadioGroup) hotPointSettings.findViewById(R.id.speed);

        speed_RG.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener(){

            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (checkedId == R.id.lowSpeed){
                    mSpeed = 3.0f;
                } else if (checkedId == R.id.MidSpeed){
                    mSpeed = 5.0f;
                } else if (checkedId == R.id.HighSpeed){
                    mSpeed = 10.0f;
                }
            }

        });

        new AlertDialog.Builder(this)
                .setTitle("")
                .setView(hotPointSettings)
                .setPositiveButton("完成",new DialogInterface.OnClickListener(){
                    public void onClick(DialogInterface dialog, int id) {

                        String altitudeString = wpAltitude_TV.getText().toString();
                        String radiusString = wpRadius_TV.getText().toString();
                        String numberString = wpNumber_TV.getText().toString();
                        altitude = Integer.parseInt(nulltoIntegerDefalt(altitudeString));
                        radius = Integer.parseInt(nulltoIntegerDefalt(radiusString));
                        number = Integer.parseInt(nulltoIntegerDefalt(numberString));
                        Log.e(TAG,"飞行高度 "+altitude);
                        Log.e(TAG,"环绕半径 "+radius);
                        Log.e(TAG,"拍摄照片数目"+number);
                        Log.e(TAG,"角速度 "+mSpeed);
                    }

                })
                .setNegativeButton("取消", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }

                })
                .create()
                .show();
    }

    String nulltoIntegerDefalt(String value){
        if(!isIntValue(value)) value="0";
        return value;
    }

    boolean isIntValue(String val)
    {
        try {
            val=val.replace(" ","");
            Integer.parseInt(val);
        } catch (Exception e) {return false;}
        return true;
    }

    private void setHotPointMission()    {
        List<TimelineElement> elements = new ArrayList<>();

        missionControl = MissionControl.getInstance();

        HotpointMission hotpointMission = new HotpointMission();
        hotpointMission.setHotpoint(new LocationCoordinate2D(HotPointLat, HotPointLng));
        hotpointMission.setAltitude(altitude);
        hotpointMission.setRadius(radius);
        hotpointMission.setAngularVelocity(mSpeed);
        HotpointStartPoint startPoint = HotpointStartPoint.NEAREST;
        hotpointMission.setStartPoint(startPoint);
        HotpointHeading heading = HotpointHeading.TOWARDS_HOT_POINT;
        hotpointMission.setHeading(heading);

        elements.add(new TakeOffAction());

        double a = java.lang.Math.atan(altitude/radius);
        float b=(float)(a*180/java.lang.Math.PI);
        Attitude attitude = new Attitude(-b, Rotation.NO_ROTATION, Rotation.NO_ROTATION);
        GimbalAttitudeAction gimbalAction = new GimbalAttitudeAction(attitude);
        gimbalAction.setCompletionTime(2);
        elements.add(gimbalAction);

        int angle = 360/number;

        for(int i=0;i<number;i++)
        {
            elements.add(new HotpointAction(hotpointMission, angle));
            elements.add(new ShootPhotoAction());
        }

        elements.add(new GoHomeAction());

        attitude = new Attitude(0, Rotation.NO_ROTATION, Rotation.NO_ROTATION);
        gimbalAction = new GimbalAttitudeAction(attitude);
        gimbalAction.setCompletionTime(2);
        elements.add(gimbalAction);

        missionControl.scheduleElements(elements);
    }

    private void startHotpointMission(){

        if(altitude>=10.0f&&radius>=5.0f&&number>=1&&mSpeed>0.0f)
        {
            setHotPointMission();
            if (MissionControl.getInstance().scheduledCount() > 0) {
                MissionControl.getInstance().startTimeline();
            } else {
                new CommonCallbacks.CompletionCallback(){
                    @Override
                    public void onResult(DJIError error) {
                        setResultToToast("执行任务： " + (error == null ? "成功" : error.getDescription()));
                    }
                };
            }

        }
        else {
            setResultToToast("请输入正确参数");
        }
    }

    private void stopHotpointMission(){
        MissionControl.getInstance().stopTimeline();
    }

    private void pauseHotpointMission(){
        MissionControl.getInstance().pauseTimeline();
    }

    private void resumeHotpointMission(){
        /*Camera camera = DemoApplication.getCameraInstance();
        camera.setDigitalZoomFactor(1,new CommonCallbacks.CompletionCallback(){
            @Override
            public void onResult(DJIError error) {
                setResultToToast("切换远景： " + (error == null ? "成功" : error.getDescription()));
            }
        });*/
        MissionControl.getInstance().resumeTimeline();
    }

    private void Factor1Action(){
        Camera camera = DemoApplication.getCameraInstance();
        camera.setDigitalZoomFactor(1,new CommonCallbacks.CompletionCallback(){
            @Override
            public void onResult(DJIError error) {
                setResultToToast("切换远景： " + (error == null ? "成功" : error.getDescription()));
            }
        });
    }

    private void Factor2Action(){
        Camera camera = DemoApplication.getCameraInstance();
        camera.setDigitalZoomFactor(2,new CommonCallbacks.CompletionCallback(){
            @Override
            public void onResult(DJIError error) {
                setResultToToast("切换近景： " + (error == null ? "成功" : error.getDescription()));
            }
        });
    }
    private void captureAction(){

        Camera camera = DemoApplication.getCameraInstance();

        /*camera.getDigitalZoomFactor(new CommonCallbacks.CompletionCallbackWith<Float>() {
            @Override
            public void onSuccess(Float aFloat) {
                setResultToToast("当前值："+aFloat);
            }

            @Override
            public void onFailure(DJIError djiError) {
                setResultToToast(djiError.getDescription());
            }
        });*/

        camera.startShootPhoto(new CommonCallbacks.CompletionCallback(){
            @Override
            public void onResult(DJIError error) {
                setResultToToast("拍照： " + (error == null ? "成功" : error.getDescription()));
            }
        });
    }


}
