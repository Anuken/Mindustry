uniform sampler2D u_texture0;

// Viewport dimensions along X and Y
uniform vec2 u_viewport;
uniform float u_intensity;
uniform vec2 u_lightPosition;
uniform vec3 u_color;

varying vec2 v_texCoords;

vec3 lensflare(vec2 uv,vec2 pos) {
	vec2 main = uv-pos;
	vec2 uvd = uv*(length(uv));
	
	float dist=length(main); dist = pow(dist,.1);
	
	
	float f1 = max(0.01-pow(length(uv+1.2*pos),1.9),.0)*7.0;

	float f2 = max(1.0/(1.0+32.0*pow(length(uvd+0.8*pos),2.0)),.0)*00.1;
	float f22 = max(1.0/(1.0+32.0*pow(length(uvd+0.85*pos),2.0)),.0)*00.08;
	float f23 = max(1.0/(1.0+32.0*pow(length(uvd+0.9*pos),2.0)),.0)*00.06;
	
	vec2 uvx = mix(uv,uvd,-0.5);
	
	float f4 = max(0.01-pow(length(uvx+0.4*pos),2.4),.0)*6.0;
	float f42 = max(0.01-pow(length(uvx+0.45*pos),2.4),.0)*5.0;
	float f43 = max(0.01-pow(length(uvx+0.5*pos),2.4),.0)*3.0;
	
	uvx = mix(uv,uvd,-.4);
	
	float f5 = max(0.01-pow(length(uvx+0.2*pos),5.5),.0)*2.0;
	float f52 = max(0.01-pow(length(uvx+0.4*pos),5.5),.0)*2.0;
	float f53 = max(0.01-pow(length(uvx+0.6*pos),5.5),.0)*2.0;
	
	uvx = mix(uv,uvd,-0.5);
	
	float f6 = max(0.01-pow(length(uvx-0.3*pos),1.6),.0)*6.0;
	float f62 = max(0.01-pow(length(uvx-0.325*pos),1.6),.0)*3.0;
	float f63 = max(0.01-pow(length(uvx-0.35*pos),1.6),.0)*5.0;
	
	vec3 c = vec3(.0);
	
	c.r+=f2+f4+f5+f6; c.g+=f22+f42+f52+f62; c.b+=f23+f43+f53+f63;
	c = c*1.3 - vec3(length(uvd)*.05);
	
	return c * u_intensity;
}

// color modifier
vec3 cc(vec3 color, float factor,float factor2) {
	float w = color.x+color.y+color.z;
	return mix(color,vec3(w)*factor,w*factor2);
}

void main(void) {
    vec4 sampled = texture2D(u_texture0, v_texCoords);
	vec2 uv = v_texCoords - 0.5;
	uv.x *= u_viewport.x / u_viewport.y;
	vec2 lpos = u_lightPosition;
	lpos.x *= u_viewport.x / u_viewport.y;
	vec3 color = u_color * lensflare(uv, lpos);
	gl_FragColor = vec4(sampled.rgb + cc(color, 0.5, 0.1), sampled.a);
}