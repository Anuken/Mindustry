package arc.fx.filters;

import arc.*;
import arc.fx.*;
import arc.graphics.*;

public final class VignettingFilter extends FxFilter{
    private Texture lutTexture = null;

    public float vignetteX = 0.8f;
    public float vignetteY = 0.25f;
    public float centerX = 0.5f;
    public float centerY = 0.5f;
    public float intensity = 1f;
    public float saturation = 0f;
    public float saturationMul = 0f;

    public boolean saturationEnabled;

    public boolean lutEnabled = false;
    public float lutIntensity = 1f;
    public int lutIndex1 = -1;
    public int lutIndex2 = -1;
    public float lutStep;
    public float lutStepOffset;
    public float lutIndexOffset = 0f;

    public VignettingFilter(boolean controlSaturation){
        super(compileShader(
        Core.files.internal("vfxshaders/screenspace.vert"),
        Core.files.internal("vfxshaders/vignetting.frag"),
        (controlSaturation ?
        "#define CONTROL_SATURATION\n#define ENABLE_GRADIENT_MAPPING" :
        "#define ENABLE_GRADIENT_MAPPING")));
        saturationEnabled = controlSaturation;
        rebind();
    }

    /**
     * Sets the texture with which gradient mapping will be performed.
     */
    public void setLut(Texture texture){
        lutTexture = texture;
        lutEnabled = (lutTexture != null);

        if(lutEnabled){
            lutStep = 1f / (float)texture.height;
            lutStepOffset = lutStep / 2f; // center texel
        }
    }

    @Override
    public void setParams(){
        shader.setUniformi("u_texture0", u_texture0);

        shader.setUniformi("u_lutIndex1", lutIndex1);
        shader.setUniformi("u_lutIndex2", lutIndex2);
        shader.setUniformf("u_lutIndexOffset", lutIndexOffset);

        shader.setUniformi("u_texture1", u_texture1);
        shader.setUniformf("u_lutIntensity", lutIntensity);
        shader.setUniformf("u_lutStep", lutStep);
        shader.setUniformf("u_lutStepOffset", lutStepOffset);

        if(saturationEnabled){
            shader.setUniformf("u_saturation", saturation);
            shader.setUniformf("u_saturationMul", saturationMul);
        }

        shader.setUniformf("u_vignetteIntensity", intensity);
        shader.setUniformf("u_vignetteX", vignetteX);
        shader.setUniformf("u_vignetteY", vignetteY);
        shader.setUniformf("u_centerX", centerX);
        shader.setUniformf("u_centerY", centerY);
    }

    @Override
    protected void onBeforeRender(){
        inputTexture.bind(u_texture0);
        if(lutEnabled){
            lutTexture.bind(u_texture1);
        }
    }
}
