attribute vec4 a_position;

varying vec4 v_position;
varying mat4 v_model;

uniform mat4 u_model;
uniform mat4 u_projection;

void main(){
    v_position = a_position;
    v_model = u_model;
    gl_Position = u_projection*u_model*a_position;
}