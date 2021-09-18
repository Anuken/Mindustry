attribute vec4 a_position;
attribute vec3 a_normal;
attribute vec4 a_color;

uniform mat4 u_proj;
uniform mat4 u_trans;
uniform vec3 u_lightdir;
uniform vec3 u_camdir;
uniform vec3 u_ambientColor;

varying vec4 v_col;

const vec3 diffuse = vec3(0.01);
const float shinefalloff = 4.0;
const float shinelen = 0.2;

void main(){
    vec3 specular = vec3(0.0, 0.0, 0.0);

    vec3 lightReflect = normalize(reflect(a_normal, u_lightdir));
    float specularFactor = dot(u_camdir, lightReflect);
    if(specularFactor > 0.0){
        specular = vec3(1.0 * pow(specularFactor, 64.0)) * (1.0-a_color.a);  //specular power = 32
    }

	vec3 norc = (u_ambientColor + specular) * (diffuse + vec3(clamp((dot(a_normal, u_lightdir) + 1.0) / 2.0, 0.0, 1.0)));

	v_col = vec4(a_color.rgb, 1.0) * vec4(norc, 1.0);
    gl_Position = u_proj * u_trans * a_position;
}
