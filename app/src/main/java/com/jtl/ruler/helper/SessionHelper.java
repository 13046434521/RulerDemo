package com.jtl.ruler.helper;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.support.annotation.RequiresPermission;

import com.google.ar.core.ArCoreApk;
import com.google.ar.core.Session;
import com.google.ar.core.exceptions.CameraNotAvailableException;
import com.google.ar.core.exceptions.UnavailableApkTooOldException;
import com.google.ar.core.exceptions.UnavailableArcoreNotInstalledException;
import com.google.ar.core.exceptions.UnavailableDeviceNotCompatibleException;
import com.google.ar.core.exceptions.UnavailableSdkTooOldException;
import com.google.ar.core.exceptions.UnavailableUserDeclinedInstallationException;

/**
 * @author：TianLong
 * @date：2019/11/29 17:31
 */
public class SessionHelper {
    private static final String TAG = SessionHelper.class.getSimpleName();
    private Session mSession;
    private boolean isInstallRequested = false;

    public static SessionHelper getInstance() {
        return SessionHelperHolder.SESSION_HELPER;
    }

    private static class SessionHelperHolder {
        private static final SessionHelper SESSION_HELPER = new SessionHelper();
    }


    public Session initialize(Context context) {
        if (mSession == null) {
            try {
                switch (ArCoreApk.getInstance().requestInstall((Activity) context, !isInstallRequested)) {
                    case INSTALL_REQUESTED:
                        isInstallRequested = true;
                        return null;
                    default:
                        break;
                }
                mSession = new Session(context);
            } catch (UnavailableDeviceNotCompatibleException | UnavailableUserDeclinedInstallationException
                    | UnavailableArcoreNotInstalledException | UnavailableSdkTooOldException | UnavailableApkTooOldException e) {
                e.printStackTrace();
                return null;
            }
        }
        return mSession;
    }

    public void onResume() {
        try {
            if (mSession != null) {
                mSession.resume();
            }
        } catch (CameraNotAvailableException e) {
            e.printStackTrace();
        }
    }

    public void onPause() {
        if (mSession != null) {
            mSession.pause();
        }
    }

    public void onClose() {
        if (mSession != null) {
            mSession.close();
        }
    }
}
