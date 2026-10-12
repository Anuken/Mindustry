package arc.scene.event;

import arc.input.GestureDetector;
import arc.input.GestureDetector.GestureListener;
import arc.input.KeyCode;
import arc.math.geom.Vec2;
import arc.scene.Element;

/**
 * Detects tap, long press, fling, pan, zoom, and pinch gestures on an actor. If there is only a need to detect tap, use
 * {@link ClickListener}.
 * @author Nathan Sweet
 * @see GestureDetector
 */
public class ElementGestureListener implements EventListener{
    static final Vec2 tmpCoords = new Vec2(), tmpCoords2 = new Vec2();

    private final GestureDetector detector;
    InputEvent event;
    Element actor, touchDownTarget;

    /** @see GestureDetector#GestureDetector(arc.input.GestureDetector.GestureListener) */
    public ElementGestureListener(){
        this(20, 0.4f, 1.1f, 0.15f);
    }

    /** @see GestureDetector#GestureDetector(float, float, float, float, arc.input.GestureDetector.GestureListener) */
    public ElementGestureListener(float halfTapSquareSize, float tapCountInterval, float longPressDuration, float maxFlingDelay){
        detector = new GestureDetector(halfTapSquareSize, tapCountInterval, longPressDuration, maxFlingDelay, new GestureListener(){
            private final Vec2 initialPointer1 = new Vec2(), initialPointer2 = new Vec2();
            private final Vec2 pointer1 = new Vec2(), pointer2 = new Vec2();

            @Override
            public boolean tap(float stageX, float stageY, int count, KeyCode button){
                actor.stageToLocalCoordinates(tmpCoords.set(stageX, stageY));
                ElementGestureListener.this.tap(event, tmpCoords.x, tmpCoords.y, count, button);
                return true;
            }

            @Override
            public boolean longPress(float stageX, float stageY){
                actor.stageToLocalCoordinates(tmpCoords.set(stageX, stageY));
                return ElementGestureListener.this.longPress(actor, tmpCoords.x, tmpCoords.y);
            }

            @Override
            public boolean fling(float velocityX, float velocityY, KeyCode button){
                stageToLocalAmount(tmpCoords.set(velocityX, velocityY));
                ElementGestureListener.this.fling(event, tmpCoords.x, tmpCoords.y, button);
                return true;
            }

            @Override
            public boolean pan(float stageX, float stageY, float deltaX, float deltaY){
                stageToLocalAmount(tmpCoords.set(deltaX, deltaY));
                deltaX = tmpCoords.x;
                deltaY = tmpCoords.y;
                actor.stageToLocalCoordinates(tmpCoords.set(stageX, stageY));
                ElementGestureListener.this.pan(event, tmpCoords.x, tmpCoords.y, deltaX, deltaY);
                return true;
            }

            @Override
            public boolean zoom(float initialDistance, float distance){
                ElementGestureListener.this.zoom(event, initialDistance, distance);
                return true;
            }

            @Override
            public boolean pinch(Vec2 stageInitialPointer1, Vec2 stageInitialPointer2, Vec2 stagePointer1,
                                 Vec2 stagePointer2){
                actor.stageToLocalCoordinates(initialPointer1.set(stageInitialPointer1));
                actor.stageToLocalCoordinates(initialPointer2.set(stageInitialPointer2));
                actor.stageToLocalCoordinates(pointer1.set(stagePointer1));
                actor.stageToLocalCoordinates(pointer2.set(stagePointer2));
                ElementGestureListener.this.pinch(event, initialPointer1, initialPointer2, pointer1, pointer2);
                return true;
            }

            private void stageToLocalAmount(Vec2 amount){
                actor.stageToLocalCoordinates(amount);
                amount.sub(actor.stageToLocalCoordinates(tmpCoords2.set(0, 0)));
            }
        });
    }

    @Override
    public boolean handle(SceneEvent e){
        if(!(e instanceof InputEvent)) return false;
        InputEvent event = (InputEvent)e;

        switch(event.type){
            case touchDown:
                actor = event.listenerActor;
                touchDownTarget = event.targetActor;
                detector.touchDown(event.stageX, event.stageY, event.pointer, event.keyCode);
                actor.stageToLocalCoordinates(tmpCoords.set(event.stageX, event.stageY));
                touchDown(event, tmpCoords.x, tmpCoords.y, event.pointer, event.keyCode);
                return true;
            case touchUp:
                if(event.isTouchFocusCancel()) return false;
                this.event = event;
                actor = event.listenerActor;
                detector.touchUp(event.stageX, event.stageY, event.pointer, event.keyCode);
                actor.stageToLocalCoordinates(tmpCoords.set(event.stageX, event.stageY));
                touchUp(event, tmpCoords.x, tmpCoords.y, event.pointer, event.keyCode);
                return true;
            case touchDragged:
                this.event = event;
                actor = event.listenerActor;
                detector.touchDragged(event.stageX, event.stageY, event.pointer);
                return true;
        }
        return false;
    }

    public void touchDown(InputEvent event, float x, float y, int pointer, KeyCode button){
    }

    public void touchUp(InputEvent event, float x, float y, int pointer, KeyCode button){
    }

    public void tap(InputEvent event, float x, float y, int count, KeyCode button){
    }

    /**
     * If true is returned, additional gestures will not be triggered. No event is provided because this event is triggered by time
     * passing, not by an InputEvent.
     */
    public boolean longPress(Element actor, float x, float y){
        return false;
    }

    public void fling(InputEvent event, float velocityX, float velocityY, KeyCode button){
    }

    /** The delta is the difference in stage coordinates since the last pan. */
    public void pan(InputEvent event, float x, float y, float deltaX, float deltaY){
    }

    public void zoom(InputEvent event, float initialDistance, float distance){
    }

    public void pinch(InputEvent event, Vec2 initialPointer1, Vec2 initialPointer2, Vec2 pointer1, Vec2 pointer2){
    }

    public GestureDetector getGestureDetector(){
        return detector;
    }

    public Element getTouchDownTarget(){
        return touchDownTarget;
    }
}
