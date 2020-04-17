#ifdef GL_ES
#define LOWP lowp
#define MED mediump
precision lowp float;
#else
#define LOWP  
#define MED 
#endif

uniform sampler2D u_texture;
varying MED vec2 v_texCoords0;
varying MED vec2 v_texCoords1;
varying MED vec2 v_texCoords2;
varying MED vec2 v_texCoords3;
varying MED vec2 v_texCoords4;
const float center = 0.2270270270; 
const float close = 0.3162162162;
const float far = 0.0702702703;
void main()
{	 
   gl_FragColor = far * texture2D(u_texture, v_texCoords0) 
				+ close * texture2D(u_texture, v_texCoords1)
				+ center * texture2D(u_texture, v_texCoords2) 
				+ close * texture2D(u_texture, v_texCoords3)
				+ far * texture2D(u_texture, v_texCoords4); 
}	
