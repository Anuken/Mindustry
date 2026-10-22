package arc.scene.actions;

import arc.func.Prov;
import arc.graphics.Color;
import arc.math.Interp;
import arc.scene.Action;
import arc.scene.Element;
import arc.scene.event.EventListener;
import arc.scene.event.Touchable;
import arc.util.pooling.Pools;

/**
 * Static convenience methods for using pooled actions, intended for static import.
 * @author Nathan Sweet
 */
public class Actions{
    /** Returns a new or pooled action of the specified type. */
    public static <T extends Action> T action(Class<T> type, Prov<T> sup){
        T action = Pools.obtain(type, sup);
        action.setPool(Pools.get(type, sup));
        return action;
    }

    public static AddAction addAction(Action action){
        AddAction addAction = action(AddAction.class, AddAction::new);
        addAction.setAction(action);
        return addAction;
    }

    public static AddAction addAction(Action action, Element targetActor){
        AddAction addAction = action(AddAction.class, AddAction::new);
        addAction.setTarget(targetActor);
        addAction.setAction(action);
        return addAction;
    }

    public static RemoveAction removeAction(Action action){
        RemoveAction removeAction = action(RemoveAction.class, RemoveAction::new);
        removeAction.setAction(action);
        return removeAction;
    }

    public static RemoveAction removeAction(Action action, Element targetActor){
        RemoveAction removeAction = action(RemoveAction.class, RemoveAction::new);
        removeAction.setTarget(targetActor);
        removeAction.setAction(action);
        return removeAction;
    }

    /** Sets the origin to the center. */
    public static Action originCenter(){
        return new OriginAction();
    }

    /** Moves the actor instantly. */
    public static MoveToAction moveTo(float x, float y){
        return moveTo(x, y, 0, null);
    }

    public static MoveToAction moveTo(float x, float y, float duration){
        return moveTo(x, y, duration, null);
    }

    public static MoveToAction moveTo(float x, float y, float duration, Interp interpolation){
        MoveToAction action = action(MoveToAction.class, MoveToAction::new);
        action.setPosition(x, y);
        action.setDuration(duration);
        action.setInterpolation(interpolation);
        return action;
    }

    public static MoveToAction moveToAligned(float x, float y, int alignment){
        return moveToAligned(x, y, alignment, 0, null);
    }

    public static MoveToAction moveToAligned(float x, float y, int alignment, float duration){
        return moveToAligned(x, y, alignment, duration, null);
    }

    public static MoveToAction moveToAligned(float x, float y, int alignment, float duration, Interp interpolation){
        MoveToAction action = action(MoveToAction.class, MoveToAction::new);
        action.setPosition(x, y, alignment);
        action.setDuration(duration);
        action.setInterpolation(interpolation);
        return action;
    }

    /** Moves the actor instantly. */
    public static MoveByAction moveBy(float amountX, float amountY){
        return moveBy(amountX, amountY, 0, null);
    }

    public static MoveByAction moveBy(float amountX, float amountY, float duration){
        return moveBy(amountX, amountY, duration, null);
    }

    public static RunnableAction translateTo(float amountX, float amountY){
        RunnableAction action = action(RunnableAction.class, RunnableAction::new);
        action.setRunnable(() -> action.getActor().setTranslation(amountX, amountY));
        return action;
    }

    public static TranslateByAction translateBy(float amountX, float amountY, float duration, Interp interpolation){
        TranslateByAction action = action(TranslateByAction.class, TranslateByAction::new);
        action.setAmount(amountX, amountY);
        action.setDuration(duration);
        action.setInterpolation(interpolation);
        return action;
    }

    public static TranslateByAction translateBy(float amountX, float amountY){
        return translateBy(amountX, amountY, 0, null);
    }

    public static TranslateByAction translateBy(float amountX, float amountY, float duration){
        return translateBy(amountX, amountY, duration, null);
    }

    public static MoveByAction moveBy(float amountX, float amountY, float duration, Interp interpolation){
        MoveByAction action = action(MoveByAction.class, MoveByAction::new);
        action.setAmount(amountX, amountY);
        action.setDuration(duration);
        action.setInterpolation(interpolation);
        return action;
    }

    /** Sizes the actor instantly. */
    public static SizeToAction sizeTo(float x, float y){
        return sizeTo(x, y, 0, null);
    }

    public static SizeToAction sizeTo(float x, float y, float duration){
        return sizeTo(x, y, duration, null);
    }

    public static SizeToAction sizeTo(float x, float y, float duration, Interp interpolation){
        SizeToAction action = action(SizeToAction.class, SizeToAction::new);
        action.setSize(x, y);
        action.setDuration(duration);
        action.setInterpolation(interpolation);
        return action;
    }

    /** Sizes the actor instantly. */
    public static SizeByAction sizeBy(float amountX, float amountY){
        return sizeBy(amountX, amountY, 0, null);
    }

    public static SizeByAction sizeBy(float amountX, float amountY, float duration){
        return sizeBy(amountX, amountY, duration, null);
    }

    public static SizeByAction sizeBy(float amountX, float amountY, float duration, Interp interpolation){
        SizeByAction action = action(SizeByAction.class, SizeByAction::new);
        action.setAmount(amountX, amountY);
        action.setDuration(duration);
        action.setInterpolation(interpolation);
        return action;
    }

    /** Scales the actor instantly. */
    public static ScaleToAction scaleTo(float x, float y){
        return scaleTo(x, y, 0, null);
    }

    public static ScaleToAction scaleTo(float x, float y, float duration){
        return scaleTo(x, y, duration, null);
    }

    public static ScaleToAction scaleTo(float x, float y, float duration, Interp interpolation){
        ScaleToAction action = action(ScaleToAction.class, ScaleToAction::new);
        action.setScale(x, y);
        action.setDuration(duration);
        action.setInterpolation(interpolation);
        return action;
    }

    /** Scales the actor instantly. */
    public static ScaleByAction scaleBy(float amountX, float amountY){
        return scaleBy(amountX, amountY, 0, null);
    }

    public static ScaleByAction scaleBy(float amountX, float amountY, float duration){
        return scaleBy(amountX, amountY, duration, null);
    }

    public static ScaleByAction scaleBy(float amountX, float amountY, float duration, Interp interpolation){
        ScaleByAction action = action(ScaleByAction.class, ScaleByAction::new);
        action.setAmount(amountX, amountY);
        action.setDuration(duration);
        action.setInterpolation(interpolation);
        return action;
    }

    /** Rotates the actor instantly. */
    public static RotateToAction rotateTo(float rotation){
        return rotateTo(rotation, 0, null);
    }

    public static RotateToAction rotateTo(float rotation, float duration){
        return rotateTo(rotation, duration, null);
    }

    public static RotateToAction rotateTo(float rotation, float duration, Interp interpolation){
        RotateToAction action = action(RotateToAction.class, RotateToAction::new);
        action.setRotation(rotation);
        action.setDuration(duration);
        action.setInterpolation(interpolation);
        return action;
    }

    /** Rotates the actor instantly. */
    public static RotateByAction rotateBy(float rotationAmount){
        return rotateBy(rotationAmount, 0, null);
    }

    public static RotateByAction rotateBy(float rotationAmount, float duration){
        return rotateBy(rotationAmount, duration, null);
    }

    public static RotateByAction rotateBy(float rotationAmount, float duration, Interp interpolation){
        RotateByAction action = action(RotateByAction.class, RotateByAction::new);
        action.setAmount(rotationAmount);
        action.setDuration(duration);
        action.setInterpolation(interpolation);
        return action;
    }

    /** Sets the actor's color instantly. */
    public static ColorAction color(Color color){
        return color(color, 0, null);
    }

    /** Transitions from the color at the time this action starts to the specified color. */
    public static ColorAction color(Color color, float duration){
        return color(color, duration, null);
    }

    /** Transitions from the color at the time this action starts to the specified color. */
    public static ColorAction color(Color color, float duration, Interp interpolation){
        ColorAction action = action(ColorAction.class, ColorAction::new);
        action.setEndColor(color);
        action.setDuration(duration);
        action.setInterpolation(interpolation);
        return action;
    }

    /** Sets the actor's alpha instantly. */
    public static AlphaAction alpha(float a){
        return alpha(a, 0, null);
    }

    /** Transitions from the alpha at the time this action starts to the specified alpha. */
    public static AlphaAction alpha(float a, float duration){
        return alpha(a, duration, null);
    }

    /** Transitions from the alpha at the time this action starts to the specified alpha. */
    public static AlphaAction alpha(float a, float duration, Interp interpolation){
        AlphaAction action = action(AlphaAction.class, AlphaAction::new);
        action.setAlpha(a);
        action.setDuration(duration);
        action.setInterpolation(interpolation);
        return action;
    }

    /** Transitions from the alpha at the time this action starts to an alpha of 0. */
    public static AlphaAction fadeOut(float duration){
        return alpha(0, duration, null);
    }

    /** Transitions from the alpha at the time this action starts to an alpha of 0. */
    public static AlphaAction fadeOut(float duration, Interp interpolation){
        AlphaAction action = action(AlphaAction.class, AlphaAction::new);
        action.setAlpha(0);
        action.setDuration(duration);
        action.setInterpolation(interpolation);
        return action;
    }

    /** Transitions from the alpha at the time this action starts to an alpha of 1. */
    public static AlphaAction fadeIn(float duration){
        return alpha(1, duration, null);
    }

    /** Transitions from the alpha at the time this action starts to an alpha of 1. */
    public static AlphaAction fadeIn(float duration, Interp interpolation){
        AlphaAction action = action(AlphaAction.class, AlphaAction::new);
        action.setAlpha(1);
        action.setDuration(duration);
        action.setInterpolation(interpolation);
        return action;
    }

    public static VisibleAction show(){
        return visible(true);
    }

    public static VisibleAction hide(){
        return visible(false);
    }

    public static VisibleAction visible(boolean visible){
        VisibleAction action = action(VisibleAction.class, VisibleAction::new);
        action.setVisible(visible);
        return action;
    }

    public static TouchableAction touchable(Touchable touchable){
        TouchableAction action = action(TouchableAction.class, TouchableAction::new);
        action.touchable(touchable);
        return action;
    }

    public static RemoveActorAction remove(){
        return action(RemoveActorAction.class, RemoveActorAction::new);
    }

    public static RemoveActorAction remove(Element removeActor){
        RemoveActorAction action = action(RemoveActorAction.class, RemoveActorAction::new);
        action.setTarget(removeActor);
        return action;
    }

    public static DelayAction delay(float duration){
        DelayAction action = action(DelayAction.class, DelayAction::new);
        action.setDuration(duration);
        return action;
    }

    public static DelayAction delay(float duration, Action delayedAction){
        DelayAction action = action(DelayAction.class, DelayAction::new);
        action.setDuration(duration);
        action.setAction(delayedAction);
        return action;
    }

    public static TimeScaleAction timeScale(float scale, Action scaledAction){
        TimeScaleAction action = action(TimeScaleAction.class, TimeScaleAction::new);
        action.setScale(scale);
        action.setAction(scaledAction);
        return action;
    }

    public static SequenceAction sequence(Action action1){
        SequenceAction action = action(SequenceAction.class, SequenceAction::new);
        action.addAction(action1);
        return action;
    }

    public static SequenceAction sequence(Action action1, Action action2){
        SequenceAction action = action(SequenceAction.class, SequenceAction::new);
        action.addAction(action1);
        action.addAction(action2);
        return action;
    }

    public static SequenceAction sequence(Action action1, Action action2, Action action3){
        SequenceAction action = action(SequenceAction.class, SequenceAction::new);
        action.addAction(action1);
        action.addAction(action2);
        action.addAction(action3);
        return action;
    }

    public static SequenceAction sequence(Action action1, Action action2, Action action3, Action action4){
        SequenceAction action = action(SequenceAction.class, SequenceAction::new);
        action.addAction(action1);
        action.addAction(action2);
        action.addAction(action3);
        action.addAction(action4);
        return action;
    }

    public static SequenceAction sequence(Action action1, Action action2, Action action3, Action action4, Action action5){
        SequenceAction action = action(SequenceAction.class, SequenceAction::new);
        action.addAction(action1);
        action.addAction(action2);
        action.addAction(action3);
        action.addAction(action4);
        action.addAction(action5);
        return action;
    }

    public static SequenceAction sequence(Action... actions){
        SequenceAction action = action(SequenceAction.class, SequenceAction::new);
        for(int i = 0, n = actions.length; i < n; i++)
            action.addAction(actions[i]);
        return action;
    }

    public static SequenceAction sequence(){
        return action(SequenceAction.class, SequenceAction::new);
    }

    public static ParallelAction parallel(Action action1){
        ParallelAction action = action(ParallelAction.class, ParallelAction::new);
        action.addAction(action1);
        return action;
    }

    public static ParallelAction parallel(Action action1, Action action2){
        ParallelAction action = action(ParallelAction.class, ParallelAction::new);
        action.addAction(action1);
        action.addAction(action2);
        return action;
    }

    public static ParallelAction parallel(Action action1, Action action2, Action action3){
        ParallelAction action = action(ParallelAction.class, ParallelAction::new);
        action.addAction(action1);
        action.addAction(action2);
        action.addAction(action3);
        return action;
    }

    public static ParallelAction parallel(Action action1, Action action2, Action action3, Action action4){
        ParallelAction action = action(ParallelAction.class, ParallelAction::new);
        action.addAction(action1);
        action.addAction(action2);
        action.addAction(action3);
        action.addAction(action4);
        return action;
    }

    public static ParallelAction parallel(Action action1, Action action2, Action action3, Action action4, Action action5){
        ParallelAction action = action(ParallelAction.class, ParallelAction::new);
        action.addAction(action1);
        action.addAction(action2);
        action.addAction(action3);
        action.addAction(action4);
        action.addAction(action5);
        return action;
    }

    public static ParallelAction parallel(Action... actions){
        ParallelAction action = action(ParallelAction.class, ParallelAction::new);
        for(int i = 0, n = actions.length; i < n; i++)
            action.addAction(actions[i]);
        return action;
    }

    public static ParallelAction parallel(){
        return action(ParallelAction.class, ParallelAction::new);
    }

    public static RepeatAction repeat(int count, Action repeatedAction){
        RepeatAction action = action(RepeatAction.class, RepeatAction::new);
        action.setCount(count);
        action.setAction(repeatedAction);
        return action;
    }

    public static RepeatAction forever(Action repeatedAction){
        RepeatAction action = action(RepeatAction.class, RepeatAction::new);
        action.setCount(RepeatAction.FOREVER);
        action.setAction(repeatedAction);
        return action;
    }

    public static RunnableAction run(Runnable runnable){
        RunnableAction action = action(RunnableAction.class, RunnableAction::new);
        action.setRunnable(runnable);
        return action;
    }

    public static LayoutAction layout(boolean enabled){
        LayoutAction action = action(LayoutAction.class, LayoutAction::new);
        action.setLayoutEnabled(enabled);
        return action;
    }

    public static AfterAction after(Action action){
        AfterAction afterAction = action(AfterAction.class, AfterAction::new);
        afterAction.setAction(action);
        return afterAction;
    }

    public static AddListenerAction addListener(EventListener listener, boolean capture){
        AddListenerAction addAction = action(AddListenerAction.class, AddListenerAction::new);
        addAction.setListener(listener);
        addAction.setCapture(capture);
        return addAction;
    }

    public static AddListenerAction addListener(EventListener listener, boolean capture, Element targetActor){
        AddListenerAction addAction = action(AddListenerAction.class, AddListenerAction::new);
        addAction.setTarget(targetActor);
        addAction.setListener(listener);
        addAction.setCapture(capture);
        return addAction;
    }

    public static RemoveListenerAction removeListener(EventListener listener, boolean capture){
        RemoveListenerAction addAction = action(RemoveListenerAction.class, RemoveListenerAction::new);
        addAction.setListener(listener);
        addAction.setCapture(capture);
        return addAction;
    }

    public static RemoveListenerAction removeListener(EventListener listener, boolean capture, Element targetActor){
        RemoveListenerAction addAction = action(RemoveListenerAction.class, RemoveListenerAction::new);
        addAction.setTarget(targetActor);
        addAction.setListener(listener);
        addAction.setCapture(capture);
        return addAction;
    }
}
