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

/**
 * @author：TianLong
 * @date：2019/12/1 12:47
 */
public class PictureCircleRender {
    private static final String TAG = PictureCircleRender.class.getSimpleName();
    private static final String FRAGMENT_SHADER = "shader/picture_circle_frag.glsl";
    private static final String VERTEX_SHADER = "shader/picture_circle_vert.glsl";
    private static final String PATH = "pointCircle.png";
    //    private static final String PATH = "picture.jpeg";
    private static final int FLOAT_SIZE_BYTES = 4;

    private int a_Position;
    private int u_MvpMatrix;
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
    /**
     * 纹理坐标
     */
    private float[] textureCoord = new float[]{
            0.0f, 0.0f,
            0.0f, 1.0f,
            1.0f, 0.0f,
            1.0f, 1.0f,
    };

    /**
     * 顶点坐标
     */
    private float[] vertexCoord = new float[]{
            -0.06f, 0, 0.06f,
            -0.06f, 0, -0.06f,
            0.06f, 0, 0.06f,
            0.06f, 0, -0.06f
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
        //关联Shader
        GLES20.glAttachShader(mProgram, vertexShader);
        GLES20.glAttachShader(mProgram, fragmentShader);

        GLES20.glLinkProgram(mProgram);
        GLES20.glUseProgram(mProgram);

        a_Position = GLES20.glGetAttribLocation(mProgram, "a_Position");
        a_TexCoord = GLES20.glGetAttribLocation(mProgram, "a_TexCoord");
        u_MvpMatrix = GLES20.glGetUniformLocation(mProgram, "u_MvpMatrix");
        u_TextureUnit = GLES20.glGetUniformLocation(mProgram, "u_TextureUnit");

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

            mPictureCircle.recycle();
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

        //这句话是数据拷贝，从CPU拷贝纸GPU内存中，耗时1-11毫秒不等。
        // 因此渲染一张固定图片时不建议把它放在onDrawFrame当中。除非数据一直在变
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, mBitmapWidth, mBitmapHeight, 0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, mBitmapBuffer);
        ShaderHelper.checkGLError("initTexture");
    }

    public void upData(@NonNull Anchor anchor, @NonNull Camera camera) {
        float[] model = new float[16];
        float[] viewMatrix = new float[16];
        float[] projectMatrix = new float[16];
        mvpMatrix = new float[16];

        //横排列改为竖排列
        anchor.getPose().toMatrix(model, 0);
        camera.getViewMatrix(viewMatrix, 0);
        camera.getProjectionMatrix(projectMatrix, 0, 0.1f, 100f);

        //MVP矩阵   乘法顺序为：P * V * M
        Matrix.multiplyMM(mvpMatrix, 0, viewMatrix, 0, model, 0);
        Matrix.multiplyMM(mvpMatrix, 0, projectMatrix, 0, mvpMatrix, 0);
    }

    public void onDraw() {
        GLES20.glUseProgram(mProgram);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);

        GLES20.glUniformMatrix4fv(u_MvpMatrix, 1, false, mvpMatrix, 0);
        GLES20.glUniform1i(u_TextureUnit, 0);

        GLES20.glEnableVertexAttribArray(a_Position);
        GLES20.glEnableVertexAttribArray(a_TexCoord);

        //开启混色
        GLES20.glEnable(GLES20.GL_BLEND);
        //混色方式设置
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);

        //public static void glVertexAttribPointer(插槽位置,有几个分量（x,y,z,w）,数据类型,是否归一化,0,数据)
        //告诉GPU如何遍历VBO的内存块
        GLES20.glVertexAttribPointer(a_Position, 3, GLES20.GL_FLOAT, false, 0, mVertexCoord);
        GLES20.glVertexAttribPointer(a_TexCoord, 2, GLES20.GL_FLOAT, false, 0, mTextureCoord);

        //绘制图元类型，从第几个点开始绘制，绘制多少个点
        //他会遍历，vbo里的数据。并把4个点分别传入shader里，他们的viewMatrix，projectMatrix，是一模一样的
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);

        GLES20.glDisableVertexAttribArray(a_TexCoord);
        GLES20.glDisableVertexAttribArray(a_Position);

        //关闭混色
        GLES20.glDisable(GLES20.GL_BLEND);

        //绑定纹理设置成0（解绑纹理）
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        //Program设置成0（解绑Program）
        GLES20.glUseProgram(0);

        ShaderHelper.checkGLError("onDraw");
    }
}
