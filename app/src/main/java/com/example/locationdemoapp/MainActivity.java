package com.example.locationdemoapp;

import android.Manifest;
import android.content.pm.PackageManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static String IP = "192.168.137.1";
    private WifiManager wifiManager;
    List<ScanResult> scanResult;
    TextView status;
    boolean isStart = false;
    String[] apForLocation = {"A8:57:4E:2D:D7:2C", "B0:89:00:E3:25:10", "14:E6:E4:2E:0B:5C", "48:8A:D2:0B:C5:54"};
    List<String> ap_Name = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getPermission();
        status = findViewById(R.id.status);
        wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        for (int i = 0; i < apForLocation.length; i ++) {
            ap_Name.add(apForLocation[i]);
        }
    }

    public void startLocaion(View view) {
        if (isStart != true) {
            isStart = true;
            Toast.makeText(getApplicationContext(), "App Start!", Toast.LENGTH_SHORT).show();
            status.setText("状态：定位中");
            new Thread(new startLocationThread()).start();
        }
    }

    public void stopLocation(View view) {
        if (isStart != false) {
            isStart = false;
            Toast.makeText(getApplicationContext(), "App Stop!", Toast.LENGTH_SHORT).show();
            status.setText("状态：停止");
        }
    }

    class startLocationThread implements Runnable {

        @Override
        public void run() {
            while (isStart) {
                try {
                    Thread.sleep(15000);
                    wifiManager.startScan();
                    scanResult = wifiManager.getScanResults();
                    StringBuilder stringBuilder = new StringBuilder();
                    for (ScanResult sc : scanResult) {
                        if (ap_Name.contains(sc.BSSID.toUpperCase())) {
                            stringBuilder.append(sc.SSID + " : " + sc.BSSID + " : " + sc.level + System.getProperty("line.separator"));
                        }
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void getPermission() {
        String[] permissions = new String[]{Manifest.permission.CALL_PHONE, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.INTERNET, Manifest.permission.ACCESS_WIFI_STATE, Manifest.permission.ACCESS_FINE_LOCATION
                ,Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.CHANGE_WIFI_STATE};
        List<String> mPermissionList = new ArrayList<>();
        for (int i = 0; i < permissions.length; i++) {
            if (ContextCompat.checkSelfPermission(getApplicationContext(), permissions[i]) != PackageManager.PERMISSION_GRANTED) {
                mPermissionList.add(permissions[i]);
            }
        }
        if (mPermissionList.isEmpty()) {//未授予的权限为空，表示都授予了
            Log.d("MainActivity","1223");
        } else {//请求权限方法
            String[] permissionsss = mPermissionList.toArray(new String[mPermissionList.size()]);//将List转为数组
            ActivityCompat.requestPermissions(MainActivity.this, permissionsss, 1);
        }
    }
}
