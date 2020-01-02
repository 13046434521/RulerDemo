package com.jtl.ruler.render;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.support.annotation.NonNull;

import com.google.ar.core.Anchor;
import com.google.ar.core.Camera;
import com.jtl.ruler.helper.ShaderHelper;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.List;

/**
 * @author：TianLong
 * @date：2020/1/2 14:18
 */
public class LineRender {
    private static final String TAG = LineRender.class.getSimpleName();
    private static final String VERTEX_SHADER = "shader/line_vert.glsl";
    private static final String FRAGMENT_SHADER = "shader/line_frag.glsl";
    private static final int FLOAT_SIZE_BYTES = 4;
    private int mProgram;
    private int[] textureId;

    private int a_Position;
    private int u_Color;
    private int u_MvpMatrix;

    private float[] mvp_Matrix;
    private float[] point_Color;
    private FloatBuffer mVertexCoord;
    private float[] positions;

    public void createdGLThread(Context context) {
        initProgram(context);
        initData();
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
        u_MvpMatrix = GLES20.glGetUniformLocation(mProgram, "u_MvpMatrix");
        u_Color = GLES20.glGetUniformLocation(mProgram, "u_Color");
        //解绑Shader
        GLES20.glDetachShader(mProgram, vertexShader);
        GLES20.glDetachShader(mProgram, fragmentShader);
        //删除Shader
        GLES20.glDeleteShader(vertexShader);
        GLES20.glDeleteShader(fragmentShader);

        ShaderHelper.checkGLError("initProgram");
    }

    private void initData() {
        mvp_Matrix = new float[16];
        point_Color = new float[]{1f, 0f, 0f, 0f};
//        ByteBuffer vertexBuffer = ByteBuffer.allocateDirect(3 * 10 * FLOAT_SIZE_BYTES);
//        mVertexCoord = vertexBuffer.order(ByteOrder.nativeOrder()).asFloatBuffer();
//        mVertexCoord.position(0);
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

    private volatile int pointCount = 0;

    public void upData(@NonNull List<Anchor> anchors, @NonNull Camera camera) {
        upData(anchors, null, camera);
    }

    public void upData(@NonNull List<Anchor> anchors, @NonNull Anchor anchor, @NonNull Camera camera) {
        if (anchor != null) {
            positions = new float[3 * anchors.size() + 3];
            pointCount = anchors.size() + 1;
            for (int i = 0; i < anchors.size(); i++) {
                positions[i * 3 + 0] = anchors.get(i).getPose().tx();
                positions[i * 3 + 1] = anchors.get(i).getPose().ty();
                positions[i * 3 + 2] = anchors.get(i).getPose().tz();
            }
            positions[anchors.size() * 3 + 0] = anchor.getPose().tx();
            positions[anchors.size() * 3 + 1] = anchor.getPose().ty();
            positions[anchors.size() * 3 + 2] = anchor.getPose().tz();
        } else {
            positions = new float[3 * anchors.size()];
            pointCount = anchors.size();
            for (int i = 0; i < anchors.size(); i++) {
                positions[i * 3 + 0] = anchors.get(i).getPose().tx();
                positions[i * 3 + 1] = anchors.get(i).getPose().ty();
                positions[i * 3 + 2] = anchors.get(i).getPose().tz();
            }
        }


        ByteBuffer vertexBuffer = ByteBuffer.allocateDirect(positions.length * FLOAT_SIZE_BYTES);
        mVertexCoord = vertexBuffer.order(ByteOrder.nativeOrder()).asFloatBuffer();
        mVertexCoord.put(positions).position(0);

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

        GLES20.glUniform4fv(u_Color, 1, point_Color, 0);
        GLES20.glUniformMatrix4fv(u_MvpMatrix, 1, false, mvp_Matrix, 0);

        GLES20.glEnableVertexAttribArray(a_Position);
        GLES20.glVertexAttribPointer(a_Position, 3, GLES20.GL_FLOAT, false, 0, mVertexCoord);
        GLES20.glLineWidth(5);

        for (int i = 0; i < pointCount / 2; i++) {
            int j = i * 2;
            GLES20.glDrawArrays(GLES20.GL_LINES, j, 2);
        }

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        GLES20.glUseProgram(0);

        ShaderHelper.checkGLError("onDraw");
    }
}
