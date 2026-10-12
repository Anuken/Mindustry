
uniform sampler2D u_texture0;
uniform vec2 u_resolution;
uniform float u_time;
varying vec2 v_texCoords;

vec3 scanline(vec2 coord, vec3 screen) {
    const float scale = 0.66;
    const float amt = 0.02; // intensity of effect
    const float spd = 1.0; // speed of scrolling rows transposed per second

	screen.rgb += vec3(sin((coord.y / scale - (u_time * spd * 6.28))) * amt);
	return screen;
}

vec3 channelSplit(sampler2D tex, vec2 coord) {
    const float spread = 0.008;
	vec3 frag;
	frag.r = texture2D(tex, vec2(coord.x - spread * sin(u_time), coord.y)).r;
	frag.g = texture2D(tex, vec2(coord.x, 					     coord.y)).g;
	frag.b = texture2D(tex, vec2(coord.x + spread * sin(u_time), coord.y)).b;
	return frag;
}

void main() {
	vec2 uv = v_texCoords;
	vec3 channelSplit = channelSplit(u_texture0, uv);
	vec2 screenSpace = uv * u_resolution.xy;
	vec3 scanline = scanline(screenSpace, channelSplit);
	gl_FragColor = vec4(scanline, 1.0);
}