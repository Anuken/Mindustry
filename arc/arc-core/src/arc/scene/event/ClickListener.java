package arc.scene.event;

import arc.Core;
import arc.input.KeyCode;
import arc.scene.Element;
import arc.util.*;

/**
 * Detects mouse over, mouse or finger touch presses, and clicks on an element. A touch must go down over the element and is
 * considered pressed as long as it is over the element or within the {@link #setTapSquareSize(float) tap square}. This behavior
 * makes it easier to press buttons on a touch interface when the initial touch happens near the edge of the element. Double clicks
 * can be detected using {@link #getTapCount()}. Any touch (not just the first) will trigger this listener. While pressed, other
 * touch downs are ignored.
 * @author Nathan Sweet
 */
public class ClickListener extends InputListener{
    /** Time in seconds {@link #isVisualPressed()} reports true after a press resulting in a click is released. */
    public static float visualPressedDuration = 0.1f;
    public static Runnable clicked = () -> {};

    protected float tapSquareSize = 14, touchDownX = -1, touchDownY = -1;
    protected int pressedPointer = -1;
    protected KeyCode pressedButton;
    protected KeyCode button = KeyCode.mouseLeft;
    protected boolean pressed, over, overAny, cancelled;
    protected long visualPressedTime;
    protected long tapCountInterval = (long)(0.4f * 1000000000L);
    protected int tapCount;
    protected long lastTapTime;
    protected boolean stop = false;

    /** Create a listener where {@link #clicked(InputEvent, float, float)} is only called for left clicks. */
    public ClickListener(){}

    public ClickListener(KeyCode button){
        this.button = button;
    }

    @Override
    public boolean touchDown(InputEvent event, float x, float y, int pointer, KeyCode button){
        if(pressed) return false;
        if(pointer == 0 && this.button != null && button != this.button) return false;
        pressed = true;
        pressedPointer = pointer;
        pressedButton = button;
        touchDownX = x;
        touchDownY = y;
        visualPressedTime = Time.millis() + (long)(visualPressedDuration * 1000);
        return true;
    }

    @Override
    public void touchDragged(InputEvent event, float x, float y, int pointer){
        if(pointer != pressedPointer || cancelled) return;
        pressed = isOver(event.listenerActor, x, y);
        if(pressed && pointer == 0 && button != null && !Core.input.keyDown(button)) pressed = false;
        if(!pressed){
            // Once outside the tap square, don't use the tap square anymore.
            invalidateTapSquare();
        }
    }

    @Override
    public void touchUp(InputEvent event, float x, float y, int pointer, KeyCode button){
        if(pointer == pressedPointer){
            if(!cancelled){
                boolean touchUpOver = isOver(event.listenerActor, x, y);
                // Ignore touch up if the wrong mouse button.
                if(touchUpOver && pointer == 0 && this.button != null && button != this.button) touchUpOver = false;
                if(touchUpOver){
                    long time = Time.nanos();
                    if(time - lastTapTime > tapCountInterval) tapCount = 0;
                    tapCount++;
                    lastTapTime = time;

                    clicked.run();
                    clicked(event, x, y);
                }
            }
            pressed = false;
            pressedPointer = -1;
            pressedButton = null;
            cancelled = false;
        }
    }

    @Override
    public void enter(InputEvent event, float x, float y, int pointer, Element fromActor){
        if(pointer == -1 && !cancelled) over = true;
        if(!cancelled) overAny = true;
    }

    @Override
    public void exit(InputEvent event, float x, float y, int pointer, Element toActor){
        if(pointer == -1 && !cancelled) over = false;
        if(!cancelled) overAny = false;
    }

    /** If a touch down is being monitored, the drag and touch up events are ignored until the next touch up. */
    public void cancel(){
        if(pressedPointer == -1) return;
        cancelled = true;
        pressed = false;
    }

    public void clicked(InputEvent event, float x, float y){
    }

    /** Returns true if the specified position is over the specified element or within the tap square. */
    public boolean isOver(Element element, float x, float y){
        element.localToStageCoordinates(Tmp.v1.set(x, y));
        Element hit = Core.scene.hit(Tmp.v1.x, Tmp.v1.y, true);
        return hit != null && hit.isDescendantOf(element);
    }

    public boolean inTapSquare(float x, float y){
        return (!(touchDownX == -1) || !(touchDownY == -1)) && Math.abs(x - touchDownX) < tapSquareSize && Math.abs(y - touchDownY) < tapSquareSize;
    }

    /** Returns true if a touch is within the tap square. */
    public boolean inTapSquare(){
        return touchDownX != -1;
    }

    /** The tap square will not longer be used for the current touch. */
    public void invalidateTapSquare(){
        touchDownX = -1;
        touchDownY = -1;
    }

    /** Returns true if a touch is over the element or within the tap square. */
    public boolean isPressed(){
        return pressed;
    }

    /**
     * Returns true if a touch is over the element or within the tap square or has been very recently. This allows the UI to show a
     * press and release that was so fast it occurred within a single frame.
     */
    public boolean isVisualPressed(){
        if(pressed) return true;
        if(visualPressedTime <= 0) return false;
        if(visualPressedTime > Time.millis()) return true;
        visualPressedTime = 0;
        return false;
    }

    /** Returns true if the mouse or touch is over the element or pressed and within the tap square. */
    public boolean isOver(){
        return over || pressed;
    }

    public float getTapSquareSize(){
        return tapSquareSize;
    }

    public void setTapSquareSize(float halfTapSquareSize){
        tapSquareSize = halfTapSquareSize;
    }

    /** @param tapCountInterval time in seconds that must pass for two touch down/up sequences to be detected as consecutive taps. */
    public void setTapCountInterval(float tapCountInterval){
        this.tapCountInterval = (long)(tapCountInterval * 1000000000L);
    }

    /** Returns the number of taps within the tap count interval for the most recent click event. */
    public int getTapCount(){
        return tapCount;
    }

    public void setTapCount(int tapCount){
        this.tapCount = tapCount;
    }

    public float getTouchDownX(){
        return touchDownX;
    }

    public float getTouchDownY(){
        return touchDownY;
    }

    /** The button that initially pressed this button or -1 if the button is not pressed. */
    public KeyCode getPressedButton(){
        return pressedButton;
    }

    /** The pointer that initially pressed this button or -1 if the button is not pressed. */
    public int getPressedPointer(){
        return pressedPointer;
    }

    public KeyCode getButton(){
        return button;
    }

    /** Sets the button to listen for, all other buttons are ignored. Use null for any button. */
    public void setButton(KeyCode button){
        this.button = button;
    }
}
