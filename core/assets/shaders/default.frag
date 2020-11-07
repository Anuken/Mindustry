varying lowp vec4 v_color;
varying lowp vec4 v_mix_color;
varying vec2 v_texCoords;
uniform sampler2D u_texture;

void main(){
    vec4 c = texture2D(u_texture, v_texCoords);
    gl_FragColor = v_color * mix(c, vec4(v_mix_color.rgb, c.a), v_mix_color.a);
}
