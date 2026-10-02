
// Unprocessed image
uniform sampler2D u_texture0;
// Last frame
uniform sampler2D u_texture1;
// Last frame alpha
uniform float u_blurOpacity;

varying vec2 v_texCoords;

void main() {
    gl_FragColor = max(texture2D(u_texture0, v_texCoords), texture2D(u_texture1, v_texCoords) * u_blurOpacity);
}
