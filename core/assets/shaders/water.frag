#define HIGHP

uniform sampler2D u_texture;

uniform vec2 u_campos;
uniform vec2 u_resolution;
uniform float u_time;

varying vec2 v_texCoords;

const float mscl = 40.0;
const float mth = 7.0;

void main(){

	vec2 c = v_texCoords;

	vec2 v = vec2(1.0/u_resolution.x, 1.0/u_resolution.y);
	vec2 coords = vec2(c.x / v.x + u_campos.x, c.y / v.y + u_campos.y);

	float stime = u_time / 5.0;

    vec3 color = texture2D(u_texture, c + vec2(sin(stime/3.0 + coords.y/0.75) * v.x, 0.0)).rgb * vec3(0.9, 0.9, 1);

    float tester = mod((coords.x + coords.y*1.1 + sin(stime / 8.0 + coords.x/5.0 - coords.y/100.0)*2.0) +
                           sin(stime / 20.0 + coords.y/3.0) * 1.0 +
                           sin(stime / 10.0 - coords.y/2.0) * 2.0 +
                           sin(stime / 7.0 + coords.y/1.0) * 0.5 +
                           sin(coords.x / 3.0 + coords.y / 2.0) +
                           sin(stime / 20.0 + coords.x/4.0) * 1.0, mscl);

    if(tester < mth){
        color *= 1.2;
    }

	gl_FragColor = vec4(color.rgb, 1.0);
}