//SMAA pass 3 (neighborhood blending) - fragment stage. Port of SMAANeighborhoodBlendingPS (no reprojection).
//Inputs:  u_texture = original scene color (LINEAR filtering, clamp to edge - the blend uses bilinear taps)
//         u_blend   = blend weights from pass 2 on texture unit 1
//Output:  final anti-aliased color.

#define HIGHP

uniform sampler2D u_texture;
uniform sampler2D u_blend;
uniform vec4 u_metrics;

varying vec2 v_texCoords;
varying vec4 v_offset;

out vec4 fragColor;

void main(){
    vec2 texcoord = v_texCoords;

    //fetch the blending weights for the current pixel
    vec4 a;
    a.x = texture2D(u_blend, v_offset.xy).a;  //right
    a.y = texture2D(u_blend, v_offset.zw).g;  //bottom
    a.wz = texture2D(u_blend, texcoord).xz;   //left, top

    //is there any blending weight with a value greater than 0.0?
    if(dot(a, vec4(1.0)) < 1e-5){
        fragColor = textureLod(u_texture, texcoord, 0.0);
    }else{
        bool h = max(a.x, a.z) > max(a.y, a.w); //max(horizontal) > max(vertical)

        //calculate the blending offsets
        vec4 blendingOffset = h ? vec4(a.x, 0.0, a.z, 0.0) : vec4(0.0, a.y, 0.0, a.w);
        vec2 blendingWeight = h ? a.xz : a.yw;
        blendingWeight /= dot(blendingWeight, vec2(1.0));

        //calculate the texture coordinates
        vec4 blendingCoord = blendingOffset * vec4(u_metrics.xy, -u_metrics.xy) + texcoord.xyxy;

        //two bilinear taps blend across the edge
        vec4 color = blendingWeight.x * textureLod(u_texture, blendingCoord.xy, 0.0);
        color += blendingWeight.y * textureLod(u_texture, blendingCoord.zw, 0.0);

        fragColor = color;
    }
}
