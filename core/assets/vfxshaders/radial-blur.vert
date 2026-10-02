attribute vec4 a_position;
attribute vec2 a_texCoord0;

varying vec2 v_texCoord0;

uniform float u_offsetX;
uniform float u_offsetY;

void main() {
	v_texCoord0 = a_texCoord0 - vec2(u_offsetX, u_offsetY);
	gl_Position = a_position;
}