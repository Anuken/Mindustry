uniform sampler2D u_texture;

uniform float u_time;
uniform float u_progress;
uniform vec4 u_color;
uniform vec2 u_uv;
uniform vec2 u_uv2;
uniform vec2 u_texsize;

varying vec4 v_color;
varying vec2 v_texCoords;

void main(){
    vec2 coords = (v_texCoords - u_uv) / (u_uv2 - u_uv);
    vec2 v = vec2(1.0/u_texsize.x, 1.0/u_texsize.y);

	vec4 c = texture2D(u_texture, v_texCoords);

    c.a *= u_progress;
    c.a *= step(abs(sin(coords.y*3.0 + u_time)), 0.9);

    gl_FragColor = c * v_color;
}
