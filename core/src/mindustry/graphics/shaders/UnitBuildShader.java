package mindustry.graphics.shaders;

import arc.graphics.*;
import arc.graphics.g2d.*;

public class UnitBuildShader extends LoadShader{
    public float progress, time;
    public Color color = new Color();
    public TextureRegion region;

    public UnitBuildShader(){
        super("unitbuild", "default_batch");
    }

    @Override
    public void apply(){
        setUniformf("u_time", time);
        setUniformf("u_color", color);
        setUniformf("u_progress", progress);
        setUniformf("u_uv", region.u, region.v);
        setUniformf("u_uv2", region.u2, region.v2);
        setUniformf("u_texsize", region.texture.width, region.texture.height);
    }
}
