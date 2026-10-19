package arc.fx.filters;

import arc.*;
import arc.fx.*;
import arc.graphics.*;

public final class MixFilter extends FxFilter{
    private Texture inputTexture2 = null;
    public float mix = 0.5f;

    public MixFilter(){
        super(compileShader(
        Core.files.internal("vfxshaders/screenspace.vert"),
        Core.files.internal("vfxshaders/mix.frag")));

        rebind();
    }

    public MixFilter setInput(FrameBuffer buffer1, FrameBuffer buffer2){
        this.inputTexture = buffer1.texture;
        this.inputTexture2 = buffer2.texture;
        return this;
    }

    public MixFilter setInput(Texture texture1, Texture texture2){
        this.inputTexture = texture1;
        this.inputTexture2 = texture2;
        return this;
    }

    @Override
    public MixFilter setInput(FrameBuffer input){
        throw new UnsupportedOperationException("Use #setInput(FboWrapper, FboWrapper)} instead.");
    }

    @Override
    public MixFilter setInput(Texture input){
        throw new UnsupportedOperationException("Use #setInput(Texture, Texture)} instead.");
    }

    @Override
    public void resize(int width, int height){

    }

    @Override
    public void setParams(){
        shader.setUniformi("u_texture0", u_texture0);
        shader.setUniformi("u_texture1", u_texture1);
        shader.setUniformf("u_mix", mix);
    }

    @Override
    protected void onBeforeRender(){
        inputTexture.bind(u_texture0);
        inputTexture2.bind(u_texture1);
    }
}
