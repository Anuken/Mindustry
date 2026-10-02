package arc.scene.ui.layout;

import arc.func.*;
import arc.graphics.*;
import arc.scene.*;
import arc.scene.event.*;
import arc.scene.style.*;
import arc.scene.ui.*;
import arc.scene.ui.Button.*;
import arc.scene.ui.Label.*;
import arc.scene.ui.ScrollPane.*;
import arc.scene.ui.TextField.*;
import arc.scene.ui.Tooltip.*;
import arc.scene.utils.*;
import arc.util.*;
import arc.util.pooling.Pool.*;

/**
 * A cell for a {@link Table}.
 * @author Nathan Sweet
 */
public class Cell<T extends Element> implements Poolable{
    private static boolean dset;
    private static Cell defaults;
    static final float unset = Float.NEGATIVE_INFINITY;

    float minWidth, minHeight;
    float maxWidth, maxHeight;
    float padTop, padLeft, padBottom, padRight;
    float fillX, fillY;
    int align;
    int expandX, expandY;
    int colspan;
    boolean uniformX, uniformY;

    Element element;
    float elementX, elementY;
    float elementWidth, elementHeight;
    boolean endRow;
    int column, row;
    int cellAboveIndex;
    float computedPadTop, computedPadLeft, computedPadBottom, computedPadRight;
    private Table table;

    public Cell(){
        reset();
    }

    /**
     * Returns the defaults to use for all cells. This can be used to avoid needing to set the same defaults for every table (eg,
     * for spacing).
     */
    public static Cell defaults(){
        if(!dset){
            dset = true;
            defaults = new Cell();
            defaults.minWidth = unset;
            defaults.minHeight = unset;
            defaults.maxWidth = unset;
            defaults.maxHeight = unset;
            defaults.padTop = 0;
            defaults.padLeft = 0;
            defaults.padBottom = 0;
            defaults.padRight = 0;
            defaults.fillX = 0;
            defaults.fillY = 0;
            defaults.align = 0;
            defaults.expandX = 0;
            defaults.expandY = 0;
            defaults.colspan = 1;
            defaults.uniformX = false;
            defaults.uniformY = false;
        }
        return defaults;
    }

    public void setLayout(Table table){
        this.table = table;
    }

    /** Sets the element in this cell and adds the element to the cell's table. If null, removes any current element. */
    public <A extends Element> Cell<A> setElement(A newElement){
        if(element != newElement){
            if(element != null) element.remove();
            element = newElement;
            if(newElement != null) table.addChild(newElement);
        }
        return (Cell<A>)this;
    }

    /** Removes the current element for the cell, if any. */
    public Cell<T> clearElement(){
        setElement(null);
        return this;
    }

    public Cell<T> with(Cons<T> c){
        c.get((T)element);
        return this;
    }

    public Cell<T> self(Cons<Cell<T>> c){
        c.get(this);
        return this;
    }

    /** getElement shortcut */
    public T get(){
        return (T)element;
    }

    /** Returns true if the cell's element is not null. */
    public boolean hasElement(){
        return element != null;
    }

    public float prefWidth(){
        return element == null ? 0f : element.getPrefWidth();
    }

    public float prefHeight(){
        return element == null ? 0f : element.getPrefHeight();
    }

    public float maxWidth(){
        return maxWidth == unset ? element == null ? 0 : element.getMaxWidth() : maxWidth;
    }

    public float maxHeight(){
        return maxHeight == unset ? element == null ? 0 : element.getMaxHeight() : maxHeight;
    }

    public float minWidth(){
        return minWidth == unset ? element == null ? 0 : element.getMinWidth() : minWidth;
    }

    public float minHeight(){
        return minHeight == unset ? element == null ? 0 : element.getMinHeight() : minHeight;
    }

    public Cell<T> tooltip(String text){
        return tooltip(text, false);
    }

    public Cell<T> tooltip(String text, boolean mobile){
        element.addListener(Tooltips.getInstance().create(text, mobile));
        return this;
    }

    public Cell<T> tooltip(Cons<Table> builder){
        element.addListener(new Tooltip(builder));
        return this;
    }

    public Cell<T> tooltip(boolean mobile, Cons<Table> builder){
        Tooltip tt = new Tooltip(builder);
        tt.allowMobile = mobile;
        element.addListener(tt);
        return this;
    }

    /** Sets the minWidth, prefWidth, maxWidth, minHeight, prefHeight, and maxHeight to the specified value. */
    public Cell<T> size(float size){
        minWidth = minHeight = maxWidth = maxHeight = scl(size);
        return this;
    }

    /** Sets the minWidth, prefWidth, maxWidth, minHeight, prefHeight, and maxHeight to the specified values. */
    public Cell<T> size(float width, float height){
        minWidth = maxWidth = scl(width);
        minHeight = maxHeight = scl(height);
        return this;
    }

    public Cell<T> name(String name){
        get().name = name;
        return this;
    }

    public Cell<T> update(Cons<T> updater){
        T t = get();
        t.update(() -> updater.get(t));
        return this;
    }

    public Cell<T> disabled(Boolf<T> vis){
        if(element instanceof Button){
            T t = get();
            ((Button)element).setDisabled(() -> vis.get(t));
        }else if(element instanceof Disableable){
            T t = get();
            element.update(() -> ((Disableable)element).setDisabled(vis.get(t)));
        }
        return this;
    }

    public Cell<T> scrollX(boolean allowScrollX){
        if(element instanceof ScrollPane){
            ((ScrollPane)element).setScrollingDisabledX(!allowScrollX);
        }
        return this;
    }

    public Cell<T> scrollY(boolean allowScrollY){
        if(element instanceof ScrollPane){
            ((ScrollPane)element).setScrollingDisabledY(!allowScrollY);
        }
        return this;
    }

    public Cell<T> disabled(boolean disabled){
        if(get() instanceof Disableable){
            ((Disableable)get()).setDisabled(disabled);
        }
        return this;
    }

    public Cell<T> touchable(Touchable touchable){
        get().touchable = touchable;
        return this;
    }

    public Cell<T> touchable(Prov<Touchable> touchable){
        get().touchable(touchable);
        return this;
    }

    public Cell<T> visible(Boolp prov){
        get().visible(prov);
        return this;
    }

    public Cell<T> visible(boolean visible){
        get().visible = visible;
        return this;
    }

    public Cell<T> scaling(Scaling scaling){
        if(element instanceof Image){
            ((Image)element).setScaling(scaling);
        }
        return this;
    }

    public Cell<T> valid(TextFieldValidator val){
        if(element instanceof TextField){
            ((TextField)element).setValidator(val);
        }
        return this;
    }

    /** For text fields. */
    public Cell<T> maxTextLength(int length){
        if(element instanceof TextField){
            ((TextField)element).setMaxLength(length);
        }
        return this;
    }

    public Cell<T> wrap(){
        return wrap(false);
    }

    /** @param wrapFit if true, labels will have a prefWidth of 0, and need to have an explicit wrap size set. if false, they will occupy all horizontal space unless an explicit size is set. */
    public Cell<T> wrap(boolean wrapFit){
        if(get() instanceof Label){
            ((Label)get()).setWrap(true, wrapFit);
        }else if(get() instanceof TextButton){
            ((TextButton)get()).getLabel().setWrap(true, wrapFit);
        }
        return this;
    }

    public Cell<T> ellipsis(boolean ellipsis){
        if(get() instanceof Label){
            ((Label)get()).setEllipsis(ellipsis);
        }else if(get() instanceof TextButton){
            ((TextButton)get()).getLabel().setEllipsis(ellipsis);
        }
        return this;
    }

    public Cell<T> labelAlign(int label, int line){
        if(get() instanceof Label){
            ((Label)get()).setAlignment(label, line);
        }
        return this;
    }

    public Cell<T> labelAlign(int label){
        return labelAlign(label, label);
    }

    public <N extends Button> Cell<T> group(ButtonGroup<N> group){
        if(get() instanceof Button){
            group.add((N)get());
        }
        return this;
    }

    public Cell<T> wrapLabel(boolean toggle){
        if(get() instanceof TextButton){
            ((TextButton)(get())).getLabel().setWrap(toggle);
        }
        return this;
    }

    public Cell<T> checked(boolean toggle){
        if(get() instanceof Button){
            ((Button)(get())).setChecked(toggle);
        }
        return this;
    }

    public Cell<T> checked(Boolf<T> toggle){
        T t = get();
        if(t instanceof Button){
            t.update(() -> ((Button)t).setChecked(toggle.get(t)));
        }
        return this;
    }

    public Cell<T> fontScale(float scale){
        if(element instanceof Label){
            ((Label)element).setFontScale(scale);
        }
        return this;
    }

    public Cell<T> color(Color color){
        get().setColor(color);
        return this;
    }

    public Cell<T> margin(float margin){
        if(get() instanceof Table){
            ((Table) get()).margin(margin);
        }
        return this;
    }

    public Cell<T> marginTop(float margin){
        if(get() instanceof Table){
            ((Table) get()).marginTop(margin);
        }
        return this;
    }

    public Cell<T> marginBottom(float margin){
        if(get() instanceof Table){
            ((Table) get()).marginBottom(margin);
        }
        return this;
    }

    public Cell<T> marginLeft(float margin){
        if(get() instanceof Table){
            ((Table) get()).marginLeft(margin);
        }
        return this;
    }

    public Cell<T> marginRight(float margin){
        if(get() instanceof Table){
            ((Table) get()).marginRight(margin);
        }
        return this;
    }

    /** Sets the minWidth, prefWidth, and maxWidth to the specified value. */
    public Cell<T> width(float width){
        minWidth = maxWidth = scl(width);
        return this;
    }

    /** Sets the button or label style.*/
    public Cell<T> style(Style style){
        if(style == null) return this;
        //copy styles to prevent extra mutation
        if(element instanceof Label){
            ((Label)element).setStyle(new LabelStyle((LabelStyle)style));
        }else if(element instanceof Button){
            ((Button)element).setStyle(new ButtonStyle((ButtonStyle)style));
        }else if(element instanceof ScrollPane){
            ((ScrollPane)element).setStyle(new ScrollPaneStyle((ScrollPaneStyle)style));
        }
        return this;
    }

    /** Sets the minHeight, prefHeight, and maxHeight to the specified value. */
    public Cell<T> height(float height){
        minHeight = maxHeight = scl(height);
        return this;
    }

    /** Sets the minWidth and minHeight to the specified value. */
    public Cell<T> minSize(float size){
        minWidth = minHeight = scl(size);
        return this;
    }

    /** Sets the minWidth and minHeight to the specified values. */
    public Cell<T> minSize(float width, float height){
        minWidth = scl(width);
        minHeight = scl(height);
        return this;
    }

    public Cell<T> minWidth(float minWidth){
        this.minWidth = scl(minWidth);
        return this;
    }

    public Cell<T> minHeight(float minHeight){
        this.minHeight = scl(minHeight);
        return this;
    }

    /** Sets the maxWidth and maxHeight to the specified value. */
    public Cell<T> maxSize(float size){
        maxWidth = maxHeight = scl(size);
        return this;
    }

    /** Sets the maxWidth and maxHeight to the specified values. */
    public Cell<T> maxSize(float width, float height){
        maxWidth = scl(width);
        maxHeight = scl(height);
        return this;
    }

    public Cell<T> maxWidth(float maxWidth){
        this.maxWidth = scl(maxWidth);
        return this;
    }

    public Cell<T> maxHeight(float maxHeight){
        this.maxHeight = scl(maxHeight);
        return this;
    }

    /** Sets the marginTop, marginLeft, marginBottom, and marginRight to the specified value. */
    public Cell<T> pad(float pad){
        padTop = padLeft = padBottom = padRight = scl(pad);
        return this;
    }

    public Cell<T> pad(float top, float left, float bottom, float right){
        padTop = scl(top);
        padLeft = scl(left);
        padBottom = scl(bottom);
        padRight = scl(right);
        return this;
    }

    public Cell<T> padTop(float padTop){
        this.padTop = scl(padTop);
        return this;
    }

    public Cell<T> padLeft(float padLeft){
        this.padLeft = scl(padLeft);
        return this;
    }

    public Cell<T> padBottom(float padBottom){
        this.padBottom = scl(padBottom);
        return this;
    }

    public Cell<T> padRight(float padRight){
        this.padRight = scl(padRight);
        return this;
    }

    /** Sets fillX and fillY to 1. */
    public Cell<T> fill(){
        fillX = 1;
        fillY = 1;
        return this;
    }

    /** Sets fillX to 1. */
    public Cell<T> fillX(){
        fillX = 1;
        return this;
    }

    /** Sets fillY to 1. */
    public Cell<T> fillY(){
        fillY = 1;
        return this;
    }

    public Cell<T> fill(float x, float y){
        fillX = x;
        fillY = y;
        return this;
    }

    /** Sets fillX and fillY to 1 if true, 0 if false. */
    public Cell<T> fill(boolean x, boolean y){
        fillX = x ? 1 : 0;
        fillY = y ? 1 : 0;
        return this;
    }

    /** Sets fillX and fillY to 1 if true, 0 if false. */
    public Cell<T> fill(boolean fill){
        fillX = fill ? 1 : 0;
        fillY = fill ? 1 : 0;
        return this;
    }

    /**
     * Sets the alignment of the element within the cell. Set to {@link Align#center}, {@link Align#top}, {@link Align#bottom},
     * {@link Align#left}, {@link Align#right}, or any combination of those.
     */
    public Cell<T> align(int align){
        this.align = align;
        return this;
    }

    /** Sets the alignment of the element within the cell to {@link Align#center}. This clears any other alignment. */
    public Cell<T> center(){
        align = Align.center;
        return this;
    }

    /** Adds {@link Align#top} and clears {@link Align#bottom} for the alignment of the element within the cell. */
    public Cell<T> top(){
        align = (align | Align.top) & ~Align.bottom;
        return this;
    }

    /** Adds {@link Align#left} and clears {@link Align#right} for the alignment of the element within the cell. */
    public Cell<T> left(){
        align = (align | Align.left) & ~Align.right;
        return this;
    }

    /** Adds {@link Align#bottom} and clears {@link Align#top} for the alignment of the element within the cell. */
    public Cell<T> bottom(){
        align = (align | Align.bottom) & ~Align.top;
        return this;
    }

    /** Adds {@link Align#right} and clears {@link Align#left} for the alignment of the element within the cell. */
    public Cell<T> right(){
        align = (align | Align.right) & ~Align.left;
        return this;
    }

    /** Sets expandX, expandY, fillX, and fillY to 1. */
    public Cell<T> grow(){
        expandX = 1;
        expandY = 1;
        fillX = 1;
        fillY = 1;
        return this;
    }

    /** Sets expandX and fillX to 1. */
    public Cell<T> growX(){
        expandX = 1;
        fillX = 1;
        return this;
    }

    /** Sets expandY and fillY to 1. */
    public Cell<T> growY(){
        expandY = 1;
        fillY = 1;
        return this;
    }

    /** Sets expandX and expandY to 1. */
    public Cell<T> expand(){
        expandX = 1;
        expandY = 1;
        return this;
    }

    /** Sets expandX to 1. */
    public Cell<T> expandX(){
        expandX = 1;
        return this;
    }

    /** Sets expandY to 1. */
    public Cell<T> expandY(){
        expandY = 1;
        return this;
    }

    public Cell<T> expand(int x, int y){
        expandX = x;
        expandY = y;
        return this;
    }

    /** Sets expandX and expandY to 1 if true, 0 if false. */
    public Cell<T> expand(boolean x, boolean y){
        expandX = x ? 1 : 0;
        expandY = y ? 1 : 0;
        return this;
    }

    public Cell<T> colspan(int colspan){
        this.colspan = colspan;
        return this;
    }

    /** Sets uniformX and uniformY to true. */
    public Cell<T> uniform(){
        uniformX = true;
        uniformY = true;
        return this;
    }

    /** Sets uniformX to true. */
    public Cell<T> uniformX(){
        uniformX = true;
        return this;
    }

    /** Sets uniformY to true. */
    public Cell<T> uniformY(){
        uniformY = true;
        return this;
    }

    public Cell<T> uniform(boolean x, boolean y){
        uniformX = x;
        uniformY = y;
        return this;
    }

    public void setBounds(float x, float y, float width, float height){
        elementX = x;
        elementY = y;
        elementWidth = width;
        elementHeight = height;
    }

    public boolean isEndRow(){
        return endRow;
    }

    public void row(){
        table.row();
    }

    public Table getTable(){
        return table;
    }

    /** Sets all constraint fields to null. */
    void clear(){
        minWidth = unset;
        minHeight = unset;
        maxWidth = unset;
        maxHeight = unset;
        padTop = 0;
        padLeft = 0;
        padBottom = 0;
        padRight = 0;
        fillX = 0;
        fillY = 0;
        align = 0;
        expandX = 0;
        expandY = 0;
        colspan = 1;
        uniformX = false;
        uniformY = false;
    }

    float scl(float value){
        return Scl.scl(value);
    }

    /** Reset state so the cell can be reused, setting all constraints to their {@link #defaults() default} values. */
    @Override
    public void reset(){
        element = null;
        table = null;
        endRow = false;
        cellAboveIndex = -1;

        Cell defaults = defaults();
        if(defaults != null) set(defaults);
    }

    public Cell<T> set(Cell cell){
        minWidth = cell.minWidth;
        minHeight = cell.minHeight;
        maxWidth = cell.maxWidth;
        maxHeight = cell.maxHeight;
        padTop = cell.padTop;
        padLeft = cell.padLeft;
        padBottom = cell.padBottom;
        padRight = cell.padRight;
        fillX = cell.fillX;
        fillY = cell.fillY;
        align = cell.align;
        expandX = cell.expandX;
        expandY = cell.expandY;
        colspan = cell.colspan;
        uniformX = cell.uniformX;
        uniformY = cell.uniformY;
        return this;
    }

    public String toString(){
        return element != null ? element.toString() : super.toString();
    }
}
