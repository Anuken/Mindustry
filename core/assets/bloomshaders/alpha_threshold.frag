
uniform lowp sampler2D u_texture0;
uniform lowp vec2 threshold;
varying vec2 v_texCoords;

void main(){
	vec4 color = texture2D(u_texture0, v_texCoords);
	if(color.r + color.g + color.b > 0.5 * 3.0){
		gl_FragColor = color;
	}else{
		gl_FragColor = vec4(0.0);
	}
 	//gl_FragColor = (texture2D(u_texture0, v_texCoords) - vec4(threshold.r))  * threshold.g;
}