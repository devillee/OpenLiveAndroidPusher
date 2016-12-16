package org.openlive.android;

import org.openlive.android.rtmp.RtmpClient;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

public class MainActivity extends Activity {
	private HWH264Encoder videoEncoder;
	private HWAACEncoder audioEncoder;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		RtmpClient.connect("rtmp://112.74.98.168:1935/hls/test", true, 1920, 1080);
		
		videoEncoder = new HWH264Encoder();
		videoEncoder.startEncoder();

		audioEncoder = new HWAACEncoder();
		audioEncoder.startEncoder();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (videoEncoder != null) {
			videoEncoder.stopEncoder();
		}
	}
}
