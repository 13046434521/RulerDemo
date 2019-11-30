#extension GL_OES_EGL_image_external : require

precision mediump float;
uniform samplerExternalOES u_TextrueUnit;
varying vec2 v_TexCoord;

void main() {
    gl_FragColor = texture2D(u_TextrueUnit,v_TexCoord);
}
