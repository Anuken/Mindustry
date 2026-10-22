package arc.scene.ui.layout;

import arc.func.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.scene.*;
import arc.scene.event.*;
import arc.scene.style.*;
import arc.scene.ui.*;
import arc.scene.ui.Button.*;
import arc.scene.ui.ImageButton.*;
import arc.scene.ui.Label.*;
import arc.scene.ui.ScrollPane.*;
import arc.scene.ui.TextButton.*;
import arc.scene.ui.TextField.*;
import arc.struct.*;
import arc.util.*;
import arc.util.pooling.*;

import static arc.Core.*;
import static arc.scene.ui.layout.Cell.*;

/**
 * A group that sizes and positions children using table constraints. By default, touchable is
 * {@link Touchable#childrenOnly}.
 * <p>
 * The preferred and minimum sizes are that of the children when laid out in columns and rows.
 * @author Nathan Sweet
 */
public class Table extends WidgetGroup{
    private static float[] columnWeightedWidth, rowWeightedHeight;
    private static Pool<Cell> cellPool = Pools.get(Cell.class, Cell::new);

    protected final Seq<Cell> cells = new Seq<>(4);
    protected final Cell cellDefaults;

    float marginTop = unset, marginLeft = unset, marginBot = unset, marginRight = unset;
    int align = Align.center;
    Drawable background;
    boolean round = true;

    protected int columns, rows;
    protected boolean implicitEndRow;
    protected Cell rowDefaults;
    protected boolean sizeInvalid = true;
    protected float[] columnMinWidth, rowMinHeight;
    protected float[] columnPrefWidth, rowPrefHeight;
    protected float tableMinWidth, tableMinHeight;
    protected float tablePrefWidth, tablePrefHeight;
    protected float[] columnWidth, rowHeight;
    protected float[] expandWidth, expandHeight;
    protected boolean clip;

    public Table(){
        cellDefaults = obtainCell();

        setTransform(false);
        this.touchable = Touchable.childrenOnly;
    }

    public Table(Drawable background){
        this();

        background(background);
    }

    public Table(Drawable background, Cons<Table> cons){
        this(background);
        cons.get(this);
    }

    public Table(Cons<Table> cons){
        this();
        cons.get(this);
    }

    protected Cell obtainCell(){
        Cell cell = cellPool.obtain();
        cell.setLayout(this);
        return cell;
    }

    /** Adds and fills a new table within this one. */
    public Table fill(){
        Table table = new Table();
        table.setFillParent(true);
        add(table);
        return table;
    }

    @Override
    public void draw(){
        validate();
        if(isTransform()){
            applyTransform(computeTransform());
            drawBackground(0, 0);
            if(clip){
                Draw.flush();
                float padLeft = getMarginLeft(), padBottom = getMarginBottom();
                if(clipBegin(padLeft, padBottom, getWidth() - padLeft - getMarginRight(),
                getHeight() - padBottom - getMarginTop())){
                    drawChildren();
                    Draw.flush();
                    clipEnd();
                }
            }else{
                drawChildren();
            }
            resetTransform();
        }else{
            drawBackground(x, y);
            super.draw();
        }
    }

    /**
     * Called to draw the background, before clipping is applied (if enabled). Default implementation draws the background
     * drawable.
     */
    protected void drawBackground(float x, float y){
        if(background == null) return;
        Color color = this.color;
        Draw.color(color.r, color.g, color.b, color.a * parentAlpha);
        background.draw(x, y, width, height);
    }

    /** @see #setBackground(Drawable) */
    public Table background(Drawable background){
        setBackground(background);
        return this;
    }

    public Drawable getBackground(){
        return background;
    }

    /** @param background May be null to clear the background. */
    public void setBackground(Drawable background){
        if(this.background == background) return;
        float padTopOld = getMarginTop(), padLeftOld = getMarginLeft(), padBottomOld = getMarginBottom(), padRightOld = getMarginRight();
        this.background = background; // The default margin values use the background's padding.
        float padTopNew = getMarginTop(), padLeftNew = getMarginLeft(), padBottomNew = getMarginBottom(), padRightNew = getMarginRight();
        if(padTopOld + padBottomOld != padTopNew + padBottomNew || padLeftOld + padRightOld != padLeftNew + padRightNew)
            invalidateHierarchy();
        else if(padTopOld != padTopNew || padLeftOld != padLeftNew || padBottomOld != padBottomNew || padRightOld != padRightNew)
            invalidate();
    }

    @Override
    public Element hit(float x, float y, boolean touchable){
        if(clip){
            if(touchable && this.touchable == Touchable.disabled) return null;
            if(x < 0 || x >= getWidth() || y < 0 || y >= getHeight()) return null;
        }
        return super.hit(x, y, touchable);
    }

    public boolean getClip(){
        return clip;
    }

    /**
     * Causes the contents to be clipped if they exceed the table element's bounds. Enabling clipping will set
     * {@link #setTransform(boolean)} to true.
     */
    public void setClip(boolean enabled){
        clip = enabled;
        setTransform(enabled);
        invalidate();
    }

    @Override
    public void invalidate(){
        sizeInvalid = true;
        super.invalidate();
    }

    /** Adds a new cell to the table with the specified element. */
    public <T extends Element> Cell<T> add(T element){
        Cell<T> cell = obtainCell();
        cell.element = element;

        // The row was ended for layout, not by the user, so revert it.
        if(implicitEndRow){
            implicitEndRow = false;
            rows--;
            cells.peek().endRow = false;
        }

        Seq<Cell> cells = this.cells;
        int cellCount = cells.size;
        if(cellCount > 0){
            // Set cell column and row.
            Cell lastCell = cells.peek();
            if(!lastCell.endRow){
                cell.column = lastCell.column + lastCell.colspan;
                cell.row = lastCell.row;
            }else{
                cell.column = 0;
                cell.row = lastCell.row + 1;
            }
            // Set the index of the cell above.
            if(cell.row > 0){
                outer:
                for(int i = cellCount - 1; i >= 0; i--){
                    Cell other = cells.get(i);
                    for(int column = other.column, nn = column + other.colspan; column < nn; column++){
                        if(column == cell.column){
                            cell.cellAboveIndex = i;
                            break outer;
                        }
                    }
                }
            }
        }else{
            cell.column = 0;
            cell.row = 0;
        }
        cells.add(cell);

        cell.set(cellDefaults);

        if(element != null) addChild(element);

        return cell;
    }

    public void add(Element... elements){
        for(Element element : elements) add(element);
    }

    public Cell<Collapser> collapser(Cons<Table> cons, Boolp shown){
        return collapser(cons, false, shown);
    }

    public Cell<Collapser> collapser(Table table, Boolp shown){
        return collapser(table, false, shown);
    }

    public Cell<Collapser> collapser(Cons<Table> cons, boolean animate, Boolp shown){
        Collapser col = new Collapser(cons, !shown.get());
        col.setCollapsed(animate, () -> !shown.get());
        return add(col);
    }

    public Cell<Collapser> collapser(Table table, boolean animate, Boolp shown){
        Collapser col = new Collapser(table, !shown.get());
        col.setCollapsed(animate, () -> !shown.get());
        return add(col);
    }

    public Cell<Table> table(){
        return table((Drawable)null);
    }

    public Cell<Table> table(Drawable background){
        Table table = new Table(background);
        return add(table);
    }

    public Cell<Table> table(Cons<Table> cons){
        Table table = new Table();
        cons.get(table);
        return add(table);
    }

    public Cell<WrapTable> wrapTable(Cons<WrapTable> cons){
        WrapTable table = new WrapTable();
        cons.get(table);
        return add(table);
    }

    public Cell<Table> table(Drawable background, Cons<Table> cons){
        return table(background, Align.center, cons);
    }

    public Cell<Table> table(Drawable background, int align, Cons<Table> cons){
        Table table = new Table(background);
        table.align(align);
        cons.get(table);
        return add(table);
    }

    public Cell<Label> label(Prov<CharSequence> text){
        return add(new Label(text));
    }

    public Cell<Label> labelWrap(Prov<CharSequence> text){
        Label label = new Label(text);
        label.setWrap(true);
        return add(label);
    }

    public Cell<Label> labelWrap(String text){
        Label label = new Label(text);
        label.setWrap(true);
        return add(label);
    }

    public Cell<ScrollPane> pane(Cons<Table> consumer){
        return pane(scene.getStyle(ScrollPaneStyle.class), consumer);
    }

    public Cell<ScrollPane> pane(ScrollPaneStyle style, Cons<Table> consumer){
        Table table = new Table();
        consumer.get(table);
        ScrollPane pane = new ScrollPane(table, style);
        return add(pane);
    }

    public Cell<ScrollPane> pane(ScrollPaneStyle style, Element element){
        ScrollPane pane = new ScrollPane(element, style);
        return add(pane);
    }

    public Cell<ScrollPane> pane(Element element){
        return pane(scene.getStyle(ScrollPaneStyle.class), element);
    }

    /** Adds a new cell with a label. */
    public Cell<Label> add(CharSequence text){
        return add(new Label(text));
    }

    /** Adds a new cell with a label. */
    public Cell<Label> add(CharSequence text, float scl){
        Label l = new Label(text);
        l.setFontScale(scl);
        return add(l);
    }

    /** Adds a new cell with a label. */
    public Cell<Label> add(CharSequence text, LabelStyle labelStyle, float scl){
        Label l = new Label(text, labelStyle);
        l.setFontScale(scl);
        return add(l);
    }

    public Cell<Label> add(CharSequence text, Color color, float scl){
        Label l = new Label(text);
        l.setColor(color);
        l.setFontScale(scl);
        return add(l);
    }

    /** Adds a new cell with a label. */
    public Cell<Label> add(CharSequence text, LabelStyle labelStyle){
        return add(new Label(text, labelStyle));
    }

    /** Adds a new cell with a label. */
    public Cell<Label> add(CharSequence text, Color color){
        return add(new Label(text, new LabelStyle(scene.getStyle(LabelStyle.class).font, color)));
    }

    /** Adds a cell without an element. */
    public Cell add(){
        return add((Element)null);
    }

    /**
     * Adds a new cell to the table with the specified elements in a {@link Stack}.
     * @param elements May be null to add a stack without any elements.
     */
    public Cell<Stack> stack(Element... elements){
        Stack stack = new Stack();
        if(elements != null){
            for(int i = 0, n = elements.length; i < n; i++)
                stack.addChild(elements[i]);
        }
        return add(stack);
    }

    public Cell<Spacer> spacer(Floatp width, Floatp height){
        return add(new Spacer(width, height));
    }

    public Cell<Spacer> spacerX(Floatp width){
        return add(new Spacer(width, () -> 0f));
    }

    public Cell<Spacer> spacerY(Floatp height){
        return add(new Spacer(() -> 0f, height));
    }

    public Cell<Image> image(Prov<TextureRegion> reg){
        return add(new Image(reg.get())).update(i -> {
            ((TextureRegionDrawable)i.getDrawable()).setRegion(reg.get());
            i.layout();
        });
    }

    public Cell<Image> imageDraw(Prov<Drawable> reg){
        return add(new Image(reg.get())).update(i -> {
            i.setDrawable(reg.get());
            i.layout();
        });
    }

    public Cell<Image> image(){
        return add(new Image());
    }

    public Cell<Image> image(Drawable name){
        return add(new Image(name));
    }

    public Cell<Image> image(Drawable name, Color color){
        Image image = new Image(name);
        image.setColor(color);
        return add(image);
    }

    public Cell<Image> image(TextureRegion region){
        return add(new Image(region));
    }

    public Cell<CheckBox> check(String text, Boolc listener){
        CheckBox button = new CheckBox(text, listener);
        return add(button);
    }

    public Cell<CheckBox> check(String text, boolean checked, Boolc listener){
        CheckBox button = new CheckBox(text, listener);
        button.setChecked(checked);
        return add(button);
    }

    public Cell<CheckBox> check(String text, float imagesize, boolean checked, Boolc listener){
        CheckBox button = new CheckBox(text, listener);
        button.getImageCell().size(imagesize);
        button.setChecked(checked);
        return add(button);
    }

    public Cell<Button> button(Cons<Button> cons, Runnable listener){
        Button button = new Button();
        button.clearChildren();
        button.clicked(listener);
        cons.get(button);
        return add(button);
    }

    public Cell<Button> button(Cons<Button> cons, ButtonStyle style, Runnable listener){
        Button button = new Button(style);
        button.clearChildren();
        button.clicked(listener);
        cons.get(button);
        return add(button);
    }

    public Cell<TextButton> button(String text, Runnable listener){
        TextButton button = new TextButton(text);
        if(listener != null) button.changed(listener);
        return add(button);
    }

    public Cell<TextButton> button(String text, TextButtonStyle style, Runnable listener){
        TextButton button = new TextButton(text, style);
        if(listener != null) button.changed(listener);

        return add(button);
    }

    public Cell<ImageButton> button(Drawable icon, Runnable listener){
        ImageButton button = new ImageButton(icon);
        if(listener != null) button.changed(listener);
        return add(button);
    }

    public Cell<ImageButton> button(Drawable icon, float isize, Runnable listener){
        ImageButton button = new ImageButton(icon);
        if(listener != null) button.changed(listener);
        button.resizeImage(isize);
        return add(button);
    }

    public Cell<ImageButton> button(Drawable icon, ImageButtonStyle style, float isize, Runnable listener){
        ImageButton button = new ImageButton(icon, style);
        button.clicked(listener);
        button.resizeImage(isize);
        return add(button);
    }

    public Cell<ImageButton> button(Drawable icon, ImageButtonStyle style, Runnable listener){
        ImageButton button = new ImageButton(icon, style);
        button.clicked(listener);
        button.resizeImage(icon.imageSize());
        return add(button);
    }

    public Cell<TextField> field(String text, Cons<String> listener){
        TextField field = new TextField(text, listener);
        return add(field);
    }

    public Cell<TextArea> area(String text, Cons<String> listener){
        TextArea area = new TextArea(text);
        area.changed(() -> listener.get(area.getText()));
        return add(area);
    }

    public Cell<TextArea> area(String text, TextFieldStyle style, Cons<String> listener){
        TextArea area = new TextArea(text, style);
        area.changed(() -> listener.get(area.getText()));
        return add(area);
    }

    public Cell<TextField> field(String text, TextFieldFilter filter, Cons<String> listener){
        TextField field = new TextField(text, listener);
        field.setFilter(filter);
        return add(field);
    }

    public Cell<TextField> field(String text, TextFieldStyle style, Cons<String> listener){
        TextField field = new TextField(text, listener);
        field.setStyle(style);
        return add(field);
    }

    public Cell<?> rect(DrawRect draw){
        return add(new Element(){
            @Override
            public void draw(){
                draw.draw(x, y, getWidth(), getHeight());
            }
        });
    }

    public Cell<TextButton> buttonRow(String text, Drawable image, Runnable clicked){
        TextButton button = new TextButton(text);
        button.clearChildren();
        button.add(new Image(image)).update(i -> i.setColor(button.isDisabled() ? Color.gray : Color.white));
        button.row();
        button.add(button.getLabel()).padTop(4).padLeft(4).padRight(4).wrap().growX();
        button.clicked(clicked);
        return add(button);
    }

    public Cell<TextButton> button(String text, Drawable image, Runnable clicked){
        return button(text, image, image.imageSize(), clicked);
    }

    public Cell<TextButton> button(String text, Drawable image, float imagesize, Runnable clicked){
        return button(text, image, scene.getStyle(TextButtonStyle.class), imagesize, clicked);
    }

    public Cell<TextButton> button(String text, Drawable image, TextButtonStyle style, float imagesize, Runnable clicked){
        TextButton button = new TextButton(text, style);
        button.add(new Image(image)).size(imagesize);
        button.getCells().reverse();
        button.clicked(clicked);
        return add(button);
    }

    public Cell<TextButton> button(String text, Drawable image, TextButtonStyle style, Runnable clicked){
        return button(text, image, style, image.imageSize(), clicked);
    }

    public Cell<TextButton> buttonCenter(String text, Drawable image, float imagesize, Runnable clicked){
        return buttonCenter(text, image, scene.getStyle(TextButtonStyle.class), imagesize, clicked);
    }

    public Cell<TextButton> buttonCenter(String text, Drawable image, Runnable clicked){
        return buttonCenter(text, image, scene.getStyle(TextButtonStyle.class), image.imageSize(), clicked);
    }

    public Cell<TextButton> buttonCenter(String text, Drawable image, TextButtonStyle style, float imagesize, Runnable clicked){
        TextButton button = new TextButton(text, style);
        button.add(new Image(image)).size(imagesize);
        button.getCells().reverse();
        button.clicked(clicked);
        button.getLabelCell().padLeft(-imagesize);
        return add(button);
    }

    public Cell<Slider> slider(float min, float max, float step, Floatc listener){
        return slider(min, max, step, 0f, listener);
    }

    public Cell<Slider> slider(float min, float max, float step, float defvalue, Floatc listener){
        Slider slider = new Slider(min, max, step, false);
        slider.setValue(defvalue);
        if(listener != null)
            slider.moved(listener);
        return add(slider);
    }

    public Cell<Slider> slider(float min, float max, float step, float defvalue, boolean onUp, Floatc listener){
        Slider slider = new Slider(min, max, step, false);
        slider.setValue(defvalue);
        if(listener != null){
            if(!onUp){
                slider.moved(listener);
            }else{
                slider.released(() -> listener.get(slider.getValue()));
            }

        }
        return add(slider);
    }

    @Override
    public boolean removeChild(Element element){
        return removeChild(element, true);
    }

    @Override
    public boolean removeChild(Element element, boolean unfocus){
        if(!super.removeChild(element, unfocus)) return false;
        Cell cell = getCell(element);
        if(cell != null) cell.element = null;
        return true;
    }

    /** Removes all actors and cells from the table. */
    @Override
    public void clearChildren(){
        Seq<Cell> cells = this.cells;
        for(int i = cells.size - 1; i >= 0; i--){
            Cell cell = cells.get(i);
            Element actor = cell.element;
            if(actor != null) actor.remove();
        }
        cellPool.freeAll(cells);
        cells.clear();
        rows = 0;
        columns = 0;
        if(rowDefaults != null) cellPool.free(rowDefaults);
        rowDefaults = null;
        implicitEndRow = false;

        super.clearChildren();
    }

    /**
     * Removes all actors and cells from the table (same as {@link #clearChildren()}) and additionally resets all table properties
     * and cell, column, and row defaults.
     */
    public void reset(){
        clearChildren();
        marginTop = unset;
        marginLeft = unset;
        marginBot = unset;
        marginRight = unset;
        align = Align.center;
        cellDefaults.reset();
    }

    /** Indicates that subsequent cells should be added to a new row and returns this table.*/
    public Table row(){
        if(cells.size > 0){
            if(!implicitEndRow) endRow();
            invalidate();
        }
        implicitEndRow = false;
        if(rowDefaults != null) cellPool.free(rowDefaults);
        rowDefaults = obtainCell();
        rowDefaults.clear();
        return this;
    }

    private void endRow(){
        Seq<Cell> cells = this.cells;
        int rowColumns = 0;
        for(int i = cells.size - 1; i >= 0; i--){
            Cell cell = cells.get(i);
            if(cell.endRow) break;
            rowColumns += cell.colspan;
        }
        columns = Math.max(columns, rowColumns);
        rows++;
        cells.peek().endRow = true;
    }

    /** Returns the cell for the specified actor in this table, or null. */
    public <T extends Element> Cell getCell(T actor){
        Seq<Cell> cells = this.cells;
        for(int i = 0, n = cells.size; i < n; i++){
            Cell c = cells.get(i);
            if(c.element == actor) return c;
        }
        return null;
    }

    /** Returns the cells for this table. */
    public Seq<Cell> getCells(){
        return cells;
    }

    @Override
    public float getPrefWidth(){
        if(sizeInvalid) computeSize();
        float width = tablePrefWidth;
        if(background != null) return Math.max(width, background.getMinWidth());
        return width;
    }

    @Override
    public float getPrefHeight(){
        if(sizeInvalid) computeSize();
        float height = tablePrefHeight;
        if(background != null) return Math.max(height, background.getMinHeight());
        return height;
    }

    @Override
    public float getMinWidth(){
        if(sizeInvalid) computeSize();
        return tableMinWidth;
    }

    @Override
    public float getMinHeight(){
        if(sizeInvalid) computeSize();
        return tableMinHeight;
    }

    /** The cell values that will be used as the defaults for all cells. */
    public Cell defaults(){
        return cellDefaults;
    }

    /** Sets the marginTop, marginLeft, marginBottom, and marginRight around the table to the specified value. */
    public Table margin(float pad){
        margin(pad, pad, pad, pad);
        return this;
    }

    public Table margin(float top, float left, float bottom, float right){
        marginTop = Scl.scl(top);
        marginLeft = Scl.scl(left);
        marginBot = Scl.scl(bottom);
        marginRight = Scl.scl(right);
        sizeInvalid = true;
        return this;
    }

    /** Padding at the top edge of the table. */
    public Table marginTop(float padTop){
        this.marginTop = Scl.scl(padTop);
        sizeInvalid = true;
        return this;
    }

    /** Padding at the left edge of the table. */
    public Table marginLeft(float padLeft){
        this.marginLeft = Scl.scl(padLeft);
        sizeInvalid = true;
        return this;
    }

    /** Padding at the bottom edge of the table. */
    public Table marginBottom(float padBottom){
        this.marginBot = Scl.scl(padBottom);
        sizeInvalid = true;
        return this;
    }

    /** Padding at the right edge of the table. */
    public Table marginRight(float padRight){
        this.marginRight = Scl.scl(padRight);
        sizeInvalid = true;
        return this;
    }

    /**
     * Alignment of the logical table within the table actor. Set to {@link Align#center}, {@link Align#top}, {@link Align#bottom}
     * , {@link Align#left}, {@link Align#right}, or any combination of those.
     */
    public Table align(int align){
        this.align = align;
        return this;
    }

    /** Sets the alignment of the logical table within the table actor to {@link Align#center}. This clears any other alignment. */
    public Table center(){
        align = Align.center;
        return this;
    }

    /** Adds {@link Align#top} and clears {@link Align#bottom} for the alignment of the logical table within the table actor. */
    public Table top(){
        align |= Align.top;
        align &= ~Align.bottom;
        return this;
    }

    /** Adds {@link Align#left} and clears {@link Align#right} for the alignment of the logical table within the table actor. */
    public Table left(){
        align |= Align.left;
        align &= ~Align.right;
        return this;
    }

    /** Adds {@link Align#bottom} and clears {@link Align#top} for the alignment of the logical table within the table actor. */
    public Table bottom(){
        align |= Align.bottom;
        align &= ~Align.top;
        return this;
    }

    /** Adds {@link Align#right} and clears {@link Align#left} for the alignment of the logical table within the table actor. */
    public Table right(){
        align |= Align.right;
        align &= ~Align.left;
        return this;
    }

    public float getMarginTop(){
        return marginTop != unset ? marginTop : background == null ? 0f : background.getTopHeight();
    }

    public float getMarginLeft(){
        return marginLeft != unset ? marginLeft : background == null ? 0f : background.getLeftWidth();
    }

    public float getMarginBottom(){
        return marginBot != unset ? marginBot : background == null ? 0f : background.getBottomHeight();
    }

    public float getMarginRight(){
        return marginRight != unset ? marginRight : background == null ? 0f : background.getRightWidth();
    }

    public int getAlign(){
        return align;
    }

    /**
     * Returns the row index for the y coordinate, or -1 if there are no cells.
     * @param y The y coordinate, where 0 is the top of the table.
     */
    public int getRow(float y){
        Seq<Cell> cells = this.cells;
        int row = 0;
        y += getMarginTop();
        int i = 0, n = cells.size;
        if(n == 0) return -1;
        if(n == 1) return 0;
        while(i < n){
            Cell c = cells.get(i++);
            if(c.elementY + c.computedPadTop < y) break;
            if(c.endRow) row++;
        }
        return row;
    }

    /** If true (the default), positions and sizes are rounded to integers. */
    public void setRound(boolean round){
        this.round = round;
    }

    public int getRows(){
        return rows;
    }

    public int getColumns(){
        return columns;
    }

    /** Returns the height of the specified row. */
    public float getRowHeight(int rowIndex){
        return rowHeight[rowIndex];
    }

    /** Returns the width of the specified column. */
    public float getColumnWidth(int columnIndex){
        return columnWidth[columnIndex];
    }

    private float[] ensureSize(float[] array, int size){
        if(array == null || array.length < size) return new float[size];
        for(int i = 0, n = array.length; i < n; i++)
            array[i] = 0;
        return array;
    }

    @Override
    public void layout(){
        float width = getWidth();
        float height = getHeight();

        layout(0, 0, width, height);

        Seq<Cell> cells = this.cells;
        if(round){
            for(int i = 0, n = cells.size; i < n; i++){
                Cell c = cells.get(i);
                float actorWidth = Math.round(c.elementWidth);
                float actorHeight = Math.round(c.elementHeight);
                float actorX = Math.round(c.elementX);
                //the table's own height may be fractional (e.g. a ScrollPane widget sized to its pref height), so round the flipped y as a whole
                float actorY = Math.round(height - c.elementY - actorHeight);
                c.setBounds(actorX, actorY, actorWidth, actorHeight);
                Element actor = c.element;
                if(actor != null) actor.setBounds(actorX, actorY, actorWidth, actorHeight);
            }
        }else{
            for(int i = 0, n = cells.size; i < n; i++){
                Cell c = cells.get(i);
                float actorHeight = c.elementHeight;
                float actorY = height - c.elementY - actorHeight;
                c.elementY = actorY;
                Element actor = c.element;
                if(actor != null) actor.setBounds(c.elementX, actorY, c.elementWidth, actorHeight);
            }
        }
        // Validate children separately from sizing actors to ensure actors without a cell are validated.
        Seq<Element> children = getChildren();
        for(int i = 0, n = children.size; i < n; i++){
            Element child = children.get(i);
            child.validate();
        }
    }

    protected void computeSize(){
        sizeInvalid = false;

        Seq<Cell> cells = this.cells;
        int cellCount = cells.size;

        // Implicitly End the row for layout purposes.
        if(cellCount > 0 && !cells.peek().endRow){
            endRow();
            implicitEndRow = true;
        }

        int columns = this.columns, rows = this.rows;
        this.columnMinWidth = ensureSize(this.columnMinWidth, columns);
        this.rowMinHeight = ensureSize(this.rowMinHeight, rows);
        this.columnPrefWidth = ensureSize(this.columnPrefWidth, columns);
        this.rowPrefHeight = ensureSize(this.rowPrefHeight, rows);
        this.columnWidth = ensureSize(this.columnWidth, columns);
        this.rowHeight = ensureSize(this.rowHeight, rows);
        this.expandWidth = ensureSize(this.expandWidth, columns);
        this.expandHeight = ensureSize(this.expandHeight, rows);

        for(int i = 0; i < cellCount; i++){
            Cell c = cells.get(i);
            int column = c.column, row = c.row, colspan = c.colspan;

            // Collect rows that expand and colspan=1 columns that expand.
            if(c.expandY != 0 && expandHeight[row] == 0) expandHeight[row] = c.expandY;
            if(colspan == 1 && c.expandX != 0 && expandWidth[column] == 0) expandWidth[column] = c.expandX;

            // Compute padding for cells.
            c.computedPadLeft = c.padLeft;
            c.computedPadTop = c.padTop;
            c.computedPadRight = c.padRight;
            c.computedPadBottom = c.padBottom;

            // Determine minimum and preferred cell sizes.
            float prefWidth = c.prefWidth();
            float prefHeight = c.prefHeight();
            float minWidth = c.minWidth();
            float minHeight = c.minHeight();
            float maxWidth = c.maxWidth();
            float maxHeight = c.maxHeight();
            if(prefWidth < minWidth) prefWidth = minWidth;
            if(prefHeight < minHeight) prefHeight = minHeight;
            if(maxWidth > 0 && prefWidth > maxWidth) prefWidth = maxWidth;
            if(maxHeight > 0 && prefHeight > maxHeight) prefHeight = maxHeight;

            if(colspan == 1){ // Spanned column min and pref width is added later.
                float hpadding = c.computedPadLeft + c.computedPadRight;
                columnPrefWidth[column] = Math.max(columnPrefWidth[column], prefWidth + hpadding);
                columnMinWidth[column] = Math.max(columnMinWidth[column], minWidth + hpadding);
            }
            float vpadding = c.computedPadTop + c.computedPadBottom;
            rowPrefHeight[row] = Math.max(rowPrefHeight[row], prefHeight + vpadding);
            rowMinHeight[row] = Math.max(rowMinHeight[row], minHeight + vpadding);
        }

        float uniformMinWidth = 0, uniformMinHeight = 0;
        float uniformPrefWidth = 0, uniformPrefHeight = 0;
        for(int i = 0; i < cellCount; i++){
            Cell c = cells.get(i);
            int column = c.column;

            // Colspan with expand will expand all spanned columns if none of the spanned columns have expand.
            int expandX = c.expandX;
            outer:
            if(expandX != 0){
                int nn = column + c.colspan;
                for(int ii = column; ii < nn; ii++)
                    if(expandWidth[ii] != 0) break outer;
                for(int ii = column; ii < nn; ii++)
                    expandWidth[ii] = expandX;
            }

            // Collect uniform sizes.
            if(c.uniformX && c.colspan == 1){
                float hpadding = c.computedPadLeft + c.computedPadRight;
                uniformMinWidth = Math.max(uniformMinWidth, columnMinWidth[column] - hpadding);
                uniformPrefWidth = Math.max(uniformPrefWidth, columnPrefWidth[column] - hpadding);
            }
            if(c.uniformY){
                float vpadding = c.computedPadTop + c.computedPadBottom;
                uniformMinHeight = Math.max(uniformMinHeight, rowMinHeight[c.row] - vpadding);
                uniformPrefHeight = Math.max(uniformPrefHeight, rowPrefHeight[c.row] - vpadding);
            }
        }

        // Size uniform cells to the same width/height.
        if(uniformPrefWidth > 0 || uniformPrefHeight > 0){
            for(int i = 0; i < cellCount; i++){
                Cell c = cells.get(i);
                if(uniformPrefWidth > 0 && c.uniformX && c.colspan == 1){
                    float hpadding = c.computedPadLeft + c.computedPadRight;
                    columnMinWidth[c.column] = uniformMinWidth + hpadding;
                    columnPrefWidth[c.column] = uniformPrefWidth + hpadding;
                }
                if(uniformPrefHeight > 0 && c.uniformY){
                    float vpadding = c.computedPadTop + c.computedPadBottom;
                    rowMinHeight[c.row] = uniformMinHeight + vpadding;
                    rowPrefHeight[c.row] = uniformPrefHeight + vpadding;
                }
            }
        }

        // Distribute any additional min and pref width added by colspanned cells to the columns spanned.
        for(int i = 0; i < cellCount; i++){
            Cell c = cells.get(i);
            int colspan = c.colspan;
            if(colspan == 1) continue;
            int column = c.column;

            Element a = c.element;
            float minWidth = c.minWidth();
            float prefWidth = c.prefWidth();
            float maxWidth = c.maxWidth();
            if(prefWidth < minWidth) prefWidth = minWidth;
            if(maxWidth > 0 && prefWidth > maxWidth) prefWidth = maxWidth;

            float spannedMinWidth = -(c.computedPadLeft + c.computedPadRight), spannedPrefWidth = spannedMinWidth;
            float totalExpandWidth = 0;
            for(int ii = column, nn = ii + colspan; ii < nn; ii++){
                spannedMinWidth += columnMinWidth[ii];
                spannedPrefWidth += columnPrefWidth[ii];
                totalExpandWidth += expandWidth[ii]; // Distribute extra space using expand, if any columns have expand.
            }

            float extraMinWidth = Math.max(0, minWidth - spannedMinWidth);
            float extraPrefWidth = Math.max(0, prefWidth - spannedPrefWidth);
            for(int ii = column, nn = ii + colspan; ii < nn; ii++){
                float ratio = totalExpandWidth == 0 ? 1f / colspan : expandWidth[ii] / totalExpandWidth;
                columnMinWidth[ii] += extraMinWidth * ratio;
                columnPrefWidth[ii] += extraPrefWidth * ratio;
            }
        }

        // Determine table min and pref size.
        tableMinWidth = 0;
        tableMinHeight = 0;
        tablePrefWidth = 0;
        tablePrefHeight = 0;
        for(int i = 0; i < columns; i++){
            tableMinWidth += columnMinWidth[i];
            tablePrefWidth += columnPrefWidth[i];
        }
        for(int i = 0; i < rows; i++){
            tableMinHeight += rowMinHeight[i];
            tablePrefHeight += Math.max(rowMinHeight[i], rowPrefHeight[i]);
        }
        float hpadding = getMarginLeft() + getMarginRight();
        float vpadding = getMarginTop() + getMarginBottom();
        tableMinWidth = tableMinWidth + hpadding;
        tableMinHeight = tableMinHeight + vpadding;
        tablePrefWidth = Math.max(tablePrefWidth + hpadding, tableMinWidth);
        tablePrefHeight = Math.max(tablePrefHeight + vpadding, tableMinHeight);
    }

    /**
     * Positions and sizes children of the table using the cell associated with each child. The values given are the position
     * within the parent and size of the table.
     */
    protected void layout(float layoutX, float layoutY, float layoutWidth, float layoutHeight){
        Seq<Cell> cells = this.cells;
        int cellCount = cells.size;

        if(sizeInvalid) computeSize();

        float padLeft = getMarginLeft();
        float hpadding = padLeft + getMarginRight();
        float padTop = getMarginTop();
        float vpadding = padTop + getMarginBottom();

        int columns = this.columns, rows = this.rows;
        float[] expandWidth = this.expandWidth, expandHeight = this.expandHeight;
        float[] columnWidth = this.columnWidth, rowHeight = this.rowHeight;

        float totalExpandWidth = 0, totalExpandHeight = 0;
        for(int i = 0; i < columns; i++)
            totalExpandWidth += expandWidth[i];
        for(int i = 0; i < rows; i++)
            totalExpandHeight += expandHeight[i];

        // Size columns and rows between min and pref size using (preferred - min) size to weight distribution of extra space.
        float[] columnWeightedWidth;
        float totalGrowWidth = tablePrefWidth - tableMinWidth;
        if(totalGrowWidth == 0)
            columnWeightedWidth = columnMinWidth;
        else{
            float extraWidth = Math.min(totalGrowWidth, Math.max(0, layoutWidth - tableMinWidth));
            columnWeightedWidth = Table.columnWeightedWidth = ensureSize(Table.columnWeightedWidth, columns);
            float[] columnMinWidth = this.columnMinWidth, columnPrefWidth = this.columnPrefWidth;
            for(int i = 0; i < columns; i++){
                float growWidth = columnPrefWidth[i] - columnMinWidth[i];
                float growRatio = growWidth / totalGrowWidth;
                columnWeightedWidth[i] = columnMinWidth[i] + extraWidth * growRatio;
            }
        }

        float[] rowWeightedHeight;
        float totalGrowHeight = tablePrefHeight - tableMinHeight;
        if(totalGrowHeight == 0)
            rowWeightedHeight = rowMinHeight;
        else{
            rowWeightedHeight = Table.rowWeightedHeight = ensureSize(Table.rowWeightedHeight, rows);
            float extraHeight = Math.min(totalGrowHeight, Math.max(0, layoutHeight - tableMinHeight));
            float[] rowMinHeight = this.rowMinHeight, rowPrefHeight = this.rowPrefHeight;
            for(int i = 0; i < rows; i++){
                float growHeight = rowPrefHeight[i] - rowMinHeight[i];
                float growRatio = growHeight / totalGrowHeight;
                rowWeightedHeight[i] = rowMinHeight[i] + extraHeight * growRatio;
            }
        }

        // Determine actor and cell sizes (before expand or fill).
        for(int i = 0; i < cellCount; i++){
            Cell c = cells.get(i);
            int column = c.column, row = c.row;

            float spannedWeightedWidth = 0;
            int colspan = c.colspan;
            for(int ii = column, nn = ii + colspan; ii < nn; ii++)
                spannedWeightedWidth += columnWeightedWidth[ii];
            float weightedHeight = rowWeightedHeight[row];

            float prefWidth = c.prefWidth();
            float prefHeight = c.prefHeight();
            float minWidth = c.minWidth();
            float minHeight = c.minHeight();
            float maxWidth = c.maxWidth();
            float maxHeight = c.maxHeight();
            if(prefWidth < minWidth) prefWidth = minWidth;
            if(prefHeight < minHeight) prefHeight = minHeight;
            if(maxWidth > 0 && prefWidth > maxWidth) prefWidth = maxWidth;
            if(maxHeight > 0 && prefHeight > maxHeight) prefHeight = maxHeight;

            c.elementWidth = Math.min(spannedWeightedWidth - c.computedPadLeft - c.computedPadRight, prefWidth);
            c.elementHeight = Math.min(weightedHeight - c.computedPadTop - c.computedPadBottom, prefHeight);

            if(colspan == 1) columnWidth[column] = Math.max(columnWidth[column], spannedWeightedWidth);
            rowHeight[row] = Math.max(rowHeight[row], weightedHeight);
        }

        // Distribute remaining space to any expanding columns/rows.
        if(totalExpandWidth > 0){
            float extra = layoutWidth - hpadding;
            for(int i = 0; i < columns; i++)
                extra -= columnWidth[i];
            float used = 0;
            int lastIndex = 0;
            for(int i = 0; i < columns; i++){
                if(expandWidth[i] == 0) continue;
                float amount = extra * expandWidth[i] / totalExpandWidth;
                columnWidth[i] += amount;
                used += amount;
                lastIndex = i;
            }
            columnWidth[lastIndex] += extra - used;
        }
        if(totalExpandHeight > 0){
            float extra = layoutHeight - vpadding;
            for(int i = 0; i < rows; i++)
                extra -= rowHeight[i];
            float used = 0;
            int lastIndex = 0;
            for(int i = 0; i < rows; i++){
                if(expandHeight[i] == 0) continue;
                float amount = extra * expandHeight[i] / totalExpandHeight;
                rowHeight[i] += amount;
                used += amount;
                lastIndex = i;
            }
            rowHeight[lastIndex] += extra - used;
        }

        // Distribute any additional width added by colspanned cells to the columns spanned.
        for(int i = 0; i < cellCount; i++){
            Cell c = cells.get(i);
            int colspan = c.colspan;
            if(colspan == 1) continue;

            float extraWidth = 0;
            for(int column = c.column, nn = column + colspan; column < nn; column++)
                extraWidth += columnWeightedWidth[column] - columnWidth[column];
            extraWidth -= Math.max(0, c.computedPadLeft + c.computedPadRight);

            extraWidth /= colspan;
            if(extraWidth > 0){
                for(int column = c.column, nn = column + colspan; column < nn; column++)
                    columnWidth[column] += extraWidth;
            }
        }

        // Determine table size.
        float tableWidth = hpadding, tableHeight = vpadding;
        for(int i = 0; i < columns; i++)
            tableWidth += columnWidth[i];
        for(int i = 0; i < rows; i++)
            tableHeight += rowHeight[i];

        // Position table within the container.
        int align = this.align;
        float x = layoutX + padLeft;
        if((align & Align.right) != 0)
            x += layoutWidth - tableWidth;
        else if((align & Align.left) == 0) // Center
            x += (layoutWidth - tableWidth) / 2;

        float y = layoutY + padTop;
        if((align & Align.bottom) != 0)
            y += layoutHeight - tableHeight;
        else if((align & Align.top) == 0) // Center
            y += (layoutHeight - tableHeight) / 2;

        // Position actors within cells.
        float currentX = x, currentY = y;
        for(int i = 0; i < cellCount; i++){
            Cell c = cells.get(i);

            float spannedCellWidth = 0;
            for(int column = c.column, nn = column + c.colspan; column < nn; column++)
                spannedCellWidth += columnWidth[column];
            spannedCellWidth -= c.computedPadLeft + c.computedPadRight;

            currentX += c.computedPadLeft;

            float fillX = c.fillX, fillY = c.fillY;
            if(fillX > 0){
                c.elementWidth = Math.max(spannedCellWidth * fillX, c.minWidth());
                float maxWidth = c.maxWidth;
                if(maxWidth > 0) c.elementWidth = Math.min(c.elementWidth, maxWidth);
            }
            if(fillY > 0){
                c.elementHeight = Math.max(rowHeight[c.row] * fillY - c.computedPadTop - c.computedPadBottom, c.minHeight());
                float maxHeight = c.maxHeight();
                if(maxHeight > 0) c.elementHeight = Math.min(c.elementHeight, maxHeight);
            }

            align = c.align;
            if((align & Align.left) != 0)
                c.elementX = currentX;
            else if((align & Align.right) != 0)
                c.elementX = currentX + spannedCellWidth - c.elementWidth;
            else
                c.elementX = currentX + (spannedCellWidth - c.elementWidth) / 2;

            if((align & Align.top) != 0)
                c.elementY = currentY + c.computedPadTop;
            else if((align & Align.bottom) != 0)
                c.elementY = currentY + rowHeight[c.row] - c.elementHeight - c.computedPadBottom;
            else
                c.elementY = currentY + (rowHeight[c.row] - c.elementHeight + c.computedPadTop - c.computedPadBottom) / 2;

            if(c.endRow){
                currentX = x;
                currentY += rowHeight[c.row];
            }else
                currentX += spannedCellWidth + c.computedPadRight;
        }
    }

    public interface DrawRect{
        void draw(float x, float y, float width, float height);
    }
}
