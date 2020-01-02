precision mediump float;
uniform sampler2D u_TextureUnit;
varying vec2 v_TexCoord;
uniform vec4 u_Color;
void main() {
//    gl_FragColor = texture2D(u_TextureUnit, v_TexCoord);
//    if (length(gl_PointCoord - vec2(0.5)) > 0.5){
//        discard;
//    }
    gl_FragColor = u_Color;
}
