package com.ummaps.smoothride;

import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.view.Menu;
import android.view.View;
import android.webkit.WebView;
import android.widget.Button;

public class AboutActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_about);
		WebView myWebView = (WebView) findViewById(R.id.webview1);
		myWebView.loadUrl("http://www.umich.edu/~bmargosi/smoothride/index.html");
		setContentView(R.layout.activity_about);

		final AboutActivity current = this;
        final Button button = (Button) findViewById(R.id.button1);
        button.setOnClickListener(new View.OnClickListener() {
        	public void onClick(View v) {
        		Intent i = new Intent(current, MainActivity.class);  
        		startActivityForResult(i, 2);
        	}
        });
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_about, menu);
		return true;
	}
	
}
