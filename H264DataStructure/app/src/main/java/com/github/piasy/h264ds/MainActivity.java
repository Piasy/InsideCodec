package com.github.piasy.h264ds;

import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import butterknife.ButterKnife;
import butterknife.OnClick;
import com.github.piasy.h264ds.ds.NALU;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        ButterKnife.bind(this);
    }

    @OnClick(R.id.mBtnStart)
    public void startExtract() {
        new FrameExtractor(
                new File(Environment.getExternalStorageDirectory(), "alien-covenant.mp4")
        ).start();
    }

    @OnClick(R.id.mBtnParse)
    public void startParse() {
        new Thread() {
            @Override
            public void run() {
                String[] filenames = new String[] {
                        "encoded_0_33312.nalu",
                        "encoded_1_66312.nalu",
                        "encoded_2_100312.nalu",
                        "encoded_3_133312.nalu",
                        "encoded_4_166312.nalu",
                        "encoded_5_200312.nalu",
                        "encoded_6_233312.nalu",
                        "encoded_7_266312.nalu",
                        "encoded_8_300312.nalu",
                        "encoded_9_333312.nalu",
                };

                for (String name : filenames) {
                    try {
                        File file = new File("/sdcard/h264ds/" + name);
                        ByteBuffer byteBuffer = ByteBuffer.allocate((int) file.length());
                        FileInputStream inputStream = new FileInputStream(file);
                        inputStream.read(byteBuffer.array());
                        inputStream.close();

                        NALU nalu = NALU.parse(byteBuffer, 0, byteBuffer.capacity());
                        Log.i(TAG, name + " " + byteBuffer.capacity() + " => " + nalu);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }.start();
    }
}
