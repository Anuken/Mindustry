
varying vec2 v_texCoords;

 // Unprocessed image
uniform sampler2D u_texture0;

uniform float u_exposure;
uniform float u_gamma;

void main() {
    vec3 hdrColor = texture2D(u_texture0, v_texCoords).rgb;

     // Exposure tone mapping
    vec3 mapped = vec3(1.0) - exp(-hdrColor * u_exposure);
    // Gamma correction
    mapped = pow(mapped, vec3(1.0 / u_gamma));

    gl_FragColor = vec4(mapped, 1.0);
}