#define HIGHP

#define NSCALE 180.0 / 2.0

uniform sampler2D u_texture;
uniform sampler2D u_noise;

uniform vec2 u_campos;
uniform vec2 u_resolution;
uniform float u_time;

varying vec2 v_texCoords;

void main(){
    vec2 c = v_texCoords.xy;
    vec2 coords = vec2(c.x * u_resolution.x + u_campos.x, c.y * u_resolution.y + u_campos.y);

    float btime = u_time / 70000.0;
    float noise = sin((texture2D(u_noise, (coords) / NSCALE + vec2(btime) * vec2(-0.9, 0.8)).r + texture2D(u_noise, (coords) / NSCALE + vec2(abs(sin(btime)) * 1.1) * vec2(-0.8, -1.0)).r) / 2.0);
    vec4 color = texture2D(u_texture, c);

    if(noise > 0.54 && noise < 0.68){
        color.rgb *= vec3(1.4);
    }else if(!(noise > 0.40 && noise < 0.54)){
        color.rgb *= vec3(1.2);
    }

    gl_FragColor = color;
}
