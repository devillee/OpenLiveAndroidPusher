package org.openlive.android;

import java.io.IOException;
import java.net.SocketException;

import org.openlive.rtmp.RtmpClient;

import com.github.faucamp.simplertmp.RtmpHandler;
import com.github.faucamp.simplertmp.RtmpHandler.RtmpListener;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

public class MainActivity extends Activity implements RtmpListener {
	private HWH264Encoder videoEncoder;
	private HWAACEncoder audioEncoder;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		RtmpClient.getInstance().setVideoResolution(1280, 720);
		RtmpClient.getInstance().setRtmpHandler(new RtmpHandler(this));
		RtmpClient.getInstance().startPublish("rtmp://112.74.98.168:1935/live/test");

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
		
		RtmpClient.getInstance().stopPublish();
		
		if (videoEncoder != null) {
			videoEncoder.stopEncoder();
		}
		if (audioEncoder != null) {
			audioEncoder.stopEncoder();
		}
		
	}

	@Override
	public void onRtmpConnecting(String msg) {
		Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
	}

	@Override
	public void onRtmpConnected(String msg) {
		Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
	}

	@Override
	public void onRtmpVideoStreaming() {

	}

	@Override
	public void onRtmpAudioStreaming() {
	}

	@Override
	public void onRtmpStopped() {
		Toast.makeText(getApplicationContext(), "onRtmpStopped", Toast.LENGTH_SHORT).show();
	}

	@Override
	public void onRtmpDisconnected() {
		Toast.makeText(getApplicationContext(), "onRtmpDisconnected", Toast.LENGTH_SHORT).show();
	}

	@Override
	public void onRtmpVideoFpsChanged(double fps) {

	}

	@Override
	public void onRtmpVideoBitrateChanged(double bitrate) {

	}

	@Override
	public void onRtmpAudioBitrateChanged(double bitrate) {

	}

	@Override
	public void onRtmpSocketException(SocketException e) {

	}

	@Override
	public void onRtmpIOException(IOException e) {

	}

	@Override
	public void onRtmpIllegalArgumentException(IllegalArgumentException e) {

	}

	@Override
	public void onRtmpIllegalStateException(IllegalStateException e) {

	}
}
