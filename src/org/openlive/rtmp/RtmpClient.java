package org.openlive.rtmp;

import java.nio.ByteBuffer;

import com.github.faucamp.simplertmp.RtmpHandler;

import android.media.MediaCodec;

/**
 * Created by Leo Ma on 2016/7/25.
 */
public class RtmpClient {

	private static RtmpClient instance;

	public static RtmpClient getInstance() {
		synchronized (RtmpClient.class) {
			if (instance == null) {
				instance = new RtmpClient();
			}
		}
		return instance;
	}

	private SrsFlvMuxer mFlvMuxer;

	public void setRtmpHandler(RtmpHandler handler) {
		mFlvMuxer = new SrsFlvMuxer(handler);
	}

	private RtmpClient() {

	}

	public void startPublish(String rtmpUrl) {
		if (mFlvMuxer != null) {
			mFlvMuxer.start(rtmpUrl);
			mFlvMuxer.setVideoResolution(1280, 720);
		}
	}

	public void stopPublish() {
		if (mFlvMuxer != null) {
			mFlvMuxer.stop();
		}
	}

	public void setVideoResolution(int width, int height) {
		if (mFlvMuxer != null) {
			mFlvMuxer.setVideoResolution(width, height);
		}
	}

	public void publishVideoSample(ByteBuffer byteBuf, MediaCodec.BufferInfo bufferInfo) {
		if (mFlvMuxer != null) {
			mFlvMuxer.writeSampleData(SrsFlvMuxer.VIDEO_TRACK, byteBuf, bufferInfo);
		}
	}

	public void publishAudioSample(ByteBuffer byteBuf, MediaCodec.BufferInfo bufferInfo) {
		if (mFlvMuxer != null) {
			mFlvMuxer.writeSampleData(SrsFlvMuxer.AUDIO_TRACK, byteBuf, bufferInfo);
		}
	}

}
