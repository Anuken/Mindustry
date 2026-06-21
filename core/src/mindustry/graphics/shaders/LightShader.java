package mindustry.graphics.shaders;

import arc.graphics.*;

public class LightShader extends LoadShader{
    public Color ambient = new Color(0.01f, 0.01f, 0.04f, 0.99f);

    public LightShader(){
        super("light", "screenspace");
    }

    @Override
    public void apply(){
        setUniformf("u_ambient", ambient);
    }

}
