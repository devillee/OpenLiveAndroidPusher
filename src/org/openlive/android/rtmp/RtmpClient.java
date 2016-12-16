package org.openlive.android.rtmp;

import android.util.Log;

public class RtmpClient {

	public static final int TYPE_VIDEO = 0;
	public static final int TYPE_AUDIO = 1;
	
	static {
		try {
			System.loadLibrary("rtmp");
		} catch (UnsatisfiedLinkError ule) {
			Log.d("librtmp", ule.getMessage());
		}

	}

	public static native int open(String url, boolean isPublishMode, int width, int height);

	public static native int read(byte[] data, int offset, int size);

	public static native int write(byte[] data, int size, int type, int ts);

	public static native int close();
}
