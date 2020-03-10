#define LIGHT

#ifdef LIGHT
    #define LAYERS 30.
    #define DEPTH .5
    #define WIDTH .3
    #define SPEED .6
    #define SIZE 0.2
#else
    #define LAYERS 200.
    #define DEPTH .1
    #define WIDTH .8
    #define SPEED 1.5
    #define SIZE 1.0
#endif

varying vec2 v_texCoords;

uniform vec2 u_pos;
uniform vec2 u_resolution;
uniform float u_time;
uniform sampler2D u_texture0;

void main(){
    gl_FragColor = texture2D(u_texture0, v_texCoords);

    vec2 uv = (v_texCoords * u_resolution + u_pos) / 1000.0;
    const mat3 p = mat3(
        13.3231, 23.5112, 21.7112,
        21.1212, 28.7312, 11.9312,
        21.8112, 14.7212, 61.3934
    );
    float dof = 5.*sin(u_time*.1);

    //TODO this is very slow
    for(float i=0.0; i<LAYERS; i++){
        vec2 q = uv* (1.+i*DEPTH);
        q += vec2( q.y* WIDTH *( fract(i*7.238917) - .5 ),
        SPEED* u_time / (1.+i*DEPTH*.03) );
        vec3 n = vec3(floor(q), 31.189+i), m = floor(n)/1e5 + fract(n), mp = (31.9+m) / fract(p*m),
        r = fract(mp);
        vec2 s = abs(fract(q)-.5 +.9*r.xy-.45) + .01*abs(2.0*fract(10.*q.yx) - 1.0);
        float d = .6 * (s.x+s.y) + max(s.x,s.y) -.01,
        edge = .005*SIZE + .05 * SIZE * min( .5* abs(i-5.-dof), 1.);

        gl_FragColor += smoothstep(edge,-edge,d) * r.x / (1.+.02*i*DEPTH);
    }
}
