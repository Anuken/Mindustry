
uniform sampler2D u_texture;
uniform vec4 u_ambient;

varying vec2 v_texCoords;

void main(){
	vec4 color = texture2D(u_texture, v_texCoords);
	gl_FragColor = clamp(vec4(mix(u_ambient.rgb, color.rgb, color.a), u_ambient.a - color.a), 0.0, 1.0);
}
