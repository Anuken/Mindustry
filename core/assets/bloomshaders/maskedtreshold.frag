uniform lowp sampler2D u_texture0;
uniform lowp vec2 threshold;
varying vec2 v_texCoords;

void main(){
	vec4 tex = texture2D(u_texture0, v_texCoords);
	vec3 colors = (tex.rgb - threshold.r)  * threshold.g * tex.a;
 	gl_FragColor = vec4(colors, tex.a);
}