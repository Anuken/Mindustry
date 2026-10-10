#define HIGHP

#define ALPHA 0.18
#define INNER 1.0

uniform sampler2D u_texture;
uniform sampler2D u_field;
uniform vec2 u_texsize;
uniform float u_time;
uniform float u_dp;
uniform float u_radius;
uniform vec2 u_offset;

varying vec2 v_texCoords;

void main(){
    vec2 T = v_texCoords.xy;
    vec2 coords = (T * u_texsize) + u_offset;

    T += vec2(sin(coords.y / 3.0 + u_time / 20.0), sin(coords.x / 3.0 + u_time / 20.0)) / u_texsize;

    vec4 color = texture2D(u_texture, T);
    vec4 field = texture2D(u_field, T);
    float sd = field.a * 32.0 - 4.0;
    float edge = clamp(u_radius + 0.5 - sd, 0.0, 1.0) * clamp(sd + INNER + 0.5, 0.0, 1.0);

    vec4 fill = vec4(field.rgb, 0.0);
    if(color.a > 0.0){
        if(mod(coords.x / u_dp + coords.y / u_dp + sin(coords.x / u_dp / 5.0) * 3.0 + sin(coords.y / u_dp / 5.0) * 3.0  + u_time / 4.0, 10.0) < 2.0){
            color *= 1.65;
        }

        fill = vec4(color.rgb, ALPHA);
    }

    gl_FragColor = mix(fill, vec4(field.rgb, 1.0), edge);
}
