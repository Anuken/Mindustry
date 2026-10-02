package arc.fx.filters;

import arc.*;
import arc.fx.*;
import arc.graphics.*;
import arc.math.geom.*;

/**
 * Lens flare effect.
 * @author Toni Sagrista
 **/
public final class LensFlareFilter extends FxFilter{
    private final Vec2 viewport = new Vec2();

    public final Vec2 lightPosition = new Vec2(0.5f, 0.5f);
    public final Color color = new Color(1f, 0.8f, 0.2f, 1f);
    public float intensity = 5.0f;

    public LensFlareFilter(){
        super(compileShader(
        Core.files.internal("vfxshaders/screenspace.vert"),
        Core.files.internal("vfxshaders/lensflare.frag")));
        rebind();
    }

    @Override
    public void resize(int width, int height){
        viewport.set(width, height);
        rebind();
    }

    @Override
    public void setParams(){
        shader.setUniformi("u_texture0", u_texture0);
        shader.setUniformf("u_lightPosition", lightPosition);
        shader.setUniformf("u_intensity", intensity);
        shader.setUniformf("u_color", color.r, color.g, color.b);
        shader.setUniformf("u_viewport", viewport);
    }
}
