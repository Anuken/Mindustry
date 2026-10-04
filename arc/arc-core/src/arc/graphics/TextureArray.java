package arc.graphics;

import arc.files.*;
import arc.graphics.gl.*;

/**
 * OpenGL ES wrapper for TextureArray
 * @author Tomski
 */
public class TextureArray extends GLTexture{
    public int depth;

    public TextureArray(){
        super(Gl.texture2dArray, Gl.genTexture());
    }

    public TextureArray(Pixmap[] pixmaps){
        this();

        load(pixmaps, false);
    }

    public TextureArray(Fi[] files, boolean useMipMaps){
        this();

        load(files, useMipMaps);
    }

    public TextureArray(int width, int height, int depth){
        this();
        init(width, height, depth);
    }

    void init(int width, int height, int depth){
        this.width = width;
        this.height = height;
        this.depth = depth;

        bind();
        Gl.texImage3D(glTarget, 0, Gl.rgba8, width, height, depth, 0,  Gl.rgba, Gl.unsignedByte, null);
        setFilter(minFilter, magFilter);
        setWrap(uWrap, vWrap);
    }

    public void load(Fi[] files, boolean useMipMaps){
        for(int i = 0; i < files.length; i++){
            Pixmap pix = new Pixmap(files[i]);
            if(width == 0) init(pix.width, pix.height, files.length);

            load(pix, i);
            pix.dispose();
        }

        if(useMipMaps) Gl.generateMipmap(glTarget);
    }

    public void load(Pixmap[] pixmaps, boolean useMipmaps){
        init(pixmaps[0].width, pixmaps[0].height, pixmaps.length);

        for(int i = 0; i < pixmaps.length; i++){
            Pixmap pixmap = pixmaps[i];
            Gl.texSubImage3D(glTarget, 0, 0, 0, i, pixmap.width, pixmap.height, 1, pixmap.getGLInternalFormat(), pixmap.getGLType(), pixmap.pixels);
        }

        if(useMipmaps) Gl.generateMipmap(glTarget);
    }

    public void load(Pixmap pixmap, int depth){
        bind();
        Gl.texSubImage3D(glTarget, 0, 0, 0, depth, pixmap.width, pixmap.height, 1, pixmap.getGLInternalFormat(), pixmap.getGLType(), pixmap.pixels);
    }

    /**
     * Resizes the texture array to the new depth, preserving existing layers via FBO blit.
     */
    public void resizeDepth(int newDepth){
        if(newDepth == depth) return;

        //this is extremely slow and requires a 2-pass solution to keep the handle the same.
        int newTex = Gl.genTexture();
        Gl.bindTexture(Gl.texture2dArray, newTex);
        Gl.texImage3D(Gl.texture2dArray, 0, Gl.rgba8, width, height, depth, 0, Gl.rgba, Gl.unsignedByte, null);

        int readFbo = Gl.genFramebuffer(), drawFbo = Gl.genFramebuffer();
        int layersToCopy = Math.min(depth, newDepth);

        for(int layer = 0; layer < layersToCopy; layer++){
            Gl.bindFramebuffer(Gl.readFramebuffer, readFbo);
            Gl.framebufferTextureLayer(Gl.readFramebuffer, Gl.colorAttachment0, glHandle, 0, layer);

            Gl.bindFramebuffer(Gl.drawFramebuffer, drawFbo);
            Gl.framebufferTextureLayer(Gl.drawFramebuffer, Gl.colorAttachment0, newTex, 0, layer);

            Gl.blitFramebuffer(
            0, 0, width, height,
            0, 0, width, height,
            Gl.colorBufferBit, Gl.nearest);
        }

        Gl.bindTexture(Gl.texture2dArray, glHandle);
        Gl.texImage3D(Gl.texture2dArray, 0, Gl.rgba8, width, height, newDepth, 0, Gl.rgba, Gl.unsignedByte, null);

        for(int layer = 0; layer < layersToCopy; layer++){
            Gl.bindFramebuffer(Gl.readFramebuffer, readFbo);
            Gl.framebufferTextureLayer(Gl.readFramebuffer, Gl.colorAttachment0, newTex, 0, layer);

            Gl.bindFramebuffer(Gl.drawFramebuffer, drawFbo);
            Gl.framebufferTextureLayer(Gl.drawFramebuffer, Gl.colorAttachment0, glHandle, 0, layer);

            Gl.blitFramebuffer(
            0, 0, width, height,
            0, 0, width, height,
            Gl.colorBufferBit, Gl.nearest);
        }

        Gl.deleteFramebuffer(readFbo);
        Gl.deleteFramebuffer(drawFbo);
        Gl.deleteTexture(newTex);

        depth = newDepth;

        bind();
        setFilter(minFilter, magFilter);
        setWrap(uWrap, vWrap);
    }

    @Override
    public int getDepth(){
        return depth;
    }

}
