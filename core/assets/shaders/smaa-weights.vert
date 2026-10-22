//SMAA pass 2 (blending weight calculation) - vertex stage. Port of SMAABlendingWeightCalculationVS.
//u_metrics = (1/width, 1/height, width, height).
//SMAA_MAX_SEARCH_STEPS must match the value in smaa-weights.frag.

#define SMAA_MAX_SEARCH_STEPS 16

attribute vec4 a_position;
attribute vec2 a_texCoord0;

uniform vec4 u_metrics;

varying vec2 v_texCoords;
varying vec2 v_pixcoord;
varying vec4 v_offset0;
varying vec4 v_offset1;
varying vec4 v_offset2;

void main(){
    v_texCoords = a_texCoord0;
    v_pixcoord = a_texCoord0 * u_metrics.zw;

    //search offsets, quarter/eighth pixel biases are part of the algorithm
    v_offset0 = u_metrics.xyxy * vec4(-0.25, -0.125,  1.25, -0.125) + a_texCoord0.xyxy;
    v_offset1 = u_metrics.xyxy * vec4(-0.125, -0.25, -0.125,  1.25) + a_texCoord0.xyxy;

    //search limits
    v_offset2 = u_metrics.xxyy * (vec4(-2.0, 2.0, -2.0, 2.0) * float(SMAA_MAX_SEARCH_STEPS))
              + vec4(v_offset0.xz, v_offset1.yw);

    gl_Position = a_position;
}
