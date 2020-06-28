uniform mat4 u_projTrans;

attribute vec4 a_position;
attribute vec2 a_texCoord0;
attribute vec4 a_color;

varying vec4 v_color;
varying vec2 v_texCoords;

uniform vec2 u_viewportInverse;

void main(){
    gl_Position = u_projTrans * a_position;
    v_texCoords = a_texCoord0;
    v_color = a_color;
}
