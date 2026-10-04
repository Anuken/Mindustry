
varying vec2 v_texCoord0;

uniform sampler2D u_texture0;
uniform float u_offsetX;
uniform float u_offsetY;

void main() {
	gl_FragColor = texture2D(u_texture0, (v_texCoord0) + vec2(u_offsetX, u_offsetY));
}