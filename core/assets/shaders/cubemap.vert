attribute vec3 a_position;

varying vec3 v_texCoords;

uniform mat4 u_proj;

const float SCALE = 50.0;

void main(){
    v_texCoords = a_position;
    gl_Position = u_proj * vec4(a_position * SCALE, 1.0);
}