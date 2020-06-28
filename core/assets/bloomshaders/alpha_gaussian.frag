
uniform lowp sampler2D u_texture;
varying vec2 v_texCoords0;
varying vec2 v_texCoords1;
varying vec2 v_texCoords2;
varying vec2 v_texCoords3;
varying vec2 v_texCoords4;
const float center = 0.2270270270; 
const float close = 0.3162162162;
const float far = 0.0702702703;

void main(){
   gl_FragColor = far * texture2D(u_texture, v_texCoords0) 
				+ close * texture2D(u_texture, v_texCoords1)
				+ center * texture2D(u_texture, v_texCoords2) 
				+ close * texture2D(u_texture, v_texCoords3)
				+ far * texture2D(u_texture, v_texCoords4); 
}	
