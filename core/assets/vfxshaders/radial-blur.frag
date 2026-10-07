#ifndef PASSES
#error Please define PASSES
#endif

const int passes = PASSES;

varying vec2 v_texCoord0;

uniform sampler2D u_texture0;
uniform float u_blurDiv;
uniform float u_offsetX;
uniform float u_offsetY;
uniform float u_zoom;

void main() {
	vec2 offset = vec2(u_offsetX, u_offsetY);
	vec4 color = vec4(0.0);
	float zoom = u_zoom;
	for( int i = 0; i < passes; ++i )	{
		color += texture2D(u_texture0, (v_texCoord0 * zoom) + offset);
		zoom += u_blurDiv;
	}
	gl_FragColor = color / vec4(float(passes));
}