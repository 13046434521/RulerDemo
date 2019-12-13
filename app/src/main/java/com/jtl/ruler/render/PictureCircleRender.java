package com.jtl.ruler.render;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.support.annotation.NonNull;
import android.util.Log;

import com.google.ar.core.Anchor;
import com.google.ar.core.Camera;
import com.jtl.ruler.helper.ShaderHelper;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * @author：TianLong
 * @date：2019/12/1 12:47
 */
public class PictureCircleRender {
    private static final String TAG = PictureCircleRender.class.getSimpleName();
    private static final String FRAGMENT_SHADER = "shader/picture_circle_frag.glsl";
    private static final String VERTEX_SHADER = "shader/picture_circle_vert.glsl";
    //    private static final String PATH = "pointCircle.png";
    private static final String PATH = "point.jpeg";
    private static final int FLOAT_SIZE_BYTES = 4;
    private int a_Position;
    private int u_mvpMatrix;
    private int a_TexCoord;
    private int u_TextureUnit;
    private int mProgram;
    private int textureId;

    private FloatBuffer mTextureCoord;
    private FloatBuffer mVertexCoord;
    private ByteBuffer mBitmapBuffer;
    private Bitmap mPictureCircle;
    private int mBitmapHeight;
    private int mBitmapWidth;

    private float[] mvpMatrix = new float[16];

    public void createdGLThread(Context context) {
        initProgram(context);
        initData(context);
        initTexture();
    }

    private void initProgram(Context context) {
        mProgram = GLES20.glCreateProgram();
        int fragmentShader = ShaderHelper.loadGLShader(TAG, context, GLES20.GL_FRAGMENT_SHADER, FRAGMENT_SHADER);
        int vertexShader = ShaderHelper.loadGLShader(TAG, context, GLES20.GL_VERTEX_SHADER, VERTEX_SHADER);
        //关联Shader
        GLES20.glAttachShader(mProgram, vertexShader);
        GLES20.glAttachShader(mProgram, fragmentShader);

        GLES20.glLinkProgram(mProgram);
        GLES20.glUseProgram(mProgram);

        a_Position = GLES20.glGetAttribLocation(mProgram, "a_Position");
        a_TexCoord = GLES20.glGetAttribLocation(mProgram, "a_TexCoord");
        u_mvpMatrix = GLES20.glGetUniformLocation(mProgram, "u_mvpMatrix");
        u_TextureUnit = GLES20.glGetUniformLocation(mProgram, "u_TextureCoord");

        //解绑Shader
        GLES20.glDetachShader(mProgram, vertexShader);
        GLES20.glDetachShader(mProgram, fragmentShader);
        //删除Shader
        GLES20.glDeleteShader(vertexShader);
        GLES20.glDeleteShader(fragmentShader);

        ShaderHelper.checkGLError("initProgram");
    }

    private void initData(Context context) {
        ByteBuffer textureBuffer = ByteBuffer.allocateDirect(textureCoord.length * FLOAT_SIZE_BYTES);
        mTextureCoord = textureBuffer.order(ByteOrder.nativeOrder()).asFloatBuffer();
        mTextureCoord.put(textureCoord).position(0);

        ByteBuffer vertexBuffer = ByteBuffer.allocateDirect(vertexCoord.length * FLOAT_SIZE_BYTES);
        mVertexCoord = vertexBuffer.order(ByteOrder.nativeOrder()).asFloatBuffer();
        mVertexCoord.put(vertexCoord).position(0);

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
        int[] texture = new int[1];
        GLES20.glGenTextures(1, texture, 0);
        textureId = texture[0];

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);

        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);

        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, mBitmapWidth, mBitmapHeight, 0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, mBitmapBuffer);
        ShaderHelper.checkGLError("initTexture");
    }

    public void upData(@NonNull Anchor anchor, @NonNull Camera camera) {
        if (anchor == null || camera == null) {
            Log.w(TAG, (anchor == null ? "anchor==null" : "anchor!=null") + "   " + (camera == null ? "camera==null" : "camera!=null"));
            return;
        }

        float[] model = new float[16];
        float[] viewMatrix = new float[16];
        float[] projectMatrix = new float[16];
        mvpMatrix = new float[16];

        anchor.getPose().toMatrix(model, 0);        //横排列改为竖排列
        camera.getViewMatrix(viewMatrix, 0);
        camera.getProjectionMatrix(projectMatrix, 0, 0.1f, 1000f);

        //MVP矩阵   乘法顺序为：P * V * M
        Matrix.multiplyMM(mvpMatrix, 0, viewMatrix, 0, model, 0);
        Matrix.multiplyMM(mvpMatrix, 0, projectMatrix, 0, mvpMatrix, 0);
        Matrix.setIdentityM(mvpMatrix, 0);
        Log.w(TAG, "upData");
    }

    public void onDraw() {
        Matrix.setIdentityM(mvpMatrix, 0);

        GLES20.glUseProgram(mProgram);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);

        GLES20.glEnableVertexAttribArray(a_Position);
        GLES20.glEnableVertexAttribArray(a_TexCoord);
        GLES20.glUniformMatrix4fv(u_mvpMatrix, 1, false, mvpMatrix, 0);
        GLES20.glUniform1i(u_TextureUnit, 1);

        GLES20.glVertexAttribPointer(a_Position, 2, GLES20.GL_FLOAT, false, 0, mVertexCoord);
        GLES20.glVertexAttribPointer(a_TexCoord, 2, GLES20.GL_FLOAT, false, 0, mTextureCoord);
        GLES20.glDisableVertexAttribArray(a_Position);
        GLES20.glDisableVertexAttribArray(a_TexCoord);

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        GLES20.glUseProgram(0);

        ShaderHelper.checkGLError("onDraw");
    }


    //纹理坐标
    private float[] textureCoord = new float[]{
            0.0f, 0.0f,
            0.0f, 1.0f,
            1.0f, 0.0f,
            1.0f, 1.0f,
    };

    //顶点坐标
    private float[] vertexCoord = new float[]{
            -1.0f, 1.0f,
            -1.0f, -1.0f,
            1.0f, 1.0f,
            1.0f, -1.0f
    };
//    float distance = 0.06f;
//    float[] vertexCoord = new float[]{
//            -distance, +distance
//            -distance, -distance
//            +distance, +distance
//            +distance, -distance
//    };

//    private static final float position = 0.06f;
//    private static final float[] vertexCoord =
//            new float[]{
//                    -position, 0.0f, -position,
//                    +position, 0.0f, -position,
//                    +position, 0.0f, +position,
//                    -position, 0.0f, -position,
//                    +position, 0.0f, +position,
//                    -position, 0.0f, +position,
//            };
//    private static final float[] textureCoord = {
//            0.0f, 1.0f,
//            1.0f, 1.0f,
//            1.0f, 0.0f,
//            0.0f, 1.0f,
//            1.0f, 0.0f,
//            0.0f, 0.0f,
//    };
}
