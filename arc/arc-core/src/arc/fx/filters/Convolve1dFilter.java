package arc.fx.filters;

import arc.*;
import arc.fx.*;

public final class Convolve1dFilter extends FxFilter{
    public int length;
    public float[] weights;
    public float[] offsets;

    public Convolve1dFilter(int length){
        this(length, new float[length], new float[length * 2]);
    }

    public Convolve1dFilter(int length, float[] weights_data){
        this(length, weights_data, new float[length * 2]);
    }

    public Convolve1dFilter(int length, float[] weights_data, float[] offsets){
        super(compileShader(
        Core.files.internal("vfxshaders/screenspace.vert"),
        Core.files.internal("vfxshaders/convolve-1d.frag"),
        "#define LENGTH " + length));
        setWeights(length, weights_data, offsets);
        rebind();
    }

    public void setWeights(int length, float[] weights, float[] offsets){
        this.weights = weights;
        this.length = length;
        this.offsets = offsets;
    }

    @Override
    public void dispose(){
        super.dispose();
        weights = null;
        offsets = null;
        length = 0;
    }

    @Override
    public void setParams(){
        shader.setUniformi("u_texture0", u_texture0);
        shader.setUniform1fv("SampleWeights", weights, 0, length);
        shader.setUniform2fv("SampleOffsets", offsets, 0, length * 2);
    }
}
