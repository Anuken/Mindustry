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
	
	vec3 original = texture2D(u_texture0, v_texCoords).rgb;
	vec3 bloom = texture2D(u_texture1, v_texCoords).rgb * BloomIntensity; 	
    original = OriginalIntensity * (original - original * bloom);	 	
 	gl_FragColor.rgb =  original + bloom; 	
}