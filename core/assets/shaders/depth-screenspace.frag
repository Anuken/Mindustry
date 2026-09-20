#define HIGHP

in vec2 v_texCoords;

uniform sampler2D u_color;
uniform sampler2D u_depth;

void main(){
    fragColor = texture(u_color, v_texCoords);
    gl_FragDepth = texture(u_depth, v_texCoords).r;
}