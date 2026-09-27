//SMAA pass 1 (luma edge detection) - vertex stage. Port of SMAAEdgeDetectionVS.
//u_metrics = (1/width, 1/height, width, height) of the buffer being processed.

attribute vec4 a_position;
attribute vec2 a_texCoord0;

uniform vec4 u_metrics;

varying vec2 v_texCoords;
varying vec4 v_offset0;
varying vec4 v_offset1;
varying vec4 v_offset2;

void main(){
    v_texCoords = a_texCoord0;

    v_offset0 = u_metrics.xyxy * vec4(-1.0, 0.0, 0.0, -1.0) + a_texCoord0.xyxy;
    v_offset1 = u_metrics.xyxy * vec4( 1.0, 0.0, 0.0,  1.0) + a_texCoord0.xyxy;
    v_offset2 = u_metrics.xyxy * vec4(-2.0, 0.0, 0.0, -2.0) + a_texCoord0.xyxy;

    gl_Position = a_position;
}
