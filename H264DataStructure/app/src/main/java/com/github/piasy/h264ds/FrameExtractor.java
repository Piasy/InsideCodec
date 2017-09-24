package com.github.piasy.h264ds;

import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.os.Environment;
import android.util.Log;
import com.github.piasy.h264ds.ds.NALU;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Created by Piasy{github.com/Piasy} on 03/08/2017.
 */

public class FrameExtractor {
    private static final String TAG = "FrameExtractor";

    private final File mVideoFile;

    private MediaExtractor mExtractor;
    private ByteBuffer mFrameBuffer;
    private volatile boolean mRunning;

    private int mFrameIdx;
    private boolean mDumping = false;

    public FrameExtractor(final File videoFile) {
        mVideoFile = videoFile;
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
                    mFrameBuffer = ByteBuffer.allocate(1024 * 1024);

                    extract();
                } catch (IOException e) {
                    Log.e(TAG, "start exception");
                } finally {
                    if (mExtractor != null) {
                        mExtractor.release();
                    }
                }
            }
        }).start();
    }

    private void extract() throws IOException {
        File dumpDir = new File(Environment.getExternalStorageDirectory(), "h264ds");
        if (!dumpDir.exists()) {
            dumpDir.mkdirs();
        }

        mFrameIdx = 0;
        while (mRunning) {
            int chunkSize = mExtractor.readSampleData(mFrameBuffer, 0);
            if (chunkSize < 0) {
                mRunning = false;
                Log.i(TAG, "extractor EOS reached");
                break;
            }
            if (mExtractor.getSampleFlags() == MediaExtractor.SAMPLE_FLAG_SYNC && mFrameIdx > 0) {
                mRunning = false;
                break;
            }

            long presentationTimeUs = mExtractor.getSampleTime();

            parseNALU(mFrameBuffer, 0, chunkSize);

            if (mDumping) {
                File frame = new File(dumpDir,
                        "frame_" + mFrameIdx + "_" + presentationTimeUs + ".nalu");
                FileOutputStream outputStream = new FileOutputStream(frame);
                byte[] data = new byte[chunkSize];
                mFrameBuffer.get(data);
                outputStream.write(data);
                outputStream.close();
            }

            mFrameIdx++;
            mExtractor.advance();
        }
    }

    private void parseNALU(ByteBuffer buffer, int offset, int size) {
        NALU nalu = NALU.parse(buffer, offset, size);
        if (nalu != null) {
            Log.i(TAG, nalu.toString());
        }
        buffer.rewind();
    }

    public void stop() {
        mRunning = false;
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
