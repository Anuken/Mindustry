#define HIGHP

#define MAXR 24
#define INNER_SEARCH 3.0

uniform sampler2D u_texture;
uniform vec2 u_invres;
uniform float u_radius;

varying vec2 v_texCoords;

//stores the horizontal pixel distance to the nearest texel of the opposite region, plus the nearest solid color
void main(){
    vec4 c = texture2D(u_texture, v_texCoords);
    bool solid = c.a > 0.9;
    float limit = solid ? INNER_SEARCH : u_radius + 2.0;
    float h = 31.0;

    for(int i = 1; i <= MAXR; i++){
        float x = float(i);
        if(x > limit) break;

        vec4 l = texture2D(u_texture, v_texCoords - vec2(x * u_invres.x, 0.0));
        vec4 r = texture2D(u_texture, v_texCoords + vec2(x * u_invres.x, 0.0));

        if((l.a > 0.9) != solid){
            h = x;
            if(!solid) c = l;
            break;
        }
        if((r.a > 0.9) != solid){
            h = x;
            if(!solid) c = r;
            break;
        }
    }

    gl_FragColor = vec4(c.rgb / max(c.a, 0.001), ((solid ? 32.0 : 0.0) + h + 0.5) / 64.0);
}
