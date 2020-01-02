package com.jtl.ruler.render;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.support.annotation.NonNull;

import com.google.ar.core.Anchor;
import com.google.ar.core.Camera;
import com.jtl.ruler.helper.ShaderHelper;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.List;

/**
 * @author：TianLong
 * @date：2020/1/2 14:18
 */
public class PictureLengthRender {
    private static final String TAG = PictureLengthRender.class.getSimpleName();
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

            mPictureCircle.recycle();
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

        //这句话是数据拷贝，从CPU拷贝纸GPU显存中，耗时1-11毫秒不等。
        // 因此渲染一张固定图片时不建议把它放在onDrawFrame当中。除非数据一直在变
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, mBitmapWidth, mBitmapHeight, 0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, mBitmapBuffer);
        ShaderHelper.checkGLError("initTexture");
    }

    public void upData(@NonNull List<Anchor> anchors, @NonNull Camera camera) {
        upData(anchors, null, camera);
    }

    public void upData(@NonNull List<Anchor> anchors, @NonNull Anchor anchor, @NonNull Camera camera) {
        if (anchor != null) {
            positions = new float[3 * anchors.size() + 3];

            for (int i = 0; i < anchors.size(); i++) {
                positions[i * 3] = anchors.get(i).getPose().tx();
                positions[i * 3 + 1] = anchors.get(i).getPose().ty();
                positions[i * 3 + 2] = anchors.get(i).getPose().tz();
            }
            positions[anchors.size() * 3] = anchor.getPose().tx();
            positions[anchors.size() * 3 + 1] = anchor.getPose().ty();
            positions[anchors.size() * 3 + 2] = anchor.getPose().tz();
        } else {
            positions = new float[3 * anchors.size()];
            for (int i = 0; i < anchors.size(); i++) {
                positions[i * 3 + 0] = anchors.get(i).getPose().tx();
                positions[i * 3 + 1] = anchors.get(i).getPose().ty();
                positions[i * 3 + 2] = anchors.get(i).getPose().tz();
            }
        }

        //每个点3个分量，每两个点算出一个中心点
        pointCount = positions.length / 6;
        //中心点（两个点的中心）
        centerPosition = new float[pointCount * 3];
        vertexCoord = new float[centerPosition.length * 4];
        for (int i = 0; i < centerPosition.length / 3; i++) {
            centerPosition[i * 3] = (positions[i * 2 * 3] + positions[i * 2 * 3 + 3]) / 2;
            centerPosition[i * 3 + 1] = (positions[i * 2 * 3 + 1] + positions[i * 2 * 3 + 4]) / 2;
            centerPosition[i * 3 + 2] = (positions[i * 2 * 3 + 2] + positions[i * 2 * 3 + 5]) / 2;

            //第一个点
            vertexCoord[i * 3 * 4] = centerPosition[i * 3] - 0.3f;
            vertexCoord[i * 3 * 4 + 1] = centerPosition[i * 3 + 1];
            vertexCoord[i * 3 * 4 + 2] = centerPosition[i * 3 + 2] + 0.2f;

            //第二个点
            vertexCoord[i * 3 * 4 + 3] = centerPosition[i * 3] - 0.3f;
            vertexCoord[i * 3 * 4 + 4] = centerPosition[i * 3 + 1];
            vertexCoord[i * 3 * 4 + 5] = centerPosition[i * 3 + 2] - 0.2f;

            //第三个点
            vertexCoord[i * 3 * 4 + 6] = centerPosition[i * 3] + 0.3f;
            vertexCoord[i * 3 * 4 + 7] = centerPosition[i * 3 + 1];
            vertexCoord[i * 3 * 4 + 8] = centerPosition[i * 3 + 2] + 0.2f;

            //第四个点
            vertexCoord[i * 3 * 4 + 9] = centerPosition[i * 3] + 0.3f;
            vertexCoord[i * 3 * 4 + 10] = centerPosition[i * 3 + 1];
            vertexCoord[i * 3 * 4 + 11] = centerPosition[i * 3 + 2] - 0.2f;
        }

        ByteBuffer vertexBuffer = ByteBuffer.allocateDirect(vertexCoord.length * FLOAT_SIZE_BYTES);
        mVertexCoord = vertexBuffer.order(ByteOrder.nativeOrder()).asFloatBuffer();
        mVertexCoord.put(vertexCoord).position(0);

        float[] model = new float[16];
        float[] projectMatrix = new float[16];
        float[] viewMatrix = new float[16];

        Matrix.setIdentityM(mvp_Matrix, 0);
        if (anchor != null) {
            anchor.getPose().toMatrix(model, 0);
        } else {
            anchors.get(anchors.size() - 1).getPose().toMatrix(model, 0);
        }
        camera.getViewMatrix(viewMatrix, 0);
        camera.getProjectionMatrix(projectMatrix, 0, 0.1f, 100f);

        //MVP矩阵   乘法顺序为：P * V * M
        Matrix.multiplyMM(mvp_Matrix, 0, viewMatrix, 0, model, 0);
        Matrix.multiplyMM(mvp_Matrix, 0, projectMatrix, 0, mvp_Matrix, 0);

    }

    public void onDraw() {
        GLES20.glUseProgram(mProgram);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId[0]);
        ShaderHelper.checkGLError("onDraw");
        GLES20.glUniform1i(u_TextureUnit, 0);
        ShaderHelper.checkGLError("onDraw");
        GLES20.glUniformMatrix4fv(u_MvpMatrix, 1, false, mvp_Matrix, 0);
        ShaderHelper.checkGLError("onDraw");
        GLES20.glEnableVertexAttribArray(a_Position);
        ShaderHelper.checkGLError("onDraw");
        GLES20.glEnableVertexAttribArray(a_TexCoord);
        ShaderHelper.checkGLError("onDraw");
        GLES20.glVertexAttribPointer(a_Position, 3, GLES20.GL_FLOAT, false, 0, mVertexCoord);
        GLES20.glVertexAttribPointer(a_TexCoord, 2, GLES20.GL_FLOAT, false, 0, mTextureCoord);
        ShaderHelper.checkGLError("onDraw");
        for (int i = 0; i < pointCount; i++) {
            int j = i * 4;
            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, j, 4);
        }

//        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);

        GLES20.glDisableVertexAttribArray(a_TexCoord);
        GLES20.glDisableVertexAttribArray(a_Position);
        ShaderHelper.checkGLError("onDraw");
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        GLES20.glUseProgram(0);

        ShaderHelper.checkGLError("onDraw");
    }
}
