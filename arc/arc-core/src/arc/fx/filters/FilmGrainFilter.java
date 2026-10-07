package arc.fx.filters;

import arc.*;
import arc.fx.*;
import arc.util.*;

public class FilmGrainFilter extends FxFilter{
    public float seed = 0f;

    public FilmGrainFilter(){
        super(compileShader(
        Core.files.internal("vfxshaders/screenspace.vert"),
        Core.files.internal("vfxshaders/film-grain.frag")));
        rebind();
    }

    public void setSeed(float seed){
        this.seed = seed;
        rebind();
    }

    @Override
    public void setParams(){
        shader.setUniformi("u_texture0", u_texture0);
        shader.setUniformf("u_seed", seed);
    }

    @Override
    public void update(){
        this.time = (this.time + Time.delta /60f) % 1f;
        seed = time;
    }
}
