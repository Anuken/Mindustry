
varying vec3 v_texCoords;

uniform samplerCube u_cubemap;

void main(){
    gl_FragColor = textureCube(u_cubemap, v_texCoords);
}