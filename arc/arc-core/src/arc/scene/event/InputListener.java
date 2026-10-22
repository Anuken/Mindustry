package arc.scene.event;

import arc.input.KeyCode;
import arc.math.geom.Vec2;
import arc.scene.Element;

/**
 * EventListener for low-level input events. Unpacks {@link InputEvent}s and calls the appropriate method. By default the methods
 * here do nothing with the event. Users are expected to override the methods they are interested in, like this:
 */
public class InputListener implements EventListener{
    private static final Vec2 tmpCoords = new Vec2();

    @Override
    public boolean handle(SceneEvent e){
        if(!(e instanceof InputEvent)) return false;
        InputEvent event = (InputEvent)e;

        switch(event.type){
            case keyDown: return keyDown(event, event.keyCode);
            case keyUp: return keyUp(event, event.keyCode);
            case keyTyped: return keyTyped(event, event.character);
        }

        event.toCoordinates(event.listenerActor, tmpCoords);

        switch(event.type){
            case touchDown:
                return touchDown(event, tmpCoords.x, tmpCoords.y, event.pointer, event.keyCode);
            case touchUp:
                touchUp(event, tmpCoords.x, tmpCoords.y, event.pointer, event.keyCode);
                return true;
            case touchDragged:
                touchDragged(event, tmpCoords.x, tmpCoords.y, event.pointer);
                return true;
            case mouseMoved:
                return mouseMoved(event, tmpCoords.x, tmpCoords.y);
            case scrolled:
                return scrolled(event, tmpCoords.x, tmpCoords.y, event.scrollAmountX, event.scrollAmountY);
            case enter:
                enter(event, tmpCoords.x, tmpCoords.y, event.pointer, event.relatedActor);
                return false;
            case exit:
                exit(event, tmpCoords.x, tmpCoords.y, event.pointer, event.relatedActor);
                return false;
        }
        return false;
    }

    /**
     * Called when a mouse button or a finger touch goes down on the element. If true is returned, this listener will receive all
     * touchDragged and touchUp events, even those not over this element, until touchUp is received. Also when true is returned, the
     * event is {@link SceneEvent#handle() handled}.
     * @see InputEvent
     */
    public boolean touchDown(InputEvent event, float x, float y, int pointer, KeyCode button){
        return false;
    }

    /**
     * Called when a mouse button or a finger touch goes up anywhere, but only if touchDown previously returned true for the mouse
     * button or touch. The touchUp event is always {@link SceneEvent#handle() handled}.
     * @see InputEvent
     */
    public void touchUp(InputEvent event, float x, float y, int pointer, KeyCode button){
    }

    /**
     * Called when a mouse button or a finger touch is moved anywhere, but only if touchDown previously returned true for the mouse
     * button or touch. The touchDragged event is always {@link SceneEvent#handle() handled}.
     * @see InputEvent
     */
    public void touchDragged(InputEvent event, float x, float y, int pointer){
    }

    /**
     * Called any time the mouse is moved when a button is not down. This event only occurs on the desktop. When true is returned,
     * the event is {@link SceneEvent#handle() handled}.
     * @see InputEvent
     */
    public boolean mouseMoved(InputEvent event, float x, float y){
        return false;
    }

    /**
     * Called any time the mouse cursor or a finger touch is moved over an element. On the desktop, this event occurs even when no
     * mouse buttons are pressed (pointer will be -1).
     * @param fromActor May be null.
     * @see InputEvent
     */
    public void enter(InputEvent event, float x, float y, int pointer, Element fromActor){
    }

    /**
     * Called any time the mouse cursor or a finger touch is moved out of an element. On the desktop, this event occurs even when no
     * mouse buttons are pressed (pointer will be -1).
     * @param toActor May be null.
     * @see InputEvent
     */
    public void exit(InputEvent event, float x, float y, int pointer, Element toActor){
    }

    /** Called when the mouse wheel has been scrolled. When true is returned, the event is {@link SceneEvent#handle() handled}. */
    public boolean scrolled(InputEvent event, float x, float y, float amountX, float amountY){
        return false;
    }

    /** Called when a key goes down. When true is returned, the event is {@link SceneEvent#handle() handled}. */
    public boolean keyDown(InputEvent event, KeyCode keycode){
        return false;
    }

    /** Called when a key goes up. When true is returned, the event is {@link SceneEvent#handle() handled}. */
    public boolean keyUp(InputEvent event, KeyCode keycode){
        return false;
    }

    /** Called when a key is typed. When true is returned, the event is {@link SceneEvent#handle() handled}. */
    public boolean keyTyped(InputEvent event, char character){
        return false;
    }
}
