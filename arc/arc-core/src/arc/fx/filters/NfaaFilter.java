package arc.fx.filters;

import arc.*;
import arc.fx.*;
import arc.math.geom.*;

/**
 * Normal filtered anti-aliasing filter.
 * @author Toni Sagrista
 */
public final class NfaaFilter extends FxFilter{
    private final Vec2 viewportInverse = new Vec2();

    public NfaaFilter(){
        this(false);
    }

    public NfaaFilter(boolean supportAlpha){
        super(compileShader(
        Core.files.internal("vfxshaders/screenspace.vert"),
        Core.files.internal("vfxshaders/nfaa.frag"), supportAlpha ? "#define SUPPORT_ALPHA" : ""));
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
    }
}
