#extension GL_OES_EGL_image_external : require

precision mediump float;
attribute vec4 a_Position;
attribute vec2 a_TexCoord;
uniform mat4 u_mvpMatrix;
varying vec2 v_TexCoord;

void main() {
    gl_Position = u_mvpMatrix * a_Position;
    v_TexCoord = a_TexCoord;
}
