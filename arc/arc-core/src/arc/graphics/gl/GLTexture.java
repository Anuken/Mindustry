package arc.graphics.gl;

import arc.graphics.*;
import arc.util.*;

/**
 * Class representing an OpenGL texture by its target and handle. Keeps track of its state like the TextureFilter and TextureWrap.
 * Also provides some (protected) static methods to create TextureData and upload image data.
 * @author badlogic, Xoppa
 */
public abstract class GLTexture implements Disposable{
    /** The target of this texture, used when binding the texture, e.g. GL_TEXTURE_2D */
    public final int glTarget;
    /** Do not change. This is read-only and only set after texture data is loaded. */
    public int width, height;

    protected int glHandle;
    protected TextureFilter minFilter = TextureFilter.nearest;
    protected TextureFilter magFilter = TextureFilter.nearest;
    protected TextureWrap uWrap = TextureWrap.clampToEdge;
    protected TextureWrap vWrap = TextureWrap.clampToEdge;

    /** Generates a new OpenGL texture with the specified target. */
    public GLTexture(int glTarget){
        this(glTarget, Gl.genTexture());
    }

    public GLTexture(int glTarget, int glHandle){
        this.glTarget = glTarget;
        this.glHandle = glHandle;
    }

    /** @return the depth of the texture in pixels */
    public int getDepth(){
        return 0;
    }

    /** Binds this texture. The texture will be bound to the currently active texture unit specified. */
    public void bind(){
        Gl.bindTexture(glTarget, glHandle);
    }

    /**
     * Binds the texture to the given texture unit. Sets the currently active texture unit.
     * @param unit the unit (0 to MAX_TEXTURE_UNITS).
     */
    public void bind(int unit){
        Gl.activeTexture(Gl.texture0 + unit);
        Gl.bindTexture(glTarget, glHandle);
    }

    /** @return The {@link TextureFilter} used for minification. */
    public TextureFilter getMinFilter(){
        return minFilter;
    }

    /** @return The {@link TextureFilter} used for magnification. */
    public TextureFilter getMagFilter(){
        return magFilter;
    }

    /** @return The {@link TextureWrap} used for horizontal (U) texture coordinates. */
    public TextureWrap getUWrap(){
        return uWrap;
    }

    /** @return The {@link TextureWrap} used for vertical (V) texture coordinates. */
    public TextureWrap getVWrap(){
        return vWrap;
    }

    /** @return The OpenGL handle for this texture. */
    public int getHandle(){
        return glHandle;
    }

    /** advanced usage only */
    public void overwriteHandle(int handle){
        this.glHandle = handle;
    }

    public void setWrap(TextureWrap wrap){
        setWrap(wrap, wrap);
    }

    /**
     * Sets the {@link TextureWrap} for this texture on the u and v axis. This will bind this texture!
     * @param u the u wrap
     * @param v the v wrap
     */
    public void setWrap(TextureWrap u, TextureWrap v){
        bind();
        Gl.texParameteri(glTarget, Gl.textureWrapS, (this.uWrap = u).getGLEnum());
        Gl.texParameteri(glTarget, Gl.textureWrapT, (this.vWrap = v).getGLEnum());
    }

    public void setFilter(TextureFilter filter){
        setFilter(filter, filter);
    }

    /**
     * Sets the {@link TextureFilter} for this texture for minification and magnification. This will bind this texture!
     * @param minFilter the minification filter
     * @param magFilter the magnification filter
     */
    public void setFilter(TextureFilter minFilter, TextureFilter magFilter){
        bind();
        Gl.texParameteri(glTarget, Gl.textureMinFilter, (this.minFilter = minFilter).glEnum);
        Gl.texParameteri(glTarget, Gl.textureMagFilter, (this.magFilter = magFilter).glEnum);
    }

    @Override
    public boolean isDisposed(){
        return glHandle == 0;
    }

    @Override
    public void dispose(){
        if(glHandle != 0){
            Gl.deleteTexture(glHandle);
            glHandle = 0;
        }
    }
}
