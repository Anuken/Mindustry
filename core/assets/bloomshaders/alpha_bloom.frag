uniform lowp sampler2D u_texture0;
uniform lowp sampler2D u_texture1;
uniform lowp float BloomIntensity;
uniform lowp float OriginalIntensity;

varying vec2 v_texCoords;

void main(){
    vec4 original = texture2D(u_texture0, v_texCoords) * OriginalIntensity;
    vec4 bloom = texture2D(u_texture1, v_texCoords) * BloomIntensity;
    original = original *  (vec4(1.0) - bloom);
    vec4 combined =  original + bloom;
    float mx = min(max(combined.r, max(combined.g, combined.b)), 1.0);
    gl_FragColor = vec4(combined.rgb / max(mx, 0.0001), mx);
}
