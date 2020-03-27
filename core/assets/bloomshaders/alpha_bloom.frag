#ifdef GL_ES
#define LOWP lowp
#define MED mediump
precision lowp float;
#else
#define LOWP  
#define MED 
#endif
uniform sampler2D u_texture0;
uniform sampler2D u_texture1;
uniform float BloomIntensity;
uniform float OriginalIntensity;

varying MED vec2 v_texCoords;

void main()
{
	
	vec4 original = texture2D(u_texture0, v_texCoords) * OriginalIntensity;
	vec4 bloom = texture2D(u_texture1, v_texCoords) * BloomIntensity; 	
    original = original *  (vec4(1.0) - bloom);	 	
 	gl_FragColor =  original + bloom; 	
}