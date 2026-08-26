#define HIGHP

in vec3 a_position;

out vec3 v_position;

uniform mat4 u_projView;
uniform mat4 u_trans;
uniform float u_outerRadius;

void main(){
    vec4 pos = u_trans * vec4(a_position * u_outerRadius, 1.0);

    v_position = pos.xyz;
    gl_Position = u_projView * pos;
}