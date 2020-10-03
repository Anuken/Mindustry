attribute vec4 a_position;
attribute vec3 a_normal;
attribute vec4 a_color;

uniform mat4 u_proj;
uniform mat4 u_trans;

varying vec4 v_col;

void main(){
	v_col = a_color;
    gl_Position = u_proj * u_trans * a_position;
}
