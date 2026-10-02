package arc.fx;

import arc.*;
import arc.files.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.util.*;

/**
 * Base class for any single-pass filter.
 */
public abstract class FxFilter implements Disposable{
    protected static final int u_texture0 = 0;
    protected static final int u_texture1 = 1;
    protected static final int u_texture2 = 2;
    protected static final int u_texture3 = 3;

    protected final Shader shader;

    protected Texture inputTexture = null;
    protected FrameBuffer outputBuffer = null;
    protected boolean disabled = false, autobind = false;

    public float time = 0f;

    public FxFilter(){
        this(null);
    }

    public FxFilter(String vert, String frag){
        this(compileShader(Core.files.internal("vfxshaders/" +vert+".vert"), Core.files.internal("vfxshaders/" +frag+".frag")));
    }

    public FxFilter(Shader shader){
        this.shader = shader;
    }

    public static Shader compileShader(Fi vertexFile, Fi fragmentFile){
        return compileShader(vertexFile, fragmentFile, "");
    }

    public static Shader compileShader(Fi vertexFile, Fi fragmentFile, String defines){
        return new Shader(defines + "\n" + vertexFile.readString(), defines + "\n" + fragmentFile.readString());
    }

    public FxFilter setInput(Texture input){
        this.inputTexture = input;
        return this;
    }

    public FxFilter setInput(FrameBuffer input){
        return setInput(input.texture);
    }

    public FxFilter setOutput(FrameBuffer output){
        this.outputBuffer = output;
        return this;
    }

    @Override
    public void dispose(){
        shader.dispose();
    }

    /**
     * This method should be called once filter will be added.
     * Also it must be called on every application resize as usual.
     */
    public void resize(int width, int height){

    }

    public void rebind(){
        if(shader == null) return;

        shader.bind();
        setParams();
    }

    /**
     * Concrete objects shall be responsible to recreate or rebind its own resources whenever its needed, usually when the OpenGL
     * context is lost. Eg., framebuffer textures should be updated and shader parameters should be reuploaded/rebound.
     */
    protected void setParams(){
        if(shader != null){
            shader.setUniformi("u_texture0", u_texture0);
        }
    }

    /*
     * Sets the parameter to the specified value for this filter. This is for one-off operations since the shader is being bound
     * and unbound once per call: for a batch-ready version of this function see and use setParams instead.
     */
    public void render(){
        boolean manualBufferBind = outputBuffer != null && !outputBuffer.isBound();
        if(manualBufferBind){
            outputBuffer.begin();
        }

        // Gives a chance to filters to perform needed operations just before the rendering operation takes place.
        onBeforeRender();

        shader.bind();
        if(autobind){
            setParams();
        }
        Draw.blit(shader);

        if(manualBufferBind){
            outputBuffer.end();
        }
    }

    /** This method gets called just before rendering. */
    protected void onBeforeRender(){
        inputTexture.bind(u_texture0);
    }

    /** Concrete objects shall implements its own rendering, given the source and destination buffers. */
    public void render(FrameBuffer src, FrameBuffer dst){
        setInput(src).setOutput(dst).render();
    }

    /** Whether or not this effect is disabled and shouldn't be processed */
    public boolean isDisabled(){
        return disabled;
    }

    /** Sets this effect disabled or not */
    public void setDisabled(boolean enabled){
        this.disabled = enabled;
    }

    public void update(){
        time = Time.time;
    }
}
