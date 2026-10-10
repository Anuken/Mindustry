//SMAA pass 2 (blending weight calculation) - fragment stage. Port of SMAABlendingWeightCalculationPS,
//including the diagonal and corner pattern handling. Non-temporal, so subsample indices are all zero.
//
//Inputs:  u_texture = edges from pass 1 (LINEAR filtering, clamp to edge - the searches rely on bilinear fetches)
//         u_area    = AreaTex   (160x560, RG8, LINEAR, clamp)    on texture unit 1
//         u_search  = SearchTex (64x16,   R8,  point/linear, clamp) on texture unit 2
//Output:  RGBA = blend weights.

#define HIGHP

//SMAA_PRESET_HIGH values
#define SMAA_MAX_SEARCH_STEPS 16
#define SMAA_MAX_SEARCH_STEPS_DIAG 8
#define SMAA_CORNER_ROUNDING 25
//uncomment to disable diagonal / corner handling (PRESET_LOW / MEDIUM do this)
//#define SMAA_DISABLE_DIAG_DETECTION
//#define SMAA_DISABLE_CORNER_DETECTION

#define SMAA_AREATEX_MAX_DISTANCE 16
#define SMAA_AREATEX_MAX_DISTANCE_DIAG 20
#define SMAA_AREATEX_PIXEL_SIZE (1.0 / vec2(160.0, 560.0))
#define SMAA_AREATEX_SUBTEX_SIZE (1.0 / 7.0)
#define SMAA_SEARCHTEX_SIZE vec2(66.0, 33.0)
#define SMAA_SEARCHTEX_PACKED_SIZE vec2(64.0, 16.0)
#define SMAA_CORNER_ROUNDING_NORM (float(SMAA_CORNER_ROUNDING) / 100.0)

//offsets to textureLodOffset must be compile-time constants, so this has to be a macro
#define sampleOffset(tex, coord, off) textureLodOffset(tex, coord, 0.0, off)

uniform sampler2D u_texture; //edges
uniform sampler2D u_area;
uniform sampler2D u_search;
uniform vec4 u_metrics;

varying vec2 v_texCoords;
varying vec2 v_pixcoord;
varying vec4 v_offset0;
varying vec4 v_offset1;
varying vec4 v_offset2;

out vec4 fragColor;

//---- diagonal pattern handling ----
#ifndef SMAA_DISABLE_DIAG_DETECTION

//conditionally decodes edges that were fetched with a bilinear tap at a diagonal
vec2 decodeDiag(vec2 e){
    e.r = e.r * abs(5.0 * e.r - 5.0 * 0.75);
    return round(e);
}

vec4 decodeDiag(vec4 e){
    e.rb = e.rb * abs(5.0 * e.rb - 5.0 * 0.75);
    return round(e);
}

vec2 searchDiag1(vec2 texcoord, vec2 dir, out vec2 e){
    vec4 coord = vec4(texcoord, -1.0, 1.0);
    vec3 t = vec3(u_metrics.xy, 1.0);
    while(coord.z < float(SMAA_MAX_SEARCH_STEPS_DIAG - 1) && coord.w > 0.9){
        coord.xyz = t * vec3(dir, 1.0) + coord.xyz;
        e = textureLod(u_texture, coord.xy, 0.0).rg;
        coord.w = dot(e, vec2(0.5));
    }
    return coord.zw;
}

vec2 searchDiag2(vec2 texcoord, vec2 dir, out vec2 e){
    vec4 coord = vec4(texcoord, -1.0, 1.0);
    coord.x += 0.25 * u_metrics.x; //see @SearchDiag2Optimization in the original
    vec3 t = vec3(u_metrics.xy, 1.0);
    while(coord.z < float(SMAA_MAX_SEARCH_STEPS_DIAG - 1) && coord.w > 0.9){
        coord.xyz = t * vec3(dir, 1.0) + coord.xyz;

        //bilinear fetch is used here, so the result must be decoded
        e = textureLod(u_texture, coord.xy, 0.0).rg;
        e = decodeDiag(e);

        coord.w = dot(e, vec2(0.5));
    }
    return coord.zw;
}

//area lookup for diagonal patterns
vec2 areaDiag(vec2 dist, vec2 e, float offset){
    vec2 texcoord = vec2(SMAA_AREATEX_MAX_DISTANCE_DIAG) * e + dist;

    //move to texel centers
    texcoord = SMAA_AREATEX_PIXEL_SIZE * texcoord + 0.5 * SMAA_AREATEX_PIXEL_SIZE;

    //diagonal areas are on the second half of the texture
    texcoord.x += 0.5;

    //subsample offset
    texcoord.y += SMAA_AREATEX_SUBTEX_SIZE * offset;

    return textureLod(u_area, texcoord, 0.0).rg;
}

vec2 calculateDiagWeights(vec2 texcoord, vec2 e){
    vec2 weights = vec2(0.0);

    //subsample indices are zero for non-temporal use: z = w = 0.0
    vec4 d;
    vec2 end;

    if(e.r > 0.0){
        d.xz = searchDiag1(texcoord, vec2(-1.0, 1.0), end);
        d.x += float(end.y > 0.9);
    }else{
        d.xz = vec2(0.0);
    }
    d.yw = searchDiag1(texcoord, vec2(1.0, -1.0), end);

    if(d.x + d.y > 2.0){ //d.x + d.y + 1 > 3
        //fetch the crossing edges
        vec4 coords = vec4(-d.x + 0.25, d.x, d.y, -d.y - 0.25) * u_metrics.xyxy + texcoord.xyxy;
        vec4 c;
        c.xy = sampleOffset(u_texture, coords.xy, ivec2(-1, 0)).rg;
        c.zw = sampleOffset(u_texture, coords.zw, ivec2( 1, 0)).rg;
        c.yxwz = decodeDiag(c.xyzw);

        //merge crossing edges at each side into a single value
        vec2 cc = vec2(2.0) * c.xz + c.yw;

        //remove the crossing edge if we didn't find the end of the line
        if(d.z >= 0.9) cc.x = 0.0;
        if(d.w >= 0.9) cc.y = 0.0;

        weights += areaDiag(d.xy, cc, 0.0);
    }

    //second diagonal
    d.xz = searchDiag2(texcoord, vec2(-1.0, -1.0), end);
    if(sampleOffset(u_texture, texcoord, ivec2(1, 0)).r > 0.0){
        d.yw = searchDiag2(texcoord, vec2(1.0, 1.0), end);
        d.y += float(end.y > 0.9);
    }else{
        d.yw = vec2(0.0);
    }

    if(d.x + d.y > 2.0){ //d.x + d.y + 1 > 3
        vec4 coords = vec4(-d.x, -d.x, d.y, d.y) * u_metrics.xyxy + texcoord.xyxy;
        vec4 c;
        c.x  = sampleOffset(u_texture, coords.xy, ivec2(-1,  0)).g;
        c.y  = sampleOffset(u_texture, coords.xy, ivec2( 0, -1)).r;
        c.zw = sampleOffset(u_texture, coords.zw, ivec2( 1,  0)).gr;
        vec2 cc = vec2(2.0) * c.xz + c.yw;

        if(d.z >= 0.9) cc.x = 0.0;
        if(d.w >= 0.9) cc.y = 0.0;

        weights += areaDiag(d.xy, cc, 0.0).gr;
    }

    return weights;
}
#endif

//---- horizontal / vertical searches ----

//scan the search texture for the real end of the line
float searchLength(vec2 e, float offset){
    //the texture is 66x33 but packed into 64x16, the two halves store left and right variants
    vec2 scale = SMAA_SEARCHTEX_SIZE * vec2(0.5, -1.0);
    vec2 bias = SMAA_SEARCHTEX_SIZE * vec2(offset, 1.0);

    //scale and bias to access texel centers
    scale += vec2(-1.0, 1.0);
    bias += vec2(0.5, -0.5);

    //convert from pixel coordinates to texcoords
    scale *= 1.0 / SMAA_SEARCHTEX_PACKED_SIZE;
    bias *= 1.0 / SMAA_SEARCHTEX_PACKED_SIZE;

    return textureLod(u_search, scale * e + bias, 0.0).r;
}

float searchXLeft(vec2 texcoord, float end){
    //bilinear filtering lets us check two edges per fetch, hence the step of 2 pixels
    vec2 e = vec2(0.0, 1.0);
    while(texcoord.x > end && e.g > 0.8281 && e.r == 0.0){
        e = textureLod(u_texture, texcoord, 0.0).rg;
        texcoord = -vec2(2.0, 0.0) * u_metrics.xy + texcoord;
    }
    float offset = -(255.0 / 127.0) * searchLength(e, 0.0) + 3.25;
    return u_metrics.x * offset + texcoord.x;
}

float searchXRight(vec2 texcoord, float end){
    vec2 e = vec2(0.0, 1.0);
    while(texcoord.x < end && e.g > 0.8281 && e.r == 0.0){
        e = textureLod(u_texture, texcoord, 0.0).rg;
        texcoord = vec2(2.0, 0.0) * u_metrics.xy + texcoord;
    }
    float offset = -(255.0 / 127.0) * searchLength(e, 0.5) + 3.25;
    return -u_metrics.x * offset + texcoord.x;
}

float searchYUp(vec2 texcoord, float end){
    vec2 e = vec2(1.0, 0.0);
    while(texcoord.y > end && e.r > 0.8281 && e.g == 0.0){
        e = textureLod(u_texture, texcoord, 0.0).rg;
        texcoord = -vec2(0.0, 2.0) * u_metrics.xy + texcoord;
    }
    float offset = -(255.0 / 127.0) * searchLength(e.gr, 0.0) + 3.25;
    return u_metrics.y * offset + texcoord.y;
}

float searchYDown(vec2 texcoord, float end){
    vec2 e = vec2(1.0, 0.0);
    while(texcoord.y < end && e.r > 0.8281 && e.g == 0.0){
        e = textureLod(u_texture, texcoord, 0.0).rg;
        texcoord = vec2(0.0, 2.0) * u_metrics.xy + texcoord;
    }
    float offset = -(255.0 / 127.0) * searchLength(e.gr, 0.5) + 3.25;
    return -u_metrics.y * offset + texcoord.y;
}

//area lookup for orthogonal patterns
vec2 area(vec2 dist, float e1, float e2, float offset){
    //rounding prevents precision errors of bilinear filtering
    vec2 texcoord = vec2(SMAA_AREATEX_MAX_DISTANCE) * round(4.0 * vec2(e1, e2)) + dist;

    //move to texel centers
    texcoord = SMAA_AREATEX_PIXEL_SIZE * texcoord + 0.5 * SMAA_AREATEX_PIXEL_SIZE;

    //subsample offset
    texcoord.y = SMAA_AREATEX_SUBTEX_SIZE * offset + texcoord.y;

    return textureLod(u_area, texcoord, 0.0).rg;
}

//---- corner detection ----
void detectHorizontalCornerPattern(inout vec2 weights, vec4 texcoord, vec2 d){
    #ifndef SMAA_DISABLE_CORNER_DETECTION
    vec2 leftRight = step(d.xy, d.yx);
    vec2 rounding = (1.0 - SMAA_CORNER_ROUNDING_NORM) * leftRight;

    rounding /= leftRight.x + leftRight.y; //reduce blending for pixels in the center of a line

    vec2 factor = vec2(1.0);
    factor.x -= rounding.x * sampleOffset(u_texture, texcoord.xy, ivec2(0,  1)).r;
    factor.x -= rounding.y * sampleOffset(u_texture, texcoord.zw, ivec2(1,  1)).r;
    factor.y -= rounding.x * sampleOffset(u_texture, texcoord.xy, ivec2(0, -2)).r;
    factor.y -= rounding.y * sampleOffset(u_texture, texcoord.zw, ivec2(1, -2)).r;

    weights *= clamp(factor, 0.0, 1.0);
    #endif
}

void detectVerticalCornerPattern(inout vec2 weights, vec4 texcoord, vec2 d){
    #ifndef SMAA_DISABLE_CORNER_DETECTION
    vec2 leftRight = step(d.xy, d.yx);
    vec2 rounding = (1.0 - SMAA_CORNER_ROUNDING_NORM) * leftRight;

    rounding /= leftRight.x + leftRight.y;

    vec2 factor = vec2(1.0);
    factor.x -= rounding.x * sampleOffset(u_texture, texcoord.xy, ivec2( 1, 0)).g;
    factor.x -= rounding.y * sampleOffset(u_texture, texcoord.zw, ivec2( 1, 1)).g;
    factor.y -= rounding.x * sampleOffset(u_texture, texcoord.xy, ivec2(-2, 0)).g;
    factor.y -= rounding.y * sampleOffset(u_texture, texcoord.zw, ivec2(-2, 1)).g;

    weights *= clamp(factor, 0.0, 1.0);
    #endif
}

void main(){
    vec2 texcoord = v_texCoords;
    vec4 weights = vec4(0.0);

    vec2 e = texture2D(u_texture, texcoord).rg;

    if(e.g > 0.0){ //edge at north
        #ifndef SMAA_DISABLE_DIAG_DETECTION
        //diagonals have both north and west edges, so searching for them in one of the boundaries is enough
        weights.rg = calculateDiagWeights(texcoord, e);

        //skip horizontal/vertical processing if a diagonal pattern was found
        if(weights.r == -weights.g){ //weights.r + weights.g == 0.0
        #endif

        vec2 d;

        //find the distance to the left
        vec3 coords;
        coords.x = searchXLeft(v_offset0.xy, v_offset2.x);
        coords.y = v_offset1.y; //offset[1].y = texcoord.y - 0.25 * texel.y (interpolated by hardware)
        d.x = coords.x;

        //fetch the left crossing edges, use bilinear filtering to fetch two edges in a single tap
        float e1 = textureLod(u_texture, coords.xy, 0.0).r;

        //find the distance to the right
        coords.z = searchXRight(v_offset0.zw, v_offset2.y);
        d.y = coords.z;

        //convert distances from texcoords to pixels; abs() because of the round() offsets
        d = abs(round(u_metrics.zz * d - v_pixcoord.xx));

        //square root to get more accuracy for small distances (the area texture is compressed)
        vec2 sqrt_d = sqrt(d);

        //fetch the right crossing edges
        float e2 = sampleOffset(u_texture, coords.zy, ivec2(1, 0)).r;

        //pattern-specific area
        weights.rg = area(sqrt_d, e1, e2, 0.0);

        //fix corners
        coords.y = texcoord.y;
        detectHorizontalCornerPattern(weights.rg, coords.xyzy, d);

        #ifndef SMAA_DISABLE_DIAG_DETECTION
        }else{
            e.r = 0.0; //skip vertical processing
        }
        #endif
    }

    if(e.r > 0.0){ //edge at west
        vec2 d;

        //find the distance to the top
        vec3 coords;
        coords.y = searchYUp(v_offset1.xy, v_offset2.z);
        coords.x = v_offset0.x; //offset[0].x = texcoord.x - 0.25 * texel.x
        d.x = coords.y;

        float e1 = textureLod(u_texture, coords.xy, 0.0).g;

        //find the distance to the bottom
        coords.z = searchYDown(v_offset1.zw, v_offset2.w);
        d.y = coords.z;

        d = abs(round(u_metrics.ww * d - v_pixcoord.yy));

        vec2 sqrt_d = sqrt(d);

        float e2 = sampleOffset(u_texture, coords.xz, ivec2(0, 1)).g;

        weights.ba = area(sqrt_d, e1, e2, 0.0);

        coords.x = texcoord.x;
        detectVerticalCornerPattern(weights.ba, coords.xyxz, d);
    }

    fragColor = weights;
}
