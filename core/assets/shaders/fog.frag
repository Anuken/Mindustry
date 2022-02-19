#define HIGHP
#define QUANT 0.3

uniform sampler2D u_texture;

varying vec4 v_color;
varying vec2 v_texCoords;

void main(){
    vec4 color = texture2D(u_texture, v_texCoords.xy);
    gl_FragColor = vec4(1.0, 1.0, 1.0, (1.0 - floor(color.r / QUANT) * QUANT) * (step(color.r, 0.99))) * v_color;
}
