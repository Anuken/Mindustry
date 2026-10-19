
uniform sampler2D u_texture0;
uniform sampler2D u_texture1;
uniform float u_mix;

varying vec2 v_texCoords;

void main() {
	gl_FragColor = mix(
		texture2D(u_texture0, v_texCoords),
		texture2D(u_texture1, v_texCoords),
		u_mix);
}