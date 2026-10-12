package arc.fx.filters;

import arc.fx.*;
import arc.util.*;

public final class ZoomFilter extends FxFilter{
    public float originX = 0.5f;
    public float originY = 0.5f;
    public float zoom = 1f;

    public ZoomFilter(){
        super("zoom", "zoom");
        rebind();
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
        this.originX = originX;
        this.originY = originY;
    }

    @Override
    public void setParams(){
        shader.setUniformi("u_texture0", u_texture0);
        shader.setUniformf("u_offsetX", originX);
        shader.setUniformf("u_offsetY", originY);
        shader.setUniformf("u_zoom", zoom);
    }
}
