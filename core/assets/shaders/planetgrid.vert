attribute vec4 a_position;
attribute vec4 a_color;

uniform mat4 u_proj;
uniform mat4 u_trans;

varying vec4 v_col;
varying vec4 v_position;

void main() {
    gl_Position = u_proj * u_trans * a_position;
    v_col = a_color;
    v_position = a_position;
}
