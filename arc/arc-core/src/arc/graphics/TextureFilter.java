package arc.graphics;

import arc.graphics.gl.*;

public enum TextureFilter{
    /** Fetch the nearest texel that best maps to the pixel on screen. */
    nearest(Gl.nearest),

    /** Fetch four nearest texels that best maps to the pixel on screen. */
    linear(Gl.linear),

    /** @see TextureFilter#mipMapLinearLinear */
    mipMap(Gl.linearMipmapLinear),

    /**
     * Fetch the best fitting image from the mip map chain based on the pixel/texel ratio and then sample the texels with a
     * nearest filter.
     */
    mipMapNearestNearest(Gl.nearestMipmapNearest),

    /**
     * Fetch the best fitting image from the mip map chain based on the pixel/texel ratio and then sample the texels with a
     * Linear filter.
     */
    mipMapLinearNearest(Gl.linearMipmapNearest),

    /**
     * Fetch the two best fitting images from the mip map chain and then sample the nearest texel from each of the two images,
     * combining them to the final output pixel.
     */
    mipMapNearestLinear(Gl.nearestMipmapLinear),

    /**
     * Fetch the two best fitting images from the mip map chain and then sample the four nearest texels from each of the two
     * images, combining them to the final output pixel.
     */
    mipMapLinearLinear(Gl.linearMipmapLinear);

    public static final TextureFilter[] all = values();

    public final int glEnum;

    TextureFilter(int glEnum){
        this.glEnum = glEnum;
    }

    public boolean isMipMap(){
        return glEnum != Gl.nearest && glEnum != Gl.linear;
    }
}
