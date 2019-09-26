#ifdef GL_ES
#define MED mediump
#else  
#define MED 
#endif
attribute vec4 a_position; 
attribute vec2 a_texCoord0; 
varying MED vec2 v_texCoords;
void main()
{
	v_texCoords = a_texCoord0;
	gl_Position = a_position;
}