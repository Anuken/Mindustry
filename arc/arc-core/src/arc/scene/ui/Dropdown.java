package arc.scene.ui;

import arc.*;
import arc.func.*;
import arc.graphics.*;
import arc.graphics.font.*;
import arc.input.*;
import arc.math.*;
import arc.math.geom.*;
import arc.scene.*;
import arc.scene.event.ChangeListener.*;
import arc.scene.event.*;
import arc.scene.style.*;
import arc.scene.ui.Button.*;
import arc.scene.ui.Label.*;
import arc.scene.ui.ScrollPane.*;
import arc.scene.ui.TextButton.*;
import arc.scene.ui.layout.*;
import arc.scene.utils.*;
import arc.struct.*;
import arc.util.*;
import arc.util.pooling.*;

import java.util.*;

/**
 * A button that shows the selected item and opens a scrollable list of all items when clicked, based on libGDX's SelectBox.
 * The list opens below the button, or above it when there is more room there, and closes on a click outside of it or on escape.
 * While open, the arrow keys move the highlight and enter selects the highlighted item.
 * <p>
 * A selection made by the user calls the listener and fires a {@link ChangeEvent}; {@link #setSelected} does neither.
 * The button is as wide as the widest item, so it doesn't resize when the selection changes.
 */
public class Dropdown<T> extends Table implements Disableable{
    /** Maximum number of items visible at once before the list scrolls. */
    public int maxListCount = 8;
    /** Height of each item in the list. */
    public float itemHeight = 40f;

    DropdownStyle style;
    final Seq<T> items = new Seq<>();
    @Nullable T selected;
    Func<T, String> stringifier = String::valueOf;
    Cons<T> listener = t -> {};
    boolean disabled;
    @Nullable Boolp disabledProvider;
    float maxItemWidth;

    final ClickListener clickListener;
    final Label label, measure;
    final Image arrow;
    final Cell<Image> arrowCell;
    final DropdownList list;

    public Dropdown(){
        this(Core.scene.getStyle(DropdownStyle.class));
    }

    public Dropdown(DropdownStyle style){
        touchable = Touchable.enabled;

        label = new Label("");
        label.setEllipsis(true);
        measure = new Label("");
        arrow = new Image();
        arrow.setScaling(Scaling.fit);

        add(label).growX().minWidth(0f).left();
        arrowCell = add(arrow).padLeft(8f);

        list = new DropdownList();
        setStyle(style);

        addListener(clickListener = new ClickListener(){
            @Override
            public void clicked(InputEvent event, float x, float y){
                if(isDisabled() || items.isEmpty()) return;
                if(list.isShown()){
                    list.hide();
                }else{
                    list.show();
                }
            }
        });
        addListener(new HandCursorListener());
    }

    public Dropdown(Seq<T> items, @Nullable T selected, Cons<T> listener){
        this();
        setItems(items);
        setSelected(selected);
        this.listener = listener;
    }

    public Dropdown(T[] items, @Nullable T selected, Cons<T> listener){
        this(Seq.with(items), selected, listener);
    }

    public void setStyle(DropdownStyle style){
        if(style == null) throw new IllegalArgumentException("style cannot be null.");
        this.style = style;

        LabelStyle labelStyle = new LabelStyle(style.font, Color.white);
        label.setStyle(labelStyle);
        measure.setStyle(labelStyle);
        arrow.setDrawable(style.arrow);
        arrowCell.size(style.arrowSize);
        list.setStyle(style);

        measureItems();
    }

    /** Returns the style, which is shared and should not be modified. */
    public DropdownStyle getStyle(){
        return style;
    }

    /** Sets the items, keeping the selection if it is still present and selecting the first item otherwise. */
    public void setItems(Seq<T> newItems){
        if(newItems != items){
            items.clear();
            items.addAll(newItems);
        }
        list.hide();
        measureItems();
        setSelected(selected);
    }

    @SafeVarargs
    public final void setItems(T... newItems){
        setItems(Seq.with(newItems));
    }

    /** Returns the backing item list. Call {@link #setItems(Seq)} with it after modifying it. */
    public Seq<T> getItems(){
        return items;
    }

    /** Sets how items are converted to the displayed text. Text starting with '@' is looked up in the bundle. */
    public void setStringifier(Func<T, String> stringifier){
        this.stringifier = stringifier;
        measureItems();
        updateLabel();
    }

    /** Sets the listener called when the user selects a different item. */
    public void setListener(Cons<T> listener){
        this.listener = listener;
    }

    public @Nullable T getSelected(){
        return selected;
    }

    public int getSelectedIndex(){
        return selected == null ? -1 : items.indexOf(selected, false);
    }

    /** Selects the item without notifying listeners. Items that aren't in the list select the first item instead. */
    public void setSelected(@Nullable T item){
        selected = item != null && items.contains(item, false) ? item : items.any() ? items.first() : null;
        updateLabel();
    }

    /** Selects the item at the index without notifying listeners, or clears the selection for -1. */
    public void setSelectedIndex(int index){
        selected = index < 0 ? null : items.get(index);
        updateLabel();
    }

    public boolean isOpen(){
        return list.isShown();
    }

    public void showList(){
        if(!items.isEmpty()) list.show();
    }

    public void hideList(){
        list.hide();
    }

    public ScrollPane getScrollPane(){
        return list.pane;
    }

    public Label getLabel(){
        return label;
    }

    public Image getArrow(){
        return arrow;
    }

    @Override
    public boolean isDisabled(){
        return disabled;
    }

    @Override
    public void setDisabled(boolean disabled){
        this.disabled = disabled;
        if(disabled) list.hide();
    }

    public void setDisabled(Boolp provider){
        disabledProvider = provider;
    }

    public boolean isOver(){
        return clickListener.isOver();
    }

    public boolean isPressed(){
        return clickListener.isVisualPressed();
    }

    void choose(T item){
        list.hide();
        if(Objects.equals(item, selected)) return;

        selected = item;
        updateLabel();
        listener.get(item);

        ChangeEvent event = Pools.obtain(ChangeEvent.class, ChangeEvent::new);
        fire(event);
        Pools.free(event);
    }

    private void updateLabel(){
        label.setText(selected == null ? "" : stringifier.get(selected));
    }

    private void measureItems(){
        maxItemWidth = 0f;
        for(int i = 0; i < items.size; i++){
            measure.setText(stringifier.get(items.get(i)));
            maxItemWidth = Math.max(maxItemWidth, measure.getPrefWidth());
        }
        invalidateHierarchy();
    }

    @Override
    public void act(float delta){
        super.act(delta);
        if(disabledProvider != null) setDisabled(disabledProvider.get());
    }

    @Override
    public void draw(){
        validate();

        boolean open = list.isShown(), over = isOver();

        Drawable background;
        if(disabled && style.disabled != null) background = style.disabled;
        else if(isPressed() && style.down != null) background = style.down;
        else if(open && style.checked != null) background = style.checked;
        else if(over && style.over != null) background = style.over;
        else background = style.up;
        setBackground(background);

        Color fontColor;
        if(disabled && style.disabledFontColor != null) fontColor = style.disabledFontColor;
        else if((open || over) && style.overFontColor != null) fontColor = style.overFontColor;
        else fontColor = style.fontColor;
        label.color.set(fontColor);
        arrow.color.set(fontColor);

        super.draw();
    }

    @Override
    public float getPrefWidth(){
        float width = super.getPrefWidth() + Math.max(0f, maxItemWidth - label.getPrefWidth());
        return style.up == null ? width : Math.max(width, style.up.getMinWidth());
    }

    @Override
    public float getPrefHeight(){
        float height = super.getPrefHeight();
        return style.up == null ? height : Math.max(height, style.up.getMinHeight());
    }

    boolean shownInScene(){
        if(getScene() == null) return false;
        for(Element e = this; e != null; e = e.parent){
            if(!e.visible) return false;
        }
        return true;
    }

    private class DropdownList extends Table{
        final Table content = new Table();
        final ScrollPane pane;
        final Cell<ScrollPane> paneCell;
        int highlighted = -1;
        boolean above;
        @Nullable Element lastScrollFocus;

        final InputListener hideListener = new InputListener(){
            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, KeyCode button){
                Element target = event.targetActor;
                if(target != null && (target.isDescendantOf(DropdownList.this) || target.isDescendantOf(Dropdown.this))) return false;
                hide();
                //consume the click so it doesn't reach whatever is below the list
                event.stop();
                return true;
            }

            @Override
            public boolean keyDown(InputEvent event, KeyCode key){
                switch(key){
                    case escape:
                    case back:
                        hide();
                        break;
                    case up:
                        highlight(highlighted - 1);
                        break;
                    case down:
                        highlight(highlighted + 1);
                        break;
                    case enter:
                        if(highlighted >= 0 && highlighted < items.size) choose(items.get(highlighted));
                        break;
                    default:
                        return false;
                }
                event.stop();
                return true;
            }
        };

        DropdownList(){
            touchable = Touchable.enabled;
            content.top();
            pane = new ScrollPane(content);
            pane.setScrollingDisabledX(true);
            pane.setOverscroll(false, false);
            paneCell = add(pane).growX();
        }

        void setStyle(DropdownStyle style){
            setBackground(style.listBackground);
            if(style.scrollStyle != null) pane.setStyle(style.scrollStyle);
        }

        boolean isShown(){
            return getScene() != null;
        }

        void show(){
            Scene scene = Dropdown.this.getScene();
            if(scene == null || isShown()) return;

            content.clearChildren();
            for(int i = 0; i < items.size; i++){
                content.add(new ItemButton(i)).growX().height(itemHeight).row();
            }
            highlighted = getSelectedIndex();

            lastScrollFocus = scene.getScrollFocus();
            scene.add(this);
            scene.addCaptureListener(hideListener);
            scene.setScrollFocus(pane);

            layoutList();
            if(highlighted >= 0) scrollToItem(highlighted, true);
        }

        void hide(){
            Scene scene = getScene();
            if(scene == null) return;

            scene.removeCaptureListener(hideListener);
            remove();
            if(scene.getScrollFocus() == null && lastScrollFocus != null && lastScrollFocus.getScene() != null){
                scene.setScrollFocus(lastScrollFocus);
            }
            lastScrollFocus = null;
        }

        /** Sizes the list to fit its items and the space next to the dropdown, preferring the space below. */
        void layoutList(){
            Scene scene = getScene();
            float bottom = Dropdown.this.localToStageCoordinates(Tmp.v1.setZero()).y;
            float spaceBelow = bottom, spaceAbove = scene.getHeight() - bottom - Dropdown.this.getHeight();
            float margins = getMarginTop() + getMarginBottom(), rowHeight = Scl.scl(itemHeight);
            int shown = Math.min(items.size, maxListCount);

            above = shown * rowHeight + margins > spaceBelow && spaceAbove > spaceBelow;
            int rows = Mathf.clamp((int)(((above ? spaceAbove : spaceBelow) - margins) / rowHeight), 1, shown);

            paneCell.height(rows * itemHeight);
            invalidateHierarchy();
            pack();
            setWidth(Math.max(getWidth(), Dropdown.this.getWidth()));
            validate();
            pane.validate();
            content.validate();
            updatePosition();
        }

        void updatePosition(){
            Vec2 pos = Dropdown.this.localToStageCoordinates(Tmp.v1.setZero());
            setPosition(pos.x, above ? pos.y + Dropdown.this.getHeight() : pos.y - getHeight());
            keepInStage();
        }

        void highlight(int index){
            if(items.isEmpty()) return;
            highlighted = Mathf.clamp(index, 0, items.size - 1);
            scrollToItem(highlighted, false);
        }

        void scrollToItem(int index, boolean center){
            Element item = content.getChildren().get(index);
            pane.scrollTo(item.x, item.y, item.getWidth(), item.getHeight(), false, center);
            if(center) pane.updateVisualScroll();
        }

        @Override
        public void act(float delta){
            super.act(delta);
            if(disabled || !shownInScene()){
                hide();
            }else{
                updatePosition();
            }
        }

        private class ItemButton extends TextButton{
            final int index;

            ItemButton(int index){
                super(stringifier.get(items.get(index)), Dropdown.this.style.itemStyle);
                this.index = index;
                getLabel().setAlignment(Align.left);
                getLabelCell().padLeft(10f).padRight(10f);
                clicked(() -> choose(items.get(index)));
                hovered(() -> highlighted = index);
            }

            @Override
            public boolean isOver(){
                return highlighted == index;
            }

            @Override
            public boolean isChecked(){
                return Objects.equals(items.get(index), selected);
            }
        }
    }

    /**
     * The style for a {@link Dropdown}. The inherited drawables are used for the button, with {@link #checked} shown while the list is open.
     */
    public static class DropdownStyle extends ButtonStyle{
        public Font font;
        public Color fontColor = Color.white;
        /** Optional. Used while hovered or open. */
        public @Nullable Color overFontColor, disabledFontColor;
        /** Optional. The icon at the right of the button. */
        public @Nullable Drawable arrow;
        public float arrowSize = 16f;
        /** Optional. The background of the opened list. */
        public @Nullable Drawable listBackground;
        /** Optional. */
        public @Nullable ScrollPaneStyle scrollStyle;
        /** The style of each item in the list; the checked drawable marks the selected item and over marks the highlighted one. */
        public TextButtonStyle itemStyle;

        public DropdownStyle(){
        }

        public DropdownStyle(DropdownStyle style){
            super(style);
            font = style.font;
            fontColor = style.fontColor;
            overFontColor = style.overFontColor;
            disabledFontColor = style.disabledFontColor;
            arrow = style.arrow;
            arrowSize = style.arrowSize;
            listBackground = style.listBackground;
            scrollStyle = style.scrollStyle;
            itemStyle = style.itemStyle;
        }
    }
}
