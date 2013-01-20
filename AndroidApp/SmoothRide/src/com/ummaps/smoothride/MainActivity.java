package com.ummaps.smoothride;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Set;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.jeffboody.BlueSmirf.BlueSmirfSPP;


public class MainActivity extends Activity implements Runnable, Handler.Callback, OnItemSelectedListener {
    
	private static final String TAG = "SmoothRide";
	
	// BT state
	private BlueSmirfSPP		mSPP;
	private boolean				mIsThreadRunning;
	private String				mBluetoothAddress;
	private ArrayList<String>	mArrayListBluetoothAddress;
	
	// UI
	private TextView			mTextViewStatus;
	private Spinner				mSpinnerDevices;
	private ArrayAdapter		mArrayAdapterDevices;
	private Handler				mHandler;
	
	// Arduino commands and data
	private int					mCmdGetData;
	private int					mSmallBump;
	private int					mBigBump;
	
	public MainActivity() {
		mIsThreadRunning			= false;
		mBluetoothAddress			= null;
		mSPP						= new BlueSmirfSPP();
		mArrayListBluetoothAddress	= new ArrayList<String>();
		
		mCmdGetData = 0xA7;
	}
	
	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	
        super.onCreate(savedInstanceState);
        
        // Initialize UI
        setContentView(R.layout.activity_main);
        mTextViewStatus			= (TextView) findViewById(R.id.ID_STATUS);
        ArrayList<String> items = new ArrayList<String>();
        mSpinnerDevices			= (Spinner) findViewById(R.id.ID_PAIRED_DEVICES);
        mArrayAdapterDevices	= new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, items);
        mHandler				= new Handler(this);
        mSpinnerDevices.setOnItemSelectedListener(this);
        mArrayAdapterDevices.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mSpinnerDevices.setAdapter(mArrayAdapterDevices);
        
        /* Use the LocationManager class to obtain GPS locations */
        LocationManager locManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
        LocationListener locListener = new MyLocationListener();
        locManager.requestLocationUpdates( LocationManager.GPS_PROVIDER, 0, 0, locListener);
    }

    @Override
    protected void onResume() {
    	super.onResume();
    	
    	// update the paired device(s)
    	BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
    	Set<BluetoothDevice> devices = adapter.getBondedDevices();
    	mArrayAdapterDevices.clear();
    	mArrayListBluetoothAddress.clear();
    	if (devices.size() > 0) {
    		for (BluetoothDevice device : devices) {
    			mArrayAdapterDevices.add(device.getName());
    			mArrayListBluetoothAddress.add(device.getAddress());
    		}
    		
    		// request that the user selects a device
    		if (mBluetoothAddress == null) {
    			mSpinnerDevices.performClick();
    		}
    	} else {
    		mBluetoothAddress = null;
    	}
    	
    	UpdateUI();
    }
    
    @Override
    protected void onPause() {
    	mSPP.disconnect();
    	super.onPause();
    }
    
    @Override
    protected void onDestroy() {
    	super.onDestroy();
    }
    
    /*
     * Default actions bar
     */
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }
    
	/*
	 * Spinner callback
	 */

	public void onItemSelected(AdapterView<?> parent, View view, int pos, long id)
	{
		mBluetoothAddress = mArrayListBluetoothAddress.get(pos);
	}

	public void onNothingSelected(AdapterView<?> parent)
	{
		mBluetoothAddress = null;
	}    

	/*
	 * buttons
	 */

	public void onBluetoothSettings(View view)
	{
		Intent i = new Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS);
		startActivity(i);
	}

	public void onConnectLink(View view)
	{
		if(mIsThreadRunning == false)
		{
			mIsThreadRunning = true;
			UpdateUI();
			Thread t = new Thread(this);
			t.start();
		}
	}

	public void onDisconnectLink(View view)
	{
		mSPP.disconnect();
	}	
	
	/*
	 * GPS Listeners
	 */
	
    public class MyLocationListener implements LocationListener
    {
        TextView gpstv = (TextView) findViewById(R.id.textView1);

        @Override
        public void onLocationChanged(Location loc){
        //    Log.d("tag", "Finding Latitude");
            double lat = loc.getLatitude();
        //    Log.d("tag", "Lat: "+String.valueOf(lat));
        //    Log.d("tag", "Finding Longitude");
            double lon = loc.getLongitude();
        //    Log.d("tag", "Lon: "+String.valueOf(lon));
            String outText = "Latitude = " + lat + "\nLongitude = " + lon;

            // Display location
            gpstv.setText(outText);
        }

        @Override
        public void onProviderDisabled(String provider){
            Toast.makeText(getApplicationContext(), "Gps Disabled", Toast.LENGTH_SHORT ).show();
        }

        @Override
        public void onProviderEnabled(String provider){
            Toast.makeText(getApplicationContext(), "Gps Enabled", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras){

        }
    }
    
	/*
	 * main loop
	 */  
    public void run() {
    	Looper.prepare();
    	mSPP.connect(mBluetoothAddress);
    	while (mSPP.isConnected()) {
    		
    		// Request data
    		mSPP.writeByte(mCmdGetData);
    		mSPP.flush();
    		
    		// Retrieve data
    		mBigBump    = mSPP.readByte();
    		mBigBump   |= mSPP.readByte() << 8;
    		mSmallBump  = mSPP.readByte(); 
    		mSmallBump |= mSPP.readByte() << 8;
    		
    		if (mSPP.isError()) {
    			mSPP.disconnect();
    		}
    		
    		mHandler.sendEmptyMessage(0);
    		
    		// wait briefly before sending the next serial command
    		try { Thread.sleep((long) (1000.0F/30.0F)); }
    		catch (InterruptedException e) { Log.e(TAG, e.getMessage());}
    		
    	}
    	
        mBigBump			= 0;
        mSmallBump  		= 0;
        mIsThreadRunning 	= false;
        mHandler.sendEmptyMessage(0);
    }   
    
	/*
	 * update UI
	 */
    
    public boolean handleMessage (Message msg) {
    	// received update request from Bluetooth IO thread
    	UpdateUI();
    	return true;
    }
    
    private void UpdateUI() {
    	if (mSPP.isConnected()) {
    		mTextViewStatus.setText("connected to " + mSPP.getBluetoothAddress() + "\n" + 
    								"Small bumps detected\t: " + mSmallBump + "\n" + 
    								"Big bumps detected\t: " + mBigBump + "\n");
    	} else if (mIsThreadRunning) {
    		mTextViewStatus.setText("connecting to " + mBluetoothAddress);
    	} else {
    		mTextViewStatus.setText("disconnected");
    	}
    }
}