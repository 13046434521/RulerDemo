package com.jtl.ruler;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import com.jtl.ruler.helper.FullScreenHelper;
import com.jtl.ruler.helper.PermissionHelper;
import com.jtl.ruler.helper.SessionHelper;
import com.jtl.ruler.view.RgbGLSurface;

/**
 * @author TianLong
 */
public class MainActivity extends AppCompatActivity {
    private RgbGLSurface mRgbGLSurface;
    private SessionCode mSessionCode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initView();
        initData();
    }

    private void initView() {
        mRgbGLSurface = findViewById(R.id.gl_main_rgb);
    }

    private void initData() {
        if (PermissionHelper.hasCameraPermission(this)) {
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
        FullScreenHelper.getInstance().setFullScreenOnWindowFocusChanged(this, hasFocus);
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
}
