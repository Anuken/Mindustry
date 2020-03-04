#ifdef GL_ES
precision mediump float;
precision lowp int;
#define INTEGER lowp int
#else
#define INTEGER int
#endif

attribute vec4 a_position;
attribute vec3 a_normal;

uniform mat4 u_proj;
uniform mat4 u_trans;

uniform float u_time;
uniform int u_octaves;
uniform float u_falloff;
uniform float u_scale;
uniform float u_power;
uniform float u_spread;

uniform float u_magnitude;
uniform float u_seed;

uniform INTEGER u_colornum;

varying float v_height;

float rand(vec2 co){
    return fract(sin(dot(co.xy, vec2(12.9898, 78.233))) * 43758.5453);
}

vec4 permute(vec4 x){ return mod(((x*34.0)+1.0)*x, 289.0); }
float permute(float x){ return floor(mod(((x*34.0)+1.0)*x, 289.0)); }
vec4 taylorInvSqrt(vec4 r){ return 1.79284291400159 - 0.85373472095314 * r; }
float taylorInvSqrt(float r){ return 1.79284291400159 - 0.85373472095314 * r; }

vec4 grad4(float j, vec4 ip){
    const vec4 ones = vec4(1.0, 1.0, 1.0, -1.0);
    vec4 p, s;
    p.xyz = floor(fract (vec3(j) * ip.xyz) * 7.0) * ip.z - 1.0;
    p.w = 1.5 - dot(abs(p.xyz), ones.xyz);
    s = vec4(lessThan(p, vec4(0.0)));
    p.xyz = p.xyz + (s.xyz*2.0 - 1.0) * s.www;
    return p;
}

float snoise(vec4 v){
    const vec2 C = vec2(0.138196601125010504, 0.309016994374947451);
    vec4 i = floor(v + dot(v, C.yyyy));
    vec4 x0 = v - i + dot(i, C.xxxx);
    vec4 i0;
    vec3 isX = step(x0.yzw, x0.xxx);
    vec3 isYZ = step(x0.zww, x0.yyz);
    i0.x = isX.x + isX.y + isX.z;
    i0.yzw = 1.0 - isX;
    i0.y += isYZ.x + isYZ.y;
    i0.zw += 1.0 - isYZ.xy;
    i0.z += isYZ.z;
    i0.w += 1.0 - isYZ.z;
    vec4 i3 = clamp(i0, 0.0, 1.0);
    vec4 i2 = clamp(i0-1.0, 0.0, 1.0);
    vec4 i1 = clamp(i0-2.0, 0.0, 1.0);
    vec4 x1 = x0 - i1 + 1.0 * C.xxxx;
    vec4 x2 = x0 - i2 + 2.0 * C.xxxx;
    vec4 x3 = x0 - i3 + 3.0 * C.xxxx;
    vec4 x4 = x0 - 1.0 + 4.0 * C.xxxx;
    i = mod(i, 289.0);
    float j0 = permute(permute(permute(permute(i.w) + i.z) + i.y) + i.x);
    vec4 j1 = permute(permute(permute(permute (
    i.w + vec4(i1.w, i2.w, i3.w, 1.0))
    + i.z + vec4(i1.z, i2.z, i3.z, 1.0))
    + i.y + vec4(i1.y, i2.y, i3.y, 1.0))
    + i.x + vec4(i1.x, i2.x, i3.x, 1.0));
    vec4 ip = vec4(1.0/294.0, 1.0/49.0, 1.0/7.0, 0.0);
    vec4 p0 = grad4(j0, ip);
    vec4 p1 = grad4(j1.x, ip);
    vec4 p2 = grad4(j1.y, ip);
    vec4 p3 = grad4(j1.z, ip);
    vec4 p4 = grad4(j1.w, ip);
    vec4 norm = taylorInvSqrt(vec4(dot(p0, p0), dot(p1, p1), dot(p2, p2), dot(p3, p3)));
    p0 *= norm.x;
    p1 *= norm.y;
    p2 *= norm.z;
    p3 *= norm.w;
    p4 *= taylorInvSqrt(dot(p4, p4));
    vec3 m0 = max(0.6 - vec3(dot(x0, x0), dot(x1, x1), dot(x2, x2)), 0.0);
    vec2 m1 = max(0.6 - vec2(dot(x3, x3), dot(x4, x4)), 0.0);
    m0 = m0 * m0;
    m1 = m1 * m1;
    return 49.0 * (dot(m0*m0, vec3(dot(p0, x0), dot(p1, x1), dot(p2, x2)))+ dot(m1*m1, vec2(dot(p3, x3), dot(p4, x4))));
}

float onoise(vec4 pos, int octaves, float falloff, float scl, float po){
    float sum = 0.0;
    float samp = 0.0;
    float amp = 1.0;
    float cscl = scl;

    for (int i = 0; i < octaves; i ++){
        sum += (snoise(pos / vec4(cscl, cscl, cscl, 1.0)) + 1.0) / 2.0 * amp;
        cscl /= 2.0;
        samp += amp;
        amp *= falloff;
    }

    return pow(sum / samp, po);
}

void main(){
    vec4 pos = a_position;

    float height = onoise(vec4(a_position.xyz, u_time + u_seed), u_octaves, u_falloff, u_scale, u_power);

    int cindex = int(height * float(u_colornum));

    float dst = 1.0 - (u_magnitude/2.0) + height*u_magnitude;

    v_height = (height + (onoise(vec4(a_position.xyz, u_time + u_seed*2.0), u_octaves, u_falloff, u_scale, u_power) - 0.5) / 6.0 - 0.5) * u_spread + 0.5;

    gl_Position = u_proj * u_trans * a_position; //u_proj * (a_position + vec4(pos.xyz * (dst - 1.0), 0.0));
}