package com.example.locationdemoapp;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import com.alibaba.fastjson.JSONObject;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private final String TAG = "MainActivity";
    private static String IP = "192.168.3.10";
    private static int PORT = 8888;
    private WifiManager wifiManager;
    private static double X;
    private static double Y;
    List<ScanResult> scanResult;
    TextView status, xCordText, yCordText, rssi;
    boolean isStart = false;
    String[] apForLocation = {"48:8A:D2:0B:C5:54", "A8:57:4E:2D:D7:2C", "B0:89:00:E3:25:10", "C0:A5:DD:01:3C:80"};
    List<String> ap_Name = new ArrayList<>();
    Socket wifiSocket;
    ImageView imageView;
    Paint paint;
    Bitmap bitmap;
    Canvas canvas;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getPermission();
        initSocket();
        status = findViewById(R.id.status);
        xCordText = findViewById(R.id.x_cor);
        yCordText = findViewById(R.id.y_cor);
        rssi = findViewById(R.id.rssi);
        imageView = findViewById(R.id.bg);
        paint = new Paint();
        paint.setColor(Color.RED);
        wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        for (int i = 0; i < apForLocation.length; i ++) {
            ap_Name.add(apForLocation[i]);
        }
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("UPDATE_XY");
        MyBroadcastReceiver myBroadcastReceiver = new MyBroadcastReceiver();
        registerReceiver(myBroadcastReceiver, intentFilter);
        IntentFilter updateRssiFilter = new IntentFilter();
        updateRssiFilter.addAction("UPDATE_RSSI");
        UpdateRssi updateRssi = new UpdateRssi();
        registerReceiver(updateRssi, updateRssiFilter);
    }

    private class UpdateRssi extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            String r = intent.getStringExtra("RSSI");
            rssi.setText(r);
        }
    }

    private class MyBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            double xRatio = X / 1150;  //计算出实际xy坐标相对于整个地图的比例
            double yRation = Y / 648;
            double xCor = imageView.getHeight() - (yRation * imageView.getHeight());  //xy坐标需要转换，服务端传来的坐标是基于横向地图的，客户端是纵向的地图，原点坐标变了。
            double yCor = xRatio * imageView.getWidth();
            xCordText.setText("X坐标：" + String.format("%.2f", xCor));
            yCordText.setText("Y坐标：" + String.format("%.2f", yCor));
            bitmap = Bitmap.createBitmap(imageView.getWidth(), imageView.getHeight(), Bitmap.Config.ARGB_8888);
            canvas = new Canvas(bitmap);
            paint.setStrokeWidth(40);
            canvas.drawPoint((float) xCor, (float) yCor, paint);
            imageView.setImageBitmap(bitmap);
        }
    }

    private void initSocket() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    wifiSocket = new Socket(IP, PORT);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }


    public void startLocaion(View view) {
        if (isStart != true) {
            isStart = true;
            Toast.makeText(getApplicationContext(), "App Start!", Toast.LENGTH_SHORT).show();
            status.setText("状态：定位中");
            new Thread(new StartLocationThread()).start();
            new Thread(new GetCoordinateThread()).start();
        }
    }

    public void stopLocation(View view) {
        if (isStart != false) {
            isStart = false;
            Toast.makeText(getApplicationContext(), "App Stop!", Toast.LENGTH_SHORT).show();
            status.setText("状态：停止");
        }
    }

    class GetCoordinateThread implements Runnable {

        BufferedReader coordinate;
        Intent intent;

        GetCoordinateThread() {
            {
                try {
                    coordinate = new BufferedReader(new InputStreamReader(wifiSocket.getInputStream()));
                    intent = new Intent("UPDATE_XY");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        @Override
        public void run() {
            while (isStart) {
                try {
                    String str = null;
                    JSONObject jsonObject;
                    while ((str = coordinate.readLine()) != null) {
                        jsonObject = JSONObject.parseObject(str);
                        X = Double.parseDouble(jsonObject.get("x").toString());
                        Y = Double.parseDouble(jsonObject.get("y").toString());
                        sendBroadcast(intent);
                        System.out.println(jsonObject.toJSONString());
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        }
    }

    class StartLocationThread implements Runnable {

        BufferedWriter wifiOut;
        Intent intent;

        StartLocationThread() {
            try {
                wifiOut = new BufferedWriter(new OutputStreamWriter(wifiSocket.getOutputStream()));
                intent = new Intent("UPDATE_RSSI");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            while (isStart) {
                try {
                    wifiManager.startScan();
                    scanResult = wifiManager.getScanResults();
                    JSONObject jsonObject = new JSONObject();
                    for (ScanResult sc : scanResult) {
                        if (ap_Name.contains(sc.BSSID.toUpperCase())) {
                            jsonObject.put(sc.BSSID.toUpperCase(), sc.level);
                        }
                    }
//                    jsonObject.put("48:8A:D2:0B:C5:54", "-38");
//                    jsonObject.put("A8:57:4E:2D:D7:2C", "-43");
//                    jsonObject.put("B0:89:00:E3:25:10", "-40");
//                    jsonObject.put("C0:A5:DD:01:3C:80", "-25");
                    StringBuilder stringBuilder = new StringBuilder();
                    for (int i = 0; i < apForLocation.length; i ++) {
                        String rssi = String.valueOf(jsonObject.get(apForLocation[i]));
                        stringBuilder.append(rssi + " ");
                    }
                    intent.putExtra("RSSI", stringBuilder.toString());
                    sendBroadcast(intent);
                    wifiOut.write(jsonObject.toJSONString() + "\r\n");
                    System.out.println(jsonObject.toJSONString());
                    wifiOut.flush();
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (IOException e) {
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
