package arc.fx.filters;

import arc.*;
import arc.fx.*;

public final class RadialDistortionFilter extends FxFilter{
    public float zoom = 1f;
    public float distortion = 0.3f;

    public RadialDistortionFilter(){
        super(compileShader(
        Core.files.internal("vfxshaders/screenspace.vert"),
        Core.files.internal("vfxshaders/radial-distortion.frag")));
        rebind();
    }

    @Override
    public void setParams(){
        shader.setUniformi("u_texture0", u_texture0);
        shader.setUniformf("distortion", distortion);
        shader.setUniformf("zoom", zoom);
    }
}
