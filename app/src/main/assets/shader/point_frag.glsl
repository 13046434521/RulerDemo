precision mediump float;
uniform vec4 u_Color;
void main() {
    if (length(gl_PointCoord - vec2(0.5)) > 0.5){
        discard;
    }
    gl_FragColor = u_Color;
}
