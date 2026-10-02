package arc.graphics;

import arc.*;
import arc.files.*;
import arc.graphics.Pixmap.*;
import arc.graphics.g2d.*;
import arc.graphics.gl.*;
import arc.math.*;
import arc.util.*;

import java.nio.*;

/**
 * Class with static helper methods that provide access to the default OpenGL FrameBuffer. These methods can be used to get the
 * entire screen content or a portion thereof.
 * @author espitz
 */
public final class ScreenUtils{

    public static void saveScreenshot(Fi file){
        saveScreenshot(file, 0, 0, Core.graphics.getWidth(), Core.graphics.getHeight());
    }

    public static void saveScreenshot(Fi file, int x, int y, int width, int height){
        Pixmap pixmap = getFrameBufferPixmap(x, y, width, height, true);
        PixmapIO.writePng(file, pixmap);
        pixmap.dispose();
    }

    /**
     * Returns the default framebuffer contents as a {@link TextureRegion} with a width and height equal to the current screen
     * size. The base {@link Texture} always has {@link Mathf#nextPowerOfTwo} dimensions and RGBA8888 {@link Format}.
     * The returned TextureRegion is flipped along the Y axis by default.
     */
    public static TextureRegion getFrameBufferTexture(){
        final int w = Core.graphics.getBackBufferWidth();
        final int h = Core.graphics.getBackBufferHeight();
        return getFrameBufferTexture(0, 0, w, h);
    }

    /**
     * Returns a portion of the default framebuffer contents specified by x, y, width and height as a {@link TextureRegion} with
     * the same dimensions. The base {@link Texture} always has {@link Mathf#nextPowerOfTwo} dimensions and RGBA8888 {@link Format}.
     * manually on a context loss. If the width and height specified are larger than the framebuffer dimensions, the Texture will
     * be padded accordingly. Pixels that fall outside of the current screen will have RGBA values of 0.
     * @param x the x position of the framebuffer contents to capture
     * @param y the y position of the framebuffer contents to capture
     * @param w the width of the framebuffer contents to capture
     * @param h the height of the framebuffer contents to capture
     */
    public static TextureRegion getFrameBufferTexture(int x, int y, int w, int h){
        final int potW = Mathf.nextPowerOfTwo(w);
        final int potH = Mathf.nextPowerOfTwo(h);

        final Pixmap pixmap = getFrameBufferPixmap(x, y, w, h);
        final Pixmap potPixmap = new Pixmap(potW, potH);
        potPixmap.draw(pixmap, 0, 0);
        Texture texture = new Texture(potPixmap);
        TextureRegion textureRegion = new TextureRegion(texture, 0, h, w, -h);
        potPixmap.dispose();
        pixmap.dispose();

        return textureRegion;
    }

    public static Pixmap getFrameBufferPixmap(int x, int y, int w, int h){
        Gl.pixelStorei(Gl.packAlignment, 1);

        final Pixmap pixmap = new Pixmap(w, h);
        ByteBuffer pixels = pixmap.pixels;
        Gl.readPixels(x, y, w, h, Gl.rgba, Gl.unsignedByte, pixels);

        return pixmap;
    }

    public static Pixmap getFrameBufferPixmap(int x, int y, int w, int h, boolean flip){
        byte[] lines = getFrameBufferPixels(x, y, w, h, flip);
        Pixmap pixmap = new Pixmap(w, h);
        Buffers.copy(lines, 0, pixmap.pixels, lines.length);
        return pixmap;
    }

    /**
     * Returns the default framebuffer contents as a byte[] array with a length equal to screen width * height * 4. The byte[] will
     * always contain RGBA8888 data. Because of differences in screen and image origins the framebuffer contents should be flipped
     * along the Y axis if you intend save them to disk as a bitmap. Flipping is not a cheap operation, so use this functionality
     * wisely.
     * @param flipY whether to flip pixels along Y axis
     */
    public static byte[] getFrameBufferPixels(boolean flipY){
        final int w = Core.graphics.getBackBufferWidth();
        final int h = Core.graphics.getBackBufferHeight();
        return getFrameBufferPixels(0, 0, w, h, flipY);
    }

    /**
     * Returns a portion of the default framebuffer contents specified by x, y, width and height, as a byte[] array with a length
     * equal to the specified width * height * 4. The byte[] will always contain RGBA8888 data. If the width and height specified
     * are larger than the framebuffer dimensions, the Texture will be padded accordingly. Pixels that fall outside of the current
     * screen will have RGBA values of 0. Because of differences in screen and image origins the framebuffer contents should be
     * flipped along the Y axis if you intend save them to disk as a bitmap. Flipping is not a cheap operation, so use this
     * functionality wisely.
     * @param flipY whether to flip pixels along Y axis
     */
    public static byte[] getFrameBufferPixels(int x, int y, int w, int h, boolean flipY){
        Gl.pixelStorei(Gl.packAlignment, 1);
        final ByteBuffer pixels = Buffers.newByteBuffer(w * h * 4);
        Gl.readPixels(x, y, w, h, Gl.rgba, Gl.unsignedByte, pixels);
        final int numBytes = w * h * 4;
        byte[] lines = new byte[numBytes];
        if(flipY){
            final int numBytesPerLine = w * 4;
            for(int i = 0; i < h; i++){
                pixels.position((h - i - 1) * numBytesPerLine);
                pixels.get(lines, i * numBytesPerLine, numBytesPerLine);
            }
        }else{
            pixels.clear();
            pixels.get(lines);
        }
        return lines;

    }
}
