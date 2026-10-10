package arc.scene.event;

import arc.scene.Element;

/**
 * Listener for {@link ChangeEvent}.
 * @author Nathan Sweet
 */
abstract public class ChangeListener implements EventListener{
    @Override
    public boolean handle(SceneEvent event){
        if(!(event instanceof ChangeEvent)) return false;
        changed((ChangeEvent)event, event.targetActor);
        return false;
    }

    /** @param actor The event target, which is the actor that emitted the change event. */
    abstract public void changed(ChangeEvent event, Element actor);

    /**
     * Fired when something in an actor has changed. This is a generic event, exactly what changed in an actor will vary.
     * @author Nathan Sweet
     */
    public static class ChangeEvent extends SceneEvent{
    }
}
