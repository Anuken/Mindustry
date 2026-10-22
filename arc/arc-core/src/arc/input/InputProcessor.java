package arc.input;

import arc.ApplicationListener;
import arc.Input;

/**
 * An InputProcessor is used to receive input events from the keyboard and the touch screen (mouse on the desktop). For this it
 * has to be registered with the {@link Input#addProcessor(InputProcessor)} method. It will be called each frame before the
 * call to {@link ApplicationListener#update()}. Each method returns a boolean in case you want to use this with the
 * {@link InputMultiplexer} to chain input processors.
 * @author mzechner
 */
public interface InputProcessor{

    /**
     * Called when a key was pressed
     * @return whether the input was processed
     */
    default boolean keyDown(KeyCode keycode){
        return false;
    }

    /**
     * Called when a key was released
     * @return whether the input was processed
     */
    default boolean keyUp(KeyCode keycode){
        return false;
    }

    /**
     * Called when a key was typed
     * @param character The character
     * @return whether the input was processed
     */
    default boolean keyTyped(char character){
        return false;
    }

    /**
     * @param screenX The x coordinate, origin is in the upper left corner
     * @param screenY The y coordinate, origin is in the upper left corner
     * @param pointer the pointer for the event.
     * @param button the button
     * @return whether the input was processed
     */
    default boolean touchDown(int screenX, int screenY, int pointer, KeyCode button){
        return false;
    }

    /**
     * @param pointer the pointer for the event.
     * @param button the button
     * @return whether the input was processed
     */
    default boolean touchUp(int screenX, int screenY, int pointer, KeyCode button){
        return false;
    }

    /**
     * Called when a finger or the mouse was dragged.
     * @param pointer the pointer for the event.
     * @return whether the input was processed
     */
    default boolean touchDragged(int screenX, int screenY, int pointer){
        return false;
    }

    /**
     * @return whether the input was processed
     */
    default boolean mouseMoved(int screenX, int screenY){
        return false;
    }

    /**
     * Called when the mouse wheel was scrolled. Will not be called on iOS.
     * @param amountX the horizontal scroll amount, negative or positive depending on the direction the wheel was scrolled.
     * @param amountY the vertical scroll amount, negative or positive depending on the direction the wheel was scrolled.
     * @return whether the input was processed.
     */
    default boolean scrolled(float amountX, float amountY){
        return false;
    }
}
