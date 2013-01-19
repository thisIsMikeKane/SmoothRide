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

/*
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
*/


public class MainActivity extends Activity {
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        /* Use the LocationManager class to obtain GPS locations */
        LocationManager locManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
        LocationListener locListener = new MyLocationListener();
        locManager.requestLocationUpdates( LocationManager.GPS_PROVIDER, 0, 0, locListener);
    }

   /* To test in the emulator, use the telnet commands:
    * telnet localhost 5554
    * geo fix 30.0 30.0 */

    /* Class My Location Listener */

    public class MyLocationListener implements LocationListener
    {
        TextView tv = (TextView) findViewById(R.id.textView1);

        @Override
        public void onLocationChanged(Location loc){
            Log.d("tag", "Finding Latitude");
            double lat = loc.getLatitude();
            Log.d("tag", "Lat: "+String.valueOf(lat));
            Log.d("tag", "Finding Longitude");
            double lon = loc.getLongitude();
            Log.d("tag", "Lon: "+String.valueOf(lon));
            String Text = "My current location is: " +
            "\nLatitude = " + lat +
            "\nLongitude = " + lon;

            // Display location
            tv.setText(Text);
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
}