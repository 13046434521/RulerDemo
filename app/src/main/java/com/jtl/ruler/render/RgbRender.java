package com.jtl.ruler.render;

import android.content.Context;
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
    private static final String VERTEX_SHADER = "/shader/rgb_vert.glsl";
    private static final String FRAGMENT_SHADER = "/shader/rgb_frag.glsl";

    private int mProgram;
    private int[] textureId =new int[1];
    private int a_Position;
    private int u_TextureUnit;
    private int a_TexCoord;

    private FloatBuffer vertexCoords;
    private FloatBuffer textureCoords;
    private static final int FLOAT_SIZE_BYTES = 4 ;
    private static final int TEXTURE_SIZE = 4 * 2 ;
    private float width;
    private float height;

    protected  void createdGLThread(Context context){
        initProgram(context);
        initData();
        initTexture();
    }
    private void initProgram(Context context) {
        mProgram = GLES20.glCreateProgram();
        int vertexShader = ShaderHelper.loadGLShader(TAG,context,GLES20.GL_VERTEX_SHADER,VERTEX_SHADER);
        int fragmentShader= ShaderHelper.loadGLShader(TAG,context,GLES20.GL_FRAGMENT_SHADER,FRAGMENT_SHADER);

        GLES20.glLinkProgram(mProgram);
        GLES20.glUseProgram(mProgram);

        //关联Shader
        GLES20.glAttachShader(mProgram,vertexShader);
        GLES20.glAttachShader(mProgram,fragmentShader);

        a_Position = GLES20.glGetAttribLocation(mProgram,"a_Position");
        a_TexCoord = GLES20.glGetAttribLocation(mProgram,"a_TexCoord");
        u_TextureUnit = GLES20.glGetUniformLocation(mProgram,"u_TextureCoord");

        //解绑Shader
        GLES20.glDetachShader(mProgram,vertexShader);
        GLES20.glDetachShader(mProgram,fragmentShader);
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


        ByteBuffer textureBuffer = ByteBuffer.allocateDirect(TEXTURE_SIZE * FLOAT_SIZE_BYTES);
        textureBuffer.order(ByteOrder.nativeOrder());
        textureCoords = textureBuffer.asFloatBuffer();
        textureCoords.put(TEXTURE_COORDS).position(0);
    }

    private void initTexture() {
        GLES20.glGenTextures(1,textureId,0);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,textureId[0]);

        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);

        ShaderHelper.checkGLError("initTexture");
    }


    protected  void onSurfaceChanged(float width, float height){
        this.width = width;
        this.height = height;
    }


    protected  void onDraw(@NonNull Frame frame){
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




        ShaderHelper.checkGLError("onDraw");
    }
    //顶点坐标
    private static final float[] QUAD_COORDS =
            new float[] {
                    -1.0f, -1.0f, -1.0f, +1.0f, +1.0f, -1.0f, +1.0f, +1.0f,
            };

    //纹理坐标
    private static final float[] TEXTURE_COORDS = new float[]{
            0.0f, 0.0f,
            0.0f, 1.0f,
            1.0f, 0.0f,
            1.0f, 1.0f,
    };
}
