#ifdef GL_ES
precision highp float;
precision mediump int;
#endif

uniform sampler2D u_texture;

uniform vec2 camerapos;
uniform vec2 screensize;
uniform float time;

varying vec4 v_color;
varying vec2 v_texCoord;

vec3 permute(vec3 x) { return mod(((x*34.0)+1.0)*x, 289.0); }

float snoise(vec2 v){
    const vec4 C = vec4(0.211324865405187, 0.366025403784439,
           -0.577350269189626, 0.024390243902439);
    vec2 i  = floor(v + dot(v, C.yy) );
    vec2 x0 = v -   i + dot(i, C.xx);
    vec2 i1;
    i1 = (x0.x > x0.y) ? vec2(1.0, 0.0) : vec2(0.0, 1.0);
    vec4 x12 = x0.xyxy + C.xxzz;
    x12.xy -= i1;
    i = mod(i, 289.0);
    vec3 p = permute( permute( i.y + vec3(0.0, i1.y, 1.0 ))
    + i.x + vec3(0.0, i1.x, 1.0 ));
    vec3 m = max(0.5 - vec3(dot(x0,x0), dot(x12.xy,x12.xy),
    dot(x12.zw,x12.zw)), 0.0);
    m = m*m ;
    m = m*m ;
    vec3 x = 2.0 * fract(p * C.www) - 1.0;
    vec3 h = abs(x) - 0.5;
    vec3 ox = floor(x + 0.5);
    vec3 a0 = x - ox;
    m *= 1.79284291400159 - 0.85373472095314 * ( a0*a0 + h*h );
    vec3 g;
    g.x  = a0.x  * x0.x  + h.x  * x0.y;
    g.yz = a0.yz * x12.xz + h.yz * x12.yw;
    return 130.0 * dot(m, g);
}

void main() {

	vec2 c = v_texCoord.xy;
	vec4 color = texture2D(u_texture, c) * v_color;

	vec2 v = vec2(1.0/screensize.x, 1.0/screensize.y);
	vec2 coords = vec2(c.x / v.x + camerapos.x, c.y / v.y + camerapos.y);

	float stime = time / 5.0;

	float mscl = 30.0;
	float mth = 5.0;

    color = texture2D(u_texture, c + vec2(sin(stime/3.0 + coords.y/0.75) * v.x, 0.0)) * vec4(0.9, 0.9, 1, 1.0);
    color.a = 1.0;

    float n1 = snoise(coords / 40.0 + vec2(time) / 200.0);
    float n2 = snoise((coords + vec2(632.0)) / 25.0 + vec2(0.0, -time) / 190.0);

    float r = (n1 + n2) * 3.0;
    float tester = mod(float(int(coords.x + coords.y*1.1 + sin(stime / 8.0 + coords.x/5.0 - coords.y/100.0)*2.0)) +
                           sin(stime / 20.0 + coords.y/3.0) * 1.0 +
                           sin(stime / 10.0 + coords.y/2.0) * 2.0 +
                           sin(stime / 7.0 + coords.y/1.0) * 0.5 +
                           sin(coords.x + coords.y) +
                           sin(stime / 20.0 + coords.x/4.0) * 1.0, mscl) + r;

    if(tester < mth){
        color *= 1.2;
        color.a = 1.0;
    }

	gl_FragColor = color;
}