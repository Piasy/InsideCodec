package com.github.piasy.mediacodecrctest;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.opengl.GLES20;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.view.Surface;
import com.tencent.mars.xlog.Log;
import java.io.IOException;
import java.nio.ByteBuffer;
import org.webrtc.EglBase;
import org.webrtc.GlRectDrawer;
import org.webrtc.VideoRenderer;

/**
 * Created by Piasy{github.com/Piasy} on 03/08/2017.
 */

public class EncoderWrapper extends MediaCodec.Callback {
    private static final String TAG = "EncoderWrapper";
    private static final int TIMEOUT_US = 3_000;
    private static final int RC_INTERVAL = 5_000;

    private final HandlerThread mHandlerThread;
    private final Handler mHandler;
    private final Config mConfig;
    private final MediaCodec.BufferInfo mBufferInfo = new MediaCodec.BufferInfo();
    private final Bundle mParams = new Bundle();

    private final Handler mUiHandler = new Handler(Looper.getMainLooper());
    private final RcTest.Notifier mNotifier;

    private EglBase mEglBase;
    private GlRectDrawer mDrawer;
    private MediaCodec mEncoder;
    private Surface mEncoderSurface;
    private volatile boolean mRunning;

    private int mRcSign = 1;
    private int mCurrentBr;
    private final Runnable mRcRunnable = new Runnable() {
        @Override
        public void run() {
            if (mCurrentBr > mConfig.initBr() * 2) {
                mRcSign = -1;
            } else if (mCurrentBr < mConfig.initBr()) {
                mRcSign = 1;
            }
            mCurrentBr += mRcSign * mConfig.brStep();

            mParams.clear();
            mParams.putInt(MediaCodec.PARAMETER_KEY_VIDEO_BITRATE, mCurrentBr * 1000);
            mEncoder.setParameters(mParams);
            Log.i(TAG, "update bitrate %d", mCurrentBr);

            mUiHandler.postDelayed(this, RC_INTERVAL);
        }
    };
    private volatile int mOutputBits;
    private long mLastResetBitsTime;

    public EncoderWrapper(final Config config, final RcTest.Notifier notifier) {
        mNotifier = notifier;
        Log.i(TAG, "Test config: " + config);

        mConfig = config;
        mHandlerThread = new HandlerThread("EncoderThread");
        mHandlerThread.start();
        mHandler = new Handler(mHandlerThread.getLooper());
    }

    public void start(final EglBase eglBase) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    mEglBase = EglBase.create(eglBase.getEglBaseContext(),
                            EglBase.CONFIG_RECORDABLE);

                    mEncoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC);
                    MediaFormat encodeFormat = MediaFormat.createVideoFormat(
                            MediaFormat.MIMETYPE_VIDEO_AVC,
                            mConfig.outputWidth(), mConfig.outputHeight());
                    encodeFormat.setInteger(MediaFormat.KEY_BIT_RATE, mConfig.initBr() * 1000);
                    encodeFormat.setInteger(MediaFormat.KEY_FRAME_RATE, mConfig.outputFps());
                    encodeFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL,
                            mConfig.outputKeyFrameInterval());
                    encodeFormat.setInteger(MediaFormat.KEY_BITRATE_MODE, mConfig.brMode());
                    if (mConfig.brMode() == MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_CQ) {
                        encodeFormat.setInteger("quality", mConfig.quality());
                    }
                    encodeFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT,
                            MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
                    if (mConfig.asyncEnc()) {
                        mEncoder.setCallback(EncoderWrapper.this);
                    }
                    mEncoder.configure(encodeFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
                    Log.i(TAG, "VideoFormat encoder " + encodeFormat);

                    mEncoderSurface = mEncoder.createInputSurface();

                    mEncoder.start();

                    mCurrentBr = mConfig.initBr();

                    mEglBase.createSurface(mEncoderSurface);
                    mEglBase.makeCurrent();
                    mDrawer = new GlRectDrawer();
                    mLastResetBitsTime = System.currentTimeMillis();

                    if (!mConfig.asyncEnc()) {
                        startOutputThread();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        if (mConfig.updateBr()) {
            mUiHandler.postDelayed(mRcRunnable, RC_INTERVAL);
        }
    }

    private void startOutputThread() {
        mRunning = true;

        new Thread(new Runnable() {
            @Override
            public void run() {
                while (mRunning) {
                    int index = mEncoder.dequeueOutputBuffer(mBufferInfo, TIMEOUT_US);
                    if (index < 0) {
                        continue;
                    }

                    ByteBuffer buffer = mEncoder.getOutputBuffer(index);
                    if (buffer != null) {
                        reportEncodedImage(mBufferInfo, buffer);
                    }
                    mEncoder.releaseOutputBuffer(index, false);
                }
            }
        }).start();
    }

    public void encodeFrame(final VideoRenderer.I420Frame frame) {
        if (mEncoder == null) {
            return;
        }

        mHandler.post(new Runnable() {
            @Override
            public void run() {
                mEglBase.makeCurrent();
                GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
                mDrawer.drawOes(frame.textureId, frame.samplingMatrix, frame.width, frame.height, 0,
                        0, frame.width, frame.height);
                mEglBase.swapBuffers(frame.timestamp);
            }
        });
    }

    public void stop() {
        mUiHandler.removeCallbacks(mRcRunnable);
        mRunning = false;
        mHandlerThread.quitSafely();
    }

    @Override
    public void onInputBufferAvailable(@NonNull final MediaCodec codec, final int index) {
    }

    @Override
    public void onOutputBufferAvailable(@NonNull final MediaCodec codec, final int index,
            @NonNull final MediaCodec.BufferInfo info) {
        ByteBuffer buffer = codec.getOutputBuffer(index);
        if (buffer != null) {
            reportEncodedImage(info, buffer);
        }
        codec.releaseOutputBuffer(index, false);
    }

    @Override
    public void onError(@NonNull final MediaCodec codec,
            @NonNull final MediaCodec.CodecException e) {
    }

    @Override
    public void onOutputFormatChanged(@NonNull final MediaCodec codec,
            @NonNull final MediaFormat format) {
    }

    private void reportEncodedImage(final MediaCodec.BufferInfo info, final ByteBuffer buffer) {
        buffer.position(info.offset);
        buffer.limit(info.size);

        if ((info.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) == 0) {
            Log.i(TAG, "reportEncodedImage %d %d %d",
                    info.flags & MediaCodec.BUFFER_FLAG_KEY_FRAME, info.size,
                    info.presentationTimeUs);
            if (System.currentTimeMillis() - mLastResetBitsTime > 1000) {
                mNotifier.reportBr(mOutputBits);
                mOutputBits = 0;
                mLastResetBitsTime = System.currentTimeMillis();
            }
            mOutputBits += info.size * 8;
        }
    }
}
