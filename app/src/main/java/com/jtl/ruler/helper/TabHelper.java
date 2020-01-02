package com.jtl.ruler.helper;

import android.view.MotionEvent;

import java.util.concurrent.LinkedBlockingQueue;

/**
 * @author：TianLong
 * @date：2019/11/30 17:32
 */
public class TabHelper {
    private LinkedBlockingQueue<MotionEvent> mLinkedBlockingQueue;
    private MotionEvent mMotionEvent;

    private TabHelper() {
        mLinkedBlockingQueue = new LinkedBlockingQueue<>(16);
    }

    public static TabHelper getInstance() {
        return TabHelperHolder.TAB_HELPER;
    }

    private static class TabHelperHolder {
        private static final TabHelper TAB_HELPER = new TabHelper();
    }

    public MotionEvent getDefaultMotionEvent() {
        return mMotionEvent;
    }

    public void setDefaultMotionEvent(MotionEvent motionEvent) {
        mMotionEvent = motionEvent;
    }

    public MotionEvent poll() {
        return mLinkedBlockingQueue.poll();
    }

    public void offer(MotionEvent motionEvent) {
        mLinkedBlockingQueue.offer(motionEvent);
    }

    public boolean isEmpty() {
        return mLinkedBlockingQueue.isEmpty();
    }
}
