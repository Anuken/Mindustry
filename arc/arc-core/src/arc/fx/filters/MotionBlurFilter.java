package arc.fx.filters;

import arc.*;
import arc.fx.*;
import arc.fx.util.*;
import arc.graphics.*;

/**
 * Motion blur filter that draws the last frame (motion filter included) with a lower opacity.
 * @author Toni Sagrista
 */
public class MotionBlurFilter extends FxFilter{
    private final CopyFilter copyFilter;
    private final FxBufferQueue localBuffer;

    public float blurOpacity = 0.9f;
    public Texture lastFrameTex;

    public MotionBlurFilter(BlurFunction blurFunction){
        super(compileShader(
        Core.files.internal("vfxshaders/screenspace.vert"),
        Core.files.internal("vfxshaders/" + blurFunction.fragmentShaderName + ".frag")));

        copyFilter = new CopyFilter();

        localBuffer = new FxBufferQueue(Format.defaultColor,
        // On WebGL (GWT) we cannot render from/into the same texture simultaneously.
        // Will use ping-pong approach to avoid "writing into itself".
        Core.app.getType() == Application.ApplicationType.web ? 2 : 1);

        rebind();
    }

    @Override
    public void resize(int width, int height){
        copyFilter.resize(width, height);
        localBuffer.resize(width, height);
    }

    @Override
    public void dispose(){
        super.dispose();
        copyFilter.dispose();
        localBuffer.dispose();
    }

    @Override
    public void rebind(){
        super.rebind();
        copyFilter.rebind();
        localBuffer.rebind();
    }

    @Override
    public void setParams(){
        shader.setUniformi("u_texture0", u_texture0);
        if(lastFrameTex != null){
            shader.setUniformi("u_texture1", u_texture1);
        }
        shader.setUniformf("u_blurOpacity", this.blurOpacity);
    }

    @Override
    protected void onBeforeRender(){
        inputTexture.bind(u_texture0);
        if(lastFrameTex != null){
            lastFrameTex.bind(u_texture1);
        }
    }

    @Override
    public void render(FrameBuffer src, FrameBuffer dst){
        FrameBuffer prevFrame = this.localBuffer.changeToNext();
        setInput(src).setOutput(prevFrame).render();
        lastFrameTex = prevFrame.texture;
        copyFilter.setInput(prevFrame).setOutput(dst).render();
    }

    /** Defines which function will be used to mix the two frames to produce motion blur effect. */
    public enum BlurFunction{
        MAX("motionblur-max"),
        MIX("motionblur-mix");

        final String fragmentShaderName;

        BlurFunction(String fragmentShaderName){
            this.fragmentShaderName = fragmentShaderName;
        }
    }
}