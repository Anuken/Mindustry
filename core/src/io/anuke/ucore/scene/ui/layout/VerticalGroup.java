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
import io.anuke.ucore.scene.utils.Layout;

/** A group that lays out its children top to bottom vertically, with optional wrapping. This can be easier than using
 * {@link Table} when actors need to be inserted into or removed from the middle of the group.
 * <p>
 * The preferred width is the largest preferred width of any child. The preferred height is the sum of the children's preferred
 * heights plus spacing. The preferred size is slightly different when {@link #wrap() wrap} is enabled. The min size is the
 * preferred size and the max size is 0.
 * <p>
 * Widgets are sized using their {@link Layout#getPrefWidth() preferred height}, so widgets which return 0 as their preferred
 * height will be given a height of 0.
 * @author Nathan Sweet */
public class VerticalGroup extends WidgetGroup {
	private float prefWidth, prefHeight, lastPrefWidth;
	private boolean sizeInvalid = true;
	private FloatArray columnSizes; // column height, column width, ...

	private int align = Align.top, columnAlign;
	private boolean reverse, round = true, wrap, expand;
	private float space, wrapSpace, fill, padTop, padLeft, padBottom, padRight;

	public VerticalGroup () {
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
		prefWidth = 0;
		if (wrap) {
			prefHeight = 0;
			if (columnSizes == null)
				columnSizes = new FloatArray();
			else
				columnSizes.clear();
			FloatArray columnSizes = this.columnSizes;
			float space = this.space, wrapSpace = this.wrapSpace;
			float pad = padTop + padBottom, groupHeight = getHeight() - pad, x = 0, y = 0, columnWidth = 0;
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

				float incrY = height + (y > 0 ? space : 0);
				if (y + incrY > groupHeight && y > 0) {
					columnSizes.add(y);
					columnSizes.add(columnWidth);
					prefHeight = Math.max(prefHeight, y + pad);
					if (x > 0) x += wrapSpace;
					x += columnWidth;
					columnWidth = 0;
					y = 0;
					incrY = height;
				}
				y += incrY;
				columnWidth = Math.max(columnWidth, width);
			}
			columnSizes.add(y);
			columnSizes.add(columnWidth);
			prefHeight = Math.max(prefHeight, y + pad);
			if (x > 0) x += wrapSpace;
			prefWidth = Math.max(prefWidth, x + columnWidth);
		} else {
			prefHeight = padTop + padBottom + space * (n - 1);
			for (int i = 0; i < n; i++) {
				Element child = children.get(i);
				if (child instanceof Layout) {
					Layout layout = (Layout)child;
					prefWidth = Math.max(prefWidth, layout.getPrefWidth());
					prefHeight += layout.getPrefHeight();
				} else {
					prefWidth = Math.max(prefWidth, child.getWidth());
					prefHeight += child.getHeight();
				}
			}
		}
		prefWidth += padLeft + padRight;
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
		float space = this.space, padLeft = this.padLeft, fill = this.fill;
		float columnWidth = (expand ? getWidth() : prefWidth) - padLeft - padRight, y = prefHeight - padTop + space;

		if ((align & Align.top) != 0)
			y += getHeight() - prefHeight;
		else if ((align & Align.bottom) == 0) // center
			y += (getHeight() - prefHeight) / 2;

		float startX;
		if ((align & Align.left) != 0)
			startX = padLeft;
		else if ((align & Align.right) != 0)
			startX = getWidth() - padRight - columnWidth;
		else
			startX = padLeft + (getWidth() - padLeft - padRight - columnWidth) / 2;

		align = columnAlign;

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

			if (fill > 0) width = columnWidth * fill;

			if (layout != null) {
				width = Math.max(width, layout.getMinWidth());
				float maxWidth = layout.getMaxWidth();
				if (maxWidth > 0 && width > maxWidth) width = maxWidth;
			}

			float x = startX;
			if ((align & Align.right) != 0)
				x += columnWidth - width;
			else if ((align & Align.left) == 0) // center
				x += (columnWidth - width) / 2;

			y -= height + space;
			if (round)
				child.setBounds(Math.round(x), Math.round(y), Math.round(width), Math.round(height));
			else
				child.setBounds(x, y, width, height);

			if (layout != null) layout.validate();
		}
	}

	private void layoutWrapped () {
		float prefWidth = getPrefWidth();
		if (prefWidth != lastPrefWidth) {
			lastPrefWidth = prefWidth;
			invalidateHierarchy();
		}

		int align = this.align;
		boolean round = this.round;
		float space = this.space, padLeft = this.padLeft, fill = this.fill, wrapSpace = this.wrapSpace;
		float maxHeight = prefHeight - padTop - padBottom;
		float columnX = padLeft, groupHeight = getHeight();
		float yStart = prefHeight - padTop + space, y = 0, columnWidth = 0;

		if ((align & Align.right) != 0)
			columnX += getWidth() - prefWidth;
		else if ((align & Align.left) == 0) // center
			columnX += (getWidth() - prefWidth) / 2;

		if ((align & Align.top) != 0)
			yStart += groupHeight - prefHeight;
		else if ((align & Align.bottom) == 0) // center
			yStart += (groupHeight - prefHeight) / 2;

		groupHeight -= padTop;
		align = columnAlign;

		FloatArray columnSizes = this.columnSizes;
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

			if (y - height - space < padBottom || r == 0) {
				y = yStart;
				if ((align & Align.bottom) != 0)
					y -= maxHeight - columnSizes.get(r);
				else if ((align & Align.top) == 0) // center
					y -= (maxHeight - columnSizes.get(r)) / 2;
				if (r > 0) {
					columnX += wrapSpace;
					columnX += columnWidth;
				}
				columnWidth = columnSizes.get(r + 1);
				r += 2;
			}

			if (fill > 0) width = columnWidth * fill;

			if (layout != null) {
				width = Math.max(width, layout.getMinWidth());
				float maxWidth = layout.getMaxWidth();
				if (maxWidth > 0 && width > maxWidth) width = maxWidth;
			}

			float x = columnX;
			if ((align & Align.right) != 0)
				x += columnWidth - width;
			else if ((align & Align.left) == 0) // center
				x += (columnWidth - width) / 2;

			y -= height + space;
			if (round)
				child.setBounds(Math.round(x), Math.round(y), Math.round(width), Math.round(height));
			else
				child.setBounds(x, y, width, height);

			if (layout != null) layout.validate();
		}
	}

	public float getPrefWidth () {
		if (sizeInvalid) computeSize();
		return prefWidth;
	}

	public float getPrefHeight () {
		if (wrap) return 0;
		if (sizeInvalid) computeSize();
		return prefHeight;
	}

	/** If true (the default), positions and sizes are rounded to integers. */
	public void setRound (boolean round) {
		this.round = round;
	}

	/** The children will be displayed last to first. */
	public VerticalGroup reverse () {
		this.reverse = true;
		return this;
	}

	/** If true, the children will be displayed last to first. */
	public VerticalGroup reverse (boolean reverse) {
		this.reverse = reverse;
		return this;
	}

	public boolean getReverse () {
		return reverse;
	}

	/** Sets the vertical space between children. */
	public VerticalGroup space (float space) {
		this.space = space;
		return this;
	}

	public float getSpace () {
		return space;
	}

	/** Sets the horizontal space between columns when wrap is enabled. */
	public VerticalGroup wrapSpace (float wrapSpace) {
		this.wrapSpace = wrapSpace;
		return this;
	}

	public float getWrapSpace () {
		return wrapSpace;
	}

	/** Sets the marginTop, marginLeft, marginBottom, and marginRight to the specified value. */
	public VerticalGroup pad (float pad) {
		padTop = pad;
		padLeft = pad;
		padBottom = pad;
		padRight = pad;
		return this;
	}

	public VerticalGroup pad (float top, float left, float bottom, float right) {
		padTop = top;
		padLeft = left;
		padBottom = bottom;
		padRight = right;
		return this;
	}

	public VerticalGroup padTop (float padTop) {
		this.padTop = padTop;
		return this;
	}

	public VerticalGroup padLeft (float padLeft) {
		this.padLeft = padLeft;
		return this;
	}

	public VerticalGroup padBottom (float padBottom) {
		this.padBottom = padBottom;
		return this;
	}

	public VerticalGroup padRight (float padRight) {
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

	/** Sets the alignment of all widgets within the vertical group. Set to {@link Align#center}, {@link Align#top},
	 * {@link Align#bottom}, {@link Align#left}, {@link Align#right}, or any combination of those. */
	public VerticalGroup align (int align) {
		this.align = align;
		return this;
	}

	/** Sets the alignment of all widgets within the vertical group to {@link Align#center}. This clears any other alignment. */
	public VerticalGroup center () {
		align = Align.center;
		return this;
	}

	/** Sets {@link Align#top} and clears {@link Align#bottom} for the alignment of all widgets within the vertical group. */
	public VerticalGroup top () {
		align |= Align.top;
		align &= ~Align.bottom;
		return this;
	}

	/** Adds {@link Align#left} and clears {@link Align#right} for the alignment of all widgets within the vertical group. */
	public VerticalGroup left () {
		align |= Align.left;
		align &= ~Align.right;
		return this;
	}

	/** Sets {@link Align#bottom} and clears {@link Align#top} for the alignment of all widgets within the vertical group. */
	public VerticalGroup bottom () {
		align |= Align.bottom;
		align &= ~Align.top;
		return this;
	}

	/** Adds {@link Align#right} and clears {@link Align#left} for the alignment of all widgets within the vertical group. */
	public VerticalGroup right () {
		align |= Align.right;
		align &= ~Align.left;
		return this;
	}

	public int getAlign () {
		return align;
	}

	public VerticalGroup fill () {
		fill = 1f;
		return this;
	}

	/** @param fill 0 will use preferred height. */
	public VerticalGroup fill (float fill) {
		this.fill = fill;
		return this;
	}

	public float getFill () {
		return fill;
	}

	public VerticalGroup expand () {
		expand = true;
		return this;
	}

	/** When true and wrap is false, the columns will take up the entire vertical group width. */
	public VerticalGroup expand (boolean expand) {
		this.expand = expand;
		return this;
	}

	public boolean getExpand () {
		return expand;
	}

	/** Sets fill to 1 and expand to true. */
	public VerticalGroup grow () {
		expand = true;
		fill = 1;
		return this;
	}

	/** If false, the widgets are arranged in a single column and the preferred height is the widget heights plus spacing. If true,
	 * the widgets will wrap using the height of the vertical group. The preferred height of the group will be 0 as it is expected
	 * that something external will set the height of the group. Default is false.
	 * <p>
	 * When wrap is enabled, the group's preferred width depends on the height of the group. In some cases the parent of the group
	 * will need to layout twice: once to set the height of the group and a second time to adjust to the group's new preferred
	 * width. */
	public VerticalGroup wrap () {
		wrap = true;
		return this;
	}

	public VerticalGroup wrap (boolean wrap) {
		this.wrap = wrap;
		return this;
	}

	public boolean getWrap () {
		return wrap;
	}

	/** Sets the alignment of widgets within each column of the vertical group. Set to {@link Align#center}, {@link Align#left}, or
	 * {@link Align#right}. */
	public VerticalGroup columnAlign (int columnAlign) {
		this.columnAlign = columnAlign;
		return this;
	}

	/** Sets the alignment of widgets within each column to {@link Align#center}. This clears any other alignment. */
	public VerticalGroup columnCenter () {
		columnAlign = Align.center;
		return this;
	}

	/** Adds {@link Align#left} and clears {@link Align#right} for the alignment of widgets within each column. */
	public VerticalGroup columnLeft () {
		columnAlign |= Align.left;
		columnAlign &= ~Align.right;
		return this;
	}

	/** Adds {@link Align#right} and clears {@link Align#left} for the alignment of widgets within each column. */
	public VerticalGroup columnRight () {
		columnAlign |= Align.right;
		columnAlign &= ~Align.left;
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
