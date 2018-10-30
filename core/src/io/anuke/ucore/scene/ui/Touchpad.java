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

package io.anuke.ucore.scene.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.math.Circle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Pools;

import io.anuke.ucore.scene.Element;
import io.anuke.ucore.scene.Skin;
import io.anuke.ucore.scene.event.InputEvent;
import io.anuke.ucore.scene.event.InputListener;
import io.anuke.ucore.scene.style.Drawable;
import io.anuke.ucore.scene.event.ChangeListener.ChangeEvent;

/** An on-screen joystick. The movement area of the joystick is circular, centered on the touchpad, and its size determined by the
 * smaller touchpad dimension.
 * <p>
 * The preferred size of the touchpad is determined by the background.
 * <p>
 * {@link ChangeEvent} is fired when the touchpad knob is moved. Cancelling the event will move the knob to where it was
 * previously.
 * @author Josh Street */
public class Touchpad extends Element {
	private TouchpadStyle style;
	boolean touched;
	boolean resetOnTouchUp = true;
	private float deadzoneRadius;
	private final Circle knobBounds = new Circle(0, 0, 0);
	private final Circle touchBounds = new Circle(0, 0, 0);
	private final Circle deadzoneBounds = new Circle(0, 0, 0);
	private final Vector2 knobPosition = new Vector2();
	private final Vector2 knobPercent = new Vector2();

	/** @param deadzoneRadius The distance in pixels from the center of the touchpad required for the knob to be moved. */
	public Touchpad (float deadzoneRadius, Skin skin) {
		this(deadzoneRadius, skin.get(TouchpadStyle.class));
	}

	/** @param deadzoneRadius The distance in pixels from the center of the touchpad required for the knob to be moved. */
	public Touchpad (float deadzoneRadius, Skin skin, String styleName) {
		this(deadzoneRadius, skin.get(styleName, TouchpadStyle.class));
	}

	/** @param deadzoneRadius The distance in pixels from the center of the touchpad required for the knob to be moved. */
	public Touchpad (float deadzoneRadius, TouchpadStyle style) {
		if (deadzoneRadius < 0) throw new IllegalArgumentException("deadzoneRadius must be > 0");
		this.deadzoneRadius = deadzoneRadius;

		knobPosition.set(getWidth() / 2f, getHeight() / 2f);

		setStyle(style);
		setSize(getPrefWidth(), getPrefHeight());

		addListener(new InputListener() {
			@Override
			public boolean touchDown (InputEvent event, float x, float y, int pointer, int button) {
				if (touched) return false;
				touched = true;
				calculatePositionAndValue(x, y, false);
				return true;
			}

			@Override
			public void touchDragged (InputEvent event, float x, float y, int pointer) {
				calculatePositionAndValue(x, y, false);
			}

			@Override
			public void touchUp (InputEvent event, float x, float y, int pointer, int button) {
				touched = false;
				calculatePositionAndValue(x, y, resetOnTouchUp);
			}
		});
	}

	void calculatePositionAndValue (float x, float y, boolean isTouchUp) {
		float oldPositionX = knobPosition.x;
		float oldPositionY = knobPosition.y;
		float oldPercentX = knobPercent.x;
		float oldPercentY = knobPercent.y;
		float centerX = knobBounds.x;
		float centerY = knobBounds.y;
		knobPosition.set(centerX, centerY);
		knobPercent.set(0f, 0f);
		if (!isTouchUp) {
			if (!deadzoneBounds.contains(x, y)) {
				knobPercent.set((x - centerX) / knobBounds.radius, (y - centerY) / knobBounds.radius);
				float length = knobPercent.len();
				if (length > 1) knobPercent.scl(1 / length);
				if (knobBounds.contains(x, y)) {
					knobPosition.set(x, y);
				} else {
					knobPosition.set(knobPercent).nor().scl(knobBounds.radius).add(knobBounds.x, knobBounds.y);
				}
			}
		}
		if (oldPercentX != knobPercent.x || oldPercentY != knobPercent.y) {
			ChangeEvent changeEvent = Pools.obtain(ChangeEvent.class);
			if (fire(changeEvent)) {
				knobPercent.set(oldPercentX, oldPercentY);
				knobPosition.set(oldPositionX, oldPositionY);
			}
			Pools.free(changeEvent);
		}
	}

	public void setStyle (TouchpadStyle style) {
		if (style == null) throw new IllegalArgumentException("style cannot be null");
		this.style = style;
		invalidateHierarchy();
	}

	/** Returns the touchpad's style. Modifying the returned style may not have an effect until {@link #setStyle(TouchpadStyle)} is
	 * called. */
	public TouchpadStyle getStyle () {
		return style;
	}

	@Override
	public Element hit (float x, float y, boolean touchable) {
		return touchBounds.contains(x, y) ? this : null;
	}

	@Override
	public void layout () {
		// Recalc margin and deadzone bounds
		float halfWidth = getWidth() / 2;
		float halfHeight = getHeight() / 2;
		float radius = Math.min(halfWidth, halfHeight);
		touchBounds.set(halfWidth, halfHeight, radius);
		if (style.knob != null) radius -= Math.max(style.knob.getMinWidth(), style.knob.getMinHeight()) / 2;
		knobBounds.set(halfWidth, halfHeight, radius);
		deadzoneBounds.set(halfWidth, halfHeight, deadzoneRadius);
		// Recalc margin values and knob position
		knobPosition.set(halfWidth, halfHeight);
		knobPercent.set(0, 0);
	}

	@Override
	public void draw (Batch batch, float parentAlpha) {
		validate();

		Color c = getColor();
		batch.setColor(c.r, c.g, c.b, c.a * parentAlpha);

		float x = getX();
		float y = getY();
		float w = getWidth();
		float h = getHeight();

		final Drawable bg = style.background;
		if (bg != null) bg.draw(batch, x, y, w, h);

		final Drawable knob = style.knob;
		if (knob != null) {
			x += knobPosition.x - knob.getMinWidth() / 2f;
			y += knobPosition.y - knob.getMinHeight() / 2f;
			knob.draw(batch, x, y, knob.getMinWidth(), knob.getMinHeight());
		}
	}

	@Override
	public float getPrefWidth () {
		return style.background != null ? style.background.getMinWidth() : 0;
	}

	@Override
	public float getPrefHeight () {
		return style.background != null ? style.background.getMinHeight() : 0;
	}

	public boolean isTouched () {
		return touched;
	}

	public boolean getResetOnTouchUp () {
		return resetOnTouchUp;
	}

	/** @param reset Whether to reset the knob to the center on touch up. */
	public void setResetOnTouchUp (boolean reset) {
		this.resetOnTouchUp = reset;
	}

	/** @param deadzoneRadius The distance in pixels from the center of the touchpad required for the knob to be moved. */
	public void setDeadzone (float deadzoneRadius) {
		if (deadzoneRadius < 0) throw new IllegalArgumentException("deadzoneRadius must be > 0");
		this.deadzoneRadius = deadzoneRadius;
		invalidate();
	}

	/** Returns the x-position of the knob relative to the center of the widget. The positive direction is right. */
	public float getKnobX () {
		return knobPosition.x;
	}

	/** Returns the y-position of the knob relative to the center of the widget. The positive direction is up. */
	public float getKnobY () {
		return knobPosition.y;
	}

	/** Returns the x-position of the knob as a percentage from the center of the touchpad to the edge of the circular movement
	 * area. The positive direction is right. */
	public float getKnobPercentX () {
		return knobPercent.x;
	}

	/** Returns the y-position of the knob as a percentage from the center of the touchpad to the edge of the circular movement
	 * area. The positive direction is up. */
	public float getKnobPercentY () {
		return knobPercent.y;
	}

	/** The style for a {@link Touchpad}.
	 * @author Josh Street */
	public static class TouchpadStyle {
		/** Stretched in both directions. Optional. */
		public Drawable background;

		/** Optional. */
		public Drawable knob;

		public TouchpadStyle () {
		}

		public TouchpadStyle (Drawable background, Drawable knob) {
			this.background = background;
			this.knob = knob;
		}

		public TouchpadStyle (TouchpadStyle style) {
			this.background = style.background;
			this.knob = style.knob;
		}
	}
}
