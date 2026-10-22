package arc.scene.ui;

import arc.*;
import arc.func.*;
import arc.input.*;
import arc.math.*;
import arc.math.geom.*;
import arc.scene.*;
import arc.scene.event.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import arc.util.Timer.*;

import static arc.math.Interp.*;
import static arc.scene.actions.Actions.*;

/**
 * A listener that shows a tooltip element when another element is hovered over with the mouse.
 * @author Nathan Sweet
 */
public class Tooltip extends InputListener{
    static Vec2 tmp = new Vec2();

    public final Tooltips manager;
    public final Table container;
    public boolean allowMobile, instant = true, always;

    protected Element targetActor;
    protected Runnable show;

    public Tooltip(Cons<Table> contents){
        this(contents, Tooltips.getInstance());
    }

    public Tooltip(Cons<Table> contents, Runnable show){
        this(contents, Tooltips.getInstance());
        this.show = show;
    }

    public Tooltip(Cons<Table> contents, Tooltips manager){
        this.manager = manager;

        container = new Table(){
            @Override
            public void act(float delta){
                super.act(delta);
                if(targetActor != null && targetActor.getScene() == null) remove();
            }
        };
        contents.get(container);
        //make scale/alpha small for fade in
        container.color.a = 0.2f;
        container.setScale(0.05f);
        container.touchable = Touchable.disabled;
    }

    public Tooltips getManager(){
        return manager;
    }

    public Table getContainer(){
        return container;
    }

    /** If true, this tooltip is shown without delay when hovered. */
    public void setInstant(boolean instant){
        this.instant = instant;
    }

    /** If true, this tooltip is shown even when tooltips are not {@link Tooltips#enabled}. */
    public void setAlways(boolean always){
        this.always = always;
    }

    @Override
    public boolean touchDown(InputEvent event, float x, float y, int pointer, KeyCode button){
        if(allowMobile && Core.app.isMobile()){
            show(event.listenerActor, x, y);
        }

        if(instant){
            container.toFront();
            return true;
        }
        manager.touchDown(this);
        return true;
    }

    @Override
    public void touchUp(InputEvent event, float x, float y, int pointer, KeyCode button){
        //hide tooltip on touch up on mobile, since
        if(Core.app.isMobile() && allowMobile){
            hide();
        }
    }

    @Override
    public boolean mouseMoved(InputEvent event, float x, float y){
        if(container.hasParent()) return false;
        setContainerPosition(event.listenerActor, x, y);
        return true;
    }

    protected void setContainerPosition(Element element, float x, float y){
        this.targetActor = element;
        Scene stage = element.getScene();
        if(stage == null) return;

        container.pack();
        float offsetX = manager.offsetX, offsetY = manager.offsetY, dist = manager.edgeDistance;
        Vec2 point = element.localToStageCoordinates(tmp.set(x + offsetX, y - offsetY - container.getHeight()));
        if(point.y < dist) point = element.localToStageCoordinates(tmp.set(x + offsetX, y + offsetY));
        if(point.x < dist) point.x = dist;
        if(point.x + container.getWidth() > stage.getWidth() - dist)
            point.x = stage.getWidth() - dist - container.getWidth();
        if(point.y + container.getHeight() > stage.getHeight() - dist)
            point.y = stage.getHeight() - dist - container.getHeight();
        container.setPosition(point.x, point.y);

        point = element.localToStageCoordinates(tmp.set(element.getWidth() / 2, element.getHeight() / 2));
        point.sub(container.x, container.y);
        container.setOrigin(point.x, point.y);
    }

    @Override
    public void enter(InputEvent event, float x, float y, int pointer, Element fromActor){
        if(Core.app.isMobile()) return;
        if((pointer != -1 || Core.input.isTouched()) && !allowMobile) return;
        Element element = event.listenerActor;
        if(fromActor != null && fromActor.isDescendantOf(element)) return;

        show(element, x, y);
    }

    @Override
    public void exit(InputEvent event, float x, float y, int pointer, Element toActor){
        //on mobile, tooltips only hide once you stop holding.
        if(allowMobile && Core.app.isMobile()) return;
        if(toActor != null && toActor.isDescendantOf(event.listenerActor)) return;
        hide();
    }

    public void show(Element element, float x, float y){
        setContainerPosition(element, x, y);
        manager.enter(this);
        container.pack();

        if(show != null) show.run();
    }

    public void hide(){
        manager.hide(this);
    }

    /**
     * Keeps track of an application's tooltips.
     * @author Nathan Sweet
     */
    public static class Tooltips{
        private static Tooltips instance;

        /** Default text tooltip provider. */
        public Func<String, Tooltip> textProvider = text -> new Tooltip(t -> t.add(text));
        /**
         * Seconds from when an element is hovered to when the tooltip is shown. Default is 2.
         */
        public float initialTime = 2;
        /** Once a tooltip is shown, this is used instead of {@link #initialTime}. Default is 0. */
        public float subsequentTime = 0;
        /** Seconds to use {@link #subsequentTime}. Default is 1.5. */
        public float resetTime = 1.5f;
        /** If false, tooltips will not be shown. Default is true. */
        public boolean enabled = true;
        /** If false, tooltips will be shown without animations. Default is true. */
        public boolean animations = false;
        /** The distance from the mouse position to offset the tooltip element. Default is 15,19. */
        public float offsetX = 15, offsetY = 19;
        /**
         * The distance from the tooltip element position to the edge of the screen where the element will be shown on the other side of
         * the mouse cursor. Default is 7.
         */
        public float edgeDistance = 7;

        float time = initialTime;
        final Task resetTask = new Task(){
            @Override
            public void run(){
                time = initialTime;
            }
        };

        Tooltip showTooltip;
        final Task showTask = new Task(){
            @Override
            public void run(){
                if(showTooltip == null) return;

                Scene stage = showTooltip.targetActor.getScene();
                if(stage == null) return;
                stage.add(showTooltip.container);
                showTooltip.container.toFront();

                showTooltip.container.clearActions();
                showAction(showTooltip);

                if(!showTooltip.instant){
                    time = subsequentTime;
                    resetTask.cancel();
                }
            }
        };

        public static Tooltips getInstance(){
            if(instance == null){
                instance = new Tooltips();
            }
            return instance;
        }

        public Tooltip create(String text){
            return textProvider.get(text);
        }

        public Tooltip create(String text, boolean mobile){
            Tooltip result = textProvider.get(text);
            result.allowMobile = mobile;
            return result;
        }

        public void touchDown(Tooltip tooltip){
            showTask.cancel();
            if(tooltip.container.remove()) resetTask.cancel();
            resetTask.run();
            if(enabled || tooltip.always){
                showTooltip = tooltip;
                Timer.schedule(showTask, time);
            }
        }

        public void enter(Tooltip tooltip){
            showTooltip = tooltip;
            showTask.cancel();
            if(enabled || tooltip.always){
                if(time == 0 || tooltip.instant)
                    showTask.run();
                else
                    Timer.schedule(showTask, time);
            }
        }

        public void hide(Tooltip tooltip){
            showTooltip = null;
            showTask.cancel();
            if(tooltip.container.hasParent()){
                hideAction(tooltip);
                resetTask.cancel();
                Timer.schedule(resetTask, resetTime);
            }
        }

        /** Called when tooltip is shown. Default implementation sets actions to animate showing. */
        protected void showAction(Tooltip tooltip){
            float actionTime = animations ? (time > 0 ? 0.5f : 0.15f) : 0.1f;
            tooltip.container.setTransform(true);
            tooltip.container.addAction(parallel(fadeIn(actionTime, fade), scaleTo(1, 1, actionTime, Interp.fade)));
        }

        /**
         * Called when tooltip is hidden. Default implementation sets actions to animate hiding and to remove the element from the stage
         * when the actions are complete. A subclass must at least remove the element.
         */
        protected void hideAction(Tooltip tooltip){
            tooltip.container
            .addAction(sequence(parallel(alpha(0.2f, 0.2f, fade), scaleTo(0.05f, 0.05f, 0.2f, Interp.fade)), remove()));
        }

        /** Shows all tooltips on hover without a delay for {@link #resetTime} seconds. */
        public void instant(){
            time = 0;
            showTask.run();
            showTask.cancel();
        }
    }
}