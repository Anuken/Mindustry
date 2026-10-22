
// Possible values for LINE_TYPE:
#define CROSSLINE_HARD 0
#define VERTICAL_HARD 1
#define HORIZONTAL_HARD 2
#define VERTICAL_SMOOTH 3
#define HORIZONTAL_SMOOTH 4
#ifndef LINE_TYPE
#error Please define a LINE_TYPE
#endif

const float SCANLINE_BRIGHTNESS_MIN = SL_BRIGHTNESS_MIN;
const float SCANLINE_BRIGHTNESS_MAX = SL_BRIGHTNESS_MAX;
const float SCANLINE_F_BASELINE = SCANLINE_BRIGHTNESS_MIN;
const float SCANLINE_F_DIF = SCANLINE_BRIGHTNESS_MAX - SCANLINE_F_BASELINE;

uniform sampler2D u_texture0;
uniform vec2 u_resolution;
varying vec2 v_texCoords;

void main() {
    vec4 color = texture2D(u_texture0, v_texCoords);

#if LINE_TYPE == CROSSLINE_HARD
    //FIXME A way too bright.
    // Hard crossline pattern.
    float x = u_resolution.x * v_texCoords.x;
    float y = u_resolution.y * v_texCoords.y;
    float componentX = SCANLINE_F_BASELINE + SCANLINE_F_DIF * step(1.0, mod(x, 2.0));
    float componentY = SCANLINE_F_BASELINE + SCANLINE_F_DIF * step(1.0, mod(y, 2.0));
    float component = (componentX + componentY) * (componentX * componentY) * 1.2;
    color = color * vec4(component, component, component, 1.0);
#endif

#if LINE_TYPE == VERTICAL_HARD
    // Hard vertical line.
    float x = u_resolution.x * v_texCoords.x;
    float component = SCANLINE_F_BASELINE + SCANLINE_F_DIF * step(1.0, mod(x, 2.0));
    color = color * vec4(component, component, component, 1.0);
#endif

#if LINE_TYPE == HORIZONTAL_HARD
    // Hard horizontal line.
    float y = u_resolution.y * v_texCoords.y;
    float component = SCANLINE_F_BASELINE + SCANLINE_F_DIF * step(1.0, mod(y, 2.0));
    color = color * vec4(component, component, component, 1.0);
#endif

#if LINE_TYPE == VERTICAL_SMOOTH
    // Smooth vertical line.
    float x = u_resolution.x * v_texCoords.x;
    float factor = abs(mod(x, 2.0) - 1.0);
    float componentValue = mix(SCANLINE_BRIGHTNESS_MAX, SCANLINE_BRIGHTNESS_MIN, factor);
    color = color * vec4(componentValue, componentValue, componentValue, 1.0);
#endif

#if LINE_TYPE == HORIZONTAL_SMOOTH
    // Smooth horizontal line.
    float y = u_resolution.y * v_texCoords.y;
    float factor = abs(mod(y, 2.0) - 1.0);
    float componentValue = mix(SCANLINE_BRIGHTNESS_MAX, SCANLINE_BRIGHTNESS_MIN, factor);
    color = color * vec4(componentValue, componentValue, componentValue, 1.0);
#endif

    gl_FragColor = color;
}