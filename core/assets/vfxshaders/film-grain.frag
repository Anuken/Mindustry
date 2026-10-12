const float NOISE_AMOUNT = 0.18;

varying vec2 v_texCoords;
uniform sampler2D u_texture0;
uniform float u_seed;

void main() {
	vec2 uv = v_texCoords;
    vec4 color = texture2D(u_texture0, v_texCoords);

    float n = fract(sin(dot(uv, vec2(u_seed + 12.9898, 78.233))) * 43758.5453);
    color *= (1.0 - NOISE_AMOUNT + n * NOISE_AMOUNT) * 1.1;
	gl_FragColor = color;
}