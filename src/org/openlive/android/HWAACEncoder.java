package org.openlive.android;

import java.nio.ByteBuffer;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.media.MediaRecorder;
import android.os.SystemClock;
import android.util.Log;

public class HWAACEncoder implements Runnable {
	private static final String TAG = "HWAACEncoder";
	private static final String MIME_TYPE = "audio/mp4a-latm";
	public static final int SAMPLE_RATE = 44100;
	public static final int BIT_RATE = 128000;
	public static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;
	public static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
	public static final int FRAMES_PER_BUFFER = 24; // 1 sec @ 1024
													// samples/frame (aac)

	private AudioRecord audioRecoder;
	private int bufferSize;
	private MediaCodec mMediaCodec;
	private boolean isRun = false;
	private int bufferRead;
	private byte[] samples;
	private Thread thread;

	public void startEncoder() {
		isRun = true;
		thread = new Thread(this);
		thread.start();
	}

	public void stopEncoder() {
		isRun = false;
		if (mMediaCodec != null) {
			mMediaCodec.stop();
			mMediaCodec.release();
			mMediaCodec = null;
		}
		if (audioRecoder != null) {
			audioRecoder.stop();
			audioRecoder.release();
			audioRecoder = null;
		}

		if (thread != null) {
			try {
				thread.interrupt();
				thread.join();
				thread = null;
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	@Override
	public void run() {
		initAudioCodec();
		while (isRun) {
			if (audioRecoder != null) {
				bufferRead = audioRecoder.read(samples, 0, bufferSize);
				if (bufferRead == AudioRecord.ERROR_BAD_VALUE || bufferRead == AudioRecord.ERROR_INVALID_OPERATION) {
					Log.e(TAG, "Read error");
				}
				if (bufferRead > 0) {
					Log.e(TAG, "bufferRead len:" + bufferRead);
					encodeAudioFrame(samples);
				}
			}
		}
	}

	private void initAudioCodec() {
		bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT);
		samples = new byte[bufferSize];

		MediaFormat format = new MediaFormat();
		format.setInteger(MediaFormat.KEY_BIT_RATE, BIT_RATE);
		format.setInteger(MediaFormat.KEY_CHANNEL_COUNT, 1);
		format.setInteger(MediaFormat.KEY_SAMPLE_RATE, SAMPLE_RATE);
		format.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, bufferSize);

		mMediaCodec = MediaCodec.createEncoderByType(MIME_TYPE);
		mMediaCodec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
		mMediaCodec.start();

		audioRecoder = new AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT,
				bufferSize);
		audioRecoder.startRecording();
	}

	private void encodeAudioFrame(byte[] data) {
		ByteBuffer[] inBuffers = mMediaCodec.getInputBuffers();
		ByteBuffer[] outBuffers = mMediaCodec.getOutputBuffers();
		long timeStamp = SystemClock.uptimeMillis();

		int inBufferIndex = mMediaCodec.dequeueInputBuffer(-1);
		if (inBufferIndex >= 0) {
			ByteBuffer inputBuffer = inBuffers[inBufferIndex];
			inputBuffer.clear();
			inputBuffer.put(data, 0, data.length);
			mMediaCodec.queueInputBuffer(inBufferIndex, 0, data.length, timeStamp, 0);
		}

		int i = 0;
		byte[] buffers = new byte[1024];
		MediaCodec.BufferInfo mBufferInfo = new MediaCodec.BufferInfo();
		int outBufferIndex = mMediaCodec.dequeueOutputBuffer(mBufferInfo, 0);
		while (outBufferIndex >= 0) {
			ByteBuffer outputBuffer = outBuffers[outBufferIndex];
			byte[] outData = new byte[mBufferInfo.size];
			outputBuffer.get(outData);

			System.arraycopy(outData, 0, buffers, i, outData.length);
			i += outData.length;

			mMediaCodec.releaseOutputBuffer(outBufferIndex, false);
			outBufferIndex = mMediaCodec.dequeueOutputBuffer(mBufferInfo, 0);
		}

		if (i > 0) {
			byte[] sendData = new byte[i];
			System.arraycopy(buffers, 0, sendData, 0, i);
			Log.i(TAG, "sendData len:" + sendData.length);
		}

	}
}
