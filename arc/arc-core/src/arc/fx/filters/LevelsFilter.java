package arc.fx.filters;

import arc.*;
import arc.fx.*;

/**
 * Controls levels of brightness and contrast
 * @author tsagrista
 */
public class LevelsFilter extends FxFilter{
    public float brightness = 0.0f;
    public float contrast = 1.0f;
    public float saturation = 1.0f;
    public float hue = 1.0f;
    public float gamma = 1.0f;

    public LevelsFilter(){
        super(compileShader(Core.files.internal("vfxshaders/screenspace.vert"), Core.files.internal("vfxshaders/levels.frag")));
        rebind();
    }

    @Override
    public void setParams(){
        shader.setUniformi("u_texture0", u_texture0);
        shader.setUniformf("u_brightness", brightness);
        shader.setUniformf("u_contrast", contrast);
        shader.setUniformf("u_saturation", saturation);
        shader.setUniformf("u_hue", hue);
        shader.setUniformf("u_gamma", gamma);
    }
}
