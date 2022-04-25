
uniform lowp sampler2D u_texture;
varying vec2 v_texCoords0;
varying vec2 v_texCoords1;
varying vec2 v_texCoords2;
varying vec2 v_texCoords3;
varying vec2 v_texCoords4;
const float center = 0.2270270270; 
const float close = 0.3162162162;
const float far = 0.0702702703;

void main(){
    gl_FragColor = far * texture2D(u_texture, v_texCoords0)
				+ close * texture2D(u_texture, v_texCoords1)
				+ center * texture2D(u_texture, v_texCoords2) 
				+ close * texture2D(u_texture, v_texCoords3)
				+ far * texture2D(u_texture, v_texCoords4);


    //TODO this is broken.
    /*

    vec4
    v1 = texture2D(u_texture, v_texCoords0),
    v2 = texture2D(u_texture, v_texCoords1),
    v3 = texture2D(u_texture, v_texCoords2),
    v4 = texture2D(u_texture, v_texCoords3),
    v5 = texture2D(u_texture, v_texCoords4);

    float
    a1 = v1.a * far,
    a2 = v2.a * close,
    a3 = v3.a * center,
    a4 = v4.a * close,
    a5 = v5.a * far;

    //far close center close far

    //TODO figure this out
    //RGB values should be weighted by their respective alpha values.
    gl_FragColor = vec4(
        (v1.rgb * a1 + v2.rgb * a2 + v3.rgb * a3 + v4.rgb * a4 + v5.rgb * a5) /
        max(a1 + a2 + a3 + a4 + a5, 0.0001), //RGB must be weighed by the sum of all alpha processed. if a pixel contributes less weight, the multiplication is higher
        a1 + a2 + a3 + a4 + a5); //alpha is just the weighed sum

   */
}	
