package com.minew.beaconset.demo;

import android.Manifest;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Toast;

import com.minew.beaconset.BluetoothState;
import com.minew.beaconset.ConnectionState;
import com.minew.beaconset.MinewBeacon;
import com.minew.beaconset.MinewBeaconConnection;
import com.minew.beaconset.MinewBeaconConnectionListener;
import com.minew.beaconset.MinewBeaconManager;
import com.minew.beaconset.MinewBeaconManagerListener;
import com.minew.beaconset.R;
import android.os.Build;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import com.yuliwuli.blescan.demo.R;

import java.util.Collections;
import java.util.List;


public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_ACCESS_FINE_LOCATION = 1000;
    private MinewBeaconManager mMinewBeaconManager;
    private RecyclerView mRecycle;
    private BeaconListAdapter mAdapter;
    private static final int REQUEST_ENABLE_BT = 2;
    private boolean isScanning;

    UserRssi comp = new UserRssi();
    private ProgressDialog mpDialog;
    public static MinewBeacon clickBeacon;
    private TextView mStart_scan;
    private boolean mIsRefreshing;
    private int state;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initView();
        initManager();
        checkBluetooth();
        checkLocation();
        checkLocationPermition();
        initListener();

        dialogshow();
        mMinewBeaconManager.startService();
    }

    private void checkLocationPermition() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

            int permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION);

            if (permissionCheck == PackageManager.PERMISSION_DENIED) {

                // 권한 없음
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        REQUEST_ACCESS_FINE_LOCATION);


            } else {

                // ACCESS_FINE_LOCATION 에 대한 권한이 이미 있음.

            }


        }

// OS가 Marshmallow 이전일 경우 권한체크를 하지 않는다.
        else {

        }
    }

    private void initView() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.main_toolbar);
        setSupportActionBar(toolbar);

        mRecycle = (RecyclerView) findViewById(R.id.main_recyeler);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this);
        mRecycle.setLayoutManager(layoutManager);
        mAdapter = new BeaconListAdapter();
        mRecycle.setAdapter(mAdapter);
        mRecycle.addItemDecoration(new RecycleViewDivider(this, LinearLayoutManager
                .HORIZONTAL));
    }

    private void initManager() {
        mMinewBeaconManager = MinewBeaconManager.getInstance(this);
    }


    /*
     * check location
     * */
    private void checkLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            // Permission is not granted
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 101);
        }

    }

    /**
     * check Bluetooth state
     */
    private void checkBluetooth() {
        BluetoothState bluetoothState = mMinewBeaconManager.checkBluetoothState();
        switch (bluetoothState) {
            case BluetoothStateNotSupported:
                Toast.makeText(this, "Not Support BLE", Toast.LENGTH_SHORT).show();
                finish();
                break;
            case BluetoothStatePowerOff:
                showBLEDialog();
                break;
            case BluetoothStatePowerOn:
                break;
        }
    }

    private void initListener() {
        mStart_scan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (mMinewBeaconManager != null) {
                    BluetoothState bluetoothState = mMinewBeaconManager.checkBluetoothState();
                    switch (bluetoothState) {
                        case BluetoothStateNotSupported:
                            Toast.makeText(MainActivity.this, "Not Support BLE", Toast.LENGTH_SHORT).show();
                            finish();
                            break;
                        case BluetoothStatePowerOff:
                            showBLEDialog();
                            return;
                        case BluetoothStatePowerOn:
                            break;
                    }
                }
                if (isScanning) {
                    isScanning = false;
                    mStart_scan.setText("Start");
                    if (mMinewBeaconManager != null) {
                        mMinewBeaconManager.stopScan();
                    }
                } else {
                    isScanning = true;
                    mStart_scan.setText("Stop");
                    try {
                        mMinewBeaconManager.startScan();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        private void initListener () {
            //scan listener;
            mMinewBeaconManager.setMinewbeaconManagerListener(new MinewBeaconManagerListener() {
                @Override
                public void onUpdateBluetoothState(BluetoothState state) {
                    switch (state) {
                        case BluetoothStatePowerOff:
                            Toast.makeText(getApplicationContext(), "bluetooth off", Toast.LENGTH_SHORT).show();
                            break;
                        case BluetoothStatePowerOn:
                            Toast.makeText(getApplicationContext(), "bluetooth on", Toast.LENGTH_SHORT).show();
                            break;
                    }
                }

                @Override
                public void onRangeBeacons(List<MinewBeacon> beacons) {
                    Collections.sort(beacons, comp);

                    mAdapter.setData(beacons);
                }

                @Override
                public void onAppearBeacons(List<MinewBeacon> beacons) {

                }

                @Override
                public void onDisappearBeacons(List<MinewBeacon> beacons) {

                }
            });

            mAdapter.setOnItemClickLitener(new BeaconListAdapter.OnItemClickLitener() {
                @Override
                public void onItemClick(View view, int position) {
                    mpDialog.setMessage(getString(R.string.connecting)
                            + mAdapter.getData(position).getName());
                    mpDialog.show();
                    mMinewBeaconManager.stopScan();
                    //connect to beacon
                    MinewBeacon minewBeacon = mAdapter.getData(position);
                    MinewBeaconConnection minewBeaconConnection = new MinewBeaconConnection(MainActivity.this, minewBeacon);
                    minewBeaconConnection.setMinewBeaconConnectionListener(minewBeaconConnectionListener);
                    minewBeaconConnection.connect();
                }

                @Override
                public void onItemLongClick(View view, int position) {

                }
            });

        }

        //connect listener;
        MinewBeaconConnectionListener minewBeaconConnectionListener = new MinewBeaconConnectionListener() {
            @Override
            public void onChangeState(MinewBeaconConnection connection, ConnectionState state) {
                switch (state) {
                    case BeaconStatus_Connected:
                        mpDialog.dismiss();
                        Intent intent = new Intent(MainActivity.this, DetilActivity.class);
                        intent.putExtra("mac", connection.setting.getMacAddress());
                        startActivity(intent);
                        break;
                    case BeaconStatus_ConnectFailed:
                    case BeaconStatus_Disconnect:
                        if (mpDialog != null) {
                            mpDialog.dismiss();
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(getApplicationContext(), "연결이 끊어졌습니다.", Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                        break;
                }
            }

            @Override
            public void onWriteSettings(MinewBeaconConnection connection, boolean success) {

            }
        };

        @Override
        protected void onResume () {
            mMinewBeaconManager.startScan();
            initListener();
            super.onResume();
        }

        @Override
        protected void onPause () {
            mMinewBeaconManager.stopScan();
            super.onPause();
        }

        protected void dialogshow () {
            mpDialog = new ProgressDialog(MainActivity.this);
            mpDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            mpDialog.setTitle(null);//
            mpDialog.setIcon(null);//
            mpDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {

                @Override
                public void onCancel(DialogInterface arg0) {

                }
            });
            mpDialog.setCancelable(true);//
            mpDialog.setCanceledOnTouchOutside(false);
        }

        private void showBLEDialog () {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        }

        @Override
        protected void onActivityResult ( int requestCode, int resultCode, Intent data){
            super.onActivityResult(requestCode, resultCode, data);
            switch (requestCode) {
                case REQUEST_ENABLE_BT:
                    mMinewBeaconManager.startScan();
                    break;
            }
        }

        @Override
        protected void onDestroy () {
            mMinewBeaconManager.stopService();
            super.onDestroy();
        }
    }
}

