#define HIGHP

#define MAXR 24
#define INNER_SEARCH 3.0

uniform sampler2D u_texture;
uniform vec2 u_invres;
uniform float u_radius;

varying vec2 v_texCoords;

float rowDist(vec4 s, bool solid){
    float v = floor(s.a * 64.0);
    return (v >= 32.0) == solid ? v - (solid ? 32.0 : 0.0) : 0.0;
}

//combines row distances into a euclidean signed distance (negative inside), encoded in alpha
void main(){
    vec4 c = texture2D(u_texture, v_texCoords);
    bool solid = c.a > 0.5;
    float best = rowDist(c, solid);
    float limit = solid ? INNER_SEARCH : u_radius + 2.0;

    for(int i = 1; i <= MAXR; i++){
        float y = float(i);
        if(y >= best || y > limit) break;

        vec4 d = texture2D(u_texture, v_texCoords - vec2(0.0, y * u_invres.y));
        vec4 u = texture2D(u_texture, v_texCoords + vec2(0.0, y * u_invres.y));
        float dd = length(vec2(rowDist(d, solid), y)), du = length(vec2(rowDist(u, solid), y));

        if(dd < best){
            best = dd;
            if(!solid) c.rgb = d.rgb;
        }
        if(du < best){
            best = du;
            if(!solid) c.rgb = u.rgb;
        }
    }

    float sd = solid ? 0.5 - best : best - 0.5;
    gl_FragColor = vec4(c.rgb, clamp((sd + 4.0) / 32.0, 0.0, 1.0));
}
