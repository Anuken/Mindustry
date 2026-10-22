varying vec2 v_texCoords;

uniform sampler2D u_texture0;
uniform float u_vignetteIntensity;
uniform float u_vignetteX;
uniform float u_vignetteY;
uniform float u_centerX;
uniform float u_centerY;

#ifdef CONTROL_SATURATION
	const vec3 grayscale = vec3(0.3, 0.59, 0.11);

	uniform float u_saturation;
	uniform float u_saturationMul;

	// 0 = totally desaturated
	// 1 = saturation unchanged
	// higher = increase saturation
	vec3 adjustSaturation(vec3 color, float saturation) {
		vec3 grey = vec3(dot(color, grayscale));
		//vec3 grey = vec3((color.r + color.g + color.b) * 0.333);	// simple
		return mix(grey, color, saturation);	// correct
	}
#endif

#ifdef ENABLE_GRADIENT_MAPPING
	uniform sampler2D u_texture1;
	uniform float u_lutIntensity;

	uniform int u_lutIndex1;
	uniform int u_lutIndex2;
	uniform float u_lutIndexOffset;

	uniform float u_lutStep;
	uniform float u_lutStepOffset;

	vec3 doLookup(vec3 color) {
		vec3 curveColorA;
		vec3 curveColorB;

		float idxA = float(u_lutIndex1) * u_lutStep + u_lutStepOffset;
		float idxB = float(u_lutIndex2) * u_lutStep + u_lutStepOffset;

		curveColorA.r = texture2D(u_texture1, vec2(color.r, idxA)).r;
		curveColorA.g = texture2D(u_texture1, vec2(color.g, idxA)).g;
		curveColorA.b = texture2D(u_texture1, vec2(color.b, idxA)).b;

		curveColorB.r = texture2D(u_texture1, vec2(color.r, idxB)).r;
		curveColorB.g = texture2D(u_texture1, vec2(color.g, idxB)).g;
		curveColorB.b = texture2D(u_texture1, vec2(color.b, idxB)).b;

		return mix(color,mix(curveColorA,curveColorB,u_lutIndexOffset),u_lutIntensity);
	}
#endif

void main() {
	vec3 rgb = texture2D(u_texture0, v_texCoords).xyz;
	float d = distance(v_texCoords, vec2(u_centerX, u_centerY));
	float factor = smoothstep(u_vignetteX, u_vignetteY, d);
	rgb = rgb * factor + rgb * (1.0 - factor) * (1.0 - u_vignetteIntensity);

#ifdef CONTROL_SATURATION
	rgb = adjustSaturation(rgb, u_saturation) * u_saturationMul;
#endif

#ifdef ENABLE_GRADIENT_MAPPING
	// Theoretically, this conditional though still a branch instruction
	// should be able to shave some cycles instead of blindly performing
	// 3 lookups+mix with a 0 intensity... still, i don't like this
	if( u_lutIndex1 > -1 ) {
		rgb = doLookup(rgb);
	}
#endif

	gl_FragColor = vec4(rgb, 1);
}