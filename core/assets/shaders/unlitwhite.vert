attribute vec4 a_position;
attribute vec3 a_normal;

uniform mat4 u_proj;
uniform mat4 u_trans;
uniform vec4 u_color;

varying vec4 v_col;

void main(){
  v_col = vec4(1.0);
  gl_Position = u_proj * u_trans * a_position;
}
