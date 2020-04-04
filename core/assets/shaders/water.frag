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

void main(){

	vec2 c = v_texCoord.xy;

	vec2 v = vec2(1.0/screensize.x, 1.0/screensize.y);
	vec2 coords = vec2(c.x / v.x + camerapos.x, c.y / v.y + camerapos.y);

	float stime = time / 5.0;

	float mscl = 40.0;
	float mth = 7.0;

    vec3 color = texture2D(u_texture, c + vec2(sin(stime/3.0 + coords.y/0.75) * v.x, 0.0)).rgb * vec3(0.9, 0.9, 1);

    float r = 0.0;
    float tester = mod((coords.x + coords.y*1.1 + sin(stime / 8.0 + coords.x/5.0 - coords.y/100.0)*2.0) +
                           sin(stime / 20.0 + coords.y/3.0) * 1.0 +
                           sin(stime / 10.0 - coords.y/2.0) * 2.0 +
                           sin(stime / 7.0 + coords.y/1.0) * 0.5 +
                           sin(coords.x / 3.0 + coords.y / 2.0) +
                           sin(stime / 20.0 + coords.x/4.0) * 1.0, mscl) + r;

    if(tester < mth){
        color *= 1.2;
    }

	gl_FragColor = vec4(color.rgb, 1.0);
}