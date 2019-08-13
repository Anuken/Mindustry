package io.anuke.mindustry.graphics;

import io.anuke.arc.Core;
import io.anuke.arc.graphics.*;
import io.anuke.arc.graphics.Pixmap.Format;
import io.anuke.arc.graphics.VertexAttributes.Usage;
import io.anuke.arc.graphics.glutils.FrameBuffer;
import io.anuke.arc.graphics.glutils.Shader;

/**
 * Bloomlib allow easy but efficient way to add bloom effect as post process
 * effect
 *
 * @author kalle_h
 */
public class Bloom{

    /**
     * To use implement bloom more like a glow. Texture alpha channel can be
     * used as mask which part are glowing and which are not. see more info at:
     * http://www.gamasutra.com/view/feature/2107/realtime_glow.php
     * <p>
     * NOTE: need to be set before bloom instance is created. After that this
     * does nothing.
     */
    public static boolean useAlphaChannelAsMask = false;

    /** how many blur pass */
    public int blurPasses = 1;

    private Shader tresholdShader;
    private Shader bloomShader;

    private Mesh fullScreenQuad;

    private Texture pingPongTex1;
    private Texture pingPongTex2;
    private Texture original;

    private FrameBuffer frameBuffer;
    private FrameBuffer pingPongBuffer1;
    private FrameBuffer pingPongBuffer2;

    private Shader blurShader;

    private float bloomIntensity;
    private float originalIntensity;
    private float threshold;
    private int w;
    private int h;
    private boolean blending = false;
    private boolean capturing = false;
    private float r = 0f;
    private float g = 0f;
    private float b = 0f;
    private float a = 1f;
    private boolean disposeFBO = true;

    /**
     * IMPORTANT NOTE CALL THIS WHEN RESUMING
     */
    public void resume(){
        bloomShader.begin();
        {
            bloomShader.setUniformi("u_texture0", 0);
            bloomShader.setUniformi("u_texture1", 1);
        }
        bloomShader.end();

        setSize(w, h);
        setThreshold(threshold);
        setBloomIntesity(bloomIntensity);
        setOriginalIntesity(originalIntensity);

        original = frameBuffer.getTexture();
        pingPongTex1 = pingPongBuffer1.getTexture();
        pingPongTex2 = pingPongBuffer2.getTexture();
    }

    /**
     * Initialize bloom class that capsulate original scene capturate,
     * tresholding, gaussian blurring and blending. Default values: depth = true
     * blending = false 32bits = true
     */
    public Bloom(){
        initialize(Core.graphics.getWidth() / 4, Core.graphics.getHeight() / 4,
                null, false, false, true);
    }

    public Bloom(boolean useBlending){
        initialize(Core.graphics.getWidth() / 4, Core.graphics.getHeight() / 4,
        null, false, useBlending, true);
    }

    /**
     * Initialize bloom class that capsulate original scene capturate,
     * tresholding, gaussian blurring and blending.
     *
     * @param FBO_W
     * @param FBO_H how big fbo is used for bloom texture, smaller = more blur and
     * lot faster but aliasing can be problem
     * @param hasDepth do rendering need depth buffer
     * @param useBlending does fbo need alpha channel and is blending enabled when final
     * image is rendered. This allow to combine background graphics
     * and only do blooming on certain objects param use32bitFBO does
     * fbo use higher precision than 16bits.
     */
    public Bloom(int FBO_W, int FBO_H, boolean hasDepth, boolean useBlending,
                 boolean use32bitFBO){
        initialize(FBO_W, FBO_H, null, hasDepth, useBlending, use32bitFBO);

    }

    /**
     * EXPERT FUNCTIONALITY. no error checking. Use this only if you know what
     * you are doing. Remember that bloom.capture() clear the screen so use
     * continue instead if that is a problem.
     * <p>
     * Initialize bloom class that capsulate original scene capturate,
     * tresholding, gaussian blurring and blending.
     * <p>
     * * @param sceneIsCapturedHere diposing is user responsibility.
     *
     * @param FBO_W
     * @param FBO_H how big fbo is used for bloom texture, smaller = more blur and
     * lot faster but aliasing can be problem
     * @param useBlending does fbo need alpha channel and is blending enabled when final
     * image is rendered. This allow to combine background graphics
     * and only do blooming on certain objects param use32bitFBO does
     * fbo use higher precision than 16bits.
     */
    public Bloom(int FBO_W, int FBO_H, FrameBuffer sceneIsCapturedHere,
                 boolean useBlending, boolean use32bitFBO){

        initialize(FBO_W, FBO_H, sceneIsCapturedHere, false, useBlending,
                use32bitFBO);
        disposeFBO = false;
    }

    private void initialize(int FBO_W, int FBO_H, FrameBuffer fbo,
                            boolean hasDepth, boolean useBlending, boolean use32bitFBO){
        blending = useBlending;
        Format format = null;

        if(use32bitFBO){
            if(useBlending){
                format = Format.RGBA8888;
            }else{
                format = Format.RGB888;
            }

        }else{
            if(useBlending){
                format = Format.RGBA4444;
            }else{
                format = Format.RGB565;
            }
        }
        if(fbo == null){
            frameBuffer = new FrameBuffer(format, Core.graphics.getWidth(),
                    Core.graphics.getHeight(), hasDepth);
        }else{
            frameBuffer = fbo;
        }

        pingPongBuffer1 = new FrameBuffer(format, FBO_W, FBO_H, false);

        pingPongBuffer2 = new FrameBuffer(format, FBO_W, FBO_H, false);

        original = frameBuffer.getTexture();
        pingPongTex1 = pingPongBuffer1.getTexture();
        pingPongTex2 = pingPongBuffer2.getTexture();

        fullScreenQuad = createFullScreenQuad();
        final String alpha = useBlending ? "alpha_" : "";

        bloomShader = createShader("screenspace", alpha + "bloom");

        if(useAlphaChannelAsMask){
            tresholdShader = createShader("screenspace", "maskedtreshold");
        }else{
            tresholdShader = createShader("screenspace", alpha + "threshold");
        }

        blurShader = createShader("blurspace", alpha + "gaussian");

        setSize(FBO_W, FBO_H);
        setBloomIntesity(2.5f);
        setOriginalIntesity(0.8f);
        setThreshold(0.5f);

        bloomShader.begin();
        {
            bloomShader.setUniformi("u_texture0", 0);
            bloomShader.setUniformi("u_texture1", 1);
        }
        bloomShader.end();
    }

    /**
     * Set clearing color for capturing buffer
     *
     * @param r
     * @param g
     * @param b
     * @param a
     */
    public void setClearColor(float r, float g, float b, float a){
        this.r = r;
        this.g = g;
        this.b = b;
        this.a = a;
    }

    /**
     * Call this before rendering scene.
     */
    public void capture(){
        if(!capturing){
            capturing = true;
            frameBuffer.begin();
            Core.gl.glClearColor(r, g, b, a);
            Core.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        }
    }

    /**
     * Pause capturing to fbo.
     */
    public void capturePause(){
        if(capturing){
            capturing = false;
            frameBuffer.end();
        }
    }

    /** Start capturing again after pause, no clearing is done to framebuffer */
    public void captureContinue(){
        if(!capturing){
            capturing = true;
            frameBuffer.begin();
        }
    }

    /**
     * Call this after scene. Renders the bloomed scene.
     */
    public void render(){
        if(capturing){
            capturing = false;
            frameBuffer.end();
        }

        Core.gl.glDisable(GL20.GL_BLEND);
        Core.gl.glDisable(GL20.GL_DEPTH_TEST);
        Core.gl.glDepthMask(false);

        gaussianBlur();

        if(blending){
            Core.gl.glEnable(GL20.GL_BLEND);
            Core.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        }

        pingPongTex1.bind(1);
        original.bind(0);
        bloomShader.begin();
        {
            fullScreenQuad.render(bloomShader, GL20.GL_TRIANGLE_FAN);
        }
        bloomShader.end();

    }

    private void gaussianBlur(){

        // cut bright areas of the picture and blit to smaller fbo

        original.bind(0);
        pingPongBuffer1.begin();
        {
            tresholdShader.begin();
            {
                // tresholdShader.setUniformi("u_texture0", 0);
                fullScreenQuad.render(tresholdShader, GL20.GL_TRIANGLE_FAN, 0,
                        4);
            }
            tresholdShader.end();
        }
        pingPongBuffer1.end();

        for(int i = 0; i < blurPasses; i++){

            pingPongTex1.bind(0);

            // horizontal
            pingPongBuffer2.begin();
            {
                blurShader.begin();
                {
                    blurShader.setUniformf("dir", 1f, 0f);
                    fullScreenQuad.render(blurShader, GL20.GL_TRIANGLE_FAN, 0,
                            4);
                }
                blurShader.end();
            }
            pingPongBuffer2.end();

            pingPongTex2.bind(0);
            // vertical
            pingPongBuffer1.begin();
            {
                blurShader.begin();
                {
                    blurShader.setUniformf("dir", 0f, 1f);

                    fullScreenQuad.render(blurShader, GL20.GL_TRIANGLE_FAN, 0,
                            4);
                }
                blurShader.end();
            }
            pingPongBuffer1.end();
        }
    }

    /**
     * set intensity for bloom. higher mean more brightening for spots that are
     * over threshold
     *
     * @param intensity multiplier for blurred texture in combining phase. must be
     * positive.
     */
    public void setBloomIntesity(float intensity){
        bloomIntensity = intensity;
        bloomShader.begin();
        {
            bloomShader.setUniformf("BloomIntensity", intensity);
        }
        bloomShader.end();
    }

    /**
     * set intensity for original scene. under 1 mean darkening and over 1 means
     * lightening
     *
     * @param intensity multiplier for captured texture in combining phase. must be
     * positive.
     */
    public void setOriginalIntesity(float intensity){
        originalIntensity = intensity;
        bloomShader.begin();
        {
            bloomShader.setUniformf("OriginalIntensity", intensity);
        }
        bloomShader.end();
    }

    /**
     * Treshold for bright parts. everything under threshold is clamped to 0
     *
     * @param threshold must be in range 0..1
     */
    public void setThreshold(float threshold){
        this.threshold = threshold;
        tresholdShader.begin();
        {
            tresholdShader.setUniformf("threshold", threshold,
                    1f / (1 - threshold));
        }
        tresholdShader.end();
    }

    private void setSize(int FBO_W, int FBO_H){
        w = FBO_W;
        h = FBO_H;
        blurShader.begin();
        blurShader.setUniformf("size", FBO_W, FBO_H);
        blurShader.end();
    }

    /**
     * Call this when application is exiting.
     */
    public void dispose(){
        try{
            if(disposeFBO)
                frameBuffer.dispose();

            fullScreenQuad.dispose();

            pingPongBuffer1.dispose();
            pingPongBuffer2.dispose();

            blurShader.dispose();
            bloomShader.dispose();
            tresholdShader.dispose();
        }catch(Exception ignored){

        }
    }

    private static Mesh createFullScreenQuad(){
        float[] verts = {-1, -1, 0, 0, 1, -1, 1, 0, 1, 1, 1, 1, -1, 1, 0, 1};
        Mesh tmpMesh = new Mesh(true, 4, 0, new VertexAttribute(
                Usage.Position, 2, "a_position"), new VertexAttribute(
                Usage.TextureCoordinates, 2, "a_texCoord0"));

        tmpMesh.setVertices(verts);
        return tmpMesh;

    }

    private static Shader createShader(String vertexName, String fragmentName){
        String vertexShader = Core.files.internal("bloomshaders/" + vertexName + ".vertex.glsl").readString();
        String fragmentShader = Core.files.internal("bloomshaders/" + fragmentName + ".fragment.glsl").readString();
        return new Shader(vertexShader, fragmentShader);
    }

}
