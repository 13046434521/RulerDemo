package com.jtl.ruler.view;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.util.Log;

import com.google.ar.core.Frame;
import com.google.ar.core.Session;
import com.google.ar.core.exceptions.CameraNotAvailableException;
import com.jtl.ruler.helper.DisplayRotationHelper;
import com.jtl.ruler.helper.SessionHelper;
import com.jtl.ruler.render.RgbRender;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * @author：TianLong
 * @date：2019/11/29 16:47
 * 彩色图渲染View
 */
public class RgbGLSurface extends GLSurfaceView implements GLSurfaceView.Renderer {
    private static final String TAG = RgbGLSurface.class.getSimpleName();
    private RgbRender mRgbRender;
    private DisplayRotationHelper mDisplayRotationHelper;

    public RgbGLSurface(Context context) {
        this(context, null);
    }

    public RgbGLSurface(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init(){
        this.setPreserveEGLContextOnPause(true);
        this.setEGLContextClientVersion(2);
        this.setEGLConfigChooser(8, 8, 8, 8, 16, 0);
        this.setRenderer(this);
        this.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);

        mDisplayRotationHelper = new DisplayRotationHelper(getContext());
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        GLES20.glClearColor(0,0,0,0);

        mRgbRender = new RgbRender();
        mRgbRender.createdGLThread(getContext());
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        GLES20.glViewport(0,0,width,height);
        mDisplayRotationHelper.onSurfaceChanged(width, height);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT|GLES20.GL_COLOR_BUFFER_BIT);
        try {
            Session session = SessionHelper.getInstance().getSession();
            if (session == null) {
                Log.w(TAG, "Session == null");
                return;
            }

            mDisplayRotationHelper.updateSessionIfNeeded(session);
            session.setCameraTextureName(mRgbRender.getTextureId());
            Frame frame = session.update();
            mRgbRender.onDraw(frame);
        } catch (CameraNotAvailableException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        mDisplayRotationHelper.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        mDisplayRotationHelper.onPause();
    }
}
