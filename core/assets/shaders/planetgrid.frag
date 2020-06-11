
varying vec4 v_col;
varying vec4 v_position;

uniform vec3 u_mouse;

const vec4 shadow = vec4(0, 0, 0, 0);

void main(){
    gl_FragColor = mix(v_col, shadow, distance(u_mouse, v_position.xyz));
}
