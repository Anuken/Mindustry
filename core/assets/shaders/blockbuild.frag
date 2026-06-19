#define HIGHP

uniform highp sampler2DArray u_texture;

uniform vec2 u_texsize;
uniform vec2 u_uv;
uniform vec2 u_uv2;
uniform float u_progress;
uniform float u_time;
uniform float u_alpha;

varying vec4 v_color;
varying vec3 v_texCoords;


bool id(vec2 coords, vec4 base, float depth){
    vec4 target = texture2D(u_texture, vec3(coords, depth));
    return  target.a < 0.1 || (coords.x < u_uv.x || coords.y < u_uv.y || coords.x > u_uv2.x || coords.y > u_uv2.y);
}

bool cont(vec3 coords, vec2 v){
    const float step = 3.5;
    vec4 base = texture(u_texture, coords);
    vec2 T = coords.xy;
    return base.a > 0.1 &&
           		(id(T + vec2(0, step) * v, base, coords.z) || id(T + vec2(0, -step) * v, base, coords.z) ||
           		id(T + vec2(step, 0) * v, base, coords.z) || id(T + vec2(-step, 0) * v, base, coords.z) ||
                id(T + vec2(step, step) * v, base, coords.z) || id(T + vec2(step, -step) * v, base, coords.z) ||
                id(T + vec2(-step, -step) * v, base, coords.z) || id(T + vec2(-step, step) * v, base, coords.z));
}

vec4 blend(vec4 dst, vec4 src){
    return src * src.a + dst * (1.0 - src.a);
}

void main(){

	vec2 v = vec2(1.0/u_texsize.x, 1.0/u_texsize.y);
	vec2 coords = (v_texCoords.xy-u_uv) / v;
	float value = coords.x + coords.y;

	vec4 color = texture(u_texture, v_texCoords);

	vec2 center = ((u_uv + u_uv2)/2.0 - u_uv) /v;
	float dst = (abs(center.x - coords.x) + abs(center.y - coords.y))/2.0;

	if((mod(u_time / 1.5 + value, 20.0) < 15.0 && cont(v_texCoords, v))){
        gl_FragColor = blend(color, v_color) * vec4(vec3(1.0), u_alpha);
    }else if(dst > (1.0-u_progress) * (center.x)){
        gl_FragColor = color * vec4(vec3(1.0), u_alpha);
    }else if((dst + 2.0 > (1.0-u_progress) * (center.x)) && color.a > 0.1){
        gl_FragColor = blend(color, v_color) * vec4(vec3(1.0), u_alpha);
    }else{
        gl_FragColor = vec4(0.0);
    }
}
