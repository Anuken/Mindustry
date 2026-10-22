package arc.backend.robovm;

import arc.func.*;
import arc.graphics.HdpiUtils.*;
import arc.util.*;
import com.badlogic.gdx.backends.iosrobovm.bindings.metalangle.*;
import org.robovm.apple.uikit.*;

public class IOSApplicationConfiguration{
    /** whether to enable screen dimming. */
    public boolean preventScreenDimming = true;
    /** whether or not portrait orientation is supported. */
    public boolean orientationPortrait = true;
    /** whether or not landscape orientation is supported. */
    public boolean orientationLandscape = true;

    /** whether the status bar should be visible or not * */
    public boolean statusBarVisible = false;

    public HdpiMode hdpiMode = HdpiMode.pixels;

    /** the color format, RGB565 is the default **/
    public MGLDrawableColorFormat colorFormat = MGLDrawableColorFormat.RGBA8888;

    /** the depth buffer format, Format16 is default **/
    public MGLDrawableDepthFormat depthFormat = MGLDrawableDepthFormat._16;

    /** the stencil buffer format, None is default **/
    public MGLDrawableStencilFormat stencilFormat = MGLDrawableStencilFormat.None;

    /** the multisample format, None is default **/
    public MGLDrawableMultisample multisample = MGLDrawableMultisample.None;

    /** number of frames per second, 60 is default **/
    public int preferredFramesPerSecond = 60;

    /** handles any errors in the main loop.*/
    @Nullable
    public Cons<Throwable> errorHandler;

    /** whether to use the accelerometer, default true **/
    public boolean useAccelerometer = false;
    /** the update interval to poll the accelerometer with, in seconds **/
    public float accelerometerUpdate = 0.05f;
    /** whether or not the onScreenKeyboard should be closed on return key **/
    public boolean keyboardCloseOnReturn = true;

    /** whether the home indicator should be hidden or not **/
    public boolean hideHomeIndicator = true;

    /**
     * Edges where app gestures must be fired over system gestures.
     * Prior to iOS 11, UIRectEdge.All was default behaviour if status bar hidden, see #5110
     **/
    public UIRectEdge screenEdgesDeferringSystemGestures = UIRectEdge.None;
}
