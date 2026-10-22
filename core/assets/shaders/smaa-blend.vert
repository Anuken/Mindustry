//SMAA pass 3 (neighborhood blending) - vertex stage. Port of SMAANeighborhoodBlendingVS.
//u_metrics = (1/width, 1/height, width, height).

attribute vec4 a_position;
attribute vec2 a_texCoord0;

uniform vec4 u_metrics;

varying vec2 v_texCoords;
varying vec4 v_offset;

void main(){
    v_texCoords = a_texCoord0;
    v_offset = u_metrics.xyxy * vec4(1.0, 0.0, 0.0, 1.0) + a_texCoord0.xyxy;

    gl_Position = a_position;
}
