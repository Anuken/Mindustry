package arc;

import arc.Graphics.Cursor.*;
import arc.graphics.*;
import arc.graphics.gl.*;
import arc.graphics.gl.GLVersion.*;
import arc.math.*;
import arc.util.*;

/**
 * This interface encapsulates communication with the graphics processor. Depending on the available hardware and the current
 * {@link Application} configuration, access to {@link GLProvider} are provided here.
 * <p>
 * If supported by the backend, this interface lets you query the available display modes (graphics resolution and color depth)
 * and change it.
 * <p>
 * This interface can be used to switch between continuous and non-continuous rendering (see
 * {@link #setContinuousRendering(boolean)}), and to explicitly {@link #requestRendering()}.
 * <p>
 * @author mzechner
 */
public abstract class Graphics implements Disposable{
    /** The last cursor used. Can be Cursor or SystemCursor.*/
    private Object lastCursor;

    /** @return whether instancing with glVertexAttribDivisor and glDrawElementsInstanced is available on this platform. */
    public boolean supportsInstancing(){
        //instancing via glDrawElementsInstanced + glVertexAttribDivisor is available on GL ES 3.0; otherwise, only >= Gl 3.3 has glVertexAttribDivisor on desktop
        return getGLVersion().type == GlType.GLES || getGLVersion().atLeast(3, 3);
    }

    /** Clears the color buffer using the specified color. */
    public void clear(float r, float g, float b, float a){
        Gl.clearColor(r, g, b, a);
        Gl.clear(Gl.colorBufferBit);
    }

    /** Clears the color buffer using the specified color. */
    public void clear(Color color){
        clear(color.r, color.g, color.b, color.a);
    }

    /** Returns whether this application is probably in portrait mode, e.g. if width < height.*/
    public boolean isPortrait(){
        return getWidth() < getHeight();
    }

    /** @return the width of the client area in logical pixels. */
    public abstract int getWidth();

    /** @return the height of the client area in logical pixels */
    public abstract int getHeight();

    /** @return the aspect ratio, e.g. width/height */
    public float getAspect(){
        return (float)getWidth() / getHeight();
    }

    /** @return whether the window is 'hidden', e.g. whether width or height of the window is less than 2.
     * This is due to Windows reporting window size as 0 or 1 when minimized. This causes framebuffers to crash if resized.
     */
    public boolean isHidden(){
        return getWidth() < 2 || getHeight() < 2;
    }

    /** @return the width of the framebuffer in physical pixels */
    public abstract int getBackBufferWidth();

    /** @return the height of the framebuffer in physical pixels */
    public abstract int getBackBufferHeight();

    /** @return the safe area insets, in the order left-right-top-bottom. */
    public int[] getSafeInsets(){
        return new int[4];
    }

    /**
     * Returns the id of the current frame. The general contract of this method is that the id is incremented only when the
     * application is in the running state right before calling the {@link ApplicationListener#update()} method. Also, the id of
     * the first frame is 0; the id of subsequent frames is guaranteed to take increasing values for 2<sup>63</sup>-1 rendering
     * cycles.
     * @return the id of the current frame
     */
    public abstract long getFrameId();

    /** @return the time span between the current frame and the last frame in seconds. Might be smoothed over n frames. */
    public abstract float getDeltaTime();

    /** @return the average number of frames per second */
    public abstract int getFramesPerSecond();

    /** @return the {@link GLVersion} of this Graphics instance */
    public abstract GLVersion getGLVersion();

    /** @return the pixels per inch on the x-axis */
    public abstract float getPpiX();

    /** @return the pixels per inch on the y-axis */
    public abstract float getPpiY();

    /** @return the pixels per centimeter on the x-axis */
    public abstract float getPpcX();

    /** @return the pixels per centimeter on the y-axis. */
    public abstract float getPpcY();

    /**
     * This is a scaling factor for the Density Independent Pixel unit, following the same conventions as
     * android.util.DisplayMetrics#density, where one DIP is one pixel on an approximately 160 dpi screen. Thus on a 160dpi screen
     * this density value will be 1; on a 120 dpi screen it would be .75; etc.
     * @return the logical density of the Display.
     */
    public abstract float getDensity();

    /**
     * Sets the window to full-screen mode.
     * @return whether the operation succeeded.
     */
    public boolean setFullscreen(boolean fullscreen){
        return false;
    }

    /**
     * Sets the window position on its current monitor.
     */
    public void setWindowPosition(int x, int y){}


    /**
     * Sets the window size in pixels.
     */
    public void setWindowSize(int width, int height){}

    /**
     * Sets the title of the window. Ignored on Android.
     * @param title the title.
     */
    public abstract void setTitle(String title);

    /**
     * Enable/Disable vsynching. This is a best-effort attempt which might not work on all platforms.
     * @param vsync vsync enabled or not.
     */
    public abstract void setVSync(boolean vsync);

    /** @return the format of the color, depth and stencil buffer in a {@link BufferFormat} instance */
    public abstract BufferFormat getBufferFormat();

    /**
     * @param extension the extension name
     * @return whether the extension is supported
     */
    public abstract boolean supportsExtension(String extension);

    /** On iOS, sets the preferred frame rate. A value of 0 sets the maximum supported value for this device. */
    public void setPreferredFPS(int fps){}

    /** @return whether rendering is continuous. */
    public abstract boolean isContinuousRendering();

    /**
     * Sets whether to render continuously. In case rendering is performed non-continuously, the following events will trigger a
     * redraw:
     *
     * <ul>
     * <li>A call to {@link #requestRendering()}</li>
     * <li>Input events from the touch screen/mouse or keyboard</li>
     * <li>A {@link Runnable} is posted to the rendering thread via {@link Application#post(Runnable)}. In the case
     * of a multi-window app, all windows will request rendering if a runnable is posted to the application. To avoid this,
     * post a runnable to the window instead. </li>
     * </ul>
     * <p>
     * Life-cycle events will also be reported as usual, see {@link ApplicationListener}. This method can be called from any
     * thread.
     * @param isContinuous whether the rendering should be continuous or not.
     */
    public abstract void setContinuousRendering(boolean isContinuous);

    /** Requests a new frame to be rendered if the rendering mode is non-continuous. This method can be called from any thread. */
    public abstract void requestRendering();

    /** Whether the app is fullscreen or not */
    public abstract boolean isFullscreen();

    /**
     * Create a new cursor represented by the {@link arc.graphics.Pixmap}. The Pixmap must be in RGBA8888 format,
     * width & height must be powers-of-two greater than zero (not necessarily equal) and of a certain minimum size (32x32 is a safe bet),
     * and alpha transparency must be single-bit (i.e., 0x00 or 0xFF only). This function returns a Cursor object that can be set as the
     * system cursor by calling {@link #setCursor(Cursor)} .
     * @param pixmap the mouse cursor image as a {@link arc.graphics.Pixmap}
     * @param xHotspot the x location of the hotspot pixel within the cursor image (origin top-left corner)
     * @param yHotspot the y location of the hotspot pixel within the cursor image (origin top-left corner)
     * @return a cursor object that can be used by calling {@link #setCursor(Cursor)} or null if not supported
     */
    public abstract Cursor newCursor(Pixmap pixmap, int xHotspot, int yHotspot);

    /**
     * Creates a new cursor by scaling a pixmap and adding an outline.
     * @param pixmap The base pixmap. Unscaled.
     * @param scaling The factor by which to scale the base pixmap.
     * @param outlineColor The color of the cursor's outline.
     */
    public Cursor newCursor(Pixmap pixmap, int scaling, Color outlineColor, int outlineThickness){
        Pixmap out = Pixmaps.outline(pixmap, outlineColor, outlineThickness);
        Pixmap out2 = Pixmaps.scale(out, scaling);

        if(!Mathf.isPowerOfTwo(out2.width)){
            Pixmap old = out2;
            out2 = Pixmaps.resize(out2, Mathf.nextPowerOfTwo(out2.width), Mathf.nextPowerOfTwo(out2.width));
            old.dispose();
        }

        out.dispose();
        pixmap.dispose();

        Cursor cur = newCursor(out2, out2.width / 2, out2.height / 2);
        out2.dispose();
        return cur;
    }

    /**
     * Creates a new cursor by file name.
     * @param filename the name of the cursor .png file, found in the internal file "cursors/{name}.png"
     */
    public Cursor newCursor(String filename, int scale){
        if(scale == 1 || OS.isAndroid || OS.isIos) return newCursor(filename);
        Pixmap base = new Pixmap(Core.files.internal("cursors/" + filename + ".png"));
        Pixmap result = Pixmaps.scale(base, base.width * scale, base.height * scale);
        base.dispose();
        Cursor cur = newCursor(result, result.width /2, result.height /2);
        result.dispose();
        return cur;
    }

    /**
     * Creates a new cursor by file name.
     * @param filename the name of the cursor .png file, found in the internal file "cursors/{name}.png"
     */
    public Cursor newCursor(String filename){
        Pixmap p = new Pixmap(Core.files.internal("cursors/" + filename + ".png"));
        Cursor result = newCursor(p, p.width /2, p.height /2);
        p.dispose();
        return result;
    }

    /**
     * Creates a new cursor by file name.
     * @param filename the name of the cursor .png file, found in the internal file "cursors/{name}.png"
     */
    public Cursor newCursor(String filename, int scaling, Color outlineColor, int outlineScaling){
        return newCursor(new Pixmap(Core.files.internal("cursors/" + filename + ".png")), scaling, outlineColor, outlineScaling);
    }

    /**Sets the cursor to the default value, e.g. {@link SystemCursor#arrow}.*/
    public void restoreCursor(){
        cursor(SystemCursor.arrow);
    }

    /**Sets the display cursor.*/
    public void cursor(Cursor cursor){
        if(lastCursor == cursor) return;

        if(cursor instanceof SystemCursor){
            if(((SystemCursor)cursor).cursor != null){
                setCursor(((SystemCursor)cursor).cursor);
            }else{
                setSystemCursor((SystemCursor)cursor);
            }
        }else{
            setCursor(cursor);
        }

        lastCursor = cursor;
    }

    /**
     * Only viable on desktop and web. Browsers that support cursor:url() and support the png format (the
     * pixmap is converted to a data-url of type image/png) should also support custom cursors. Will set the mouse cursor image to
     * the image represented by the {@link Cursor}. It is recommended to call this function in the main render thread, and maximum one time per frame.
     * Internal use only!
     * @param cursor the mouse cursor as a {@link Cursor}
     */
    protected abstract void setCursor(Cursor cursor);

    /**Sets one of the predefined {@link SystemCursor}s.
     * Internal use only!*/
    protected abstract void setSystemCursor(SystemCursor systemCursor);

    @Override
    public void dispose(){
        for(SystemCursor cursor : SystemCursor.values()){
            cursor.dispose();
        }
    }

    /** Class describing the bits per pixel, depth buffer precision, stencil precision and number of MSAA samples. */
    public static class BufferFormat{
        /* number of bits per color channel */
        public final int r, g, b, a;
        /* number of bits for depth and stencil buffer */
        public final int depth, stencil;
        /** number of samples for multi-sample anti-aliasing (MSAA) **/
        public final int samples;
        /** whether coverage sampling anti-aliasing is used. in that case you have to clear the coverage buffer as well! */
        public final boolean coverageSampling;

        public BufferFormat(int r, int g, int b, int a, int depth, int stencil, int samples, boolean coverageSampling){
            this.r = r;
            this.g = g;
            this.b = b;
            this.a = a;
            this.depth = depth;
            this.stencil = stencil;
            this.samples = samples;
            this.coverageSampling = coverageSampling;
        }

        public String toString(){
            return "r: " + r + ", g: " + g + ", b: " + b + ", a: " + a + ", depth: " + depth + ", stencil: " + stencil
            + ", num samples: " + samples + ", coverage sampling: " + coverageSampling;
        }
    }

    /**
     * <p>
     * Represents a mouse cursor. Create a cursor via
     * {@link Graphics#newCursor(Pixmap, int, int)}. To
     * set the cursor use {@link Graphics#setCursor(Cursor)}.
     * To use one of the system cursors, call Graphics#setSystemCursor
     * </p>
     **/
    public interface Cursor extends arc.util.Disposable{

        enum SystemCursor implements Cursor{
            arrow,
            ibeam,
            crosshair,
            hand,
            horizontalResize,
            verticalResize;

            /** The override cursor to use when setting this cursor.*/
            @Nullable Cursor cursor;

            /** Sets the alias for this cursor. */
            public void set(Cursor cursor){
                this.cursor = cursor;
            }

            @Override
            public void dispose(){
                if(cursor != null && !(cursor instanceof SystemCursor)){
                    cursor.dispose();
                    cursor = null;
                }
            }
        }
    }
}
