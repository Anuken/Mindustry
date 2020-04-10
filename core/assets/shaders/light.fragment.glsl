#ifdef GL_ES
precision mediump float;
precision mediump int;
#endif

#define steprad 0.13

uniform sampler2D u_texture;
uniform vec4 u_ambient;

varying vec4 v_color;
varying vec2 v_texCoord;

void main(){
	vec4 color = texture2D(u_texture, v_texCoord.xy);
	//color.a = clamp(color.a, 0.0, 0.8);
	gl_FragColor = clamp(vec4(mix(u_ambient.rgb, color.rgb, color.a), u_ambient.a - color.a), 0.0, 1.0);
}
