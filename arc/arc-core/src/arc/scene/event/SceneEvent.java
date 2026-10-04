package arc.scene.event;

import arc.scene.Element;
import arc.scene.Scene;
import arc.util.pooling.Pool.Poolable;

/**
 * The base class for all events.
 * <p>
 * By default an event will "bubble" up through an element's parent's handlers.
 * <p>
 * An element's capture listeners can {@link #stop()} an event to prevent child elements from seeing it.
 * <p>
 * An Event may be marked as "handled" which will end its propagation outside of the Stage (see {@link #handle()}). The default
 * {@link Element#fire(SceneEvent)} will mark events handled if an {@link EventListener} returns true.
 * <p>
 * A cancelled event will be stopped and handled. Additionally, many elements will undo the side-effects of a canceled event. (See
 * {@link #cancel()}.)
 * @see InputEvent
 * @see Element#fire(SceneEvent)
 */
public class SceneEvent implements Poolable{
    public Element targetActor;
    public Element listenerActor;

    public boolean capture; // true means event occurred during the capture phase
    public boolean bubbles = true; // true means propagate to target's parents
    public boolean handled; // true means the event was handled (the stage will eat the input)
    public boolean stopped; // true means event propagation was stopped
    public boolean cancelled; // true means propagation was stopped and any action that this event would cause should not happen

    /**
     * Marks this event as handled. This does not affect event propagation inside scene2d, but causes the {@link Scene} event
     * methods to return true, which will eat the event so it is not passed on to the application under the stage.
     */
    public void handle(){
        handled = true;
    }

    /**
     * Marks this event cancelled. This {@link #handle() handles} the event and {@link #stop() stops} the event propagation. It
     * also cancels any default action that would have been taken by the code that fired the event. Eg, if the event is for a
     * checkbox being checked, cancelling the event could uncheck the checkbox.
     */
    public void cancel(){
        cancelled = true;
        stopped = true;
        handled = true;
    }

    /** Marks this event has being stopped. This halts event propagation. */
    public void stop(){
        stopped = true;
    }

    @Override
    public void reset(){
        targetActor = null;
        listenerActor = null;
        capture = false;
        bubbles = true;
        handled = false;
        stopped = false;
        cancelled = false;
    }

}
