package mindustry.graphics.shaders;

import arc.graphics.g2d.*;

public class BlockBuildShader extends LoadShader{
    public float progress;
    //Alpha changes the opacity of *everything*, while the provided batch color only changes the outline
    public float alpha = 1f;
    public TextureRegion region = new TextureRegion();
    public float time;

    public BlockBuildShader(){
        super("blockbuild", "default_batch");
    }

    @Override
    public void apply(){
        setUniformf("u_progress", progress);
        setUniformf("u_time", time);
        setUniformf("u_alpha", alpha);

        if(region.texture == null){
            setUniformf("u_uv", 0f, 0f);
            setUniformf("u_uv2", 1f, 1f);
            setUniformf("u_texsize", 1, 1);
        }else{
            setUniformf("u_uv", region.u, region.v);
            setUniformf("u_uv2", region.u2, region.v2);
            setUniformf("u_texsize", region.texture.width, region.texture.height);
        }
    }
}
