package com.ummaps.smoothride;

import android.content.Context;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.app.Activity;
import android.view.Menu;
import android.location.Location;
import android.location.LocationListener;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;


public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        LocationManager locManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
//        LocationListener locListener = new LocationListener();
//        LocationProvider provider = locManager.getProvider(LocationManager.GPS_PROVIDER);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }
    
    LocationListener locListener = new LocationListener() {
    	TextView latlongtv = (TextView) findViewById(R.id.textView1);
    	
    	@Override
    	public void onLocationChanged(Location loc){
    		double latitude = loc.getLatitude();
    		double longitude = loc.getLongitude();
    		String tvtext = "Lat = " + latitude + "\nLong = " + longitude;
    		latlongtv.setText(tvtext);
    	}

		@Override
		public void onProviderDisabled(String provider) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void onProviderEnabled(String provider) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void onStatusChanged(String provider, int status, Bundle extras) {
			// TODO Auto-generated method stub
			
		}
    }; 
    
 //   locManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListner);)
    
 }

