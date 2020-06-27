#define HIGHP

#define ALPHA 0.18
#define step 2.0

uniform sampler2D u_texture;
uniform vec2 u_texsize;
uniform vec2 u_invsize;
uniform float u_time;
uniform float u_dp;
uniform vec2 u_offset;

varying vec2 v_texCoords;

void main(){
    vec2 T = v_texCoords.xy;
    vec2 coords = (T * u_texsize) + u_offset;

    T += vec2(sin(coords.y / 3.0 + u_time / 20.0), sin(coords.x / 3.0 + u_time / 20.0)) / u_texsize;

	vec4 color = texture2D(u_texture, T);
	vec2 v = u_invsize;

    vec4 maxed = max(max(max(texture2D(u_texture, T + vec2(0, step) * v), texture2D(u_texture, T + vec2(0, -step) * v)), texture2D(u_texture, T + vec2(step, 0) * v)), texture2D(u_texture, T + vec2(-step, 0) * v));

	if(texture2D(u_texture, T).a < 0.9 && maxed.a > 0.9){

		gl_FragColor = vec4(maxed.rgb, maxed.a * 100.0);
	}else{

	    if(color.a > 0.0){
	        if(mod(coords.x / u_dp + coords.y / u_dp + sin(coords.x / u_dp / 5.0) * 3.0 + sin(coords.y / u_dp / 5.0) * 3.0  + u_time / 4.0, 10.0) < 2.0){
	            color *= 1.65;
	        }
	        
	        color.a = ALPHA;
	    }
		
		gl_FragColor = color;
	}
}
