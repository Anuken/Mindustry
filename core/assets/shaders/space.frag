#define HIGHP
#define NSCALE 2700.0
#define CAMSCALE (NSCALE*10.0)

uniform sampler2D u_texture;
uniform sampler2D u_stars;

uniform vec2 u_campos;
uniform vec2 u_ccampos;
uniform vec2 u_resolution;
uniform float u_time;

varying vec2 v_texCoords;

void main(){
    vec2 c = v_texCoords.xy;
    vec2 coords = vec2(c.x * u_resolution.x, c.y * u_resolution.y);

    vec4 color = texture2D(u_texture, c);
    color.rgb = texture2D(u_stars, coords/NSCALE + vec2(-0.1, -0.1) + u_ccampos / CAMSCALE).rgb;

    gl_FragColor = color;
}
