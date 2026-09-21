//SMAA pass 1 (luma edge detection) - fragment stage. Port of SMAALumaEdgeDetectionPS (no predication).
//Input:  u_texture = scene color (LINEAR filtering, clamp to edge)
//Output: RG = edge flags (left, top). Pixels with no edge are discarded, so the target MUST be cleared to 0 first.

#define HIGHP

//SMAA_PRESET_HIGH values
#define SMAA_THRESHOLD 0.1
#define SMAA_LOCAL_CONTRAST_ADAPTATION_FACTOR 2.0

uniform sampler2D u_texture;

varying vec2 v_texCoords;
varying vec4 v_offset0;
varying vec4 v_offset1;
varying vec4 v_offset2;

//explicit declaration so the Arc prelude does not add a lowp one (edge data needs full precision on GLES)
out vec4 fragColor;

const vec3 lumaWeights = vec3(0.2126, 0.7152, 0.0722);

float luma(vec2 coord){
    return dot(texture2D(u_texture, coord).rgb, lumaWeights);
}

void main(){
    vec2 threshold = vec2(SMAA_THRESHOLD);

    //left and top deltas
    float L = luma(v_texCoords);
    float Lleft = luma(v_offset0.xy);
    float Ltop = luma(v_offset0.zw);

    vec4 delta;
    delta.xy = abs(L - vec2(Lleft, Ltop));
    vec2 edges = step(threshold, delta.xy);

    //early out if there is no edge
    if(dot(edges, vec2(1.0)) == 0.0){
        discard;
    }

    //right and bottom deltas
    float Lright = luma(v_offset1.xy);
    float Lbottom = luma(v_offset1.zw);
    delta.zw = abs(L - vec2(Lright, Lbottom));

    vec2 maxDelta = max(delta.xy, delta.zw);

    //next-to-left and next-to-top deltas
    float Lleftleft = luma(v_offset2.xy);
    float Ltoptop = luma(v_offset2.zw);
    delta.zw = abs(vec2(Lleft, Ltop) - vec2(Lleftleft, Ltoptop));

    maxDelta = max(maxDelta.xy, delta.zw);
    float finalDelta = max(maxDelta.x, maxDelta.y);

    //local contrast adaptation
    edges.xy *= step(finalDelta, SMAA_LOCAL_CONTRAST_ADAPTATION_FACTOR * delta.xy);

    fragColor = vec4(edges, 0.0, 0.0);
}
