attribute  vec4 a_Position;
attribute vec2 a_TexCoord;
varying vec2 v_TexCoord;
uniform mat4 u_MvpMatrix;
uniform float u_PointSize;
void main() {
    gl_Position = u_MvpMatrix *a_Position;
    gl_PointSize = u_PointSize;
    v_TexCoord = a_TexCoord;
}
