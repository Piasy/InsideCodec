package com.github.piasy.mediacodecrctest;

import android.media.MediaFormat;
import android.os.Environment;
import com.tencent.mars.xlog.Log;
import java.io.File;
import org.webrtc.EglBase;
import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoRenderer;

/**
 * Created by Piasy{github.com/Piasy} on 01/08/2017.
 */

public class RcTest implements FrameProducer.Callback {
    private static final String TAG = "RcTest";

    private final SurfaceViewRenderer mSurfaceViewRenderer;
    private final EglBase mEglBase;
    private final FrameProducer mFrameProducer;
    private final EncoderWrapper mEncoderWrapper;

    private int mVideoWidth;
    private int mVideoHeight;

    private volatile boolean mFinished;

    public RcTest(final Config config, final SurfaceViewRenderer renderer) {
        mSurfaceViewRenderer = renderer;
        mEglBase = EglBase.create();
        File videoFile = new File(Environment.getExternalStorageDirectory(),
                "alien-covenant.mp4");
        mFrameProducer = new FrameProducer(mEglBase, videoFile, config.outputFps(), this);
        mEncoderWrapper = new EncoderWrapper(config);

        mSurfaceViewRenderer.init(mEglBase.getEglBaseContext(), null);
    }

    public void start() {
        mFrameProducer.start();
        mEncoderWrapper.start(mEglBase);
    }

    public boolean finished() {
        return mFinished;
    }

    public void stop() {
        mFrameProducer.stop();
    }

    @Override
    public void onReady(final MediaFormat mediaFormat) {
        mVideoWidth = mediaFormat.getInteger(MediaFormat.KEY_WIDTH);
        mVideoHeight = mediaFormat.getInteger(MediaFormat.KEY_HEIGHT);
        Log.i(TAG, "Video size: %dx%d", mVideoWidth, mVideoHeight);
    }

    @Override
    public void onFrame(final int oesTextureId, final float[] transformMatrix,
            final long timestampNs) {
        VideoRenderer.I420Frame frame = new VideoRenderer.I420Frame(mVideoWidth, mVideoHeight, 0,
                oesTextureId, transformMatrix, 0, timestampNs);
        mSurfaceViewRenderer.renderFrame(frame);
        mEncoderWrapper.encodeFrame(frame);
    }

    @Override
    public void onStop() {
        mFinished = true;
        mEncoderWrapper.stop();
    }
}
