#ifdef GL_ES
precision mediump float;
precision lowp int;
#endif

#define step 0.5

const int MAX_COLORS = 10;

varying float v_height;

uniform sampler2D u_colors;

void main(){
    gl_FragColor = texture2D(u_colors, vec2(v_height, 0.0));
}