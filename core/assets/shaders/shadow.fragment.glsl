#ifdef GL_ES
precision mediump float;
precision mediump int;
#endif

#define SPACE 2.0

uniform sampler2D u_texture;

uniform vec4 u_color;
uniform vec2 u_texsize;
uniform float u_scl;

varying vec4 v_color;
varying vec2 v_texCoord;

void main() {
	vec2 v = vec2(1.0/u_texsize.x, 1.0/u_texsize.y);

	vec4 c = texture2D(u_texture, v_texCoord.xy);
	float spacing = SPACE * u_scl;

	gl_FragColor = mix(vec4(0.0, 0.0, 0.0, min(c.a, u_color.a)), u_color,
	        (1.0-step(0.001, texture2D(u_texture, v_texCoord.xy).a)) *
	        step(0.001,
	                //cardinals
	                texture2D(u_texture, v_texCoord.xy + vec2(0, spacing) * v).a +
	                texture2D(u_texture, v_texCoord.xy + vec2(0, -spacing) * v).a +
	                texture2D(u_texture, v_texCoord.xy + vec2(spacing, 0) * v).a +
	                texture2D(u_texture, v_texCoord.xy + vec2(-spacing, 0) * v).a +

	                //cardinal edges
	                texture2D(u_texture, v_texCoord.xy + vec2(spacing, spacing) * v).a +
                    texture2D(u_texture, v_texCoord.xy + vec2(spacing, -spacing) * v).a +
                    texture2D(u_texture, v_texCoord.xy + vec2(-spacing, spacing) * v).a +
                    texture2D(u_texture, v_texCoord.xy + vec2(-spacing, -spacing) * v).a +

	                //cardinals * 2
	                texture2D(u_texture, v_texCoord.xy + vec2(0, spacing) * v*2.0).a +
                    texture2D(u_texture, v_texCoord.xy + vec2(0, -spacing) * v*2.0).a +
                    texture2D(u_texture, v_texCoord.xy + vec2(spacing, 0) * v*2.0).a +
                    texture2D(u_texture, v_texCoord.xy + vec2(-spacing, 0) * v*2.0).a
            ));
}