attribute  vec4 a_Position;
uniform mat4 u_MvpMatrix;
uniform float u_PointSize;
void main() {
    gl_Position = u_MvpMatrix *a_Position;
    gl_PointSize = u_PointSize;
}
