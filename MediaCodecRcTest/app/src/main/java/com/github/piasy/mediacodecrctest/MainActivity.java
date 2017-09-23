package com.github.piasy.mediacodecrctest;

import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.view.SurfaceHolder;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import com.tencent.mars.xlog.Log;
import com.tencent.mars.xlog.Xlog;
import org.webrtc.EglBase;
import org.webrtc.RendererCommon;
import org.webrtc.SurfaceViewRenderer;

public class MainActivity extends AppCompatActivity implements RcTest.Notifier {

    private static final String[] BIT_RATE_MODES = new String[] {
            "CQ", "VBR", "CBR"
    };

    static {
        System.loadLibrary("stlport_shared");
        System.loadLibrary("marsxlog");
    }

    @BindView(R.id.mBtnStart)
    Button mBtnStart;
    @BindView(R.id.mTvInfo)
    TextView mTvInfo;
    @BindView(R.id.mEtInitBr)
    EditText mEtInitBr;
    @BindView(R.id.mEtBrStep)
    EditText mEtBrStep;
    @BindView(R.id.mEtQuality)
    EditText mEtQuality;
    @BindView(R.id.mSpBitrateMode)
    Spinner mSpBitrateMode;
    @BindView(R.id.mCbAsyncEnc)
    CheckBox mCbAsyncEnc;
    @BindView(R.id.mCbUpdateBr)
    CheckBox mCbUpdateBr;
    @BindView(R.id.mSurface)
    SurfaceViewRenderer mSurface;

    private RcTest mRcTest;
    private Config mConfig;
    private EglBase mEglBase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final String logPath = Environment.getExternalStorageDirectory().getAbsolutePath()
                               + "/rc_test_log";
        final String cachePath = this.getFilesDir() + "/xlog";
        Xlog.appenderOpen(Xlog.LEVEL_INFO, Xlog.AppednerModeAsync, cachePath, logPath, "RcTest");
        Xlog.setConsoleLogOpen(true);
        Log.setLogImp(new Xlog());

        setContentView(R.layout.activity_main);

        ButterKnife.bind(this);

        ArrayAdapter outputSpinnerAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, BIT_RATE_MODES);
        outputSpinnerAdapter.setDropDownViewResource(
                android.R.layout.simple_list_item_single_choice);
        mSpBitrateMode.setAdapter(outputSpinnerAdapter);

        mEglBase = EglBase.create();
        mSurface.init(mEglBase.getEglBaseContext(), null);
        mSurface.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(final SurfaceHolder holder) {
                mBtnStart.setEnabled(true);
            }

            @Override
            public void surfaceChanged(final SurfaceHolder holder, final int format,
                    final int width, final int height) {
            }

            @Override
            public void surfaceDestroyed(final SurfaceHolder holder) {
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        mSurface.release();
        mEglBase.release();
        if (mRcTest != null) {
            mRcTest.stop();
        }

        Log.appenderFlush(true);
        Log.appenderClose();
        mSurface.release();
    }

    @OnClick(R.id.mBtnStart)
    public void startTest() {
        mSurface.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT);

        if (mRcTest != null && !mRcTest.finished()) {
            Toast.makeText(this, "Last test still running", Toast.LENGTH_SHORT).show();
            return;
        }

        mConfig = Config.builder()
                .updateBr(mCbUpdateBr.isChecked())
                .asyncEnc(mCbAsyncEnc.isChecked())
                .initBr(Integer.parseInt(mEtInitBr.getText().toString()))
                .brStep(Integer.parseInt(mEtBrStep.getText().toString()))
                .quality(Integer.parseInt(mEtQuality.getText().toString()))
                .brMode(mSpBitrateMode.getSelectedItemPosition())
                .outputWidth(448)
                .outputHeight(800)
                .outputFps(30)
                .outputKeyFrameInterval(2)
                .build();
        mRcTest = new RcTest(mConfig, mEglBase, mSurface, this);
        mRcTest.start();
    }

    @Override
    public void reportBr(final int br) {
        mTvInfo.post(new Runnable() {
            @Override
            public void run() {
                mTvInfo.setText("br: " + br);
            }
        });
    }
}
