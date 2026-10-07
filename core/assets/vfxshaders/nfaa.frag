// Normal filtered anti-aliasing.
// See http://blenderartists.org/forum/showthread.php?209574-Full-Screen-Anti-Aliasing-(NFAA-DLAA-SSAA)
// and http://www.gamedev.net/topic/580517-nfaa---a-post-process-anti-aliasing-filter-results-implementation-details/
// Copyright Styves, Martinsh
// Modified by Sagrista, Toni

uniform sampler2D u_texture0;
// The inverse of the viewport dimensions along X and Y
uniform vec2 u_viewportInverse;

varying vec2 v_texCoords;

float lumRGB(vec3 v) {
    return dot(v, vec3(0.212, 0.716, 0.072));
}

const float fScale = 1.0;

vec4 nfaa(sampler2D tex, vec2 texCoords, vec2 viewportInverse) {
    // Offset coordinates
    vec2 upOffset = vec2(0.0, viewportInverse.y) * fScale;
    vec2 rightOffset = vec2(viewportInverse.x, 0.0) * fScale;

    float topHeight = lumRGB(texture2D(tex, texCoords.xy + upOffset).rgb);
    float bottomHeight = lumRGB(texture2D(tex, texCoords.xy - upOffset).rgb);
    float rightHeight = lumRGB(texture2D(tex, texCoords.xy + rightOffset).rgb);
    float leftHeight = lumRGB(texture2D(tex, texCoords.xy - rightOffset).rgb);
    float leftTopHeight = lumRGB(texture2D(tex, texCoords.xy - rightOffset + upOffset).rgb);
    float leftBottomHeight = lumRGB(texture2D(tex, texCoords.xy - rightOffset - upOffset).rgb);
    float rightBottomHeight = lumRGB(texture2D(tex, texCoords.xy + rightOffset + upOffset).rgb);
    float rightTopHeight = lumRGB(texture2D(tex, texCoords.xy + rightOffset - upOffset).rgb);

    // Normal map creation
    float sum0 = rightTopHeight + topHeight + rightBottomHeight;
    float sum1 = leftTopHeight + bottomHeight + leftBottomHeight;
    float sum2 = leftTopHeight + leftHeight + rightTopHeight;
    float sum3 = leftBottomHeight + rightHeight + rightBottomHeight;
    float vect1 = (sum1 - sum0);
    float vect2 = (sum2 - sum3);

    // Put them together and scale.
    vec2 Normal = vec2(vect1, vect2) * viewportInverse * fScale;

    // Color
    vec4 scene0 = texture2D(tex, texCoords.xy);
    vec4 scene1 = texture2D(tex, texCoords.xy + Normal.xy);
    vec4 scene2 = texture2D(tex, texCoords.xy - Normal.xy);
    vec4 scene3 = texture2D(tex, texCoords.xy + vec2(Normal.x, -Normal.y) * 0.5);
    vec4 scene4 = texture2D(tex, texCoords.xy - vec2(Normal.x, -Normal.y) * 0.5);

#ifdef SUPPORT_ALPHA
    return vec4((scene0.rgb + scene1.rgb + scene2.rgb + scene3.rgb + scene4.rgb) * 0.2, scene0.a);
#else
    return vec4((scene0.rgb + scene1.rgb + scene2.rgb + scene3.rgb + scene4.rgb) * 0.2, 1.0);
#endif
}

float GetColorLuminance(vec3 i_vColor) {
    return dot(i_vColor, vec3(0.2126, 0.7152, 0.0722));
}

void main() {
    gl_FragColor = nfaa(u_texture0, v_texCoords, u_viewportInverse);
}
