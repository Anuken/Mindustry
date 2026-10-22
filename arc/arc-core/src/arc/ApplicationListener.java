package arc;

import arc.files.Fi;

/**
 * <p>
 * An <code>ApplicationListener</code> is called when the {@link Application} is created, resumed, rendering, paused or destroyed.
 * All methods are called in a thread that has the OpenGL context current. You can thus safely create and manipulate graphics
 * resources.
 * </p>
 *
 * <p>
 * The <code>ApplicationListener</code> interface follows the standard Android activity life-cycle and is emulated on the desktop
 * accordingly.
 * </p>
 * @author mzechner
 */
public interface ApplicationListener{
    /**
     * Called when the {@link Application} is first created.
     * Only gets called if the application is created before the listener is added.
     */
    default void init(){
    }

    /**
     * Called when the {@link Application} is resized. This can happen at any point during a non-paused state but will never happen
     * before a call to {@link #init()}.
     * @param width the new width in pixels
     * @param height the new height in pixels
     */
    default void resize(int width, int height){
    }

    /** Called when the {@link Application} should update itself. */
    default void update(){
    }

    /**
     * Called when the {@link Application} is paused, usually when it's not active or visible on screen. An Application is also
     * paused before it is destroyed.
     */
    default void pause(){
    }

    /** Called when the {@link Application} is resumed from a paused state, usually when it regains focus. */
    default void resume(){
    }

    /** Called when the {@link Application} is destroyed. Preceded by a call to {@link #pause()}. */
    default void dispose(){
    }

    /**
     * Called when the applications exits gracefully, either through `Core.app.exit()` or through a window closing.
     * Never called after a crash, unlike dispose().
     * */
    default void exit(){

    }

    /**
     * Called when an external file is dropped into the window, e.g from the desktop.
     */
    default void fileDropped(Fi file){
    }
}
