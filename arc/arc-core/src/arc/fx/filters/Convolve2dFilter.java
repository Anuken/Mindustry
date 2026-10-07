package arc.fx.filters;

import arc.fx.util.*;

/**
 * Encapsulates a separable 2D convolution kernel filter
 * @author bmanuel
 * @author metaphore
 */
public final class Convolve2dFilter extends MultipassVfxFilter{
    public final int radius;
    public final int length; // NxN taps filter, w/ N=length
    public final float[] weights, offsetsHor, offsetsVert;
    public Convolve1dFilter hor, vert;

    public Convolve2dFilter(int radius){
        this.radius = radius;
        length = (radius * 2) + 1;

        hor = new Convolve1dFilter(length);
        vert = new Convolve1dFilter(length, hor.weights);

        weights = hor.weights;
        offsetsHor = hor.offsets;
        offsetsVert = vert.offsets;
    }

    @Override
    public void dispose(){
        hor.dispose();
        vert.dispose();
    }

    @Override
    public void setParams(){
        hor.rebind();
        vert.rebind();
    }

    @Override
    public void render(PingPongBuffer buffer){
        hor.setInput(buffer.getSrcTexture())
        .setOutput(buffer.getDstBuffer())
        .render();

        buffer.swap();

        vert.setInput(buffer.getSrcTexture())
        .setOutput(buffer.getDstBuffer())
        .render();
    }
}
