package mindustry.graphics.shaders;

import arc.graphics.*;
import mindustry.graphics.*;

public class LoadShader extends Shader{

    public LoadShader(String frag, String vert){
        super(Shaders.getShaderFi(vert + ".vert"), Shaders.getShaderFi(frag + ".frag"));
    }
}
