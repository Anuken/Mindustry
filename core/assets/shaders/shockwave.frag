#define MAX_SHOCKWAVES 64
#define WAVE_RADIUS 4.0
#define DIFF_SCL 1
#define WAVE_POW 0.8

varying vec2 v_texCoords;

uniform sampler2D u_texture;
uniform vec2 u_resolution;
uniform vec2 u_campos;
uniform vec4 u_shockwaves[MAX_SHOCKWAVES];
uniform int u_shockwave_count;

void main(){
    vec2 worldCoords = v_texCoords * u_resolution + u_campos;
    vec2 uv = v_texCoords;
    vec2 displacement = vec2(0.0, 0.0);

    for(int i = 0; i < MAX_SHOCKWAVES; i ++){
        vec4 wave = u_shockwaves[i];
        float radius = wave.z;
        float strength = wave.w;
        float dst = distance(worldCoords, wave.xy);
        float realStrength = 1.0 - pow(1.0 - strength, 5.0);

        if(abs(dst - radius) <= WAVE_RADIUS){
            float diff = dst - radius;
            float pdiff = 1.0 - pow(abs(diff * DIFF_SCL), WAVE_POW);
            float diffTime = diff  * pdiff;
            vec2 relative = normalize(worldCoords - wave.xy);

            displacement += (relative * diffTime * strength) / u_resolution;
        }

        if(i >= u_shockwave_count - 1){
            break;
        }
    }

    vec4 c = texture2D(u_texture, uv + displacement);
    gl_FragColor = c;
}