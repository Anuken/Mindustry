attribute vec4 a_position;
attribute vec3 a_normal;
attribute vec4 a_color;

uniform mat4 u_proj;
uniform mat4 u_trans;
uniform vec3 u_lightdir;
uniform vec3 u_camdir;
uniform vec3 u_campos;
uniform vec3 u_ambientColor;

varying vec4 v_col;

const vec3 diffuse = vec3(0.01);

void main(){
    vec3 specular = vec3(0.0, 0.0, 0.0);

    //TODO this calculation is probably wrong
    vec3 lightReflect = normalize(reflect(a_normal, u_lightdir));
    vec3 vertexEye = normalize(u_campos - (u_trans * a_position).xyz);
    float specularFactor = dot(vertexEye, lightReflect);
    if(specularFactor > 0.0){
        specular = vec3(1.0 * pow(specularFactor, 40.0)) * (1.0-a_color.a);
    }

	vec3 norc = (u_ambientColor + specular) * (diffuse + vec3(clamp((dot(a_normal, u_lightdir) + 1.0) / 2.0, 0.0, 1.0)));

	v_col = vec4(a_color.rgb, 1.0) * vec4(norc, 1.0);
    gl_Position = u_proj * u_trans * a_position;
}
