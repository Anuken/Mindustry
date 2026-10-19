package arc.graphics;

import arc.graphics.gl.*;

public enum TextureWrap{
    mirroredRepeat(Gl.mirroredRepeat), clampToEdge(Gl.clampToEdge), repeat(Gl.repeat);

    public static final TextureWrap[] all = values();

    final int glEnum;

    TextureWrap(int glEnum){
        this.glEnum = glEnum;
    }

    public int getGLEnum(){
        return glEnum;
    }
}
