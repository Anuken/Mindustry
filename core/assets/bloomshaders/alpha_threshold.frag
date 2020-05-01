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
	vec4 color = texture2D(u_texture0, v_texCoords);
	if(color.r + color.g + color.b > 0.5 * 3.0){
		gl_FragColor = color;
	}else{
		gl_FragColor = vec4(0.0);
	}
 	//gl_FragColor = (texture2D(u_texture0, v_texCoords) - vec4(threshold.r))  * threshold.g;
}