package com.jtl.ruler;


import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.jtl.ruler.helper.PermissionHelper;
import com.jtl.ruler.helper.ScreenHelper;
import com.jtl.ruler.helper.SessionHelper;
import com.jtl.ruler.helper.TabHelper;
import com.jtl.ruler.view.RgbGLSurface;

/**
 * @author TianLong
 */
public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private static final String TAG = MainActivity.class.getSimpleName();
    private RgbGLSurface mRgbGLSurface;
    private ImageView mHitImage;
    private SessionCode mSessionCode;


    private MotionEvent mMotionEvent;
    private android.graphics.Point mPoint;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initView();
        initData();
    }

    private void initView() {
        mRgbGLSurface = findViewById(R.id.gl_main_rgb);
        mHitImage = findViewById(R.id.iv_main_hit);

        mHitImage.setOnClickListener(this::onClick);
    }

    private void initData() {
        if (PermissionHelper.hasCameraPermission(this)) {
            mPoint = ScreenHelper.getInstance().getScreenPoint(this);
            mMotionEvent = MotionEvent.obtain(0, 0, MotionEvent.ACTION_DOWN, mPoint.x / 2f, mPoint.y / 2f, 0);

            mSessionCode = SessionHelper.getInstance().initialize(this);
            Toast.makeText(this, mSessionCode.toInfo(), Toast.LENGTH_SHORT).show();
        } else {
            PermissionHelper.requestCameraPermission(this);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mRgbGLSurface != null) {
            mRgbGLSurface.onResume();
            SessionHelper.getInstance().onResume();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mRgbGLSurface != null) {
            mRgbGLSurface.onPause();
            SessionHelper.getInstance().onPause();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        SessionHelper.getInstance().onClose();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        ScreenHelper.getInstance().setFullScreenOnWindowFocusChanged(this, hasFocus);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
        if (!PermissionHelper.hasCameraPermission(this)) {
            Toast.makeText(this, "该应用需要相机权限", Toast.LENGTH_LONG)
                    .show();
            if (!PermissionHelper.shouldShowRequestPermissionRationale(this)) {
                // 直接跳至设置 修改权限
                PermissionHelper.launchPermissionSettings(this);
            }
            finish();
        }
    }


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.iv_main_hit:
                //队列为Null的情况下，才加入数据。防止多次点击情况
                if (TabHelper.getInstance().isEmpty()) {
                    TabHelper.getInstance().offer(mMotionEvent);
                }
                break;
            default:
                break;
        }
    }
}
