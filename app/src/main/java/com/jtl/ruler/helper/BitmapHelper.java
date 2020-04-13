package com.jtl.ruler.helper;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;

import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * @author：TianLong
 * @date：2020/4/11 13:31
 */
public class BitmapHelper {
    private Canvas mCanvas;
    private Paint mPaint;
    private int width = 150;
    private int height = 80;
    private int margin = 20;
    private int rectRound = 30;
    private ConcurrentLinkedQueue<Bitmap> mBitmapsQueue = new ConcurrentLinkedQueue<>();

    private BitmapHelper() {
        mCanvas = new Canvas();
        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setTextSize(30);
        mPaint.setColor(Color.BLACK);
        mPaint.setStrokeWidth(20);
        mPaint.setTextAlign(Paint.Align.CENTER);
    }

    public static BitmapHelper getInstance() {
        return BitmapHelperHolder.BITMAP_HELPER;
    }


    private static class BitmapHelperHolder {
        protected static final BitmapHelper BITMAP_HELPER = new BitmapHelper();
    }

    public synchronized Bitmap drawBitmap(Bitmap contentBitmap, String msg) {
        mCanvas.setBitmap(contentBitmap);
        RectF rectF = new RectF(0, 0, contentBitmap.getWidth(), contentBitmap.getHeight() );
        mPaint.setColor(Color.WHITE);
        mCanvas.drawRoundRect(rectF, rectRound, rectRound, mPaint);

        mPaint.setColor(Color.BLACK);
        mCanvas.drawText(msg, rectF.width() / 2, rectF.height() / 2, mPaint);
        mBitmapsQueue.offer(contentBitmap);

        return contentBitmap;
    }

    public synchronized Bitmap drawBitmap(String msg) {
        String content = msg + "cm";
        int length = content.length();
        int width = length * 30 + margin;
        int height = (int) (mPaint.getTextSize() + margin/2);
        width = Math.max(width,this.width);
        height = Math.max(height,this.height);

        Bitmap bitmap = mBitmapsQueue.poll();
        if (bitmap==null){
            bitmap=Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_4444);
            for (int i = 0; i < bitmap.getHeight(); i++) {
                for (int j = 0; j < bitmap.getWidth(); j++) {
                    int color = Color.argb(255, 255, 55, 55);
                    bitmap.setPixel(j, i, color);
                }
            }
        }
        return drawBitmap(bitmap, content);
    }

    public Bitmap getBitmap(){
        Bitmap bitmap = mBitmapsQueue.poll();
        if (bitmap==null){
            bitmap=Bitmap.createBitmap(width + margin, height + margin, Bitmap.Config.ARGB_4444);
        }

        return bitmap;
    }

    public synchronized void clean() {
        while (!mBitmapsQueue.isEmpty()) {
            mBitmapsQueue.poll().recycle();
        }
    }
}
