uniform lowp sampler2D u_texture0;
uniform lowp vec2 threshold;
varying mediump vec2 v_texCoords;

void main(){
 	gl_FragColor.rgb = (texture2D(u_texture0, v_texCoords).rgb - vec3(threshold.x))  * threshold.y;
}