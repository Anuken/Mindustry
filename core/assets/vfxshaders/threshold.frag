
uniform sampler2D u_texture0;
uniform float treshold;
uniform float tresholdInvTx;
varying vec2 v_texCoords;

void main(){
	vec4 tex = texture2D(u_texture0, v_texCoords);
	//gl_FragColor = vec4(tex.a);

	// adjust it to keep only values brighter than the specified
	// threshold, scaling back up to preserve full color range

	// threshold
	//gl_FragColor.rgb = (tex.rgb-treshold) * tresholdInvTx;

	// masked threshold (use texture's alpha channel)
	//gl_FragColor = vec4((tex.rgb-treshold) * tresholdInvTx * tex.a, tex.a);

	// alpha threshold
	gl_FragColor = (tex-treshold) * tresholdInvTx;
}