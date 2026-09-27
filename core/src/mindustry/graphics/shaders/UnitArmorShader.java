package mindustry.graphics.shaders;

import arc.graphics.g2d.*;

public class UnitArmorShader extends LoadShader{
    public float progress, time;
    public TextureRegion region;

    public UnitArmorShader(){
        super("unitarmor", "default");
    }

    @Override
    public void apply(){
        setUniformf("u_time", time);
        setUniformf("u_progress", progress);
        setUniformf("u_uv", region.u, region.v);
        setUniformf("u_uv2", region.u2, region.v2);
        setUniformf("u_texsize", region.texture.width, region.texture.height);
    }
}
