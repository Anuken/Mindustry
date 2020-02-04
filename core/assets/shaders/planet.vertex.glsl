attribute vec4 a_position;
attribute vec3 a_normal;
attribute vec4 a_color;
uniform mat4 u_projModelView;
varying vec4 v_col;

const vec3 ambientColor = vec3(1.0);
const vec3 ambientDir = normalize(vec3(1.0, 1.0, 1.0));
const vec3 diffuse = vec3(0.5);
const vec3 v1 = vec3(1.0, 0.0, 1.0);
const vec3 v2 = vec3(1.0, 0.5, 0.0);

void main(){
	vec3 norc = ambientColor * clamp((dot(a_normal, ambientDir) + 1.0) / 2.0, 0.0, 1.0);

	v_col = a_color * vec4(norc, 1.0);
    gl_Position = u_projModelView * a_position;
}
