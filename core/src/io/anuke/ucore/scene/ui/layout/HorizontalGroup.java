/*******************************************************************************
 * Copyright 2011 See AUTHORS file.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package io.anuke.ucore.scene.ui.layout;

import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.FloatArray;
import com.badlogic.gdx.utils.SnapshotArray;

import io.anuke.ucore.scene.Element;
import io.anuke.ucore.scene.event.Touchable;
import io.anuke.ucore.scene.ui.Label;
import io.anuke.ucore.scene.utils.Layout;

/** A group that lays out its children side by side horizontally, with optional wrapping. This can be easier than using
 * {@link Table} when actors need to be inserted into or removed from the middle of the group.
 * <p>
 * The preferred width is the sum of the children's preferred widths plus spacing. The preferred height is the largest preferred
 * height of any child. The preferred size is slightly different when {@link #wrap() wrap} is enabled. The min size is the
 * preferred size and the max size is 0.
 * <p>
 * Widgets are sized using their {@link Layout#getPrefWidth() preferred width}, so widgets which return 0 as their preferred width
 * will be given a width of 0 (eg, a label with {@link Label#setWrap(boolean) word wrap} enabled).
 * @author Nathan Sweet */
public class HorizontalGroup extends WidgetGroup {
	private float prefWidth, prefHeight, lastPrefHeight;
	private boolean sizeInvalid = true;
	private FloatArray rowSizes; // row width, row height, ...

	private int align = Align.left, rowAlign;
	private boolean reverse, round = true, wrap, expand;
	private float space, wrapSpace, fill, padTop, padLeft, padBottom, padRight;

	public HorizontalGroup () {
		setTouchable(Touchable.childrenOnly);
	}

	public void invalidate () {
		super.invalidate();
		sizeInvalid = true;
	}

	private void computeSize () {
		sizeInvalid = false;
		SnapshotArray<Element> children = getChildren();
		int n = children.size;
		prefHeight = 0;
		if (wrap) {
			prefWidth = 0;
			if (rowSizes == null)
				rowSizes = new FloatArray();
			else
				rowSizes.clear();
			FloatArray rowSizes = this.rowSizes;
			float space = this.space, wrapSpace = this.wrapSpace;
			float pad = padLeft + padRight, groupWidth = getWidth() - pad, x = 0, y = 0, rowHeight = 0;
			int i = 0, incr = 1;
			if (reverse) {
				i = n - 1;
				n = -1;
				incr = -1;
			}
			for (; i != n; i += incr) {
				Element child = children.get(i);

				float width, height;
				if (child instanceof Layout) {
					Layout layout = (Layout)child;
					width = layout.getPrefWidth();
					height = layout.getPrefHeight();
				} else {
					width = child.getWidth();
					height = child.getHeight();
				}

				float incrX = width + (x > 0 ? space : 0);
				if (x + incrX > groupWidth && x > 0) {
					rowSizes.add(x);
					rowSizes.add(rowHeight);
					prefWidth = Math.max(prefWidth, x + pad);
					if (y > 0) y += wrapSpace;
					y += rowHeight;
					rowHeight = 0;
					x = 0;
					incrX = width;
				}
				x += incrX;
				rowHeight = Math.max(rowHeight, height);
			}
			rowSizes.add(x);
			rowSizes.add(rowHeight);
			prefWidth = Math.max(prefWidth, x + pad);
			if (y > 0) y += wrapSpace;
			prefHeight = Math.max(prefHeight, y + rowHeight);
		} else {
			prefWidth = padLeft + padRight + space * (n - 1);
			for (int i = 0; i < n; i++) {
				Element child = children.get(i);
				if (child instanceof Layout) {
					Layout layout = (Layout)child;
					prefWidth += layout.getPrefWidth();
					prefHeight = Math.max(prefHeight, layout.getPrefHeight());
				} else {
					prefWidth += child.getWidth();
					prefHeight = Math.max(prefHeight, child.getHeight());
				}
			}
		}
		prefHeight += padTop + padBottom;
		if (round) {
			prefWidth = Math.round(prefWidth);
			prefHeight = Math.round(prefHeight);
		}
	}

	public void layout () {
		if (sizeInvalid) computeSize();

		if (wrap) {
			layoutWrapped();
			return;
		}

		boolean round = this.round;
		int align = this.align;
		float space = this.space, padBottom = this.padBottom, fill = this.fill;
		float rowHeight = (expand ? getHeight() : prefHeight) - padTop - padBottom, x = padLeft;

		if ((align & Align.right) != 0)
			x += getWidth() - prefWidth;
		else if ((align & Align.left) == 0) // center
			x += (getWidth() - prefWidth) / 2;

		float startY;
		if ((align & Align.bottom) != 0)
			startY = padBottom;
		else if ((align & Align.top) != 0)
			startY = getHeight() - padTop - rowHeight;
		else
			startY = padBottom + (getHeight() - padBottom - padTop - rowHeight) / 2;

		align = rowAlign;

		SnapshotArray<Element> children = getChildren();
		int i = 0, n = children.size, incr = 1;
		if (reverse) {
			i = n - 1;
			n = -1;
			incr = -1;
		}
		for (; i != n; i += incr) {
			Element child = children.get(i);

			float width, height;
			Layout layout = null;
			if (child instanceof Layout) {
				layout = (Layout)child;
				width = layout.getPrefWidth();
				height = layout.getPrefHeight();
			} else {
				width = child.getWidth();
				height = child.getHeight();
			}

			if (fill > 0) height = rowHeight * fill;

			if (layout != null) {
				height = Math.max(height, layout.getMinHeight());
				float maxHeight = layout.getMaxHeight();
				if (maxHeight > 0 && height > maxHeight) height = maxHeight;
			}

			float y = startY;
			if ((align & Align.top) != 0)
				y += rowHeight - height;
			else if ((align & Align.bottom) == 0) // center
				y += (rowHeight - height) / 2;

			if (round)
				child.setBounds(Math.round(x), Math.round(y), Math.round(width), Math.round(height));
			else
				child.setBounds(x, y, width, height);
			x += width + space;

			if (layout != null) layout.validate();
		}
	}

	private void layoutWrapped () {
		float prefHeight = getPrefHeight();
		if (prefHeight != lastPrefHeight) {
			lastPrefHeight = prefHeight;
			invalidateHierarchy();
		}

		int align = this.align;
		boolean round = this.round;
		float space = this.space, fill = this.fill, wrapSpace = this.wrapSpace;
		float maxWidth = prefWidth - padLeft - padRight;
		float rowY = prefHeight - padTop, groupWidth = getWidth(), xStart = padLeft, x = 0, rowHeight = 0;

		if ((align & Align.top) != 0)
			rowY += getHeight() - prefHeight;
		else if ((align & Align.bottom) == 0) // center
			rowY += (getHeight() - prefHeight) / 2;

		if ((align & Align.right) != 0)
			xStart += groupWidth - prefWidth;
		else if ((align & Align.left) == 0) // center
			xStart += (groupWidth - prefWidth) / 2;

		groupWidth -= padRight;
		align = this.rowAlign;

		FloatArray rowSizes = this.rowSizes;
		SnapshotArray<Element> children = getChildren();
		int i = 0, n = children.size, incr = 1;
		if (reverse) {
			i = n - 1;
			n = -1;
			incr = -1;
		}
		for (int r = 0; i != n; i += incr) {
			Element child = children.get(i);

			float width, height;
			Layout layout = null;
			if (child instanceof Layout) {
				layout = (Layout)child;
				width = layout.getPrefWidth();
				height = layout.getPrefHeight();
			} else {
				width = child.getWidth();
				height = child.getHeight();
			}

			if (x + width > groupWidth || r == 0) {
				x = xStart;
				if ((align & Align.right) != 0)
					x += maxWidth - rowSizes.get(r);
				else if ((align & Align.left) == 0) // center
					x += (maxWidth - rowSizes.get(r)) / 2;
				rowHeight = rowSizes.get(r + 1);
				if (r > 0) rowY -= wrapSpace;
				rowY -= rowHeight;
				r += 2;
			}

			if (fill > 0) height = rowHeight * fill;

			if (layout != null) {
				height = Math.max(height, layout.getMinHeight());
				float maxHeight = layout.getMaxHeight();
				if (maxHeight > 0 && height > maxHeight) height = maxHeight;
			}

			float y = rowY;
			if ((align & Align.top) != 0)
				y += rowHeight - height;
			else if ((align & Align.bottom) == 0) // center
				y += (rowHeight - height) / 2;

			if (round)
				child.setBounds(Math.round(x), Math.round(y), Math.round(width), Math.round(height));
			else
				child.setBounds(x, y, width, height);
			x += width + space;

			if (layout != null) layout.validate();
		}
	}

	public float getPrefWidth () {
		if (wrap) return 0;
		if (sizeInvalid) computeSize();
		return prefWidth;
	}

	public float getPrefHeight () {
		if (sizeInvalid) computeSize();
		return prefHeight;
	}

	/** If true (the default), positions and sizes are rounded to integers. */
	public void setRound (boolean round) {
		this.round = round;
	}

	/** The children will be displayed last to first. */
	public HorizontalGroup reverse () {
		this.reverse = true;
		return this;
	}

	/** If true, the children will be displayed last to first. */
	public HorizontalGroup reverse (boolean reverse) {
		this.reverse = reverse;
		return this;
	}

	public boolean getReverse () {
		return reverse;
	}

	/** Sets the horizontal space between children. */
	public HorizontalGroup space (float space) {
		this.space = space;
		return this;
	}

	public float getSpace () {
		return space;
	}

	/** Sets the vertical space between rows when wrap is enabled. */
	public HorizontalGroup wrapSpace (float wrapSpace) {
		this.wrapSpace = wrapSpace;
		return this;
	}

	public float getWrapSpace () {
		return wrapSpace;
	}

	/** Sets the marginTop, marginLeft, marginBottom, and marginRight to the specified value. */
	public HorizontalGroup pad (float pad) {
		padTop = pad;
		padLeft = pad;
		padBottom = pad;
		padRight = pad;
		return this;
	}

	public HorizontalGroup pad (float top, float left, float bottom, float right) {
		padTop = top;
		padLeft = left;
		padBottom = bottom;
		padRight = right;
		return this;
	}

	public HorizontalGroup padTop (float padTop) {
		this.padTop = padTop;
		return this;
	}

	public HorizontalGroup padLeft (float padLeft) {
		this.padLeft = padLeft;
		return this;
	}

	public HorizontalGroup padBottom (float padBottom) {
		this.padBottom = padBottom;
		return this;
	}

	public HorizontalGroup padRight (float padRight) {
		this.padRight = padRight;
		return this;
	}

	public float getPadTop () {
		return padTop;
	}

	public float getPadLeft () {
		return padLeft;
	}

	public float getPadBottom () {
		return padBottom;
	}

	public float getPadRight () {
		return padRight;
	}

	/** Sets the alignment of all widgets within the horizontal group. Set to {@link Align#center}, {@link Align#top},
	 * {@link Align#bottom}, {@link Align#left}, {@link Align#right}, or any combination of those. */
	public HorizontalGroup align (int align) {
		this.align = align;
		return this;
	}

	/** Sets the alignment of all widgets within the horizontal group to {@link Align#center}. This clears any other alignment. */
	public HorizontalGroup center () {
		align = Align.center;
		return this;
	}

	/** Sets {@link Align#top} and clears {@link Align#bottom} for the alignment of all widgets within the horizontal group. */
	public HorizontalGroup top () {
		align |= Align.top;
		align &= ~Align.bottom;
		return this;
	}

	/** Adds {@link Align#left} and clears {@link Align#right} for the alignment of all widgets within the horizontal group. */
	public HorizontalGroup left () {
		align |= Align.left;
		align &= ~Align.right;
		return this;
	}

	/** Sets {@link Align#bottom} and clears {@link Align#top} for the alignment of all widgets within the horizontal group. */
	public HorizontalGroup bottom () {
		align |= Align.bottom;
		align &= ~Align.top;
		return this;
	}

	/** Adds {@link Align#right} and clears {@link Align#left} for the alignment of all widgets within the horizontal group. */
	public HorizontalGroup right () {
		align |= Align.right;
		align &= ~Align.left;
		return this;
	}

	public int getAlign () {
		return align;
	}

	public HorizontalGroup fill () {
		fill = 1f;
		return this;
	}

	/** @param fill 0 will use preferred width. */
	public HorizontalGroup fill (float fill) {
		this.fill = fill;
		return this;
	}

	public float getFill () {
		return fill;
	}

	public HorizontalGroup expand () {
		expand = true;
		return this;
	}

	/** When true and wrap is false, the rows will take up the entire horizontal group height. */
	public HorizontalGroup expand (boolean expand) {
		this.expand = expand;
		return this;
	}

	public boolean getExpand () {
		return expand;
	}

	/** Sets fill to 1 and expand to true. */
	public HorizontalGroup grow () {
		expand = true;
		fill = 1;
		return this;
	}

	/** If false, the widgets are arranged in a single row and the preferred width is the widget widths plus spacing. If true, the
	 * widgets will wrap using the width of the horizontal group. The preferred width of the group will be 0 as it is expected that
	 * something external will set the width of the group. Default is false.
	 * <p>
	 * When wrap is enabled, the group's preferred height depends on the width of the group. In some cases the parent of the group
	 * will need to layout twice: once to set the width of the group and a second time to adjust to the group's new preferred
	 * height. */
	public HorizontalGroup wrap () {
		wrap = true;
		return this;
	}

	public HorizontalGroup wrap (boolean wrap) {
		this.wrap = wrap;
		return this;
	}

	public boolean getWrap () {
		return wrap;
	}

	/** Sets the alignment of widgets within each row of the horizontal group. Set to {@link Align#center}, {@link Align#top}, or
	 * {@link Align#bottom}. */
	public HorizontalGroup rowAlign (int row) {
		this.rowAlign = row;
		return this;
	}

	/** Sets the alignment of widgets within each row to {@link Align#center}. This clears any other alignment. */
	public HorizontalGroup rowCenter () {
		rowAlign = Align.center;
		return this;
	}

	/** Sets {@link Align#top} and clears {@link Align#bottom} for the alignment of widgets within each row. */
	public HorizontalGroup rowTop () {
		rowAlign |= Align.top;
		rowAlign &= ~Align.bottom;
		return this;
	}

	/** Sets {@link Align#bottom} and clears {@link Align#top} for the alignment of widgets within each row. */
	public HorizontalGroup rowBottom () {
		rowAlign |= Align.bottom;
		rowAlign &= ~Align.top;
		return this;
	}

	protected void drawDebugBounds (ShapeRenderer shapes) {
		super.drawDebugBounds(shapes);
		if (!getDebug()) return;
		shapes.set(ShapeType.Line);
		shapes.setColor(getScene().getDebugColor());
		shapes.rect(getX() + padLeft, getY() + padBottom, getOriginX(), getOriginY(), getWidth() - padLeft - padRight,
			getHeight() - padBottom - padTop, getScaleX(), getScaleY(), getRotation());
	}
}
