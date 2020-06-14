#define HIGHP

#define MAX_HITS 64
#define HIT_RADIUS 12.0
#define ALPHA 0.18
#define thickness 1.0
#define step 2.0

uniform sampler2D u_texture;
uniform vec2 u_texsize;
uniform float u_time;
uniform float u_dp;
uniform vec2 u_offset;
uniform vec4 u_shieldcolor;

varying vec2 v_texCoords;

void main(){
    vec2 T = v_texCoords.xy;
    vec2 coords = (T * u_texsize) + u_offset;

    T += vec2(sin(coords.y / 3.0 + u_time / 20.0), sin(coords.x / 3.0 + u_time / 20.0)) / u_texsize;
    
    float si = sin(u_time / 20.0) / 8.0;
	vec4 color = texture2D(u_texture, T);
	vec2 v = vec2(1.0/u_texsize.x, 1.0/u_texsize.y);

	if(texture2D(u_texture, T).a < 0.9 &&
       		(texture2D(u_texture, T + vec2(0, step) * v).a > 0.0 || texture2D(u_texture, T + vec2(0, -step) * v).a > 0.0 ||
       		texture2D(u_texture, T + vec2(step, 0) * v).a > 0.0 || texture2D(u_texture, T + vec2(-step, 0) * v).a > 0.0)){

		gl_FragColor = mix(u_shieldcolor, vec4(1.0), si);
	}else{

	    if(color.a > 0.0){
	        if(mod(coords.x / u_dp + coords.y / u_dp + sin(floor(coords.x / u_dp) / 5.0) * 3.0 + sin(floor(coords.y / u_dp) / 5.0) * 3.0  + u_time / 4.0, 10.0) < 2.0){
	            color *= 1.65;
	        }
	        
	        color.a = ALPHA;
	    }
		
		gl_FragColor = color;
	}
}
