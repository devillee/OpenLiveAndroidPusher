package org.openlive.android;

import java.nio.ByteBuffer;

import org.openlive.android.rtmp.RtmpClient;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
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
	private MediaCodec mAudioEncoder;
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
		if (mAudioEncoder != null) {
			mAudioEncoder.stop();
			mAudioEncoder.release();
			mAudioEncoder = null;
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
					// Log.e(TAG, "bufferRead len:" + bufferRead);
					encodeAndSendAudioFrame(samples);
				}
			}
		}
	}

	private void initAudioCodec() {
		bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT);
		samples = new byte[bufferSize];
		MediaFormat audioFormat = new MediaFormat();
		audioFormat.setString(MediaFormat.KEY_MIME, MIME_TYPE);
		audioFormat.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);
		audioFormat.setInteger(MediaFormat.KEY_SAMPLE_RATE, SAMPLE_RATE);
		audioFormat.setInteger(MediaFormat.KEY_CHANNEL_COUNT, 1);
		audioFormat.setInteger(MediaFormat.KEY_BIT_RATE, BIT_RATE);
		audioFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 16384);

		mAudioEncoder = MediaCodec.createEncoderByType(MIME_TYPE);
		mAudioEncoder.configure(audioFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
		mAudioEncoder.start();

		audioRecoder = new AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT,
				bufferSize);
		audioRecoder.startRecording();
	}

	private void encodeAndSendAudioFrame(byte[] data) {
		try {
			ByteBuffer[] inputBuffers = mAudioEncoder.getInputBuffers();
			ByteBuffer[] outputBuffers = mAudioEncoder.getOutputBuffers();
			int inputBufferIndex = mAudioEncoder.dequeueInputBuffer(-1);
			if (inputBufferIndex >= 0) {
				ByteBuffer inputBuffer = inputBuffers[inputBufferIndex];
				inputBuffer.clear();
				inputBuffer.put(data);
				mAudioEncoder.queueInputBuffer(inputBufferIndex, 0, data.length, 0, 0);
			}
			MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
			int outputBufferIndex = mAudioEncoder.dequeueOutputBuffer(bufferInfo, 0);
			// // trying to add a ADTS
			// while (outputBufferIndex >= 0) {
			// int outBitsSize = bufferInfo.size;
			// int outPacketSize = outBitsSize + 7; // 7 is ADTS size
			// ByteBuffer outputBuffer = outputBuffers[outputBufferIndex];
			//
			// outputBuffer.position(bufferInfo.offset);
			// outputBuffer.limit(bufferInfo.offset + outBitsSize);
			//
			// byte[] outData = new byte[outPacketSize];
			// addADTStoPacket(outData, outPacketSize);
			//
			// outputBuffer.get(outData, 7, outBitsSize);
			// outputBuffer.position(bufferInfo.offset);
			//
			// Log.e(TAG, outData.length + " bytes written");
			// RtmpClient.write(outData, outData.length, RtmpClient.TYPE_AUDIO,
			// presentationTimeUs2ts(System.currentTimeMillis()));
			//
			// Log.e("AudioEncoder", outData.length + " bytes written");
			//
			// mAudioEncoder.releaseOutputBuffer(outputBufferIndex, false);
			// outputBufferIndex = mAudioEncoder.dequeueOutputBuffer(bufferInfo,
			// 0);
			//
			// }

			// Without ADTS header
			while (outputBufferIndex >= 0) {
				ByteBuffer outputBuffer = outputBuffers[outputBufferIndex];
				byte[] outData = new byte[bufferInfo.size];
				outputBuffer.get(outData);
				//Log.e(TAG, outData.length + " bytes written");
				RtmpClient.writeAudio(outData, outData.length);
				baseTime = System.currentTimeMillis();
				mAudioEncoder.releaseOutputBuffer(outputBufferIndex, false);
				outputBufferIndex = mAudioEncoder.dequeueOutputBuffer(bufferInfo, 0);
			}
		} catch (Throwable t) {
			t.printStackTrace();
		}

	}

	/**
	 * Add ADTS header at the beginning of each and every AAC packet. This is
	 * needed as MediaCodec encoder generates a packet of raw AAC data.
	 *
	 * Note the packetLen must count in the ADTS header itself.
	 **/
	private void addADTStoPacket(byte[] packet, int packetLen) {
		int profile = 2; // AAC LC
		// 39=MediaCodecInfo.CodecProfileLevel.AACObjectELD;
		int freqIdx = 4; // 44.1KHz
		int chanCfg = 1; // CPE
		// fill in ADTS data
		packet[0] = (byte) 0xFF;
		packet[1] = (byte) 0xF9;
		packet[2] = (byte) (((profile - 1) << 6) + (freqIdx << 2) + (chanCfg >> 2));
		packet[3] = (byte) (((chanCfg & 3) << 6) + (packetLen >> 11));
		packet[4] = (byte) ((packetLen & 0x7FF) >> 3);
		packet[5] = (byte) (((packetLen & 7) << 5) + 0x1F);
		packet[6] = (byte) 0xFC;
	}

	private long baseTime = 0;
	private int ts;

	int presentationTimeUs2ts(long time) {
		// 16777215
		if (baseTime == 0) {
			baseTime = time;
			return 0;
		}
		ts += (int) (time - baseTime);
		return ts;
	}

}
