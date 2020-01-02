package com.jtl.ruler.view;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.os.Vibrator;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.Toast;

import com.google.ar.core.Anchor;
import com.google.ar.core.Camera;
import com.google.ar.core.Frame;
import com.google.ar.core.HitResult;
import com.google.ar.core.Plane;
import com.google.ar.core.Point;
import com.google.ar.core.Session;
import com.google.ar.core.Trackable;
import com.google.ar.core.TrackingState;
import com.google.ar.core.exceptions.CameraNotAvailableException;
import com.jtl.ruler.helper.DisplayRotationHelper;
import com.jtl.ruler.helper.SessionHelper;
import com.jtl.ruler.helper.TabHelper;
import com.jtl.ruler.render.LineRender;
import com.jtl.ruler.render.PictureCircleRender;
import com.jtl.ruler.render.PointRender;
import com.jtl.ruler.render.RgbRender;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * @author：TianLong
 * @date：2019/11/29 16:47
 * 彩色图渲染View
 */
public class RgbGLSurface extends GLSurfaceView implements GLSurfaceView.Renderer {
    private static final String TAG = RgbGLSurface.class.getSimpleName();

    private DisplayRotationHelper mDisplayRotationHelper;
    private RgbRender mRgbRender;
    private PointRender mPointRender;
    private LineRender mLineRender;
    private PictureCircleRender mPictureCircleRender;
    private ReentrantLock mReentrantLock;

    private volatile Frame mFrame;
    private volatile List<Anchor> mAnchorList;
    private Vibrator vibrator;

    public RgbGLSurface(Context context) {
        this(context, null);
    }

    public RgbGLSurface(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        this.setPreserveEGLContextOnPause(true);
        this.setEGLContextClientVersion(2);
        this.setEGLConfigChooser(8, 8, 8, 8, 16, 0);
        this.setRenderer(this);
        this.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);

        mAnchorList = new ArrayList<>(16);
        mDisplayRotationHelper = new DisplayRotationHelper(getContext());
        mReentrantLock = new ReentrantLock();
        vibrator = (Vibrator) getContext().getSystemService(getContext().VIBRATOR_SERVICE);
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        GLES20.glClearColor(0, 0, 0, 0);

        mRgbRender = new RgbRender();
        mRgbRender.createdGLThread(getContext());

        mPictureCircleRender = new PictureCircleRender();
        mPictureCircleRender.createdGLThread(getContext());

        mPointRender = new PointRender();
        mPointRender.createdGLThread(getContext());

        mLineRender = new LineRender();
        mLineRender.createdGLThread(getContext());
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        GLES20.glViewport(0, 0, width, height);
        mDisplayRotationHelper.onSurfaceChanged(width, height);
    }


    @Override
    public void onDrawFrame(GL10 gl) {
        GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT | GLES20.GL_COLOR_BUFFER_BIT);
        try {
            Session session = SessionHelper.getInstance().getSession();
            if (session == null) {
                Log.w(TAG, "Session == null");
                return;
            }
            mDisplayRotationHelper.updateSessionIfNeeded(session);
            session.setCameraTextureName(mRgbRender.getTextureId());

            mFrame = session.update();
            mRgbRender.onDraw(mFrame);

            Camera camera = mFrame.getCamera();

            mReentrantLock.lock();
            Anchor anchor = hitTest(mFrame, camera);
            if (!mAnchorList.isEmpty()) {
                mPointRender.upData(mAnchorList, camera);
                mPointRender.onDraw();

                if (mAnchorList.size() % 2 == 0) {
                    mLineRender.upData(mAnchorList, camera);
                } else {
                    mLineRender.upData(mAnchorList, anchor, camera);
                }
//                mLineRender.upData(mAnchorList, camera);
                mLineRender.onDraw();
                Log.w(TAG, mAnchorList.size() + "个Anchor");
            }
            mReentrantLock.unlock();

            if (anchor != null) {
                mPictureCircleRender.upData(anchor, camera);
                mPictureCircleRender.onDraw();
            }
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

    /**
     * HitTest
     *
     * @param frame
     * @param camera
     * @return
     */
    public Anchor hitTest(Frame frame, Camera camera) {
        MotionEvent motionEvent = TabHelper.getInstance().getDefaultMotionEvent();
        MotionEvent motionClick = TabHelper.getInstance().poll();
        Anchor anchor = null;
        if (camera.getTrackingState() == TrackingState.TRACKING) {
            for (HitResult hit : frame.hitTest(motionEvent)) {
                Trackable trackable = hit.getTrackable();
                boolean isTrackable = (trackable instanceof Plane
                        && ((Plane) trackable).isPoseInPolygon(hit.getHitPose()))
                        || (trackable instanceof Point
                        && ((Point) trackable).getOrientationMode()
                        == Point.OrientationMode.ESTIMATED_SURFACE_NORMAL);
                if (isTrackable) {
                    anchor = hit.createAnchor();

                    if (motionClick != null) {
                        if (mAnchorList.size() >= 10) {
                            mAnchorList.get(0).detach();
                            mAnchorList.remove(0);
                            mAnchorList.get(0).detach();
                            mAnchorList.remove(0);
                        }
                        mAnchorList.add(anchor);
                        vibrator.vibrate(50);
                    }
                }
            }
        }

        return anchor;
    }

    public void removeAnchorList(int index) {
        mReentrantLock.lock();
        try {
            if (mAnchorList.size() != 0 && index >= 0 && index < mAnchorList.size()) {
                mAnchorList.remove(index);
                vibrator.vibrate(50);
            } else {
                this.post(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getContext(), "Anchors个数为：" + mAnchorList.size(), Toast.LENGTH_SHORT).show();
                    }
                });
            }
        } finally {
            mReentrantLock.unlock();
        }
    }

    public void removeLastAnchorList() {
        removeAnchorList(mAnchorList.size() - 1);
    }
}
