package arc.scene.ui.layout;

import arc.scene.*;
import arc.struct.*;

/**
 * A {@link Group} that participates in layout and provides a minimum, preferred, and maximum size.
 * <p>
 * The default preferred size of a widget group is 0 and this is almost always overridden by a subclass. The default minimum size
 * returns the preferred size, so a subclass may choose to return 0 for minimum size if it wants to allow itself to be sized
 * smaller than the preferred size. The default maximum size is 0, which means no maximum size.
 * <p>
 * See {@link Layout} for details on how a widget group should participate in layout. A widget group's mutator methods should call
 * {@link #invalidate()} or {@link #invalidateHierarchy()} as needed. By default, invalidateHierarchy is called when child widgets
 * are added and removed.
 * @author Nathan Sweet
 */
public class WidgetGroup extends Group{
    private boolean needsLayout = true;
    private boolean layoutEnabled = true;

    public WidgetGroup(){
    }

    /** Creates a new widget group containing the specified actors. */
    public WidgetGroup(Element... actors){
        for(Element actor : actors)
            addChild(actor);
    }

    @Override
    public float getMinWidth(){
        return getPrefWidth();
    }

    @Override
    public float getMinHeight(){
        return getPrefHeight();
    }

    @Override
    public float getPrefWidth(){
        return 0;
    }

    @Override
    public float getPrefHeight(){
        return 0;
    }

    @Override
    public void setLayoutEnabled(boolean enabled){
        if(layoutEnabled == enabled) return;
        layoutEnabled = enabled;
        setLayoutEnabled(this, enabled);
    }

    private void setLayoutEnabled(Group parent, boolean enabled){
        SnapshotSeq<Element> children = parent.getChildren();
        for(int i = 0, n = children.size; i < n; i++){
            children.get(i).setLayoutEnabled(enabled);
        }
    }

    @Override
    public void validate(){
        if(!layoutEnabled) return;

        Group parent = this.parent;
        if(fillParent && parent != null){
            float parentWidth = parent.getWidth();
            float parentHeight = parent.getHeight();

            if(getWidth() != parentWidth || getHeight() != parentHeight){
                setWidth(parentWidth);
                setHeight(parentHeight);
                invalidate();
            }
        }

        if(!needsLayout) return;
        needsLayout = false;
        layout();
    }

    /** Returns true if the widget's layout has been {@link #invalidate() invalidated}. */
    @Override
    public boolean needsLayout(){
        return needsLayout;
    }

    @Override
    public void invalidate(){
        needsLayout = true;
    }

    @Override
    public void invalidateHierarchy(){
        invalidate();
        Group parent = this.parent;
        if(parent != null) parent.invalidateHierarchy();
    }

    @Override
    protected void childrenChanged(){
        invalidateHierarchy();
    }

    @Override
    protected void sizeChanged(){
        invalidate();
    }

    @Override
    public void pack(){
        setSize(getPrefWidth(), getPrefHeight());
        validate();
        //Some situations require another layout. Eg, a wrapped label doesn't know its pref height until it knows its width, so it
        //calls invalidateHierarchy() in layout() if its pref height has changed.
        if(needsLayout){
            setSize(getPrefWidth(), getPrefHeight());
            validate();
        }
    }

    @Override
    public void setFillParent(boolean fillParent){
        this.fillParent = fillParent;
    }

    @Override
    public void layout(){
    }

    /**
     * If this method is overridden, the super method or {@link #validate()} should be called to ensure the widget group is laid
     * out.
     */
    @Override
    public void draw(){
        validate();
        super.draw();
    }
}
