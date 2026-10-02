package arc.scene;

import arc.graphics.g2d.*;
import arc.math.geom.*;
import arc.struct.Seq;
import arc.struct.SnapshotSeq;
import arc.func.Cons;
import arc.func.Boolf;
import arc.math.Affine2;
import arc.math.Mat;
import arc.scene.event.Touchable;
import arc.scene.style.*;
import arc.scene.ui.layout.Table;
import arc.scene.ui.layout.Table.DrawRect;
import arc.scene.utils.Cullable;

/**
 * 2D scene graph node that may contain other actors.
 * <p>
 * Actors have a z-order equal to the order they were inserted into the group. Actors inserted later will be drawn on top of
 * actors added earlier. Touch events that hit more than one actor are distributed to topmost actors first.
 * @author mzechner
 * @author Nathan Sweet
 */
public abstract class Group extends Element implements Cullable{
    private static final Vec2 tmp = new Vec2();

    protected final SnapshotSeq<Element> children = new SnapshotSeq<>(true, 4, Element.class);
    private final Affine2 worldTransform = new Affine2();
    private final Mat computedTransform = new Mat();
    private final Mat oldTransform = new Mat();
    protected boolean transform = false;
    protected Rect cullingArea;

    @Override
    public void act(float delta){
        super.act(delta);
        Element[] actors = children.begin();
        for(int i = 0, n = children.size; i < n; i++){
            actors[i].updateVisibility();
            if(actors[i].visible){
                actors[i].act(delta);
            }
        }
        children.end();
    }

    @Override
    public void draw(){
        if(transform) applyTransform(computeTransform());
        drawChildren();
        if(transform) resetTransform();
    }

    protected void drawChildren(){
        parentAlpha *= this.color.a;
        SnapshotSeq<Element> children = this.children;
        Element[] actors = children.begin();
        Rect cullingArea = this.cullingArea;
        if(cullingArea != null){
            // Draw children only if inside culling area.
            float cullLeft = cullingArea.x;
            float cullRight = cullLeft + cullingArea.width;
            float cullBottom = cullingArea.y;
            float cullTop = cullBottom + cullingArea.height;
            if(transform){
                for(int i = 0, n = children.size; i < n; i++){
                    Element child = actors[i];
                    child.parentAlpha = parentAlpha;
                    if(!child.visible) continue;
                    float cx = child.x, cy = child.y;
                    child.x += child.translation.x;
                    child.y += child.translation.y;
                    if((cx <= cullRight && cy <= cullTop && cx + child.width >= cullLeft && cy + child.height >= cullBottom) || !child.cullable)
                        child.draw();
                    child.x -= child.translation.x;
                    child.y -= child.translation.y;
                }
            }else{
                // No transform for this group, offset each child.
                float offsetX = x, offsetY = y;
                x = 0;
                y = 0;
                for(int i = 0, n = children.size; i < n; i++){
                    Element child = actors[i];
                    child.parentAlpha = parentAlpha;
                    if(!child.visible) continue;
                    float cx = child.x, cy = child.y;
                    if((cx <= cullRight && cy <= cullTop && cx + child.width >= cullLeft && cy + child.height >= cullBottom) || !child.cullable){
                        child.x = cx + offsetX + child.translation.x;
                        child.y = cy + offsetY + child.translation.y;
                        child.draw();
                        child.x = cx;
                        child.y = cy;
                    }
                }
                x = offsetX;
                y = offsetY;
            }
        }else{
            // No culling, draw all children.
            if(transform){
                for(int i = 0, n = children.size; i < n; i++){
                    Element child = actors[i];
                    child.parentAlpha = parentAlpha;
                    if(!child.visible) continue;
                    child.x += child.translation.x;
                    child.y += child.translation.y;
                    child.draw();
                    child.x -= child.translation.x;
                    child.y -= child.translation.y;
                }
            }else{
                // No transform for this group, offset each child.
                float offsetX = x, offsetY = y;
                x = 0;
                y = 0;
                for(int i = 0, n = children.size; i < n; i++){
                    Element child = actors[i];
                    child.parentAlpha = parentAlpha;
                    if(!child.visible) continue;
                    float cx = child.x, cy = child.y;
                    child.x = cx + offsetX + child.translation.x;
                    child.y = cy + offsetY + child.translation.y;
                    child.draw();
                    child.x = cx;
                    child.y = cy;
                }
                x = offsetX;
                y = offsetY;
            }
        }
        children.end();
    }

    /** Returns the transform for this group's coordinate system. */
    protected Mat computeTransform(){
        Affine2 worldTransform = this.worldTransform;
        float originX = this.originX, originY = this.originY;
        worldTransform.setToTrnRotScl(x + originX, y + originY, rotation, scaleX, scaleY);
        if(originX != 0 || originY != 0) worldTransform.translate(-originX, -originY);

        // Find the first parent that transforms.
        Group parentGroup = parent;
        while(parentGroup != null){
            if(parentGroup.transform) break;
            parentGroup = parentGroup.parent;
        }
        if(parentGroup != null) worldTransform.preMul(parentGroup.worldTransform);

        computedTransform.set(worldTransform);
        return computedTransform;
    }

    /**
     * Set the batch's transformation matrix, often with the result of {@link #computeTransform()}. Note this causes the batch to
     * be flushed. {@link #resetTransform()} will restore the transform to what it was before this call.
     */
    protected void applyTransform(Mat transform){
        oldTransform.set(Draw.trans());
        Draw.trans(transform);
    }

    /**
     * Restores the batch transform to what it was before {@link #applyTransform(Mat)}. Note this causes the batch to
     * be flushed.
     */
    protected void resetTransform(){
        Draw.trans(oldTransform);
    }

    /**
     * @return May be null.
     * @see #setCullingArea(Rect)
     */
    public Rect getCullingArea(){
        return cullingArea;
    }

    /**
     * Children completely outside of this rectangle will not be drawn. This is only valid for use with unrotated and unscaled
     * actors.
     * @param cullingArea May be null.
     */
    @Override
    public void setCullingArea(Rect cullingArea){
        this.cullingArea = cullingArea;
    }

    @Override
    public Element hit(float x, float y, boolean touchable){
        if(touchable && this.touchable == Touchable.disabled) return null;
        Vec2 point = tmp;
        Element[] childrenArray = children.items;
        for(int i = children.size - 1; i >= 0; i--){
            Element child = childrenArray[i];
            //TODO: this optimization may be incorrect, needs further testing.
            if(!child.visible || (child.cullable && cullingArea != null && !cullingArea.overlaps(child.x + child.translation.x, child.y + child.translation.y, child.width, child.height))) continue;
            child.parentToLocalCoordinates(point.set(x, y));
            Element hit = child.hit(point.x, point.y, touchable);
            if(hit != null) return hit;
        }
        return super.hit(x, y, touchable);
    }

    /** Called when actors are added to or removed from the group. */
    protected void childrenChanged(){
    }

    /** Recursively iterates through every child of this group. */
    public void forEach(Cons<Element> cons){
        for(Element e : getChildren()){
            cons.get(e);
            if(e instanceof Group){
                ((Group)e).forEach(cons);
            }
        }
    }

    public Element fill(DrawRect rect){
        Element e = new Element(){
            @Override
            public void draw(){
                rect.draw(0f, 0f, Group.this.width, Group.this.height);
            }
        };
        e.setFillParent(true);
        //usually this area is used for drawing, and should not capture touch events
        e.touchable = Touchable.disabled;
        addChild(e);
        return e;
    }

    /** Adds and returns a table. This table will fill the whole scene. */
    public void fill(Cons<Table> cons){
        fill(null, cons);
    }

    /** Adds and returns a table. This table will fill the whole scene. */
    public void fill(Drawable background, Cons<Table> cons){
        Table table = background == null ? new Table() : new Table(background);
        table.setFillParent(true);
        addChild(table);
        cons.get(table);
    }

    /**
     * Adds an actor as a child of this group, removing it from its previous parent. If the actor is already a child of this
     * group, no changes are made.
     */
    public void addChild(Element actor){
        if(actor.parent != null){
            if(actor.parent == this) return;
            actor.parent.removeChild(actor, false);
        }
        children.add(actor);
        actor.parent = this;
        actor.setScene(getScene());
        childrenChanged();
    }

    /**
     * Adds an actor as a child of this group at a specific index, removing it from its previous parent. If the actor is already a
     * child of this group, no changes are made.
     * @param index May be greater than the number of children.
     */
    public void addChildAt(int index, Element actor){
        if(actor.parent != null){
            if(actor.parent == this) return;
            actor.parent.removeChild(actor, false);
        }
        if(index >= children.size)
            children.add(actor);
        else
            children.insert(index, actor);
        actor.parent = this;
        actor.setScene(getScene());
        childrenChanged();
    }

    /**
     * Adds an actor as a child of this group immediately before another child actor, removing it from its previous parent. If the
     * actor is already a child of this group, no changes are made.
     */
    public void addChildBefore(Element actorBefore, Element actor){
        if(actor.parent != null){
            if(actor.parent == this) return;
            actor.parent.removeChild(actor, false);
        }
        int index = children.indexOf(actorBefore, true);
        children.insert(index, actor);
        actor.parent = this;
        actor.setScene(getScene());
        childrenChanged();
    }

    /**
     * Adds an actor as a child of this group immediately after another child actor, removing it from its previous parent. If the
     * actor is already a child of this group, no changes are made.
     */
    public void addChildAfter(Element actorAfter, Element actor){
        if(actor.parent != null){
            if(actor.parent == this) return;
            actor.parent.removeChild(actor, false);
        }
        int index = children.indexOf(actorAfter, true);
        if(index == children.size)
            children.add(actor);
        else
            children.insert(index + 1, actor);
        actor.parent = this;
        actor.setScene(getScene());
        childrenChanged();
    }

    /** Removes an actor from this group and unfocuses it. Calls {@link #removeChild(Element, boolean)} with true. */
    public boolean removeChild(Element actor){
        return removeChild(actor, true);
    }

    /**
     * Removes an actor from this group. If the actor will not be used again and has actions, they should be
     * {@link Element#clearActions() cleared} so the actions will be returned to their
     * {@link Action#setPool(arc.util.pooling.Pool) pool}, if any. This is not done automatically.
     * @param unfocus If true, {@link Scene#unfocus(Element)} is called.
     * @return true if the actor was removed from this group.
     */
    public boolean removeChild(Element actor, boolean unfocus){
        if(!children.remove(actor, true)) return false;
        if(unfocus){
            Scene stage = getScene();
            if(stage != null) stage.unfocus(actor);
        }
        actor.parent = null;
        actor.setScene(null);
        childrenChanged();
        return true;
    }

    /** Removes all actors from this group. */
    public void clearChildren(){
        Scene stage = getScene();
        Element[] actors = children.begin();
        for(int i = 0, n = children.size; i < n; i++){
            Element child = actors[i];
            if(stage != null) stage.unfocus(child);
            child.setScene(null);
            child.parent = null;
        }
        children.end();
        children.clear();
        childrenChanged();
    }

    /** Removes all children, actions, and listeners from this group. */
    @Override
    public void clear(){
        super.clear();
        clearChildren();
    }

    /**
     * Returns the first actor found with the specified name. Note this recursively compares the name of every actor in the
     * group.
     */
    @SuppressWarnings("unchecked")
    public <T extends Element> T find(String name){
        Seq<Element> children = this.children;
        for(int i = 0, n = children.size; i < n; i++)
            if(name.equals(children.get(i).name)) return (T)children.get(i);
        for(int i = 0, n = children.size; i < n; i++){
            Element child = children.get(i);
            if(child instanceof Group){
                Element actor = ((Group)child).find(name);
                if(actor != null) return (T)actor;
            }
        }
        return null;
    }

    /** Finds only visible elements.*/
    @SuppressWarnings("unchecked")
    public <T extends Element> T findVisible(String name){
        Seq<Element> children = this.children;
        for(int i = 0, n = children.size; i < n; i++)
            if(name.equals(children.get(i).name) && children.get(i).visible) return (T)children.get(i);
        for(int i = 0, n = children.size; i < n; i++){
            Element child = children.get(i);
            if(child instanceof Group && child.visible){
                Element actor = ((Group)child).findVisible(name);
                if(actor != null) return (T)actor;
            }
        }
        return null;
    }

    /** Find element by a predicate. */
    @SuppressWarnings("unchecked")
    public <T extends Element> T find(Boolf<Element> pred){
        Seq<Element> children = this.children;
        for(int i = 0, n = children.size; i < n; i++)
            if(pred.get(children.get(i))) return (T)children.get(i);

        for(int i = 0, n = children.size; i < n; i++){
            Element child = children.get(i);
            if(child instanceof Group){
                Element actor = ((Group)child).find(pred);
                if(actor != null) return (T)actor;
            }
        }
        return null;
    }

    @Override
    protected void setScene(Scene stage){
        super.setScene(stage);
        Element[] childrenArray = children.items;
        for(int i = 0, n = children.size; i < n; i++)
            childrenArray[i].setScene(stage); // StackOverflowError here means the group is its own ancestor.
    }

    /** Swaps two actors by index. Returns false if the swap did not occur because the indexes were out of bounds. */
    public boolean swapActor(int first, int second){
        int maxIndex = children.size;
        if(first < 0 || first >= maxIndex) return false;
        if(second < 0 || second >= maxIndex) return false;
        children.swap(first, second);
        return true;
    }

    /** Swaps two actors. Returns false if the swap did not occur because the actors are not children of this group. */
    public boolean swapActor(Element first, Element second){
        int firstIndex = children.indexOf(first, true);
        int secondIndex = children.indexOf(second, true);
        if(firstIndex == -1 || secondIndex == -1) return false;
        children.swap(firstIndex, secondIndex);
        return true;
    }

    /** Returns an ordered list of child actors in this group. */
    public SnapshotSeq<Element> getChildren(){
        return children;
    }

    public boolean hasChildren(){
        return children.size > 0;
    }

    public boolean isTransform(){
        return transform;
    }

    /**
     * When true (the default), the Batch is transformed so children are drawn in their parent's coordinate system. This has a
     * performance impact because {@link Batch#flush()} must be done before and after the transform. If the actors in a group are
     * not rotated or scaled, then the transform for the group can be set to false. In this case, each child's position will be
     * offset by the group's position for drawing, causing the children to appear in the correct location even though the Batch has
     * not been transformed.
     */
    public void setTransform(boolean transform){
        this.transform = transform;
    }

    /** Converts coordinates for this group to those of a descendant actor. The descendant does not need to be a direct child. */
    public Vec2 localToDescendantCoordinates(Element descendant, Vec2 localCoords){
        Group parent = descendant.parent;
        if(parent == null) throw new IllegalArgumentException("Child is not a descendant: " + descendant);
        // First convert to the actor's parent coordinates.
        if(parent != this) localToDescendantCoordinates(parent, localCoords);
        // Then from each parent down to the descendant.
        descendant.parentToLocalCoordinates(localCoords);
        return localCoords;
    }

    /** Returns a description of the actor hierarchy, recursively. */
    @Override
    public String toString(){
        StringBuilder buffer = new StringBuilder(128);
        toString(buffer, 1);
        buffer.setLength(buffer.length() - 1);
        return buffer.toString();
    }

    void toString(StringBuilder buffer, int indent){
        buffer.append(super.toString());
        buffer.append('\n');

        Element[] actors = children.begin();
        for(int i = 0, n = children.size; i < n; i++){
            for(int ii = 0; ii < indent; ii++)
                buffer.append("|  ");
            Element actor = actors[i];
            if(actor instanceof Group)
                ((Group)actor).toString(buffer, indent + 1);
            else{
                buffer.append(actor);
                buffer.append('\n');
            }
        }
        children.end();
    }
}
