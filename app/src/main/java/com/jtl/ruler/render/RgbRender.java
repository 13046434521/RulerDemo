package com.jtl.ruler.render;

import android.content.Context;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.support.annotation.NonNull;

import com.google.ar.core.Coordinates2d;
import com.google.ar.core.Frame;
import com.jtl.ruler.helper.ShaderHelper;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * @author：TianLong
 * @date：2019/11/29 16:57
 */
public class RgbRender {
    private static final String TAG = RgbRender.class.getSimpleName();
    private static final String VERTEX_SHADER = "shader/rgb_vert.glsl";
    private static final String FRAGMENT_SHADER = "shader/rgb_frag.glsl";

    private int mProgram;
    //默认顶点坐标
    private static final float[] QUAD_COORDS =
            new float[]{
                    -1.0f, -1.0f, -1.0f, +1.0f, +1.0f, -1.0f, +1.0f, +1.0f,
            };
    private int a_Position;
    private int u_TextureUnit;
    private int a_TexCoord;

    private FloatBuffer vertexCoords;
    private FloatBuffer textureCoords;
    private static final int FLOAT_SIZE_BYTES = 4;
    private float width;
    private float height;
    private int textureId = -1;

    public void createdGLThread(Context context) {
        initProgram(context);
        initData();
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
        u_TextureUnit = GLES20.glGetUniformLocation(mProgram, "u_TextureUnit");

        //解绑Shader
        GLES20.glDetachShader(mProgram, vertexShader);
        GLES20.glDetachShader(mProgram, fragmentShader);
        //删除Shader
        GLES20.glDeleteShader(vertexShader);
        GLES20.glDeleteShader(fragmentShader);

        ShaderHelper.checkGLError("initProgram");
    }

    private void initData() {
        ByteBuffer vertexBuffer = ByteBuffer.allocateDirect(QUAD_COORDS.length * FLOAT_SIZE_BYTES);
        vertexBuffer.order(ByteOrder.nativeOrder());
        vertexCoords = vertexBuffer.asFloatBuffer();
        vertexCoords.put(QUAD_COORDS).position(0);

        ByteBuffer textureBuffer = ByteBuffer.allocateDirect(8 * FLOAT_SIZE_BYTES);
        textureBuffer.order(ByteOrder.nativeOrder());
        textureCoords = textureBuffer.asFloatBuffer();
        textureCoords.position(0);
    }

    private void initTexture() {
        int[] texture = new int[1];
        GLES20.glGenTextures(1, texture, 0);
        textureId = texture[0];

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId);

        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);

        ShaderHelper.checkGLError("initTexture");
    }

    public void onSurfaceChanged(float width, float height) {
        this.width = width;
        this.height = height;
    }

    public int getTextureId() {
        return textureId;
    }

    public void onDraw(@NonNull Frame frame) {
        // 设备如果旋转，需要重新映射UV坐标
        if (frame.hasDisplayGeometryChanged()) {
            frame.transformCoordinates2d(
                    Coordinates2d.OPENGL_NORMALIZED_DEVICE_COORDINATES,
                    vertexCoords,
                    Coordinates2d.TEXTURE_NORMALIZED,
                    textureCoords);
        }

        if (frame.getTimestamp() == 0) {
            return;
        }

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glUseProgram(mProgram);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId);
        GLES20.glUniform1i(u_TextureUnit, 0);

        GLES20.glEnableVertexAttribArray(a_Position);
        GLES20.glEnableVertexAttribArray(a_TexCoord);

        GLES20.glVertexAttribPointer(a_Position, 2, GLES20.GL_FLOAT, false, 0, vertexCoords);
        GLES20.glVertexAttribPointer(a_TexCoord, 2, GLES20.GL_FLOAT, false, 0, textureCoords);

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);

        GLES20.glDisableVertexAttribArray(a_Position);
        GLES20.glDisableVertexAttribArray(a_TexCoord);

        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 0);
        GLES20.glUseProgram(0);
        ShaderHelper.checkGLError("onDraw");
    }
}
