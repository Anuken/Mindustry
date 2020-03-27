#ifdef GL_ES
#define LOWP lowp
#define MED mediump
precision lowp float;
#else
#define LOWP  
#define MED 
#endif
uniform sampler2D u_texture0;
uniform vec2 threshold;
varying MED vec2 v_texCoords;
void main()
{
	vec4 tex = texture2D(u_texture0, v_texCoords);
	vec3 colors = (tex.rgb - threshold.r)  * threshold.g * tex.a;
 	gl_FragColor = vec4(colors, tex.a);
}