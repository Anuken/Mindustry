#ifdef GL_ES
precision highp float;
precision mediump int;
#endif

//shades of slag
#define S2 vec3(100.0, 93.0, 49.0) / 100.0
#define S1 vec3(100.0, 60.0, 25.0) / 100.0
#define NSCALE 300.0

uniform sampler2D u_texture;
uniform sampler2D u_noise;

uniform vec2 u_campos;
uniform vec2 u_resolution;
uniform float u_time;

varying vec4 v_color;
varying vec2 v_texCoord;

void main(){
    vec2 c = v_texCoord.xy;
    vec2 coords = vec2(c.x * u_resolution.x + u_campos.x, c.y * u_resolution.y + u_campos.y);

    float btime = u_time / 4000.0;
    float noise = (texture2D(u_noise, (coords) / NSCALE + vec2(btime) * vec2(-0.9, 0.8)).r + texture2D(u_noise, (coords) / NSCALE + vec2(btime * 1.1) * vec2(0.8, -1.0)).r) / 2.0;
    vec3 color = texture2D(u_texture, c).rgb;

    if(noise > 0.6){
        color = S2;
    }else if(noise > 0.54){
        color = S1;
    }

    gl_FragColor = vec4(color.rgb, 1.0);
}