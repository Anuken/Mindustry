
varying vec2 v_texCoords;

uniform sampler2D u_texture0;
uniform float u_bias;

void main() {
	vec4 tex = texture2D(u_texture0, v_texCoords);
	float avg = (tex.r + tex.g + tex.b) / 3.0;
	gl_FragColor = vec4(max(0.0, avg + u_bias)) * 50.0;
}