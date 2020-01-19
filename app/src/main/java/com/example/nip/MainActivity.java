package com.example.nip;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.provider.SyncStateContract;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.nip.Adapters.LeDeviceListAdapter;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.Socket;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {

    private TextView mTextMessage;
    private Button StartScanBtn;
    private Button StopScanBtn;
    private ListView listView;
    private LinearLayout Layout1;
    private LinearLayout Layout2;
    private LinearLayout Layout3;
    private TextView IPAddress;
    private Button PictureBtn;
    private Button StartVideoBtn;
    private Button StopVideoBtn;
    private Button SensorBtn;
    private Button DownloadBtn;
    private Button PreviewBtn;

    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothDevice mBluetoothDevice;
    private BluetoothGatt mBluetoothGatt;
    List<BluetoothDevice> deviceList=new ArrayList<>();

    // defined by Stephen
    private final static String UUID_SERVICE = "2093b22e-dc83-4e43-a1d1-587a9012d3ee";
    private final static String UUID_CHARACTERISTIC_IP="74655694-74d8-4b4e-9dea-c4a6745bede3";

    private BluetoothGattService BladeService;
    private BluetoothGattCharacteristic BladeCharacteristic;
    private boolean isIPChanged=false;
    private boolean isImageSaved=false;
    private String BladeIP="0.0.0.0";
    private int PORT=9000;
    private String HTTPResponse;
    private String filePath;

    private CommandReceiver commandReceiver;

    private Socket socket;

    final static int REQUEST_ENABLE_BT=1;
    private String TAG = "Stephen";

    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            // TODO: May add other buttons
            switch (item.getItemId()) {
                case R.id.navigation_home:
//                    mTextMessage.setText(R.string.title_home);
                    // TODO: Set first button
                    Layout1.setZ(100.f);
                    Layout2.setZ(0.f);
                    Layout3.setZ(0.f);
                    Layout1.setAlpha(1.0f);
                    Layout2.setAlpha(0.f);
                    Layout3.setAlpha(0.f);
                    return true;
                case R.id.navigation_dashboard:
//                    mTextMessage.setText(R.string.title_dashboard);
                    // TODO: Set second button
                    Layout1.setZ(0.f);
                    Layout2.setZ(100.f);
                    Layout3.setZ(0.f);
                    Layout1.setAlpha(0.f);
                    Layout2.setAlpha(1.0f);
                    Layout3.setAlpha(0.f);
                    return true;
                case R.id.navigation_notifications:
//                    mTextMessage.setText(R.string.title_notifications);
                    // TODO: Set third button
                    Layout1.setZ(0.f);
                    Layout2.setZ(0.f);
                    Layout3.setZ(100.f);
                    Layout1.setAlpha(0.f);
                    Layout2.setAlpha(0.f);
                    Layout3.setAlpha(1.f);
                    return true;
            }
            return false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getUIElements();

        RegisterListeners();

        initializeReceivers();

        initializeBLE();

        Log.i(TAG, "onCreate: start");
//        mBluetoothAdapter.startLeScan(leScanCallback);
    }

    @Override
    protected void onDestroy(){
        unregisterReceiver(commandReceiver);
        super.onDestroy();
    }

    private void getUIElements(){
        mTextMessage = (TextView) findViewById(R.id.message);
        BottomNavigationView navigation = (BottomNavigationView) findViewById(R.id.navigation);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);

        StartScanBtn = findViewById(R.id.StartScanButton);
        StopScanBtn = findViewById(R.id.StopScanButton);
        listView = findViewById(R.id.listview);
        Layout1=findViewById(R.id.Layout1);
        Layout2=findViewById(R.id.Layout2);
        Layout3=findViewById(R.id.Layout3);
        IPAddress=findViewById(R.id.IPAddress);
        PictureBtn=findViewById(R.id.PictureButton);
        StartVideoBtn=findViewById(R.id.StartVideoButton);
        StopVideoBtn=findViewById(R.id.StopVideoButton);
        SensorBtn=findViewById(R.id.SensorBtn);
        DownloadBtn=findViewById(R.id.DownloadBtn);
        PreviewBtn = findViewById(R.id.PreviewBtn);

        Layout1.setZ(100.f);
        Layout2.setZ(0.f);
        Layout3.setZ(0.f);

    }

    private void RegisterListeners(){
        StartScanBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(MainActivity.this,"Start scanning",Toast.LENGTH_SHORT).show();

                if(!mBluetoothAdapter.isEnabled()){
                    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
                }

                deviceList.clear();
                mBluetoothAdapter.startLeScan(leScanCallback);
            }
        });

        StopScanBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO: Add stop scanning function
                Toast.makeText(MainActivity.this,"Stop scanning",Toast.LENGTH_SHORT).show();

                mBluetoothAdapter.stopLeScan(leScanCallback);
            }
        });


        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                mBluetoothAdapter.stopLeScan(leScanCallback);
                mBluetoothDevice = deviceList.get(position);
                mBluetoothGatt = mBluetoothDevice.connectGatt(MainActivity.this, false, gattcallback);
//                Toast.makeText(MainActivity.this, "Connecting", Toast.LENGTH_SHORT).show();
            }
        });

        PictureBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                if(!BladeIP.equals("0.0.0.0")){
                JSONObject json = new JSONObject();
                try{
                    json.put("src", "picture_test");
                    json.put("resolution","1536p");

                }catch (JSONException e){
                    Toast.makeText(MainActivity.this, "Json error!", Toast.LENGTH_SHORT).show();
                }
                    String response = SendHTTPRequest("/cameras/0/picture", json);


//                }
            }
        });

        StartVideoBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                if(!BladeIP.equals("0.0.0.0")){
                JSONObject json = new JSONObject();
                try{
//                    json.put("src", "video_test");
                    json.put("resolution","480p_3");
                    json.put("status","START");

                }catch (JSONException e){
                    Toast.makeText(MainActivity.this, "Json error!", Toast.LENGTH_SHORT).show();
                }
                    String response = SendHTTPRequest("/cameras/0/video",json);

//                }
            }
        });

        StopVideoBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                if(!BladeIP.equals("0.0.0.0")){
                JSONObject json = new JSONObject();
                try{
                    json.put("status","STOP");

                }catch (JSONException e){
                    Toast.makeText(MainActivity.this, "Json error!", Toast.LENGTH_SHORT).show();
                }
                    String response = SendHTTPRequest("/cameras/0/video",json);

//                }
            }
        });

        SensorBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                JSONObject json = new JSONObject();
                try{
                    json.put("active","true");

                }catch (JSONException e){
                    Toast.makeText(MainActivity.this, "Json error!", Toast.LENGTH_SHORT).show();
                }
                String response = SendHTTPRequest("/sensors/4",json);
            }
        });

        DownloadBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                JSONObject json = new JSONObject();
                    String response = SendHTTPRequest("/sensors/4/data",json);
            }
        });

        PreviewBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                if(!BladeIP.equals("0.0.0.0")){
                JSONObject json = new JSONObject();

                    String response = SendHTTPRequest("/cameras/0/preview",json);

//                }
            }
        });

    }

    private void initializeReceivers(){
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("com.example.nip.HTTP_SENT");
        commandReceiver = new CommandReceiver();
        registerReceiver(commandReceiver, intentFilter);
    }

    private class CommandReceiver extends BroadcastReceiver{
        @Override
        public void onReceive(Context context, Intent intent){
            if(intent.getAction().equals("com.example.nip.HTTP_SENT")){
                // download the image
                buildSocket();
            }
        }
    }

    private void initializeBLE(){
        mBluetoothManager = (BluetoothManager)getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter=mBluetoothManager.getAdapter();
        if(mBluetoothAdapter==null||!mBluetoothAdapter.isEnabled()){
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
    }

    private BluetoothAdapter.LeScanCallback leScanCallback =
            new BluetoothAdapter.LeScanCallback() {
                @Override
                public void onLeScan(final BluetoothDevice device, int rssi,
                                     byte[] scanRecord) {
                    if(!deviceList.contains((device))){
                        deviceList.add(device);
                        Log.i(TAG, "onLeScan: Found Device!");
                        listView.setAdapter(new LeDeviceListAdapter(MainActivity.this, deviceList));
                    }
                }
            };


    private BluetoothGattCallback gattcallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(final BluetoothGatt gatt, int status, final int newState) {
            super.onConnectionStateChange(gatt, status, newState);

            runOnUiThread(new Runnable() {
                @Override
                public void run() {

                    switch (newState) {
                        //已经连接
                        case BluetoothProfile.STATE_CONNECTED:

                            Toast.makeText(MainActivity.this, "Connected", Toast.LENGTH_SHORT).show();
//                    broadcastUpdate("com.nus.hci.bladeheadpiece.GATT_CONNECTED");
                            String str="Connected: "+gatt.getDevice().getName();
                            mTextMessage.setText(str);
                            mBluetoothGatt.discoverServices();

                            break;
                        //正在连接
                        case BluetoothProfile.STATE_CONNECTING:
                            //lianjiezhuangtai.setText("正在连接");
//                    Toast.makeText(MainActivity.this, "Now Connecting", Toast.LENGTH_SHORT).show();
                            String str2="Connecting: "+gatt.getDevice().getName();
                            mTextMessage.setText(str2);
                            break;
                        //连接断开
                        case BluetoothProfile.STATE_DISCONNECTED:
                            //lianjiezhuangtai.setText("已断开");
                            Toast.makeText(MainActivity.this, "Disconnected", Toast.LENGTH_SHORT).show();
//                    broadcastUpdate("com.nus.hci.bladeheadpiece.GATT_DISCONNECTED");
                            String str3="BLE Scanning";
                            mTextMessage.setText(str3);
                            IPAddress.setText("IP Address: 0.0.0.0");
                            break;
                        //正在断开
                        case BluetoothProfile.STATE_DISCONNECTING:
//                            lianjiezhuangtai.setText("断开中");
                            Toast.makeText(MainActivity.this, "Disconnecting", Toast.LENGTH_SHORT).show();
                            break;
                    }

                }
            });

        }

        @Override
        public void onServicesDiscovered(final BluetoothGatt gatt, final int status) {
            super.onServicesDiscovered(gatt, status);

            runOnUiThread(new Runnable() {
                @Override
                public void run() {

                    if (status == BluetoothGatt.GATT_SUCCESS) {

                        final List<BluetoothGattService> serviceList=mBluetoothGatt.getServices();

                        for(final BluetoothGattService bluetoothGattService:serviceList){
                            Log.i(TAG, "onServicesDiscovered: ServerUUID "+bluetoothGattService.getUuid());

                            List<BluetoothGattCharacteristic> charc = bluetoothGattService.getCharacteristics();
                            for(BluetoothGattCharacteristic charac :charc){
                                Log.i(TAG, "onServicesDiscovered: chaUUID "+charac.getUuid());
                            }
                        }

                        for(final BluetoothGattService service:serviceList){
                            if(service.getUuid().toString().equals(UUID_SERVICE)){
                                BladeService=service;
                                break;
                            }
                        }
                        if(BladeService==null){
                            Toast.makeText(MainActivity.this, "No Blade Service Found!", Toast.LENGTH_SHORT).show();
                        }
                        else{
                            Log.i(TAG, "run: Found Service!");
                            List<BluetoothGattCharacteristic> charc = BladeService.getCharacteristics();
                            for(BluetoothGattCharacteristic charac :charc){
                                if(charac.getUuid().toString().equals(UUID_CHARACTERISTIC_IP)){
                                    BladeCharacteristic=charac;
                                }

                            }
                        }
                        if(BladeCharacteristic==null){
                            Toast.makeText(MainActivity.this, "No Blade Characteristic Found!", Toast.LENGTH_SHORT).show();
                        }
                        else{
                            Log.i(TAG, "run: Found Characteristic!");
//                            String result = BladeCharacteristic.getValue().toString();
//                            Log.i(TAG, "run: "+result);
                            mBluetoothGatt.setCharacteristicNotification(BladeCharacteristic,true);
                            mBluetoothGatt.readCharacteristic(BladeCharacteristic);

                            // test WRITE
//                            BladeCharacteristic.setValue("new value");
//                            mBluetoothGatt.writeCharacteristic(BladeCharacteristic);
                        }

                    } else {// 未发现服务
                        Log.w(TAG, "onServicesDiscovered received: " + status);
                    }

                }
            });
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic, final int status) {
            super.onCharacteristicRead(gatt, characteristic, status);


            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if(status==BluetoothGatt.GATT_SUCCESS){
                        String newIP = new String(characteristic.getValue());
//                        Log.i(TAG, "run: "+result);
                        if(!newIP.equals(BladeIP)){
                            isIPChanged=true;
                            BladeIP=newIP;
                        }
                        IPAddress.setText("IP Address: "+BladeIP);
                    }
                }
            });

        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
            if(status==BluetoothGatt.GATT_SUCCESS){
                Log.i(TAG, "onCharacteristicWrite: Write Successfully");
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
        }

        @Override
        public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorRead(gatt, descriptor, status);
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorWrite(gatt, descriptor, status);
        }

        @Override
        public void onReliableWriteCompleted(BluetoothGatt gatt, int status) {
            super.onReliableWriteCompleted(gatt, status);
        }

        @Override
        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
            super.onReadRemoteRssi(gatt, rssi, status);
        }

        @Override
        public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
            super.onMtuChanged(gatt, mtu, status);
        }
    } ;

    /*
    private void SendHTTPRequest(String command){
        new Thread(new Runnable() {
            @Override
            public void run() {
                HttpURLConnection connection=null;
                BufferedReader reader=null;
                try{
                    URL url = URL("http", BladeIP, "8080", )
                }
            }
        }).start();
    }
    */

    /*
    private void SendHTTPRequest(String Command){
        try{
            StringBuilder buf=new StringBuilder();
            buf.append("{'message':'"+Command+"'}");
            byte[] data=buf.toString().getBytes(StandardCharsets.UTF_8);

            URL url = new URL("http://"+BladeIP+":8080");
            HttpURLConnection conn = (HttpURLConnection)url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);	//如果要输出，则必须加上此句
            OutputStream out = conn.getOutputStream();
            out.write(data);

            if(conn.getResponseCode()==200){
                Toast.makeText(this, "POST",Toast.LENGTH_SHORT).show();
            }

        } catch (Exception e){
            Toast.makeText(this, "HTTP Error!", Toast.LENGTH_SHORT).show();
        }

    }
    */

    private String SendHTTPRequest(final String command, final JSONObject jsonBody) {

        new Thread(new Runnable() {
            @Override
            public void run() {

                final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
                OkHttpClient client = new OkHttpClient();
//                JSONObject json = new JSONObject();
////                String HTTPResponse;
//                try{
//                    json.put("message", command);
//
//                }catch (JSONException e){
//                    Toast.makeText(MainActivity.this, "Json error!", Toast.LENGTH_SHORT).show();
//                }
                RequestBody body = RequestBody.create(JSON, jsonBody.toString());
                Request sensorRequest = new Request.Builder()
                        .url("http://172.25.100.165:8080"+command)
                        .build();


                Log.i(TAG, "SendHTTPRequest: "+"http://"+BladeIP+":8080"+command);
                Request request = new Request.Builder()
                        .url("http://"+"172.25.100.165:8080"+command)
                        .post(body)
                        .build();

                client.newCall(sensorRequest).enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {

                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        Log.i(TAG, "onResponse: "+response.body().string());
                    }
                });
//                try{
//                    Response response = client.newCall(request).execute();
//                    String temp = response.body().string();
//                    Log.i(TAG, "run: "+temp);
//
//                    HTTPResponse=temp;
//
//
//                }catch (IOException e){
//                    runOnUiThread(new Runnable() {
//                        @Override
//                        public void run() {
//                            Toast.makeText(MainActivity.this, "HTTP Error!", Toast.LENGTH_SHORT).show();
//                            Log.e(TAG, "run: HTTP Error!");
//                        }
//                    });
////                    return "";
//                }catch (  NullPointerException n){
//                    Toast.makeText(MainActivity.this, "Null pointer!", Toast.LENGTH_SHORT).show();
////                    return "";
//                }
            }

        }).start();

        if(HTTPResponse!=null) return HTTPResponse;
        else return "";


    }

    private void buildSocket(){
        if(BladeIP.equals("0.0.0.0")){
            Log.i(TAG, "buildSocket: Need blade's IP address!");
            return;
        }

        try{
            if(socket==null) socket = new Socket(BladeIP, PORT);
            else if(isIPChanged){
                socket = new Socket(BladeIP,PORT);
                isIPChanged=false;
            }

            Log.i(TAG, "buildSocket: socket built!");

            new Thread(new Runnable() {
                @Override
                public void run() {

                    try{
                        // write a command
                        DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
                        dos.writeBytes("Download");
                        dos.flush();
                        dos.close();

                        DataInputStream dis = new DataInputStream(socket.getInputStream());

                        // get filePath from dis
                        filePath=dis.readUTF();
                        Log.i(TAG, "run: filePath: "+filePath);

                        // get the image into buf
                        int byteNum;
                        byte[] buf=new byte[8192*1024];
                        byteNum=dis.read(buf);
                        Log.i(TAG, "run: byteNum: "+byteNum);

                        // write the file
                        DataOutputStream fileOut = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(filePath)));
                        fileOut.write(buf,0, byteNum);
                        dis.close();
                        fileOut.close();
                        socket.close();

                        // log the information about image
                        isImageSaved=true;
                        Log.i(TAG, "run: File saved successfully!");
                    }catch (IOException e){
                        e.printStackTrace();
                    }

                }
            }).start();
        }catch (IOException e){
            e.printStackTrace();

        }
    }

}
