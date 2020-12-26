#define HIGHP

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
    vec4 color = texture2D(u_texture, T);

    color.a *= (0.37 + abs(sin(u_time / 15.0)) * .05 + 0.2 * (step(mod(coords.x / u_dp + coords.y / u_dp + u_time / 4.0, 10.0), 3.0)));
    gl_FragColor = color;
}
