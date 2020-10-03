#define step 3.0

uniform sampler2D u_texture;

uniform float u_time;
uniform float u_progress;
uniform vec4 u_color;
uniform vec2 u_uv;
uniform vec2 u_uv2;
uniform vec2 u_texsize;

varying vec4 v_color;
varying vec2 v_texCoords;

bool id(vec4 v){
    return v.a > 0.1;
}

bool id(vec2 coords, vec4 base){
    vec4 target = texture2D(u_texture, coords);
    return  target.a < 0.1 || (coords.x < u_uv.x || coords.y < u_uv.y || coords.x > u_uv2.x || coords.y > u_uv2.y);
}

bool cont(vec2 T, vec2 v){
    vec4 base = texture2D(u_texture, T);
    return base.a > 0.1 &&
           		(id(T + vec2(0, step) * v, base) || id(T + vec2(0, -step) * v, base) ||
           		id(T + vec2(step, 0) * v, base) || id(T + vec2(-step, 0) * v, base) ||
           		id(T + vec2(step, step) * v, base) || id(T + vec2(-step, -step) * v, base) ||
                           		id(T + vec2(step, -step) * v, base) || id(T + vec2(-step, step) * v, base));
}

void main() {
    vec2 coords = (v_texCoords.xy - u_uv) / (u_uv2 - u_uv);
    vec2 t = v_texCoords.xy;
    vec2 v = vec2(1.0/u_texsize.x, 1.0/u_texsize.y);

	vec4 c = texture2D(u_texture, v_texCoords.xy);
    float alpha = c.a;

    c.a *= u_progress;

    if(c.a > 0.01){
        float f = abs(sin(coords.x*2.0 + u_time));
        if(f > 0.9)
            f = 1.0;
        else
            f = 0.0;
        c = mix(c, u_color, f * u_color.a);
    }

    c.a *= alpha;

    gl_FragColor = c * v_color;
}
