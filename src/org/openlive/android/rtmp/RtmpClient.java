package org.openlive.android.rtmp;

import android.util.Log;

public class RtmpClient {

	public static final int TYPE_VIDEO = 0;
	public static final int TYPE_AUDIO = 1;

	private static int connect = -1;

	static {
		try {
			System.loadLibrary("rtmp");
		} catch (UnsatisfiedLinkError ule) {
			Log.d("librtmp", ule.getMessage());
		}

	}

	public static void connect(String url, boolean isPublishMode, int width, int height) {
		connect = open(url, isPublishMode, width, height);
	}

	public static int writeVideo(byte[] data, int size, int ts) {
		synchronized (RtmpClient.class) {
			if (connect > 0)
				return write(data, size, TYPE_VIDEO, ts);
		}
		return -1;
	}

	public static int writeAudio(byte[] data, int size, int ts) {
		synchronized (RtmpClient.class) {
			if (connect > 0)
				return write(data, size, TYPE_AUDIO, ts);
		}
		return -1;
	}

	public void disconnect() {
		close();
	}

	private native static int open(String url, boolean isPublishMode, int width, int height);

	private native static int read(byte[] data, int offset, int size);

	private native static int write(byte[] data, int size, int type, int ts);

	private native static int close();
}
