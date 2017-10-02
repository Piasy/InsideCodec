package com.github.piasy.h264ds;

import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.os.Environment;
import android.util.Log;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Collections;
import org.jcodec.codecs.h264.H264Decoder;
import org.jcodec.codecs.h264.io.model.Frame;
import org.jcodec.common.model.ColorSpace;
import org.jcodec.common.model.Picture;

/**
 * Created by Piasy{github.com/Piasy} on 03/08/2017.
 */

public class FrameProducer {
    private static final String TAG = "FrameProducer";

    private final File mVideoFile;
    private final int mFps;

    private MediaExtractor mExtractor;
    private H264Decoder mH264Decoder;
    private volatile boolean mRunning;
    private ByteBuffer mInputBuf = ByteBuffer.allocate(1024 * 1024);
    private Picture mDecodedPic = Picture.create(480, 640, ColorSpace.YUV420J);
    private int mDumpFrameNum;

    public FrameProducer(final File videoFile, final int fps) {
        mVideoFile = videoFile;
        mFps = fps;
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

                    // ZAAfrNGAUAW7ARAAAAMAEAAAAwPA8YMRoA==
                    ByteBuffer sps = videoFormat.getByteBuffer("csd-0");
                    // 6Wssiw==
                    ByteBuffer pps = videoFormat.getByteBuffer("csd-1");
                    ByteBuffer csd = ByteBuffer.allocate(sps.capacity() + pps.capacity())
                            .put(sps)
                            .put(pps);
                    csd.position(0);

                    mH264Decoder = H264Decoder.createH264DecoderFromCodecPrivate(csd);

                    produce();
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    if (mExtractor != null) {
                        mExtractor.release();
                    }
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
        int chunkSize = mExtractor.readSampleData(mInputBuf, 0);
        if (chunkSize < 0) {
            Log.i(TAG, "extractor EOS reached");
            return false;
        }
        long presentationTimeUs = mExtractor.getSampleTime();
        Log.i(TAG, "extract frame: " + chunkSize + " " + presentationTimeUs);

        mExtractor.advance();

        Frame frame = mH264Decoder.decodeFrame(mInputBuf, mDecodedPic.getData());
        dumpDecodedFrame(frame, presentationTimeUs);

        return true;
    }

    private void dumpDecodedFrame(Frame decodedFrame, long ts) {
        if (decodedFrame == null) {
            return;
        }
        File dir = new File(Environment.getExternalStorageDirectory(), "h264ds");
        if (!dir.exists()) {
            dir.mkdirs();
        }

        try {
            int width = decodedFrame.getWidth();
            int height = decodedFrame.getHeight();

            if (mDumpFrameNum > 10) {
                return;
            }
            Log.d("SWAVC", "dec_" + ts + "_" + width + "x" + height + ".yuv");

            byte[] dataY = decodedFrame.getData()[0];
            byte[] dataU = decodedFrame.getData()[1];
            byte[] dataV = decodedFrame.getData()[2];

            FileOutputStream outputStream = new FileOutputStream(
                    new File(dir, "dec_" + ts + "_" + width + "x" + height + ".yuv"));

            outputStream.write(dataY, 0, width * height);
            outputStream.write(dataU, 0, width * height / 4);
            outputStream.write(dataV, 0, width * height / 4);

            outputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        mDumpFrameNum++;
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
}
