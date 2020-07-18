
attribute vec4 a_position;
attribute vec2 a_texCoord0; 
uniform vec2 dir;
uniform vec2 size;
varying vec2 v_texCoords0;
varying vec2 v_texCoords1;
varying vec2 v_texCoords2;
varying vec2 v_texCoords3;
varying vec2 v_texCoords4;
const vec2 futher = vec2(3.2307692308, 3.2307692308);
const vec2 closer = vec2(1.3846153846, 1.3846153846);

void main(){
	vec2 sizeAndDir = dir / size;
	vec2 f = futher*sizeAndDir;
	vec2 c = closer*sizeAndDir;
	
	v_texCoords0 = a_texCoord0 - f;
	v_texCoords1 = a_texCoord0 - c;	
	v_texCoords2 = a_texCoord0;
	v_texCoords3 = a_texCoord0 + c;
	v_texCoords4 = a_texCoord0 + f;
	
	gl_Position = a_position;
}