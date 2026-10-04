
varying vec2 v_texCoords;
uniform sampler2D u_texture0;
uniform float u_amount;
uniform float u_speed;
uniform float u_time;

float random(vec2 n, float offset){
    return 0.5 - fract(sin(dot(n.xy + vec2(offset, 0.0), vec2(12.9898, 78.233)))* 43758.5453);
}

void main() {
    vec4 color = texture2D(u_texture0, v_texCoords);
    color += vec4(u_amount * random(v_texCoords, 0.00001 * u_speed * u_time));
    gl_FragColor = color;
}