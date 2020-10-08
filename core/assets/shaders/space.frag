#define HIGHP
#define NSCALE 2000.0
#define CAMSCALE (NSCALE*1.1)

uniform sampler2D u_texture;
uniform sampler2D u_stars;

uniform vec2 u_campos;
uniform vec2 u_ccampos;
uniform vec2 u_resolution;
uniform float u_time;

varying vec2 v_texCoords;

void main(){
    vec2 c = v_texCoords.xy;
    vec2 coords = vec2(c.x * u_resolution.x + u_campos.x, c.y * u_resolution.y + u_campos.y);

    vec4 color = texture2D(u_texture, c);
    color.rgb = texture2D(u_stars, coords / NSCALE + vec2(0.5, 0.3) - u_ccampos / CAMSCALE);

    gl_FragColor = color;
}