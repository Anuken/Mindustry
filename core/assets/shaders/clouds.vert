attribute vec4 a_position;
attribute vec3 a_normal;
attribute vec4 a_color;

uniform mat4 u_proj;
uniform mat4 u_trans;
uniform vec3 u_lightdir;
uniform vec3 u_ambientColor;
uniform float u_alpha;

varying vec4 v_col;

const vec3 diffuse = vec3(0.01);

void main(){
	vec3 norc = u_ambientColor * (diffuse + vec3(clamp((dot(a_normal, u_lightdir) + 1.0) / 2.0, 0.0, 1.0)));

	v_col = a_color * vec4(norc, u_alpha);
    gl_Position = u_proj * u_trans * a_position;
}
