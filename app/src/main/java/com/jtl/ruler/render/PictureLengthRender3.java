package com.jtl.ruler.render;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.opengl.Matrix;
import android.support.annotation.NonNull;
import android.util.Log;

import com.google.ar.core.Anchor;
import com.google.ar.core.Camera;
import com.jtl.ruler.helper.BitmapHelper;
import com.jtl.ruler.helper.ShaderHelper;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * @author：TianLong
 * @date：2020/1/2 14:18
 */
public class PictureLengthRender3 {
    private static final String TAG = PictureLengthRender3.class.getSimpleName();
    private static final String VERTEX_SHADER = "shader/picture_length_vert.glsl";
    private static final String FRAGMENT_SHADER = "shader/picture_length_frag.glsl";
    private static final String PATH = "picture.png";
    private static final int FLOAT_SIZE_BYTES = 4;
    private int mProgram;
    private int[] textureId;

    private int a_Position;
    private int u_TextureUnit;
    private int a_TexCoord;
    private int u_MvpMatrix;

    private float[] mvp_Matrix;
    private FloatBuffer mVertexCoord;
    private FloatBuffer mTextureCoord;
    private volatile int pointCount = 0;
    private float[] positions;
    private float[] centerPosition;
    private float[] vertexCoord;
    private ByteBuffer mBitmapBuffer;
    private Bitmap mPictureCircle;
    private int mBitmapHeight;
    private int mBitmapWidth;
    private ArrayList<PointPicture> mPointPictures = new ArrayList<>();
    private float[] textureCoord = new float[]{
            0.0f, 0.0f,
            0.0f, 1.0f,
            1.0f, 0.0f,
            1.0f, 1.0f,
    };

    public void createdGLThread(Context context) {
        initProgram(context);
        initData(context);
        initTexture();
    }

    private void initProgram(Context context) {
        mProgram = GLES20.glCreateProgram();
        int fragmentShader = ShaderHelper.loadGLShader(TAG, context, GLES20.GL_FRAGMENT_SHADER, FRAGMENT_SHADER);
        int vertexShader = ShaderHelper.loadGLShader(TAG, context, GLES20.GL_VERTEX_SHADER, VERTEX_SHADER);

        GLES20.glAttachShader(mProgram, fragmentShader);
        GLES20.glAttachShader(mProgram, vertexShader);

        GLES20.glLinkProgram(mProgram);
        GLES20.glUseProgram(mProgram);

        a_Position = GLES20.glGetAttribLocation(mProgram, "a_Position");
        a_TexCoord = GLES20.glGetAttribLocation(mProgram, "a_TexCoord");
        u_TextureUnit = GLES20.glGetUniformLocation(mProgram, "u_TextureUnit");
        u_MvpMatrix = GLES20.glGetUniformLocation(mProgram, "u_MvpMatrix");
        //解绑Shader
        GLES20.glDetachShader(mProgram, vertexShader);
        GLES20.glDetachShader(mProgram, fragmentShader);
        //删除Shader
        GLES20.glDeleteShader(vertexShader);
        GLES20.glDeleteShader(fragmentShader);

        ShaderHelper.checkGLError("initProgram");
    }

    private void initData(Context context) {
        mvp_Matrix = new float[16];
        ByteBuffer textureBuffer = ByteBuffer.allocateDirect(textureCoord.length * FLOAT_SIZE_BYTES);
        mTextureCoord = textureBuffer.order(ByteOrder.nativeOrder()).asFloatBuffer();
        mTextureCoord.put(textureCoord);
        mTextureCoord.position(0);
        try {
            mPictureCircle = BitmapFactory.decodeStream(context.getAssets().open(PATH));
            mBitmapHeight = mPictureCircle.getHeight();
            mBitmapWidth = mPictureCircle.getWidth();

            mBitmapBuffer = ByteBuffer.allocateDirect(mBitmapHeight * mBitmapWidth * 4);
            mBitmapBuffer.order(ByteOrder.nativeOrder());
            mPictureCircle.copyPixelsToBuffer(mBitmapBuffer);
            mBitmapBuffer.position(0);

//            mPictureCircle.recycle();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void initTexture() {
        textureId = new int[1];
        GLES20.glGenTextures(1, textureId, 0);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId[0]);

        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);

        ShaderHelper.checkGLError("initTexture");
    }

    public void upData(@NonNull List<Anchor> anchors, @NonNull Camera camera) {
        upData(anchors, null, camera);
    }

    public void upData(@NonNull List<Anchor> anchors, @NonNull Anchor anchor, @NonNull Camera camera) {
        if (anchor != null) {
            positions = new float[3 * anchors.size() + 3];

            for (int i = 0; i < anchors.size(); i++) {
                positions[0] = anchors.get(i).getPose().tx();
                positions[1] = anchors.get(i).getPose().ty();
                positions[2] = anchors.get(i).getPose().tz();
            }
            positions[anchors.size() * 3] = anchor.getPose().tx();
            positions[anchors.size() * 3 + 1] = anchor.getPose().ty();
            positions[anchors.size() * 3 + 2] = anchor.getPose().tz();
        } else {
            positions = new float[3 * anchors.size()];
            for (int i = 0; i < anchors.size(); i++) {
                positions[0] = anchors.get(i).getPose().tx();
                positions[1] = anchors.get(i).getPose().ty();
                positions[2] = anchors.get(i).getPose().tz();
            }
        }

        //每个点3个分量，每两个点算出一个中心点
        pointCount = positions.length / 6;
        //纹理由4个点构成，每个点分为x,y,z 三个坐标
        for (int i = 0; i < pointCount; i++) {
            float[] point1 = new float[]{positions[i * 2 * 3], positions[i * 2 * 3 + 1], positions[i * 2 * 3 + 2]};
            float[] point2 = new float[]{positions[i * 2 * 3 + 3], positions[i * 2 * 3 + 4], positions[i * 2 * 3 + 5]};
            PointPicture pointPicture = new PointPicture(point1, point2);
            if (i < mPointPictures.size()) {
                mPointPictures.get(i).put(pointPicture);
            } else {
                mPointPictures.add(pointPicture);
            }
        }

        float[] projectMatrix = new float[16];
        float[] viewMatrix = new float[16];

        Matrix.setIdentityM(mvp_Matrix, 0);
        camera.getViewMatrix(viewMatrix, 0);
        camera.getProjectionMatrix(projectMatrix, 0, 0.1f, 100f);

        //MVP矩阵   乘法顺序为：P * V * M
        Matrix.multiplyMM(mvp_Matrix, 0, projectMatrix, 0, viewMatrix, 0);
    }

    public void onDraw() {
        GLES20.glUseProgram(mProgram);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId[0]);

        GLES20.glUniform1i(u_TextureUnit, 0);

        GLES20.glUniformMatrix4fv(u_MvpMatrix, 1, false, mvp_Matrix, 0);

        GLES20.glEnableVertexAttribArray(a_Position);

        GLES20.glEnableVertexAttribArray(a_TexCoord);


        for (int i = 0; i < pointCount; i++) {
            int j = i * 4;
            ByteBuffer vertexBuffer = ByteBuffer.allocateDirect(mPointPictures.get(i).vertexCoord.length * FLOAT_SIZE_BYTES);
            FloatBuffer mVertexCoord = vertexBuffer.order(ByteOrder.nativeOrder()).asFloatBuffer();
            mVertexCoord.put(mPointPictures.get(i).vertexCoord).position(0);

            GLES20.glVertexAttribPointer(a_Position, 3, GLES20.GL_FLOAT, false, 0, mVertexCoord);
            GLES20.glVertexAttribPointer(a_TexCoord, 2, GLES20.GL_FLOAT, false, 0, mTextureCoord);
            //这句话是数据拷贝，从CPU拷贝纸GPU显存中，耗时1-11毫秒不等。
            // 因此渲染一张固定图片时不建议把它放在onDrawFrame当中。除非数据一直在变
//            GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, mBitmapWidth, mBitmapHeight, 0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, mBitmapBuffer);
            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, mPointPictures.get(i).mBitmap, 0);
            //渲染数据, GLES20.glDrawArrays(渲染模式，第一个顶点，一共几个顶点)
            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, j, 4);
        }

        GLES20.glDisableVertexAttribArray(a_TexCoord);
        GLES20.glDisableVertexAttribArray(a_Position);
        ShaderHelper.checkGLError("onDraw");
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        GLES20.glUseProgram(0);

        ShaderHelper.checkGLError("onDraw");
    }

    /**
     * 计算两点之间的距离
     *
     * @param point1
     * @param point2
     * @return
     */
    private String calLineLength(float[] point1, float[] point2) {
        float x = point1[0] - point2[0];
        float y = point1[1] - point2[1];
        float z = point1[2] - point2[2];
        DecimalFormat df = new DecimalFormat("0.0");
        float length = (float) Math.sqrt(x * x + y * y + z * z);
        Log.w("好奇的结果", length + "");
        return df.format(length);
    }

    private class PointPicture {
        private float[] point1;
        private float[] point2;
        private String length;
        private Bitmap mBitmap;
        private float[] centerPosition = new float[3];
        private float[] vertexCoord = new float[12];

        public PointPicture(float[] point1, float[] point2) {
            this.point1 = point1;
            this.point2 = point2;

            this.centerPosition[0] = (point1[0] + point2[0]) / 2;//x
            this.centerPosition[1] = (point1[1] + point2[1]) / 2;//y
            this.centerPosition[2] = (point1[2] + point2[2]) / 2;//z

            this.length = calLineLength(point1, point2);

            this.mBitmap = BitmapHelper.getInstance().drawBitmap(length);

            //第一个点
            vertexCoord[0] = centerPosition[0] + 0.03f;
            vertexCoord[1] = centerPosition[1];
            vertexCoord[2] = centerPosition[2] + 0.02f;

            //第二个点
            vertexCoord[3] = centerPosition[0] + 0.03f;
            vertexCoord[4] = centerPosition[1];
            vertexCoord[5] = centerPosition[2] - 0.02f;

            //第三个点
            vertexCoord[6] = centerPosition[0] - 0.03f;
            vertexCoord[7] = centerPosition[1];
            vertexCoord[8] = centerPosition[2] + 0.02f;

            //第四个点
            vertexCoord[9] = centerPosition[0] - 0.03f;
            vertexCoord[10] = centerPosition[1];
            vertexCoord[11] = centerPosition[2] - 0.02f;
        }

        public void put(PointPicture pointPicture) {
            this.mBitmap = pointPicture.mBitmap;
            this.centerPosition = pointPicture.centerPosition;
            this.point1 = pointPicture.point1;
            this.point2 = pointPicture.point2;
            this.length = pointPicture.length;
            this.vertexCoord = pointPicture.vertexCoord;
        }
    }
}
