#define HIGHP

#define NSCALE 200.0 / 1.8

uniform sampler2D u_texture;
uniform sampler2D u_noise;

uniform vec2 u_campos;
uniform vec2 u_resolution;
uniform float u_time;

varying vec2 v_texCoords;

void main(){
    vec2 c = v_texCoords.xy;
    vec2 coords = vec2(c.x * u_resolution.x + u_campos.x, c.y * u_resolution.y + u_campos.y);

    float btime = u_time / 3400.0;
    vec4 noise1 = texture2D(u_noise, (coords) / NSCALE + vec2(btime) * vec2(-0.9, 0.8));
    vec4 noise2 = texture2D(u_noise, (coords) / NSCALE + vec2(btime * 1.1) * vec2(0.8, -1.0));
    //vec4 noise3 = texture2D(u_noise, (coords) / (NSCALE * 2.0) + vec2(btime * 0.9) * vec2(0.8, 1.0));

    gl_FragColor = vec4(vec3(min(noise1.r, noise2.r)), 0.2);
}
