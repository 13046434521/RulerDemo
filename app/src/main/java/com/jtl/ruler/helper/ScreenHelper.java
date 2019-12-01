/*
 * Copyright 2017 Google Inc. All Rights Reserved.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jtl.ruler.helper;

import android.app.Activity;
import android.graphics.Point;
import android.view.View;

import java.lang.ref.WeakReference;

/**
 * @author TianLong
 */
public final class ScreenHelper {

    private ScreenHelper() {
    }

    public static ScreenHelper getInstance() {
        return ScreenHelperHolder.SCREEN_HELPER;
    }

    public void setFullScreenOnWindowFocusChanged(Activity activity, boolean hasFocus) {
        if (hasFocus) {
            activity
                    .getWindow()
                    .getDecorView()
                    .setSystemUiVisibility(
                            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }
    }

    public Point getScreenPoint(Activity activity) {
        WeakReference<Activity> weakReference = new WeakReference<Activity>(activity);
        Point mScreenPoint = new Point();
        weakReference.get().getWindowManager().getDefaultDisplay().getSize(mScreenPoint);
        return mScreenPoint;
    }

    private static class ScreenHelperHolder {
        private static final ScreenHelper SCREEN_HELPER = new ScreenHelper();
    }
}
