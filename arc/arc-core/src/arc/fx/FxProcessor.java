package arc.fx;

import arc.*;
import arc.fx.util.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.graphics.gl.*;
import arc.struct.*;
import arc.util.*;

/**
 * Provides a way to beginCapture the rendered scene to an off-screen buffer and to apply a chain of effects on it before rendering to
 * screen.
 * <p>
 * Effects can be added or removed via {@link #addEffect(FxFilter)} and {@link #removeEffect(FxFilter)}.
 * @author bmanuel
 * @author metaphore
 */
public final class FxProcessor implements Disposable{
    private final ObjectIntMap<FxFilter> priorities = new ObjectIntMap<>();
    /** All effects ever added. */
    private final Seq<FxFilter> effectsAll = new Seq<>();
    /** Maintains a per-frame updated list of enabled effects */
    private final Seq<FxFilter> effectsEnabled = new Seq<>();

    /** A mesh that is shared among basic filters to draw to full screen. */
    private final FxBufferRenderer bufferRenderer = new FxBufferRenderer();

    private final Format[] fboFormat;
    private final PingPongBuffer pingPongBuffer;

    private boolean disabled = false;
    private boolean capturing = false;
    private boolean hasCaptured = false;
    private boolean applyingEffects = false;

    private boolean blendingEnabled = false;

    private int width, height;

    public FxProcessor(){
        this(Core.graphics.getBackBufferWidth(), Core.graphics.getBackBufferHeight(), Format.defaultColor);
    }

    public FxProcessor(int w, int h){
        this(w, h, Format.defaultColor);
    }

    public FxProcessor(int bufferWidth, int bufferHeight, Format[] fboFormat){
        this.fboFormat = fboFormat;
        this.pingPongBuffer = new PingPongBuffer(bufferWidth, bufferHeight, fboFormat);
        this.width = bufferWidth;
        this.height = bufferHeight;
    }

    @Override
    public void dispose(){
        pingPongBuffer.dispose();
    }

    public void resize(int width, int height){
        if(this.width != width || this.height != height){
            this.width = width;
            this.height = height;

            pingPongBuffer.resize(width, height);

            for(FxFilter filter : effectsAll){
                filter.resize(width, height);
                filter.rebind();
            }
        }
    }

    public void rebind(){
        bufferRenderer.rebind();

        for(FxFilter filter : effectsAll){
            filter.rebind();
        }
    }

    public int getWidth(){
        return width;
    }

    public int getHeight(){
        return height;
    }

    public boolean isDisabled(){
        return disabled;
    }

    /** Sets whether or not the post-processor should be disabled */
    public void setDisabled(boolean disabled){
        this.disabled = disabled;
    }

    public boolean isBlendingEnabled(){
        return blendingEnabled;
    }

    /**
     * Enables OpenGL blending for the effect chain rendering stage.
     * Disabled by default.
     */
    public void setBlendingEnabled(boolean blendingEnabled){
        this.blendingEnabled = blendingEnabled;
    }

    /**
     * Returns the internal framebuffer format, computed from the parameters specified during construction. NOTE: the returned
     * Format will be valid after construction and NOT early!
     */
    public Format[] getFramebufferFormat(){
        return fboFormat;
    }

    public void setBufferTextureParams(TextureWrap u, TextureWrap v, TextureFilter min, TextureFilter mag){
        pingPongBuffer.setTextureParams(u, v, min, mag);
    }

    public void setTextureFilter(TextureFilter filter){
        setBufferTextureParams(TextureWrap.clampToEdge, TextureWrap.clampToEdge, filter, filter);
    }

    public boolean isCapturing(){
        return capturing;
    }

    public boolean isApplyingEffects(){
        return applyingEffects;
    }

    public boolean hasResult(){
        return hasCaptured;
    }

    /**
     * @return the last active destination buffer.
     */
    public FrameBuffer getResultBuffer(){
        return pingPongBuffer.getDstBuffer();
    }

    /**
     * @return the internal ping-pong buffer.
     */
    public PingPongBuffer getPingPongBuffer(){
        return pingPongBuffer;
    }

    public boolean hasEnabledEffects(){
        return effectsAll.contains(fx -> !fx.isDisabled());
    }

    /**
     * Adds an effect to the effect chain and transfers ownership to the VfxManager.
     * The order of the inserted effects IS important, since effects will be applied in a FIFO fashion,
     * the first added is the first being applied.
     * <p>
     * For more control over the order supply the effect with a priority - {@link #addEffect(FxFilter, int)}.
     * @see #addEffect(FxFilter, int)
     */
    public void addEffect(FxFilter effect){
        addEffect(effect, 0);
    }

    public void addEffect(FxFilter effect, int priority){
        effectsAll.add(effect);
        priorities.put(effect, priority);
        effectsAll.sort(e -> priorities.get(effect, 0));
        effect.resize(width, height);
        effect.rebind();
    }

    /**
     * Removes the specified effect from the effect chain.
     */
    public void removeEffect(FxFilter effect){
        effectsAll.remove(effect);
    }

    /**
     * Removes all effects from the effect chain.
     */
    public void removeAllEffects(){
        effectsAll.clear();
    }

    /**
     * Changes the order of the effect in the effect chain.
     */
    public void setEffectPriority(FxFilter effect, int priority){
        priorities.put(effect, priority);
        effectsAll.sort(e -> priorities.get(effect, 0));
    }

    /**
     * Cleans up managed {@link PingPongBuffer}s' with {@link Color#clear}.
     */
    public void clear(){
        clear(Color.clear);
    }

    /**
     * Cleans up managed {@link PingPongBuffer}s' with specified color.
     */
    public void clear(Color color){
        if(capturing) throw new IllegalStateException("Cannot clean up buffers when capturing.");
        if(applyingEffects) throw new IllegalStateException("Cannot clean up buffers when applying effects.");

        pingPongBuffer.clear(color);
        hasCaptured = false;
    }

    /**
     * Starts capturing the scene.
     * @return true or false, whether or not capturing has been initiated.
     * Capturing will fail if the manager is disabled or capturing is already started.
     */
    public boolean begin(){
        if(applyingEffects){
            throw new IllegalStateException("You cannot capture when you're applying the effects.");
        }

        if(disabled) return false;
        if(capturing) return false;

        Draw.flush();

        capturing = true;
        pingPongBuffer.begin();
        return true;
    }

    /**
     * Stops capturing the scene.
     * @return false if there was no capturing before that call.
     */
    public boolean end(){
        if(!capturing) return false;

        Draw.flush();

        hasCaptured = true;
        capturing = false;
        pingPongBuffer.end();
        return true;
    }

    /**
     * Applies the effect chain, if there is one.
     */
    public void applyEffects(){
        if(capturing){
            throw new IllegalStateException("You should call VfxManager.endCapture() before applying the effects.");
        }

        if(disabled) return;
        if(!hasCaptured) return;

        effectsAll.each(FxFilter::update);

        Seq<FxFilter> effectChain = effectsEnabled.selectFrom(effectsAll, e -> !e.isDisabled());

        applyingEffects = true;
        int count = effectChain.size;
        if(count > 0){
            // Enable blending to preserve buffer's alpha values.
            if(blendingEnabled){
                Gl.enable(Gl.blend);
            }else{
                //disable otherwise
                Gl.disable(Gl.blend);
            }

            Gl.disable(Gl.cullFace);
            Gl.disable(Gl.depthTest);

            // Render the effect chain.
            pingPongBuffer.swap(); // Swap buffers to get captured result in src buffer.
            pingPongBuffer.begin();
            for(int i = 0; i < count; i++){
                FxFilter effect = effectChain.get(i);
                effect.render(pingPongBuffer.getSrcBuffer(), pingPongBuffer.getDstBuffer());
                if(i < count - 1){
                    pingPongBuffer.swap();
                }
            }
            pingPongBuffer.end();

            // Ensure default texture unit #0 is active.
            Gl.activeTexture(Gl.texture0); //TODO Do we need this?

            if(blendingEnabled){
                Gl.disable(Gl.blend);
            }
        }
        applyingEffects = false;
    }

    public void render(){
        if(capturing){
            throw new IllegalStateException("You should call VfxManager.endCapture() before rendering the result.");
        }
        if(disabled) return;
        if(!hasCaptured) return;

        // Enable blending to preserve buffer's alpha values.
        if(blendingEnabled){
            Gl.enable(Gl.blend);
        }else{
            Gl.disable(Gl.blend);
        }
        bufferRenderer.renderToScreen(pingPongBuffer.getDstBuffer());
        if(blendingEnabled){
            Gl.disable(Gl.blend);
        }
    }

    public void render(int x, int y, int width, int height){
        if(capturing){
            throw new IllegalStateException("You should call VfxManager.endCapture() before rendering the result.");
        }
        if(disabled) return;
        if(!hasCaptured) return;

        // Enable blending to preserve buffer's alpha values.
        if(blendingEnabled){
            Gl.enable(Gl.blend);
        }
        bufferRenderer.renderToScreen(pingPongBuffer.getDstBuffer(), x, y, width, height);
        if(blendingEnabled){
            Gl.disable(Gl.blend);
        }
    }

    public void render(FrameBuffer output){
        if(capturing){
            throw new IllegalStateException("You should call VfxManager.endCapture() before rendering the result.");
        }
        if(disabled) return;
        if(!hasCaptured) return;

        // Enable blending to preserve buffer's alpha values.
        if(blendingEnabled){
            Gl.enable(Gl.blend);
        }
        bufferRenderer.renderToFbo(pingPongBuffer.getDstBuffer(), output);
        if(blendingEnabled){
            Gl.disable(Gl.blend);
        }
    }
}