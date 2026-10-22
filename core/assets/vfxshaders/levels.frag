varying vec2 v_texCoords;

uniform sampler2D u_texture0;

uniform float u_brightness;
uniform float u_contrast;
uniform float u_saturation;
uniform float u_hue;
uniform float u_gamma;

vec3 rgb2hsv(vec3 c) {
    vec4 K = vec4(0.0, -1.0 / 3.0, 2.0 / 3.0, -1.0);
    vec4 p = mix(vec4(c.bg, K.wz), vec4(c.gb, K.xy), step(c.b, c.g));
    vec4 q = mix(vec4(p.xyw, c.r), vec4(c.r, p.yzx), step(p.x, c.r));

    float d = q.x - min(q.w, q.y);
    float e = 1.0e-10;
    return vec3(abs(q.z + (q.w - q.y) / (6.0 * d + e)), d / (q.x + e), q.x);
}

vec3 hsv2rgb(vec3 c) {
    vec4 K = vec4(1.0, 2.0 / 3.0, 1.0 / 3.0, 3.0);
    vec3 p = abs(fract(c.xxx + K.xyz) * 6.0 - K.www);
    return c.z * mix(K.xxx, clamp(p - K.xxx, 0.0, 1.0), c.y);
}

void main() {
   vec4 pixelColor = texture2D(u_texture0, v_texCoords);
   pixelColor.rgb /= pixelColor.a;

  // Apply contrast
  pixelColor.rgb = ((pixelColor.rgb - 0.5) * max(u_contrast, 0.0)) + 0.5;

  vec3 hsv = rgb2hsv(pixelColor.rgb);
  // Apply saturation
  hsv.y *= u_saturation;
  // Apply hue
  hsv.x *= u_hue;

  pixelColor.rgb = hsv2rgb(hsv);

  // Apply brightness
  pixelColor.rgb += u_brightness;

  // Return final pixel color
  pixelColor.rgb *= pixelColor.a;

  gl_FragColor = pixelColor;

  // Gamma correction
  gl_FragColor.rgb = pow(gl_FragColor.rgb, vec3(1.0 / u_gamma));
}