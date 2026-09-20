#define HIGHP

in vec4 a_position;
in vec2 a_texCoord0;

out vec2 v_texCoords;

void main(){
    v_texCoords = a_texCoord0;
    gl_Position = a_position;
}