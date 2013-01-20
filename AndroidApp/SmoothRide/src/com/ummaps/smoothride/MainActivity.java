package com.ummaps.smoothride;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Set;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

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
import android.os.StrictMode;
import android.util.Base64;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
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
	private TextView			tvTextViewSent;
	private Spinner				mSpinnerDevices;
	private ArrayAdapter		mArrayAdapterDevices;
	private Handler				mHandler;
	
	// Arduino commands and data
	private int					mCmdGetData;
	private int					mSmallBump;
	private int					mBigBump;
	private int					mSmallBumpThresh;
	private int					mBigBumpThresh;
	private double 				distTrvld;
	private double 				tempDistTrvld;
	private String 				responseText;
	
	public MainActivity() {
		mIsThreadRunning			= false;
		mBluetoothAddress			= null;
		mSPP						= new BlueSmirfSPP();
		mArrayListBluetoothAddress	= new ArrayList<String>();
		
		mCmdGetData = 0x64; // ASCI 'd'
		mSmallBumpThresh = 25;
		mBigBumpThresh = 5;
	}
	
	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	
        super.onCreate(savedInstanceState);
        
        // Initialize UI
        setContentView(R.layout.activity_main);
        mTextViewStatus			= (TextView) findViewById(R.id.ID_STATUS);
        tvTextViewSent			= (TextView) findViewById(R.id.tvtextview);
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
        
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy); 
        
        final MainActivity current = this;
        final Button button = (Button) findViewById(R.id.buttonmap);
        button.setOnClickListener(new View.OnClickListener() {
        	public void onClick(View v) {
        		Intent i = new Intent(current, AboutActivity.class);  
        		startActivityForResult(i, 1);
        	}
        });
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
        double latStart, lonStart;
        boolean reachedChkPt = true;
        
        @Override
        public void onLocationChanged(Location loc){

            double lat = loc.getLatitude();
            double lon = loc.getLongitude();
            
            
            
            // After traveling distance, check to see if map should be updated
            if (reachedChkPt) {
            	latStart = lat;
            	lonStart = lon;
            	
            	reachedChkPt = false;
            	
            	if (mBigBump > mBigBumpThresh) {
            		mBigBump = 0;
            		
            		mHandler.sendEmptyMessage(0);
            		postData(lat, lon, 1);
            	}
            	
            	if (mSmallBump > mSmallBumpThresh) {
            		mSmallBump = 0;
            		
            		mHandler.sendEmptyMessage(0);
            		postData(lat, lon, 0);
            	}
            }
            
            tempDistTrvld = LatLong2km(lat, lon, latStart, lonStart);
            if (tempDistTrvld > 0.005) {
            	reachedChkPt = true;
            	distTrvld = tempDistTrvld;
            }
            
            String outText = "Latitude = " + lat + "\nLongitude = " + lon;

            // Display location
            gpstv.setText(outText);
        }
        
        public void postData(double lat, double lon, int howlarge) {
            // Create a new HttpClient and Post Header
            HttpClient httpclient = new DefaultHttpClient();
            HttpPost httppost = new HttpPost("https://smoothride.crowdmap.com/api?report");
            Calendar rightNow = Calendar.getInstance();

            try {
                // Add your data
                List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
                String blankstring = "";
//                nameValuePairs.add(new BasicNameValuePair("latitude", blankstring + lat));
//                nameValuePairs.add(new BasicNameValuePair("longitude", blankstring + lon));
//                nameValuePairs.add(new BasicNameValuePair("country_name", "United States"));
//                nameValuePairs.add(new BasicNameValuePair("incident_title","Pothole"));
//                nameValuePairs.add(new BasicNameValuePair("incident_description",blankstring + howlarge));
//                nameValuePairs.add(new BasicNameValuePair("incident_date",blankstring + Calendar.DATE + "/" + Calendar.MONTH + "/" + Calendar.YEAR));
//                nameValuePairs.add(new BasicNameValuePair("incident_hour",blankstring + Calendar.HOUR));
//                nameValuePairs.add(new BasicNameValuePair("incident_minute",blankstring + Calendar.MINUTE));
//                nameValuePairs.add(new BasicNameValuePair("incident_ampm",blankstring + Calendar.AM_PM));
//                nameValuePairs.add(new BasicNameValuePair("incident_category", blankstring + howlarge));
//                nameValuePairs.add(new BasicNameValuePair("location_name", "Pothole"));
//                nameValuePairs.add(new BasicNameValuePair("task", "report"));
//                httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
                
                nameValuePairs.add(new BasicNameValuePair("task", "report"));
                nameValuePairs.add(new BasicNameValuePair("latitude", blankstring + lat));
                nameValuePairs.add(new BasicNameValuePair("longitude", blankstring + lon));
                nameValuePairs.add(new BasicNameValuePair("country_name", "United States"));
                nameValuePairs.add(new BasicNameValuePair("incident_title","Pothole"));
                nameValuePairs.add(new BasicNameValuePair("incident_description","From SmoothRide"));
                nameValuePairs.add(new BasicNameValuePair("incident_date","01/19/2013"));
                nameValuePairs.add(new BasicNameValuePair("incident_hour", blankstring + Calendar.HOUR));
                nameValuePairs.add(new BasicNameValuePair("incident_minute", blankstring + Calendar.MINUTE));
                nameValuePairs.add(new BasicNameValuePair("incident_ampm", "am"));
                nameValuePairs.add(new BasicNameValuePair("incident_category", blankstring + (howlarge + 1)));
                nameValuePairs.add(new BasicNameValuePair("location_name", "Philly"));
                
                httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
                
                tvTextViewSent.setText(blankstring + httppost);
                
                httppost.setHeader("Authorization", "Basic " + Base64.encodeToString("aozgaa@umich.edu:Aichwaigror=".getBytes(), Base64.NO_WRAP));
                
                // Execute HTTP Post Request
                HttpResponse response = httpclient.execute(httppost);
                
                HttpEntity entity = response.getEntity();
                responseText = EntityUtils.toString(entity);
                
                mHandler.sendEmptyMessage(0);
                
            } catch (ClientProtocolException e) {
            } catch (IOException e) {
            }
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
    
    // from http://www.movable-type.co.uk/scripts/latlong.html
    private double LatLong2km(double lat1, double lon1, double lat2, double lon2) {
    	double R = 6371.0f; // km
    	double dLat = Math.toRadians(lat2-lat1);
    	double dLon = Math.toRadians(lon2-lon1);
    	lat1 = Math.toRadians(lat1);
    	lat2 = Math.toRadians(lat2);
    	
    	double a = Math.sin(dLat/2) * Math.sin(dLat/2) + 
    			   Math.sin(dLon/2) * Math.sin(dLon/2) * Math.cos(lat1) * Math.cos(lat2);
    	double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
    	double d = R * c;
    	
    	return d;
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
    		int tempBigBump    = mSPP.readByte();
    		tempBigBump   |= mSPP.readByte() << 8;
    		int tempSmallBump  = mSPP.readByte(); 
    		tempSmallBump |= mSPP.readByte() << 8;
    		
    		mBigBump 	+= tempBigBump;
    		mSmallBump	+= tempSmallBump;
    		
    		if (mSPP.isError()) {
    			mSPP.disconnect();
    		} else {
    			
    		}
    		
    		mHandler.sendEmptyMessage(0);
    		
    		// wait briefly before sending the next serial command
    		try { Thread.sleep((long) (100000.0F/30.0F)); }
    		catch (InterruptedException e) { Log.e(TAG, e.getMessage());}
    		
    	}
    	
        mBigBump			= 0;
        mSmallBump  		= 0;
        mIsThreadRunning 	= false;
        mHandler.sendEmptyMessage(0);
    }   
    
    /*
     * Get data from Arduino
     */
    
    
    
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
    		mTextViewStatus.setText("Connected to " + mSPP.getBluetoothAddress() + "\n" + 
    								"Small bumps detected: " + mSmallBump + "\n" + 
    								"Big bumps detected: " + mBigBump + "\n" + 
    								"Distance since last checkpoint: " + tempDistTrvld + "\n" + 
    								"POST: " + responseText);
    	} else if (mIsThreadRunning) {
    		mTextViewStatus.setText("connecting to " + mBluetoothAddress);
    	} else {
    		mTextViewStatus.setText("disconnected");
    	}
    	
    	int aaa = 1;
    }
}