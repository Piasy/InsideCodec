package com.github.piasy.mediacodecrctest;

import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.view.Surface;
import com.tencent.mars.xlog.Log;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import org.webrtc.EglBase;
import org.webrtc.SurfaceTextureHelper;

/**
 * Created by Piasy{github.com/Piasy} on 03/08/2017.
 */

public class FrameProducer implements SurfaceTextureHelper.OnTextureFrameAvailableListener {
    private static final String TAG = "FrameProducer";
    private static final int TIMEOUT_US = 10_000;

    private final File mVideoFile;
    private final int mFps;
    private final Callback mCallback;
    private final MediaCodec.BufferInfo mBufferInfo;
    private final SurfaceTextureHelper mSurfaceTextureHelper;

    private Surface mHubSurface;
    private MediaExtractor mExtractor;
    private MediaCodec mDecoder;
    private volatile boolean mRunning;

    public FrameProducer(final EglBase eglBase, final File videoFile, final int fps,
            final Callback callback) {
        mVideoFile = videoFile;
        mFps = fps;
        mCallback = callback;
        mBufferInfo = new MediaCodec.BufferInfo();

        mSurfaceTextureHelper = SurfaceTextureHelper.create("SurfaceTextureHelper",
                eglBase.getEglBaseContext());
        mSurfaceTextureHelper.startListening(this);
        mHubSurface = new Surface(mSurfaceTextureHelper.getSurfaceTexture());
    }

    public void start() {
        mRunning = true;
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    mExtractor = new MediaExtractor();
                    mExtractor.setDataSource(mVideoFile.toString());
                    int videoTrack = selectVideoTrack(mExtractor);
                    if (videoTrack < 0) {
                        Log.e(TAG, "selectVideoTrack fail");
                        return;
                    }
                    mExtractor.selectTrack(videoTrack);

                    MediaFormat videoFormat = mExtractor.getTrackFormat(videoTrack);

                    mDecoder = MediaCodec.createDecoderByType(
                            videoFormat.getString(MediaFormat.KEY_MIME));
                    mDecoder.configure(videoFormat, mHubSurface, null, 0);

                    mDecoder.start();

                    mCallback.onReady(videoFormat);

                    produce();
                } catch (IOException e) {
                    Log.printErrStackTrace(TAG, e, "start FrameProducer");
                } finally {
                    if (mExtractor != null) {
                        mExtractor.release();
                    }
                    if (mDecoder != null) {
                        mDecoder.stop();
                        mDecoder.release();
                    }
                    mSurfaceTextureHelper.dispose();
                    mCallback.onStop();
                }
            }
        }).start();
    }

    public void stop() {
        mRunning = false;
    }

    private void produce() {
        final long interval = 1000 / mFps;
        boolean running = true;
        long lastTs = System.currentTimeMillis();
        while (running && mRunning) {
            lastTs += interval;
            running = produceOneFrame();
            while (System.currentTimeMillis() < lastTs) {
                try {
                    long sleepTime = lastTs - System.currentTimeMillis();
                    Log.i(TAG, "sleep " + sleepTime);
                    Thread.sleep(sleepTime);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private boolean produceOneFrame() {
        int decoderInputBufIndex = mDecoder.dequeueInputBuffer(TIMEOUT_US);
        if (decoderInputBufIndex < 0) {
            Log.i(TAG, "decoder input buffer not available");
            return true;
        }
        ByteBuffer inputBuf = mDecoder.getInputBuffer(decoderInputBufIndex);
        if (inputBuf == null) {
            Log.i(TAG, "decoder input buffer null");
            return true;
        }
        Log.i(TAG, "decoder input buf: %d", decoderInputBufIndex);

        int chunkSize = mExtractor.readSampleData(inputBuf, 0);
        if (chunkSize < 0) {
            Log.i(TAG, "extractor EOS reached");
            mDecoder.queueInputBuffer(decoderInputBufIndex, 0, 0, 0,
                    MediaCodec.BUFFER_FLAG_END_OF_STREAM);
            return false;
        }
        long presentationTimeUs = mExtractor.getSampleTime();
        Log.i(TAG, "extract frame: %d %d", chunkSize, presentationTimeUs);

        mDecoder.queueInputBuffer(decoderInputBufIndex, 0, chunkSize, presentationTimeUs, 0);
        mExtractor.advance();

        while (true) {
            int decoderOutputBufIndex = mDecoder.dequeueOutputBuffer(mBufferInfo, TIMEOUT_US);
            if (decoderOutputBufIndex < 0) {
                break;
            }
            mDecoder.releaseOutputBuffer(decoderOutputBufIndex, true);
        }

        return true;
    }

    private int selectVideoTrack(MediaExtractor extractor) {
        // Select the first video track we find, ignore the rest.
        int numTracks = extractor.getTrackCount();
        for (int i = 0; i < numTracks; i++) {
            MediaFormat format = extractor.getTrackFormat(i);
            String mime = format.getString(MediaFormat.KEY_MIME);
            if (mime.startsWith("video/")) {
                Log.i(TAG, "Extractor selected track " + i + " (" + mime + "): " + format);
                return i;
            }
        }

        return -1;
    }

    @Override
    public void onTextureFrameAvailable(int oesTextureId, float[] transformMatrix,
            long timestampNs) {
        Log.i(TAG, "decode frame: %d", timestampNs / 1000);
        mCallback.onFrame(oesTextureId, transformMatrix, timestampNs);
        mSurfaceTextureHelper.returnTextureFrame();
    }

    public interface Callback {
        void onReady(MediaFormat mediaFormat);

        void onFrame(int oesTextureId, float[] transformMatrix, long timestampNs);

        void onStop();
    }
}
