package arc.graphics;

import arc.*;
import arc.graphics.gl.*;

/**
 * To deal with HDPI monitors properly, use the glViewport and glScissor functions of this class instead of directly calling
 * OpenGL yourself. The logical coordinate system provided by the operating system may not have the same resolution as the actual
 * drawing surface to which OpenGL draws, also known as the backbuffer. This class will ensure, that you pass the correct values
 * to OpenGL for any function that expects backbuffer coordinates instead of logical coordinates.
 * @author badlogic
 */
public class HdpiUtils{
    private static HdpiMode mode = HdpiMode.logical;

    /**
     * Allows applications to override HDPI coordinate conversion for glViewport and glScissor calls.
     * <p>
     * This function can be used to ignore the default behavior, for example when rendering a UI stage
     * to an off-screen framebuffer:
     *
     * <pre>
     * HdpiUtils.setMode(HdpiMode.Pixels);
     * fb.begin();
     * stage.draw();
     * fb.end();
     * HdpiUtils.setMode(HdpiMode.Logical);
     * </pre>
     * @param mode set to HdpiMode.Pixels to ignore HDPI conversion for glViewport and glScissor functions
     */
    public static void setMode(HdpiMode mode){
        HdpiUtils.mode = mode;
    }

    /**
     * Calls {@link Gl#scissor(int, int, int, int)}, expecting the coordinates and sizes given in logical coordinates and
     * automatically converts them to backbuffer coordinates, which may be bigger on HDPI screens.
     */
    public static void glScissor(int x, int y, int width, int height){
        if(mode == HdpiMode.logical && (Core.graphics.getWidth() != Core.graphics.getBackBufferWidth()
        || Core.graphics.getHeight() != Core.graphics.getBackBufferHeight())){
            Gl.scissor(toBackBufferX(x), toBackBufferY(y), toBackBufferX(width), toBackBufferY(height));
        }else{
            Gl.scissor(x, y, width, height);
        }
    }

    /**
     * Calls {@link Gl#viewport(int, int, int, int)}, expecting the coordinates and sizes given in logical coordinates and
     * automatically converts them to backbuffer coordinates, which may be bigger on HDPI screens.
     */
    public static void glViewport(int x, int y, int width, int height){
        if(mode == HdpiMode.logical && (Core.graphics.getWidth() != Core.graphics.getBackBufferWidth()
        || Core.graphics.getHeight() != Core.graphics.getBackBufferHeight())){
            Gl.viewport(toBackBufferX(x), toBackBufferY(y), toBackBufferX(width), toBackBufferY(height));
        }else{
            Gl.viewport(x, y, width, height);
        }
    }

    /**
     * Converts an x-coordinate given in backbuffer coordinates to
     * logical screen coordinates.
     */
    public static int toLogicalX(int backBufferX){
        return (int)(backBufferX * Core.graphics.getWidth() / (float)Core.graphics.getBackBufferWidth());
    }

    /**
     * Convers an y-coordinate given in backbuffer coordinates to
     * logical screen coordinates
     */
    public static int toLogicalY(int backBufferY){
        return (int)(backBufferY * Core.graphics.getHeight() / (float)Core.graphics.getBackBufferHeight());
    }

    /**
     * Converts an x-coordinate given in logical screen coordinates to
     * backbuffer coordinates.
     */
    public static int toBackBufferX(int logicalX){
        return (int)(logicalX * Core.graphics.getBackBufferWidth() / (float)Core.graphics.getWidth());
    }

    /**
     * Convers an y-coordinate given in backbuffer coordinates to
     * logical screen coordinates
     */
    public static int toBackBufferY(int logicalY){
        return (int)(logicalY * Core.graphics.getBackBufferHeight() / (float)Core.graphics.getHeight());
    }

    public enum HdpiMode{
        /**
         * mouse coordinates, {@link Graphics#getWidth()} and
         * {@link Graphics#getHeight()} will return logical coordinates
         * according to the system defined HDPI scaling. Rendering will be
         * performed to a backbuffer at raw resolution. Use {@link HdpiUtils}
         * when calling {@link Gl#scissor} or {@link Gl#viewport} which
         * expect raw coordinates.
         */
        logical,

        /**
         * Mouse coordinates, {@link Graphics#getWidth()} and
         * {@link Graphics#getHeight()} will return raw pixel coordinates
         * irrespective of the system defined HDPI scaling.
         */
        pixels
    }
}
