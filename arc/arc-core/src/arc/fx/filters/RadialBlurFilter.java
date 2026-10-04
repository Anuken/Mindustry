package arc.fx.filters;

import arc.*;
import arc.fx.*;
import arc.util.*;

public final class RadialBlurFilter extends FxFilter{
    public final int passes;
    public float strength = 0.2f;
    public float originX = 0.5f;
    public float originY = 0.5f;
    public float zoom = 1f;

    public RadialBlurFilter(int passes){
        super(compileShader(
        Core.files.internal("vfxshaders/radial-blur.vert"),
        Core.files.internal("vfxshaders/radial-blur.frag"),
        "#define PASSES " + passes));
        this.passes = passes;
        rebind();
    }

    public float getOriginX(){
        return originX;
    }

    public float getOriginY(){
        return originY;
    }

    /**
     * Specify the zoom origin in {@link Align} bits.
     * @see Align
     */
    public void setOrigin(int align){
        final float originX;
        final float originY;
        if((align & Align.left) != 0){
            originX = 0f;
        }else if((align & Align.right) != 0){
            originX = 1f;
        }else{
            originX = 0.5f;
        }
        if((align & Align.bottom) != 0){
            originY = 0f;
        }else if((align & Align.top) != 0){
            originY = 1f;
        }else{
            originY = 0.5f;
        }
        setOrigin(originX, originY);
    }

    /**
     * Specify the zoom origin in normalized screen coordinates.
     * @param originX horizontal origin [0..1].
     * @param originY vertical origin [0..1].
     */
    public void setOrigin(float originX, float originY){
        this.originX = originX;
        this.originY = originY;
    }

    @Override
    public void setParams(){
        shader.setUniformi("u_texture0", u_texture0);
        shader.setUniformf("u_blurDiv", this.strength / (float)passes);
        shader.setUniformf("u_offsetX", originX);
        shader.setUniformf("u_offsetY", originY);
        shader.setUniformf("u_zoom", zoom);
    }
}
