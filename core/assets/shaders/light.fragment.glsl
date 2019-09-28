#ifdef GL_ES
precision mediump float;
precision mediump int;
#endif

#define steprad 0.13

uniform sampler2D u_texture;
uniform vec4 u_ambient;

varying vec4 v_color;
varying vec2 v_texCoord;

float stepped(float inp){
    return inp;
}

void main(){
	vec4 color = texture2D(u_texture, v_texCoord.xy);
	float rounded = stepped(color.a);
	gl_FragColor = clamp(vec4(mix(u_ambient.rgb, color.rgb, rounded), u_ambient.a - rounded), 0.0, 1.0);
}
