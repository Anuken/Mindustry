package arc.fx.filters;

import arc.*;
import arc.fx.*;

/**
 * HDR filter.
 * @author Toni Sagrista
 */
public final class HdrFilter extends FxFilter{
    public float exposure;
    public float gamma;

    public HdrFilter(){
        this(3.0f, 2.2f);
    }

    public HdrFilter(float exposure, float gamma){
        super(compileShader(
        Core.files.internal("vfxshaders/screenspace.vert"),
        Core.files.internal("vfxshaders/hdr.frag")));
        this.exposure = exposure;
        this.gamma = gamma;
    }

    @Override
    public void setParams(){
        // reimplement super to batch every parameter
        shader.setUniformi("u_texture0", u_texture0);
        shader.setUniformf("u_exposure", exposure);
        shader.setUniformf("u_gamma", gamma);
    }
}