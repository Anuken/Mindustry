#ifdef GL_ES
precision mediump float;
precision mediump int;
#endif

#define p1 vec3(255.0,211.0,127.0)/255.0
#define p2 vec3(234.0,182.0,120.0)/255.0
#define p3 vec3(212.0,129.0,107.0)/255.0
#define p4 vec3(142.0,77.0,72.0)/255.0
#define roundm 0.2

uniform sampler2D u_texture;
uniform vec2 u_resolution;
uniform int u_time;
uniform vec2 u_uv;
uniform vec2 u_uv2;
uniform float u_scl;

varying vec4 v_color;
varying vec2 v_texCoord;

void main() {
    ivec2 coords = ivec2((gl_FragCoord.xy - u_resolution/2.0)/u_scl);

	//int roundx = 8;
	//int roundy = roundx;

	//coords.x = (coords.x / roundx) * roundx;
	//coords.y = (coords.y / roundy) * roundy;

	float d = (abs(float(coords.x)) - abs(float(coords.y)));

	float m = abs(sin(-float(u_time)/50.0 + d/120.0));

	if(m > 0.95) gl_FragColor.rgb = p1;
	else if(m > 0.75) gl_FragColor.rgb = p2;
	else if(m > 0.55) gl_FragColor.rgb = p3;
	else if(m > 0.35) gl_FragColor.rgb = p4;
	else gl_FragColor.rgb = vec3(0.0);

	gl_FragColor.rgb *= 0.5;

    gl_FragColor.a = mod(abs(float(coords.x)) + abs(float(coords.y)), 110.0) < 35.0 ? 1.0 : 0.0;
}