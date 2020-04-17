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
 	gl_FragColor.rgb = (texture2D(u_texture0, v_texCoords).rgb - vec3(threshold.x))  * threshold.y;
}