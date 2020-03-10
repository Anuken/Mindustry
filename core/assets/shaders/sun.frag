#ifdef GL_ES
precision mediump float;
precision lowp int;
#define INTEGER lowp int
#else
#define INTEGER int
#endif

#define gradients false

const int MAX_COLORS = 10;

uniform INTEGER u_colornum;
uniform vec4 u_colors[MAX_COLORS];

varying float v_height;

void main() {
    if(gradients){
        int from = int(v_height * float(u_colornum));
        int to = int(clamp(float(int(v_height * float(u_colornum) + 1.0)), 0.0, float(u_colornum)-1.0));
        float alpha = fract(v_height * float(u_colornum));

        gl_FragColor = vec4(mix(u_colors[from], u_colors[to], alpha));
    }else{
        int from = int(v_height * float(u_colornum));
        gl_FragColor = u_colors[from];
    }
}