package arc.fx.filters;

import arc.*;
import arc.fx.*;
import arc.math.geom.*;

/**
 * Fast approximate anti-aliasing filter.
 * @author Toni Sagrista
 */
public final class FxaaFilter extends FxFilter{
    public final Vec2 viewportInverse = new Vec2();
    public float fxaaReduceMin;
    public float fxaaReduceMul;
    public float fxaaSpanMax;

    public FxaaFilter(){
        this(1f/128f, 1f/8f, 8f, true);
    }

    public FxaaFilter(float fxaaReduceMin, float fxaaReduceMul, float fxaaSpanMax, boolean supportAlpha){
        super(compileShader(
        Core.files.internal("vfxshaders/screenspace.vert"),
        Core.files.internal("vfxshaders/fxaa.frag"),
        supportAlpha ? "#define SUPPORT_ALPHA" : ""));
        this.fxaaReduceMin = fxaaReduceMin;
        this.fxaaReduceMul = fxaaReduceMul;
        this.fxaaSpanMax = fxaaSpanMax;
        rebind();
    }

    @Override
    public void resize(int width, int height){
        this.viewportInverse.set(1f / width, 1f / height);
        rebind();
    }

    @Override
    public void setParams(){
        shader.setUniformi("u_texture0", u_texture0);
        shader.setUniformf("u_viewportInverse", viewportInverse);
        shader.setUniformf("u_fxaaReduceMin", fxaaReduceMin);
        shader.setUniformf("u_fxaaReduceMul", fxaaReduceMul);
        shader.setUniformf("u_fxaaSpanMax", fxaaSpanMax);
    }
}
