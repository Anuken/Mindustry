package mindustry.graphics;

import arc.graphics.*;
import arc.graphics.gl.*;
import arc.util.*;

import static arc.Core.*;

/**
 * Subpixel Morphological Anti-Aliasing (SMAA 1x), post-process anti-aliasing.
 * Ported from https://github.com/iryoku/smaa (MIT), see assets/shaders/smaa-*.
 * <p>
 * Usage: call {@link #begin()} before rendering the scene and {@link #end()} afterwards.
 */
public class SMAA implements Disposable{
    private boolean initialized, failed, active;

    private FrameBuffer sceneBuffer, edgesBuffer, weightsBuffer;
    private Texture areaTex, searchTex;
    private Shader edgesShader, weightsShader, blendShader;

    /** @return whether SMAA should be applied this frame. */
    public boolean enabled(){
        return settings.getBool("smaa") && !failed;
    }

    private void init(){
        initialized = true;
        try{
            sceneBuffer = new FrameBuffer();
            edgesBuffer = new FrameBuffer();
            weightsBuffer = new FrameBuffer();

            //all three buffers are sampled with bilinear filtering by the shaders (the searches and the final blend rely on it)
            for(FrameBuffer buffer : new FrameBuffer[]{sceneBuffer, edgesBuffer, weightsBuffer}){
                buffer.texture.setFilter(TextureFilter.linear, TextureFilter.linear);
                buffer.texture.setWrap(TextureWrap.clampToEdge);
            }

            areaTex = new Texture(Shaders.getShaderFi("smaa-area.png"));
            areaTex.setFilter(TextureFilter.linear, TextureFilter.linear);
            areaTex.setWrap(TextureWrap.clampToEdge);

            searchTex = new Texture(Shaders.getShaderFi("smaa-search.png"));
            searchTex.setFilter(TextureFilter.linear, TextureFilter.linear);
            searchTex.setWrap(TextureWrap.clampToEdge);

            edgesShader = new SMAAShader("smaa-edges"){
                @Override
                public void apply(){
                    setUniformi("u_texture", 0);
                    applyMetrics(this, sceneBuffer);
                }
            };

            weightsShader = new SMAAShader("smaa-weights"){
                @Override
                public void apply(){
                    areaTex.bind(1);
                    searchTex.bind(2);
                    Gl.activeTexture(Gl.texture0);

                    setUniformi("u_texture", 0);
                    setUniformi("u_area", 1);
                    setUniformi("u_search", 2);
                    applyMetrics(this, edgesBuffer);
                }
            };

            blendShader = new SMAAShader("smaa-blend"){
                @Override
                public void apply(){
                    weightsBuffer.texture.bind(1);
                    Gl.activeTexture(Gl.texture0);

                    setUniformi("u_texture", 0);
                    setUniformi("u_blend", 1);
                    applyMetrics(this, sceneBuffer);
                }
            };
        }catch(Throwable t){
            failed = true;
            Log.err("Failed to initialize SMAA, disabling it.", t);
            dispose();
        }
    }

    private static void applyMetrics(Shader shader, FrameBuffer buffer){
        shader.setUniformf("u_metrics", 1f / buffer.width, 1f / buffer.height, buffer.width, buffer.height);
    }

    /** Starts rendering the scene into the SMAA input buffer. Must be paired with {@link #end()}. */
    public void begin(){
        if(failed) return;
        if(!initialized) init();
        if(failed) return;

        int w = graphics.getWidth(), h = graphics.getHeight();
        sceneBuffer.resize(w, h);
        edgesBuffer.resize(w, h);
        weightsBuffer.resize(w, h);

        active = true;
        sceneBuffer.begin(Color.clear);
    }

    /** Stops rendering the scene, runs all SMAA passes and draws the result to the previously bound framebuffer. */
    public void end(){
        if(!active) return;
        active = false;

        sceneBuffer.end();

        //every pass writes raw data, so blending must be off
        Blending.disabled.apply();

        //pass 1: edges. this pass discards pixels without edges, so the target has to be cleared.
        edgesBuffer.begin(Color.clear);
        sceneBuffer.blit(edgesShader);
        edgesBuffer.end();

        //pass 2: blending weights
        weightsBuffer.begin(Color.clear);
        edgesBuffer.blit(weightsShader);
        weightsBuffer.end();

        //pass 3: blend the scene using the weights, straight to the screen
        sceneBuffer.blit(blendShader);

        Blending.normal.apply();
    }

    @Override
    public void dispose(){
        if(sceneBuffer != null) sceneBuffer.dispose();
        if(edgesBuffer != null) edgesBuffer.dispose();
        if(weightsBuffer != null) weightsBuffer.dispose();
        if(areaTex != null) areaTex.dispose();
        if(searchTex != null) searchTex.dispose();
        if(edgesShader != null) edgesShader.dispose();
        if(weightsShader != null) weightsShader.dispose();
        if(blendShader != null) blendShader.dispose();
        sceneBuffer = edgesBuffer = weightsBuffer = null;
        areaTex = searchTex = null;
        edgesShader = weightsShader = blendShader = null;
        initialized = false;
    }

    /** Loads smaa-[name].vert and smaa-[name].frag from the shaders folder. */
    private static class SMAAShader extends Shader{
        SMAAShader(String name){
            super(Shaders.getShaderFi(name + ".vert"), Shaders.getShaderFi(name + ".frag"));
        }
    }
}
