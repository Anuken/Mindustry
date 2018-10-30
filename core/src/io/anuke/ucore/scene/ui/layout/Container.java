
package io.anuke.ucore.scene.ui.layout;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.Align;

import io.anuke.ucore.scene.Element;
import io.anuke.ucore.scene.event.Touchable;
import io.anuke.ucore.scene.style.Drawable;
import io.anuke.ucore.scene.ui.layout.Value.Fixed;
import io.anuke.ucore.scene.utils.Layout;

/** A group with a single child that sizes and positions the child using constraints. This provides layout similar to a
 * {@link Table} with a single cell but is more lightweight.
 * @author Nathan Sweet */
public class Container<T extends Element> extends WidgetGroup {
	private T actor;
	private Value minWidth = Value.minWidth, minHeight = Value.minHeight;
	private Value prefWidth = Value.prefWidth, prefHeight = Value.prefHeight;
	private Value maxWidth = Value.zero, maxHeight = Value.zero;
	private Value padTop = Value.zero, padLeft = Value.zero, padBottom = Value.zero, padRight = Value.zero;
	private float fillX, fillY;
	private int align;
	private Drawable background;
	private boolean clip;
	private boolean round = true;

	/** Creates a container with no actor. */
	public Container () {
		setTouchable(Touchable.childrenOnly);
		setTransform(false);
	}

	public Container (T actor) {
		this();
		setActor(actor);
	}

	public void draw (Batch batch, float parentAlpha) {
		validate();
		if (isTransform()) {
			applyTransform(batch, computeTransform());
			drawBackground(batch, parentAlpha, 0, 0);
			if (clip) {
				batch.flush();
				float padLeft = this.padLeft.get(this), padBottom = this.padBottom.get(this);
				if (clipBegin(padLeft, padBottom, getWidth() - padLeft - padRight.get(this),
					getHeight() - padBottom - padTop.get(this))) {
					drawChildren(batch, parentAlpha);
					batch.flush();
					clipEnd();
				}
			} else
				drawChildren(batch, parentAlpha);
			resetTransform(batch);
		} else {
			drawBackground(batch, parentAlpha, getX(), getY());
			super.draw(batch, parentAlpha);
		}
	}

	/** Called to draw the background, before clipping is applied (if enabled). Default implementation draws the background
	 * drawable. */
	protected void drawBackground (Batch batch, float parentAlpha, float x, float y) {
		if (background == null) return;
		Color color = getColor();
		batch.setColor(color.r, color.g, color.b, color.a * parentAlpha);
		background.draw(batch, x, y, getWidth(), getHeight());
	}

	/** Sets the background drawable and adjusts the container's padding to match the background.
	 * @see #setBackground(Drawable, boolean) */
	public void setBackground (Drawable background) {
		setBackground(background, true);
	}

	/** Sets the background drawable and, if adjustPadding is true, sets the container's padding to
	 * {@link Drawable#getBottomHeight()} , {@link Drawable#getTopHeight()}, {@link Drawable#getLeftWidth()}, and
	 * {@link Drawable#getRightWidth()}.
	 * @param background If null, the background will be cleared and padding removed. */
	public void setBackground (Drawable background, boolean adjustPadding) {
		if (this.background == background) return;
		this.background = background;
		if (adjustPadding) {
			if (background == null)
				pad(Value.zero);
			else
				pad(background.getTopHeight(), background.getLeftWidth(), background.getBottomHeight(), background.getRightWidth());
			invalidate();
		}
	}

	/** @see #setBackground(Drawable) */
	public Container<T> background (Drawable background) {
		setBackground(background);
		return this;
	}

	public Drawable getBackground () {
		return background;
	}

	public void layout () {
		if (actor == null) return;

		float padLeft = this.padLeft.get(this), padBottom = this.padBottom.get(this);
		float containerWidth = getWidth() - padLeft - padRight.get(this);
		float containerHeight = getHeight() - padBottom - padTop.get(this);
		float minWidth = this.minWidth.get(actor), minHeight = this.minHeight.get(actor);
		float prefWidth = this.prefWidth.get(actor), prefHeight = this.prefHeight.get(actor);
		float maxWidth = this.maxWidth.get(actor), maxHeight = this.maxHeight.get(actor);

		float width;
		if (fillX > 0)
			width = containerWidth * fillX;
		else
			width = Math.min(prefWidth, containerWidth);
		if (width < minWidth) width = minWidth;
		if (maxWidth > 0 && width > maxWidth) width = maxWidth;

		float height;
		if (fillY > 0)
			height = containerHeight * fillY;
		else
			height = Math.min(prefHeight, containerHeight);
		if (height < minHeight) height = minHeight;
		if (maxHeight > 0 && height > maxHeight) height = maxHeight;

		float x = padLeft;
		if ((align & Align.right) != 0)
			x += containerWidth - width;
		else if ((align & Align.left) == 0) // center
			x += (containerWidth - width) / 2;

		float y = padBottom;
		if ((align & Align.top) != 0)
			y += containerHeight - height;
		else if ((align & Align.bottom) == 0) // center
			y += (containerHeight - height) / 2;

		if (round) {
			x = Math.round(x);
			y = Math.round(y);
			width = Math.round(width);
			height = Math.round(height);
		}

		actor.setBounds(x, y, width, height);
		if (actor instanceof Layout) ((Layout)actor).validate();
	}

	/** @param actor May be null. */
	public void setActor (T actor) {
		if (actor == this) throw new IllegalArgumentException("actor cannot be the Container.");
		if (actor == this.actor) return;
		if (this.actor != null) super.removeChild(this.actor);
		this.actor = actor;
		if (actor != null) super.addChild(actor);
	}

	/** @return May be null. */
	public T getActor () {
		return actor;
	}

	public boolean removeChild (Element actor) {
		if (actor == null) throw new IllegalArgumentException("actor cannot be null.");
		if (actor != this.actor) return false;
		setActor(null);
		return true;
	}

	public boolean removeChild (Element actor, boolean unfocus) {
		if (actor == null) throw new IllegalArgumentException("actor cannot be null.");
		if (actor != this.actor) return false;
		this.actor = null;
		return super.removeChild(actor, unfocus);
	}

	/** Sets the minWidth, prefWidth, maxWidth, minHeight, prefHeight, and maxHeight to the specified value. */
	public Container<T> size (Value size) {
		if (size == null) throw new IllegalArgumentException("size cannot be null.");
		minWidth = size;
		minHeight = size;
		prefWidth = size;
		prefHeight = size;
		maxWidth = size;
		maxHeight = size;
		return this;
	}

	/** Sets the minWidth, prefWidth, maxWidth, minHeight, prefHeight, and maxHeight to the specified values. */
	public Container<T> size (Value width, Value height) {
		if (width == null) throw new IllegalArgumentException("width cannot be null.");
		if (height == null) throw new IllegalArgumentException("height cannot be null.");
		minWidth = width;
		minHeight = height;
		prefWidth = width;
		prefHeight = height;
		maxWidth = width;
		maxHeight = height;
		return this;
	}

	/** Sets the minWidth, prefWidth, maxWidth, minHeight, prefHeight, and maxHeight to the specified value. */
	public Container<T> size (float size) {
		size(new Fixed(size));
		return this;
	}

	/** Sets the minWidth, prefWidth, maxWidth, minHeight, prefHeight, and maxHeight to the specified values. */
	public Container<T> size (float width, float height) {
		size(new Fixed(width), new Fixed(height));
		return this;
	}

	/** Sets the minWidth, prefWidth, and maxWidth to the specified value. */
	public Container<T> width (Value width) {
		if (width == null) throw new IllegalArgumentException("width cannot be null.");
		minWidth = width;
		prefWidth = width;
		maxWidth = width;
		return this;
	}

	/** Sets the minWidth, prefWidth, and maxWidth to the specified value. */
	public Container<T> width (float width) {
		width(new Fixed(width));
		return this;
	}

	/** Sets the minHeight, prefHeight, and maxHeight to the specified value. */
	public Container<T> height (Value height) {
		if (height == null) throw new IllegalArgumentException("height cannot be null.");
		minHeight = height;
		prefHeight = height;
		maxHeight = height;
		return this;
	}

	/** Sets the minHeight, prefHeight, and maxHeight to the specified value. */
	public Container<T> height (float height) {
		height(new Fixed(height));
		return this;
	}

	/** Sets the minWidth and minHeight to the specified value. */
	public Container<T> minSize (Value size) {
		if (size == null) throw new IllegalArgumentException("size cannot be null.");
		minWidth = size;
		minHeight = size;
		return this;
	}

	/** Sets the minWidth and minHeight to the specified values. */
	public Container<T> minSize (Value width, Value height) {
		if (width == null) throw new IllegalArgumentException("width cannot be null.");
		if (height == null) throw new IllegalArgumentException("height cannot be null.");
		minWidth = width;
		minHeight = height;
		return this;
	}

	public Container<T> minWidth (Value minWidth) {
		if (minWidth == null) throw new IllegalArgumentException("minWidth cannot be null.");
		this.minWidth = minWidth;
		return this;
	}

	public Container<T> minHeight (Value minHeight) {
		if (minHeight == null) throw new IllegalArgumentException("minHeight cannot be null.");
		this.minHeight = minHeight;
		return this;
	}

	/** Sets the minWidth and minHeight to the specified value. */
	public Container<T> minSize (float size) {
		minSize(new Fixed(size));
		return this;
	}

	/** Sets the minWidth and minHeight to the specified values. */
	public Container<T> minSize (float width, float height) {
		minSize(new Fixed(width), new Fixed(height));
		return this;
	}

	public Container<T> minWidth (float minWidth) {
		this.minWidth = new Fixed(minWidth);
		return this;
	}

	public Container<T> minHeight (float minHeight) {
		this.minHeight = new Fixed(minHeight);
		return this;
	}

	/** Sets the prefWidth and prefHeight to the specified value. */
	public Container<T> prefSize (Value size) {
		if (size == null) throw new IllegalArgumentException("size cannot be null.");
		prefWidth = size;
		prefHeight = size;
		return this;
	}

	/** Sets the prefWidth and prefHeight to the specified values. */
	public Container<T> prefSize (Value width, Value height) {
		if (width == null) throw new IllegalArgumentException("width cannot be null.");
		if (height == null) throw new IllegalArgumentException("height cannot be null.");
		prefWidth = width;
		prefHeight = height;
		return this;
	}

	public Container<T> prefWidth (Value prefWidth) {
		if (prefWidth == null) throw new IllegalArgumentException("prefWidth cannot be null.");
		this.prefWidth = prefWidth;
		return this;
	}

	public Container<T> prefHeight (Value prefHeight) {
		if (prefHeight == null) throw new IllegalArgumentException("prefHeight cannot be null.");
		this.prefHeight = prefHeight;
		return this;
	}

	/** Sets the prefWidth and prefHeight to the specified value. */
	public Container<T> prefSize (float width, float height) {
		prefSize(new Fixed(width), new Fixed(height));
		return this;
	}

	/** Sets the prefWidth and prefHeight to the specified values. */
	public Container<T> prefSize (float size) {
		prefSize(new Fixed(size));
		return this;
	}

	public Container<T> prefWidth (float prefWidth) {
		this.prefWidth = new Fixed(prefWidth);
		return this;
	}

	public Container<T> prefHeight (float prefHeight) {
		this.prefHeight = new Fixed(prefHeight);
		return this;
	}

	/** Sets the maxWidth and maxHeight to the specified value. */
	public Container<T> maxSize (Value size) {
		if (size == null) throw new IllegalArgumentException("size cannot be null.");
		maxWidth = size;
		maxHeight = size;
		return this;
	}

	/** Sets the maxWidth and maxHeight to the specified values. */
	public Container<T> maxSize (Value width, Value height) {
		if (width == null) throw new IllegalArgumentException("width cannot be null.");
		if (height == null) throw new IllegalArgumentException("height cannot be null.");
		maxWidth = width;
		maxHeight = height;
		return this;
	}

	public Container<T> maxWidth (Value maxWidth) {
		if (maxWidth == null) throw new IllegalArgumentException("maxWidth cannot be null.");
		this.maxWidth = maxWidth;
		return this;
	}

	public Container<T> maxHeight (Value maxHeight) {
		if (maxHeight == null) throw new IllegalArgumentException("maxHeight cannot be null.");
		this.maxHeight = maxHeight;
		return this;
	}

	/** Sets the maxWidth and maxHeight to the specified value. */
	public Container<T> maxSize (float size) {
		maxSize(new Fixed(size));
		return this;
	}

	/** Sets the maxWidth and maxHeight to the specified values. */
	public Container<T> maxSize (float width, float height) {
		maxSize(new Fixed(width), new Fixed(height));
		return this;
	}

	public Container<T> maxWidth (float maxWidth) {
		this.maxWidth = new Fixed(maxWidth);
		return this;
	}

	public Container<T> maxHeight (float maxHeight) {
		this.maxHeight = new Fixed(maxHeight);
		return this;
	}

	/** Sets the marginTop, marginLeft, marginBottom, and marginRight to the specified value. */
	public Container<T> pad (Value pad) {
		if (pad == null) throw new IllegalArgumentException("margin cannot be null.");
		padTop = pad;
		padLeft = pad;
		padBottom = pad;
		padRight = pad;
		return this;
	}

	public Container<T> pad (Value top, Value left, Value bottom, Value right) {
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

	public Container<T> padTop (Value padTop) {
		if (padTop == null) throw new IllegalArgumentException("marginTop cannot be null.");
		this.padTop = padTop;
		return this;
	}

	public Container<T> padLeft (Value padLeft) {
		if (padLeft == null) throw new IllegalArgumentException("marginLeft cannot be null.");
		this.padLeft = padLeft;
		return this;
	}

	public Container<T> padBottom (Value padBottom) {
		if (padBottom == null) throw new IllegalArgumentException("marginBottom cannot be null.");
		this.padBottom = padBottom;
		return this;
	}

	public Container<T> padRight (Value padRight) {
		if (padRight == null) throw new IllegalArgumentException("marginRight cannot be null.");
		this.padRight = padRight;
		return this;
	}

	/** Sets the marginTop, marginLeft, marginBottom, and marginRight to the specified value. */
	public Container<T> pad (float pad) {
		Value value = new Fixed(pad);
		padTop = value;
		padLeft = value;
		padBottom = value;
		padRight = value;
		return this;
	}

	public Container<T> pad (float top, float left, float bottom, float right) {
		padTop = new Fixed(top);
		padLeft = new Fixed(left);
		padBottom = new Fixed(bottom);
		padRight = new Fixed(right);
		return this;
	}

	public Container<T> padTop (float padTop) {
		this.padTop = new Fixed(padTop);
		return this;
	}

	public Container<T> padLeft (float padLeft) {
		this.padLeft = new Fixed(padLeft);
		return this;
	}

	public Container<T> padBottom (float padBottom) {
		this.padBottom = new Fixed(padBottom);
		return this;
	}

	public Container<T> padRight (float padRight) {
		this.padRight = new Fixed(padRight);
		return this;
	}

	/** Sets fillX and fillY to 1. */
	public Container<T> fill () {
		fillX = 1f;
		fillY = 1f;
		return this;
	}

	/** Sets fillX to 1. */
	public Container<T> fillX () {
		fillX = 1f;
		return this;
	}

	/** Sets fillY to 1. */
	public Container<T> fillY () {
		fillY = 1f;
		return this;
	}

	public Container<T> fill (float x, float y) {
		fillX = x;
		fillY = y;
		return this;
	}

	/** Sets fillX and fillY to 1 if true, 0 if false. */
	public Container<T> fill (boolean x, boolean y) {
		fillX = x ? 1f : 0;
		fillY = y ? 1f : 0;
		return this;
	}

	/** Sets fillX and fillY to 1 if true, 0 if false. */
	public Container<T> fill (boolean fill) {
		fillX = fill ? 1f : 0;
		fillY = fill ? 1f : 0;
		return this;
	}

	/** Sets the alignment of the actor within the container. Set to {@link Align#center}, {@link Align#top}, {@link Align#bottom},
	 * {@link Align#left}, {@link Align#right}, or any combination of those. */
	public Container<T> align (int align) {
		this.align = align;
		return this;
	}

	/** Sets the alignment of the actor within the container to {@link Align#center}. This clears any other alignment. */
	public Container<T> center () {
		align = Align.center;
		return this;
	}

	/** Sets {@link Align#top} and clears {@link Align#bottom} for the alignment of the actor within the container. */
	public Container<T> top () {
		align |= Align.top;
		align &= ~Align.bottom;
		return this;
	}

	/** Sets {@link Align#left} and clears {@link Align#right} for the alignment of the actor within the container. */
	public Container<T> left () {
		align |= Align.left;
		align &= ~Align.right;
		return this;
	}

	/** Sets {@link Align#bottom} and clears {@link Align#top} for the alignment of the actor within the container. */
	public Container<T> bottom () {
		align |= Align.bottom;
		align &= ~Align.top;
		return this;
	}

	/** Sets {@link Align#right} and clears {@link Align#left} for the alignment of the actor within the container. */
	public Container<T> right () {
		align |= Align.right;
		align &= ~Align.left;
		return this;
	}

	public float getMinWidth () {
		return minWidth.get(actor) + padLeft.get(this) + padRight.get(this);
	}

	public Value getMinHeightValue () {
		return minHeight;
	}

	public float getMinHeight () {
		return minHeight.get(actor) + padTop.get(this) + padBottom.get(this);
	}

	public Value getPrefWidthValue () {
		return prefWidth;
	}

	public float getPrefWidth () {
		float v = prefWidth.get(actor);
		if (background != null) v = Math.max(v, background.getMinWidth());
		return Math.max(getMinWidth(), v + padLeft.get(this) + padRight.get(this));
	}

	public Value getPrefHeightValue () {
		return prefHeight;
	}

	public float getPrefHeight () {
		float v = prefHeight.get(actor);
		if (background != null) v = Math.max(v, background.getMinHeight());
		return Math.max(getMinHeight(), v + padTop.get(this) + padBottom.get(this));
	}
	
	/*
	public Value getMaxWidthValue () {
		return maxWidth;
	}
	
	public float getMaxWidth () {
		float v = maxWidth.get(actor);
		if (v > 0) v += marginLeft.get(this) + marginRight.get(this);
		return v;
	}

	public Value getMaxHeightValue () {
		return maxHeight;
	}

	public float getMaxHeight () {
		float v = maxHeight.get(actor);
		if (v > 0) v += marginTop.get(this) + marginBottom.get(this);
		return v;
	}
*/
	/** @return May be null if this value is not set. */
	public Value getPadTopValue () {
		return padTop;
	}

	public float getPadTop () {
		return padTop.get(this);
	}

	/** @return May be null if this value is not set. */
	public Value getPadLeftValue () {
		return padLeft;
	}

	public float getPadLeft () {
		return padLeft.get(this);
	}

	/** @return May be null if this value is not set. */
	public Value getPadBottomValue () {
		return padBottom;
	}

	public float getPadBottom () {
		return padBottom.get(this);
	}

	/** @return May be null if this value is not set. */
	public Value getPadRightValue () {
		return padRight;
	}

	public float getPadRight () {
		return padRight.get(this);
	}

	/** Returns {@link #getPadLeft()} plus {@link #getPadRight()}. */
	public float getPadX () {
		return padLeft.get(this) + padRight.get(this);
	}

	/** Returns {@link #getPadTop()} plus {@link #getPadBottom()}. */
	public float getPadY () {
		return padTop.get(this) + padBottom.get(this);
	}

	public float getFillX () {
		return fillX;
	}

	public float getFillY () {
		return fillY;
	}

	public int getAlign () {
		return align;
	}

	/** If true (the default), positions and sizes are rounded to integers. */
	public void setRound (boolean round) {
		this.round = round;
	}

	/** Causes the contents to be clipped if they exceed the container bounds. Enabling clipping will set
	 * {@link #setTransform(boolean)} to true. */
	public void setClip (boolean enabled) {
		clip = enabled;
		setTransform(enabled);
		invalidate();
	}

	public boolean getClip () {
		return clip;
	}

	public Element hit (float x, float y, boolean touchable) {
		if (clip) {
			if (touchable && getTouchable() == Touchable.disabled) return null;
			if (x < 0 || x >= getWidth() || y < 0 || y >= getHeight()) return null;
		}
		return super.hit(x, y, touchable);
	}

	public void drawDebug (ShapeRenderer shapes) {
		validate();
		if (isTransform()) {
			applyTransform(shapes, computeTransform());
			if (clip) {
				shapes.flush();
				float padLeft = this.padLeft.get(this), padBottom = this.padBottom.get(this);
				boolean draw = background == null ? clipBegin(0, 0, getWidth(), getHeight())
					: clipBegin(padLeft, padBottom, getWidth() - padLeft - padRight.get(this),
						getHeight() - padBottom - padTop.get(this));
				if (draw) {
					drawDebugChildren(shapes);
					clipEnd();
				}
			} else
				drawDebugChildren(shapes);
			resetTransform(shapes);
		} else
			super.drawDebug(shapes);
	}
}
