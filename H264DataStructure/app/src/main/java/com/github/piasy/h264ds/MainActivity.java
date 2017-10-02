package com.github.piasy.h264ds;

import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import butterknife.ButterKnife;
import butterknife.OnClick;
import java.io.File;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        ButterKnife.bind(this);
    }

    @OnClick(R.id.mBtnStart)
    public void start() {
        new FrameProducer(new File(Environment.getExternalStorageDirectory(),
                "camera-test.mp4"),
                30).start();
    }
}
