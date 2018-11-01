
package io.anuke.ucore.scene.ui.layout;

import com.badlogic.gdx.Files;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Pool.Poolable;
import io.anuke.ucore.function.Consumer;
import io.anuke.ucore.function.Predicate;
import io.anuke.ucore.function.BooleanProvider;
import io.anuke.ucore.scene.Element;
import io.anuke.ucore.scene.ui.Button;
import io.anuke.ucore.scene.ui.ButtonGroup;
import io.anuke.ucore.scene.ui.Label;
import io.anuke.ucore.scene.ui.TextButton;
import io.anuke.ucore.scene.ui.layout.Value.Fixed;

/** A cell for a {@link Table}.
 * @author Nathan Sweet */
public class Cell<T extends Element> implements Poolable {
	static private final Float zerof = 0f, onef = 1f;
	static private final Integer zeroi = 0, onei = 1;
	static private final Integer centeri = onei, topi = Align.top, bottomi = Align.bottom, lefti = Align.left,
		righti = Align.right;

	static private Files files;
	static private Cell defaults;

	Value minWidth, minHeight;
	Value maxWidth, maxHeight;
	Value spaceTop, spaceLeft, spaceBottom, spaceRight;
	Value padTop, padLeft, padBottom, padRight;
	Float fillX, fillY;
	Integer align;
	Integer expandX, expandY;
	Integer colspan;
	Boolean uniformX, uniformY;

	Element actor;
	float actorX, actorY;
	float actorWidth, actorHeight;

	private Table table;
	boolean endRow;
	int column, row;
	int cellAboveIndex;
	float computedPadTop, computedPadLeft, computedPadBottom, computedPadRight;

	public Cell () {
		reset();
	}

	public void setLayout (Table table) {
		this.table = table;
	}

	/** Sets the actor in this cell and adds the actor to the cell's table. If null, removes any current actor. */
	public <A extends Element> Cell<A> setActor (A newActor) {
		if (actor != newActor) {
			if (actor != null) actor.remove();
			actor = newActor;
			if (newActor != null) table.addChild(newActor);
		}
		return (Cell<A>)this;
	}

	/** Removes the current actor for the cell, if any. */
	public Cell<T> clearActor () {
		setActor(null);
		return this;
	}

	/** Returns the actor for this cell, or null. */
	public T getElement () {
		return (T)actor;
	}
	
	/**getElement shortcut*/
	public T get(){
		return getElement();
	}

	/** Returns true if the cell's actor is not null. */
	public boolean hasActor () {
		return actor != null;
	}

	/** Sets the minWidth, prefWidth, maxWidth, minHeight, prefHeight, and maxHeight to the specified value. */
	public Cell<T> size (Value size) {
		if (size == null) throw new IllegalArgumentException("size cannot be null.");
		minWidth = size;
		minHeight = size;
		maxWidth = size;
		maxHeight = size;
		return this;
	}

	/** Sets the minWidth, prefWidth, maxWidth, minHeight, prefHeight, and maxHeight to the specified values. */
	public Cell<T> size (Value width, Value height) {
		if (width == null) throw new IllegalArgumentException("width cannot be null.");
		if (height == null) throw new IllegalArgumentException("height cannot be null.");
		minWidth = width;
		minHeight = height;
		maxWidth = width;
		maxHeight = height;
		return this;
	}

	public Cell<T> name(String name){
		getElement().setName(name);
		return this;
	}

	public Cell<T> update(Consumer<T> updater){
		getElement().update(() -> updater.accept(getElement()));
		return this;
	}

	public Cell<T> disabled(Predicate<T> vis){
		if(getElement() instanceof Button){
			((Button)getElement()).setDisabled(() -> vis.test(getElement()));
		}
		return this;
	}

	public Cell<T> disabled(boolean disabled){
		if(getElement() instanceof Button){
			((Button)getElement()).setDisabled(disabled);
		}
		return this;
	}

	public Cell<T> visible(BooleanProvider prov){
		getElement().setVisible(prov);
		return this;
	}

	public Cell<T> wrap(){
		if(getElement() instanceof Label){
			((Label)getElement()).setWrap(true);
		}else if(getElement() instanceof TextButton){
			((TextButton)getElement()).getLabel().setWrap(true);
		}
		return this;
	}

	public <N extends Button> Cell<T> group(ButtonGroup<N> group){
		if(getElement() instanceof Button){
			group.add((N)getElement());
		}
		return this;
	}

	public Cell<T> checked(boolean toggle){
		if(getElement() instanceof Button){
			((Button)(getElement())).setChecked(toggle);
		}
		return this;
	}

	public Cell<T> color(Color color){
		getElement().setColor(color);
		return this;
	}

	public Cell<T> margin(float margin){
		if(getElement() instanceof Table){
			((Table)getElement()).margin(margin);
		}
		return this;
	}

	public Cell<T> marginTop(float margin){
		if(getElement() instanceof Table){
			((Table)getElement()).marginTop(margin);
		}
		return this;
	}

	public Cell<T> marginBottom(float margin){
		if(getElement() instanceof Table){
			((Table)getElement()).marginBottom(margin);
		}
		return this;
	}

	public Cell<T> marginLeft(float margin){
		if(getElement() instanceof Table){
			((Table)getElement()).marginLeft(margin);
		}
		return this;
	}
	public Cell<T> marginRight(float margin){
		if(getElement() instanceof Table){
			((Table)getElement()).marginRight(margin);
		}
		return this;
	}

	/** Sets the minWidth, prefWidth, maxWidth, minHeight, prefHeight, and maxHeight to the specified value. */
	public Cell<T> size (float size) {
		size(new Fixed(size));
		return this;
	}

	/** Sets the minWidth, prefWidth, maxWidth, minHeight, prefHeight, and maxHeight to the specified values. */
	public Cell<T> size (float width, float height) {
		size(new Fixed(width), new Fixed(height));
		return this;
	}

	/** Sets the minWidth, prefWidth, and maxWidth to the specified value. */
	public Cell<T> width (Value width) {
		if (width == null) throw new IllegalArgumentException("width cannot be null.");
		minWidth = width;
		maxWidth = width;
		return this;
	}

	/** Sets the minWidth, prefWidth, and maxWidth to the specified value. */
	public Cell<T> width (float width) {
		width(new Fixed(width));
		return this;
	}

	/** Sets the minHeight, prefHeight, and maxHeight to the specified value. */
	public Cell<T> height (Value height) {
		if (height == null) throw new IllegalArgumentException("height cannot be null.");
		minHeight = height;
		maxHeight = height;
		return this;
	}

	/** Sets the minHeight, prefHeight, and maxHeight to the specified value. */
	public Cell<T> height (float height) {
		height(new Fixed(height));
		return this;
	}

	/** Sets the minWidth and minHeight to the specified value. */
	public Cell<T> minSize (Value size) {
		if (size == null) throw new IllegalArgumentException("size cannot be null.");
		minWidth = size;
		minHeight = size;
		return this;
	}

	/** Sets the minWidth and minHeight to the specified values. */
	public Cell<T> minSize (Value width, Value height) {
		if (width == null) throw new IllegalArgumentException("width cannot be null.");
		if (height == null) throw new IllegalArgumentException("height cannot be null.");
		minWidth = width;
		minHeight = height;
		return this;
	}

	public Cell<T> minWidth (Value minWidth) {
		if (minWidth == null) throw new IllegalArgumentException("minWidth cannot be null.");
		this.minWidth = minWidth;
		return this;
	}

	public Cell<T> minHeight (Value minHeight) {
		if (minHeight == null) throw new IllegalArgumentException("minHeight cannot be null.");
		this.minHeight = minHeight;
		return this;
	}

	/** Sets the minWidth and minHeight to the specified value. */
	public Cell<T> minSize (float size) {
		minSize(new Fixed(size));
		return this;
	}

	/** Sets the minWidth and minHeight to the specified values. */
	public Cell<T> minSize (float width, float height) {
		minSize(new Fixed(width), new Fixed(height));
		return this;
	}

	public Cell<T> minWidth (float minWidth) {
		this.minWidth = new Fixed(minWidth);
		return this;
	}

	public Cell<T> minHeight (float minHeight) {
		this.minHeight = new Fixed(minHeight);
		return this;
	}

	/** Sets the prefWidth and prefHeight to the specified value. */
	public Cell<T> prefSize (Value size) {
		if (size == null) throw new IllegalArgumentException("size cannot be null.");
		return this;
	}

	/** Sets the prefWidth and prefHeight to the specified values. */
	public Cell<T> prefSize (Value width, Value height) {
		if (width == null) throw new IllegalArgumentException("width cannot be null.");
		if (height == null) throw new IllegalArgumentException("height cannot be null.");
		return this;
	}

	public Cell<T> prefWidth (Value prefWidth) {
		if (prefWidth == null) throw new IllegalArgumentException("prefWidth cannot be null.");
		return this;
	}

	public Cell<T> prefHeight (Value prefHeight) {
		if (prefHeight == null) throw new IllegalArgumentException("prefHeight cannot be null.");
		return this;
	}

	/** Sets the prefWidth and prefHeight to the specified value. */
	public Cell<T> prefSize (float width, float height) {
		prefSize(new Fixed(width), new Fixed(height));
		return this;
	}

	/** Sets the prefWidth and prefHeight to the specified values. */
	public Cell<T> prefSize (float size) {
		prefSize(new Fixed(size));
		return this;
	}

	public Cell<T> prefWidth (float prefWidth) {
		return this;
	}

	public Cell<T> prefHeight (float prefHeight) {
		return this;
	}

	/** Sets the maxWidth and maxHeight to the specified value. */
	public Cell<T> maxSize (Value size) {
		if (size == null) throw new IllegalArgumentException("size cannot be null.");
		maxWidth = size;
		maxHeight = size;
		return this;
	}

	/** Sets the maxWidth and maxHeight to the specified values. */
	public Cell<T> maxSize (Value width, Value height) {
		if (width == null) throw new IllegalArgumentException("width cannot be null.");
		if (height == null) throw new IllegalArgumentException("height cannot be null.");
		maxWidth = width;
		maxHeight = height;
		return this;
	}

	public Cell<T> maxWidth (Value maxWidth) {
		if (maxWidth == null) throw new IllegalArgumentException("maxWidth cannot be null.");
		this.maxWidth = maxWidth;
		return this;
	}

	public Cell<T> maxHeight (Value maxHeight) {
		if (maxHeight == null) throw new IllegalArgumentException("maxHeight cannot be null.");
		this.maxHeight = maxHeight;
		return this;
	}

	/** Sets the maxWidth and maxHeight to the specified value. */
	public Cell<T> maxSize (float size) {
		maxSize(new Fixed(size));
		return this;
	}

	/** Sets the maxWidth and maxHeight to the specified values. */
	public Cell<T> maxSize (float width, float height) {
		maxSize(new Fixed(width), new Fixed(height));
		return this;
	}

	public Cell<T> maxWidth (float maxWidth) {
		this.maxWidth = new Fixed(maxWidth);
		return this;
	}

	public Cell<T> maxHeight (float maxHeight) {
		this.maxHeight = new Fixed(maxHeight);
		return this;
	}

	/** Sets the spaceTop, spaceLeft, spaceBottom, and spaceRight to the specified value. */
	public Cell<T> space (Value space) {
		if (space == null) throw new IllegalArgumentException("space cannot be null.");
		spaceTop = space;
		spaceLeft = space;
		spaceBottom = space;
		spaceRight = space;
		return this;
	}

	public Cell<T> space (Value top, Value left, Value bottom, Value right) {
		if (top == null) throw new IllegalArgumentException("top cannot be null.");
		if (left == null) throw new IllegalArgumentException("left cannot be null.");
		if (bottom == null) throw new IllegalArgumentException("bottom cannot be null.");
		if (right == null) throw new IllegalArgumentException("right cannot be null.");
		spaceTop = top;
		spaceLeft = left;
		spaceBottom = bottom;
		spaceRight = right;
		return this;
	}

	public Cell<T> spaceTop (Value spaceTop) {
		if (spaceTop == null) throw new IllegalArgumentException("spaceTop cannot be null.");
		this.spaceTop = spaceTop;
		return this;
	}

	public Cell<T> spaceLeft (Value spaceLeft) {
		if (spaceLeft == null) throw new IllegalArgumentException("spaceLeft cannot be null.");
		this.spaceLeft = spaceLeft;
		return this;
	}

	public Cell<T> spaceBottom (Value spaceBottom) {
		if (spaceBottom == null) throw new IllegalArgumentException("spaceBottom cannot be null.");
		this.spaceBottom = spaceBottom;
		return this;
	}

	public Cell<T> spaceRight (Value spaceRight) {
		if (spaceRight == null) throw new IllegalArgumentException("spaceRight cannot be null.");
		this.spaceRight = spaceRight;
		return this;
	}

	/** Sets the spaceTop, spaceLeft, spaceBottom, and spaceRight to the specified value. */
	public Cell<T> space (float space) {
		if (space < 0) throw new IllegalArgumentException("space cannot be < 0.");
		space(new Fixed(space));
		return this;
	}

	public Cell<T> space (float top, float left, float bottom, float right) {
		if (top < 0) throw new IllegalArgumentException("top cannot be < 0.");
		if (left < 0) throw new IllegalArgumentException("left cannot be < 0.");
		if (bottom < 0) throw new IllegalArgumentException("bottom cannot be < 0.");
		if (right < 0) throw new IllegalArgumentException("right cannot be < 0.");
		space(new Fixed(top), new Fixed(left), new Fixed(bottom), new Fixed(right));
		return this;
	}

	public Cell<T> spaceTop (float spaceTop) {
		if (spaceTop < 0) throw new IllegalArgumentException("spaceTop cannot be < 0.");
		this.spaceTop = new Fixed(spaceTop);
		return this;
	}

	public Cell<T> spaceLeft (float spaceLeft) {
		if (spaceLeft < 0) throw new IllegalArgumentException("spaceLeft cannot be < 0.");
		this.spaceLeft = new Fixed(spaceLeft);
		return this;
	}

	public Cell<T> spaceBottom (float spaceBottom) {
		if (spaceBottom < 0) throw new IllegalArgumentException("spaceBottom cannot be < 0.");
		this.spaceBottom = new Fixed(spaceBottom);
		return this;
	}

	public Cell<T> spaceRight (float spaceRight) {
		if (spaceRight < 0) throw new IllegalArgumentException("spaceRight cannot be < 0.");
		this.spaceRight = new Fixed(spaceRight);
		return this;
	}

	/** Sets the marginTop, marginLeft, marginBottom, and marginRight to the specified value. */
	public Cell<T> pad (Value pad) {
		if (pad == null) throw new IllegalArgumentException("margin cannot be null.");
		padTop = pad;
		padLeft = pad;
		padBottom = pad;
		padRight = pad;
		return this;
	}

	public Cell<T> pad (Value top, Value left, Value bottom, Value right) {
		if (top == null) throw new IllegalArgumentException("top cannot be null.");
		if (left == null) throw new IllegalArgumentException("left cannot be null.");
		if (bottom == null) throw new IllegalArgumentException("bottom cannot be null.");
		if (right == null) throw new IllegalArgumentException("right cannot be null.");
		padTop = top;
		padLeft = left;
		padBottom = bottom;
		padRight = right;
		return this;
	}

	public Cell<T> padTop (Value padTop) {
		if (padTop == null) throw new IllegalArgumentException("marginTop cannot be null.");
		this.padTop = padTop;
		return this;
	}

	public Cell<T> padLeft (Value padLeft) {
		if (padLeft == null) throw new IllegalArgumentException("marginLeft cannot be null.");
		this.padLeft = padLeft;
		return this;
	}

	public Cell<T> padBottom (Value padBottom) {
		if (padBottom == null) throw new IllegalArgumentException("marginBottom cannot be null.");
		this.padBottom = padBottom;
		return this;
	}

	public Cell<T> padRight (Value padRight) {
		if (padRight == null) throw new IllegalArgumentException("marginRight cannot be null.");
		this.padRight = padRight;
		return this;
	}

	/** Sets the marginTop, marginLeft, marginBottom, and marginRight to the specified value. */
	public Cell<T> pad (float pad) {
		pad(new Fixed(pad));
		return this;
	}

	public Cell<T> pad (float top, float left, float bottom, float right) {
		pad(new Fixed(top), new Fixed(left), new Fixed(bottom), new Fixed(right));
		return this;
	}

	public Cell<T> padTop (float padTop) {
		this.padTop = new Fixed(padTop);
		return this;
	}

	public Cell<T> padLeft (float padLeft) {
		this.padLeft = new Fixed(padLeft);
		return this;
	}

	public Cell<T> padBottom (float padBottom) {
		this.padBottom = new Fixed(padBottom);
		return this;
	}

	public Cell<T> padRight (float padRight) {
		this.padRight = new Fixed(padRight);
		return this;
	}

	/** Sets fillX and fillY to 1. */
	public Cell<T> fill () {
		fillX = onef;
		fillY = onef;
		return this;
	}

	/** Sets fillX to 1. */
	public Cell<T> fillX () {
		fillX = onef;
		return this;
	}

	/** Sets fillY to 1. */
	public Cell<T> fillY () {
		fillY = onef;
		return this;
	}

	public Cell<T> fill (float x, float y) {
		fillX = x;
		fillY = y;
		return this;
	}

	/** Sets fillX and fillY to 1 if true, 0 if false. */
	public Cell<T> fill (boolean x, boolean y) {
		fillX = x ? onef : zerof;
		fillY = y ? onef : zerof;
		return this;
	}

	/** Sets fillX and fillY to 1 if true, 0 if false. */
	public Cell<T> fill (boolean fill) {
		fillX = fill ? onef : zerof;
		fillY = fill ? onef : zerof;
		return this;
	}

	/** Sets the alignment of the actor within the cell. Set to {@link Align#center}, {@link Align#top}, {@link Align#bottom},
	 * {@link Align#left}, {@link Align#right}, or any combination of those. */
	public Cell<T> align (int align) {
		this.align = align;
		return this;
	}

	/** Sets the alignment of the actor within the cell to {@link Align#center}. This clears any other alignment. */
	public Cell<T> center () {
		align = centeri;
		return this;
	}

	/** Adds {@link Align#top} and clears {@link Align#bottom} for the alignment of the actor within the cell. */
	public Cell<T> top () {
		if (align == null)
			align = topi;
		else
			align = (align | Align.top) & ~Align.bottom;
		return this;
	}

	/** Adds {@link Align#left} and clears {@link Align#right} for the alignment of the actor within the cell. */
	public Cell<T> left () {
		if (align == null)
			align = lefti;
		else
			align = (align | Align.left) & ~Align.right;
		return this;
	}

	/** Adds {@link Align#bottom} and clears {@link Align#top} for the alignment of the actor within the cell. */
	public Cell<T> bottom () {
		if (align == null)
			align = bottomi;
		else
			align = (align | Align.bottom) & ~Align.top;
		return this;
	}

	/** Adds {@link Align#right} and clears {@link Align#left} for the alignment of the actor within the cell. */
	public Cell<T> right () {
		if (align == null)
			align = righti;
		else
			align = (align | Align.right) & ~Align.left;
		return this;
	}

	/** Sets expandX, expandY, fillX, and fillY to 1. */
	public Cell<T> grow () {
		expandX = onei;
		expandY = onei;
		fillX = onef;
		fillY = onef;
		return this;
	}

	/** Sets expandX and fillX to 1. */
	public Cell<T> growX () {
		expandX = onei;
		fillX = onef;
		return this;
	}

	/** Sets expandY and fillY to 1. */
	public Cell<T> growY () {
		expandY = onei;
		fillY = onef;
		return this;
	}

	/** Sets expandX and expandY to 1. */
	public Cell<T> expand () {
		expandX = onei;
		expandY = onei;
		return this;
	}

	/** Sets expandX to 1. */
	public Cell<T> expandX () {
		expandX = onei;
		return this;
	}

	/** Sets expandY to 1. */
	public Cell<T> expandY () {
		expandY = onei;
		return this;
	}

	public Cell<T> expand (int x, int y) {
		expandX = x;
		expandY = y;
		return this;
	}

	/** Sets expandX and expandY to 1 if true, 0 if false. */
	public Cell<T> expand (boolean x, boolean y) {
		expandX = x ? onei : zeroi;
		expandY = y ? onei : zeroi;
		return this;
	}

	public Cell<T> colspan (int colspan) {
		this.colspan = colspan;
		return this;
	}

	/** Sets uniformX and uniformY to true. */
	public Cell<T> uniform () {
		uniformX = Boolean.TRUE;
		uniformY = Boolean.TRUE;
		return this;
	}

	/** Sets uniformX to true. */
	public Cell<T> uniformX () {
		uniformX = Boolean.TRUE;
		return this;
	}

	/** Sets uniformY to true. */
	public Cell<T> uniformY () {
		uniformY = Boolean.TRUE;
		return this;
	}

	public Cell<T> uniform (boolean x, boolean y) {
		uniformX = x;
		uniformY = y;
		return this;
	}

	public void setActorBounds (float x, float y, float width, float height) {
		actorX = x;
		actorY = y;
		actorWidth = width;
		actorHeight = height;
	}

	public float getActorX () {
		return actorX;
	}

	public void setActorX (float actorX) {
		this.actorX = actorX;
	}

	public float getActorY () {
		return actorY;
	}

	public void setActorY (float actorY) {
		this.actorY = actorY;
	}

	public float getActorWidth () {
		return actorWidth;
	}

	public void setActorWidth (float actorWidth) {
		this.actorWidth = actorWidth;
	}

	public float getActorHeight () {
		return actorHeight;
	}

	public void setActorHeight (float actorHeight) {
		this.actorHeight = actorHeight;
	}

	public int getColumn () {
		return column;
	}

	public int getRow () {
		return row;
	}

	/** @return May be null if this cell is row defaults. */
	public Value getMinWidthValue () {
		return minWidth;
	}

	public float getMinWidth () {
		return minWidth.get(actor);
	}

	/** @return May be null if this cell is row defaults. */
	public Value getMinHeightValue () {
		return minHeight;
	}

	public float getMinHeight () {
		return minHeight.get(actor);
	}

	/** @return May be null if this cell is row defaults. */
	public Value getMaxWidthValue () {
		return maxWidth;
	}

	public float getMaxWidth () {
		return maxWidth.get(actor);
	}

	/** @return May be null if this cell is row defaults. */
	public Value getMaxHeightValue () {
		return maxHeight;
	}

	public float getMaxHeight () {
		return maxHeight.get(actor);
	}

	/** @return May be null if this value is not set. */
	public Value getSpaceTopValue () {
		return spaceTop;
	}

	public float getSpaceTop () {
		return spaceTop.get(actor);
	}

	/** @return May be null if this value is not set. */
	public Value getSpaceLeftValue () {
		return spaceLeft;
	}

	public float getSpaceLeft () {
		return spaceLeft.get(actor);
	}

	/** @return May be null if this value is not set. */
	public Value getSpaceBottomValue () {
		return spaceBottom;
	}

	public float getSpaceBottom () {
		return spaceBottom.get(actor);
	}

	/** @return May be null if this value is not set. */
	public Value getSpaceRightValue () {
		return spaceRight;
	}

	public float getSpaceRight () {
		return spaceRight.get(actor);
	}

	/** @return May be null if this value is not set. */
	public Value getPadTopValue () {
		return padTop;
	}

	public float getPadTop () {
		return padTop.get(actor);
	}

	/** @return May be null if this value is not set. */
	public Value getPadLeftValue () {
		return padLeft;
	}

	public float getPadLeft () {
		return padLeft.get(actor);
	}

	/** @return May be null if this value is not set. */
	public Value getPadBottomValue () {
		return padBottom;
	}

	public float getPadBottom () {
		return padBottom.get(actor);
	}

	/** @return May be null if this value is not set. */
	public Value getPadRightValue () {
		return padRight;
	}

	public float getPadRight () {
		return padRight.get(actor);
	}

	/** Returns {@link #getPadLeft()} plus {@link #getPadRight()}. */
	public float getPadX () {
		return padLeft.get(actor) + padRight.get(actor);
	}

	/** Returns {@link #getPadTop()} plus {@link #getPadBottom()}. */
	public float getPadY () {
		return padTop.get(actor) + padBottom.get(actor);
	}

	/** @return May be null if this value is not set. */
	public float getFillX () {
		return fillX;
	}

	/** @return May be null. */
	public float getFillY () {
		return fillY;
	}

	/** @return May be null. */
	public int getAlign () {
		return align;
	}

	/** @return May be null. */
	public int getExpandX () {
		return expandX;
	}

	/** @return May be null. */
	public int getExpandY () {
		return expandY;
	}

	/** @return May be null. */
	public int getColspan () {
		return colspan;
	}

	/** @return May be null. */
	public boolean getUniformX () {
		return uniformX;
	}

	/** @return May be null. */
	public boolean getUniformY () {
		return uniformY;
	}

	/** Returns true if this cell is the last cell in the row. */
	public boolean isEndRow () {
		return endRow;
	}

	/** The actual amount of combined padding and spacing from the last layout. */
	public float getComputedPadTop () {
		return computedPadTop;
	}

	/** The actual amount of combined padding and spacing from the last layout. */
	public float getComputedPadLeft () {
		return computedPadLeft;
	}

	/** The actual amount of combined padding and spacing from the last layout. */
	public float getComputedPadBottom () {
		return computedPadBottom;
	}

	/** The actual amount of combined padding and spacing from the last layout. */
	public float getComputedPadRight () {
		return computedPadRight;
	}

	public void row () {
		table.row();
	}

	public Table getTable () {
		return table;
	}

	/** Sets all constraint fields to null. */
	void clear () {
		minWidth = null;
		minHeight = null;
		maxWidth = null;
		maxHeight = null;
		spaceTop = null;
		spaceLeft = null;
		spaceBottom = null;
		spaceRight = null;
		padTop = null;
		padLeft = null;
		padBottom = null;
		padRight = null;
		fillX = null;
		fillY = null;
		align = null;
		expandX = null;
		expandY = null;
		colspan = null;
		uniformX = null;
		uniformY = null;
	}

	/** Reset state so the cell can be reused, setting all constraints to their {@link #defaults() default} values. */
	public void reset () {
		actor = null;
		table = null;
		endRow = false;
		cellAboveIndex = -1;

		Cell defaults = defaults();
		if (defaults != null) set(defaults);
	}

	void set (Cell cell) {
		minWidth = cell.minWidth;
		minHeight = cell.minHeight;
		maxWidth = cell.maxWidth;
		maxHeight = cell.maxHeight;
		spaceTop = cell.spaceTop;
		spaceLeft = cell.spaceLeft;
		spaceBottom = cell.spaceBottom;
		spaceRight = cell.spaceRight;
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
	}

	/** @param cell May be null. */
	void merge (Cell cell) {
		if (cell == null) return;
		if (cell.minWidth != null) minWidth = cell.minWidth;
		if (cell.minHeight != null) minHeight = cell.minHeight;
		if (cell.maxWidth != null) maxWidth = cell.maxWidth;
		if (cell.maxHeight != null) maxHeight = cell.maxHeight;
		if (cell.spaceTop != null) spaceTop = cell.spaceTop;
		if (cell.spaceLeft != null) spaceLeft = cell.spaceLeft;
		if (cell.spaceBottom != null) spaceBottom = cell.spaceBottom;
		if (cell.spaceRight != null) spaceRight = cell.spaceRight;
		if (cell.padTop != null) padTop = cell.padTop;
		if (cell.padLeft != null) padLeft = cell.padLeft;
		if (cell.padBottom != null) padBottom = cell.padBottom;
		if (cell.padRight != null) padRight = cell.padRight;
		if (cell.fillX != null) fillX = cell.fillX;
		if (cell.fillY != null) fillY = cell.fillY;
		if (cell.align != null) align = cell.align;
		if (cell.expandX != null) expandX = cell.expandX;
		if (cell.expandY != null) expandY = cell.expandY;
		if (cell.colspan != null) colspan = cell.colspan;
		if (cell.uniformX != null) uniformX = cell.uniformX;
		if (cell.uniformY != null) uniformY = cell.uniformY;
	}

	public String toString () {
		return actor != null ? actor.toString() : super.toString();
	}

	/** Returns the defaults to use for all cells. This can be used to avoid needing to set the same defaults for every table (eg,
	 * for spacing). */
	static public Cell defaults () {
		if (files == null || files != Gdx.files) {
			files = Gdx.files;
			defaults = new Cell();
			defaults.minWidth = Value.minWidth;
			defaults.minHeight = Value.minHeight;
			defaults.maxWidth = Value.maxWidth;
			defaults.maxHeight = Value.maxHeight;
			defaults.spaceTop = Value.zero;
			defaults.spaceLeft = Value.zero;
			defaults.spaceBottom = Value.zero;
			defaults.spaceRight = Value.zero;
			defaults.padTop = Value.zero;
			defaults.padLeft = Value.zero;
			defaults.padBottom = Value.zero;
			defaults.padRight = Value.zero;
			defaults.fillX = zerof;
			defaults.fillY = zerof;
			defaults.align = centeri;
			defaults.expandX = zeroi;
			defaults.expandY = zeroi;
			defaults.colspan = onei;
			defaults.uniformX = null;
			defaults.uniformY = null;
		}
		return defaults;
	}
}
