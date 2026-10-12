uniform sampler2D u_texture0;
uniform sampler2D u_texture1;
uniform float u_src1Intensity;
uniform float u_src2Intensity;
uniform float u_src1Saturation;
uniform float u_src2Saturation;

varying vec2 v_texCoords;

// The constants 0.3, 0.59, and 0.11 are chosen because the
// human eye is more sensitive to green light, and less to blue.
const vec3 GRAYSCALE = vec3(0.3, 0.59, 0.11);

// 0 = totally desaturated
// 1 = saturation unchanged
// higher = increase saturation
//const float BaseSat = 1;
//const float BloomSat = 1;

vec3 adjustSaturation(vec3 color, float saturation){
	vec3 grey = vec3(dot(color, GRAYSCALE));
	//vec3 grey = vec3((color.r+color.g+color.b)*0.333);
	return mix(grey, color, saturation);
}

void main(){
	// lookup inputs
	vec4 src1 = texture2D(u_texture0, v_texCoords) * u_src1Intensity;
	vec4 src2 = texture2D(u_texture1, v_texCoords) * u_src2Intensity;

	// adjust color saturation and intensity
	src1.rgb = adjustSaturation(src1.rgb,u_src1Saturation);
	src2.rgb = adjustSaturation(src2.rgb,u_src2Saturation);

	// darken the base image in areas where ther's a lot of bloom
	// to prevent things looking excessively burned-out
	//original *= (1.0 - clamp(bloom, 0.0, 1.0));
	src1 *= (1.0 - src2);

	// combine
	gl_FragColor = src1 + src2;
}