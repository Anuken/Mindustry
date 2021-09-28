#define HIGHP

//shades of cryofluid
#define S1 vec3(53.0, 83.0, 93.0) / 100.0
#define S2 vec3(68.0, 90.0, 97.0) / 100.0
#define NSCALE 100.0 / 2.0

uniform sampler2D u_texture;
uniform sampler2D u_noise;

uniform vec2 u_campos;
uniform vec2 u_resolution;
uniform float u_time;

varying vec2 v_texCoords;

void main(){
    vec2 c = v_texCoords.xy;
    vec2 coords = vec2(c.x * u_resolution.x + u_campos.x, c.y * u_resolution.y + u_campos.y);

    float btime = u_time / 5000.0;
    float wave = abs(sin(coords.x * 1.1 + coords.y) + 0.1 * sin(2.5 * coords.x) + 0.15 * sin(3.0 * coords.y)) / 30.0;
    float noise = wave + (texture2D(u_noise, (coords) / NSCALE + vec2(btime) * vec2(-0.2, 0.8)).r + texture2D(u_noise, (coords) / NSCALE + vec2(btime * 1.1) * vec2(0.8, -1.0)).r) / 2.0;
    vec4 color = texture2D(u_texture, c);

    if(noise > 0.54 && noise < 0.57){
        color.rgb = S2;
    }else if (noise > 0.49 && noise < 0.62){
        color.rgb = S1;
    }

    gl_FragColor = color;
}
