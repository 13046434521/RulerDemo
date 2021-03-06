package com.jtl.ruler.helper;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

import com.google.ar.core.ArCoreApk;
import com.google.ar.core.CameraConfig;
import com.google.ar.core.CameraConfigFilter;
import com.google.ar.core.Session;
import com.google.ar.core.exceptions.CameraNotAvailableException;
import com.google.ar.core.exceptions.UnavailableApkTooOldException;
import com.google.ar.core.exceptions.UnavailableArcoreNotInstalledException;
import com.google.ar.core.exceptions.UnavailableDeviceNotCompatibleException;
import com.google.ar.core.exceptions.UnavailableSdkTooOldException;
import com.google.ar.core.exceptions.UnavailableUserDeclinedInstallationException;
import com.jtl.ruler.SessionCode;

import java.util.EnumSet;
import java.util.List;

import static com.google.ar.core.ArCoreApk.InstallStatus.INSTALL_REQUESTED;

/**
 * @author：TianLong
 * @date：2019/11/29 17:31
 */
public class SessionHelper {
    private static final String TAG = SessionHelper.class.getSimpleName();
    private Session mSession;
    private boolean isInstallRequested = false;
    private SessionCode code;

    private SessionHelper() {
    }

    public static SessionHelper getInstance() {
        return SessionHelperHolder.SESSION_HELPER;
    }

    private static class SessionHelperHolder {
        private static final SessionHelper SESSION_HELPER = new SessionHelper();
    }


    public SessionCode initialize(Context context) {
        if (mSession == null) {
            try {
                ArCoreApk.InstallStatus installStatus = ArCoreApk.getInstance().requestInstall((Activity) context, !isInstallRequested);
                if (installStatus == INSTALL_REQUESTED) {
                    isInstallRequested = true;
                    code = SessionCode.ArcoreRequestedInstalled;
                } else {
                    mSession = new Session(context);
                    mSession.setCameraConfig(getConfig());
                    code = SessionCode.SessionOK;
                }
            } catch (UnavailableArcoreNotInstalledException e) {
                code = SessionCode.ArcoreNotInstalled;
            } catch (UnavailableApkTooOldException | UnavailableSdkTooOldException e) {
                code = SessionCode.ApkOrSdkTooOld;
            } catch (UnavailableDeviceNotCompatibleException e) {
                code = SessionCode.DeviceNotCompatible;
            } catch (UnavailableUserDeclinedInstallationException e) {
                code = SessionCode.UserDeclinedInstallation;
            }
        }
        Log.w(TAG, "Session.initialize:" + code.toNative() + ":" + code.toInfo());
        return code;
    }

    public void onResume() {
        try {
            if (mSession != null) {
                mSession.resume();
                Log.w(TAG, "Session.resume()");
            }
        } catch (CameraNotAvailableException e) {
            e.printStackTrace();
            Log.w(TAG, e.getMessage());
        }
    }

    public void onPause() {
        if (mSession != null) {
            mSession.pause();
            Log.w(TAG, "Session.pause()");
        }
    }

    public void onClose() {
        if (mSession != null) {
            mSession.close();
            mSession = null;//mSession 若不设为null  GC不回收
            Log.w(TAG, "Session.close()");
        }
    }

    public Session getSession() {
        return mSession;
    }

    private CameraConfig getConfig() {
        CameraConfigFilter filter = new CameraConfigFilter(mSession);
        filter.setTargetFps(EnumSet.of(CameraConfig.TargetFps.TARGET_FPS_30));
        filter.setDepthSensorUsage(EnumSet.of(CameraConfig.DepthSensorUsage.DO_NOT_USE));
        List<CameraConfig> cameraConfigList = mSession.getSupportedCameraConfigs(filter);
        for (CameraConfig cameraConfig : cameraConfigList) {
            Log.w(TAG, cameraConfig.getCameraId() + "  " + cameraConfig.getImageSize().toString() + "  " + cameraConfig.getTextureSize().toString());
        }
        return cameraConfigList.get(0);
    }
}
