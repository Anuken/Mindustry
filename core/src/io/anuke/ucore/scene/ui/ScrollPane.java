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

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import io.anuke.ucore.scene.Element;
import io.anuke.ucore.scene.Scene;
import io.anuke.ucore.scene.event.ActorGestureListener;
import io.anuke.ucore.scene.event.Event;
import io.anuke.ucore.scene.event.InputEvent;
import io.anuke.ucore.scene.event.InputListener;
import io.anuke.ucore.scene.style.Drawable;
import io.anuke.ucore.scene.ui.layout.WidgetGroup;
import io.anuke.ucore.scene.utils.Cullable;
import io.anuke.ucore.scene.utils.Layout;
import io.anuke.ucore.scene.utils.ScissorStack;

import static io.anuke.ucore.core.Core.skin;
/** A group that scrolls a child widget using scrollbars and/or mouse or touch dragging.
 * <p>
 * The widget is sized to its preferred size. If the widget's preferred width or height is less than the size of this scroll pane,
 * it is set to the size of this scroll pane. Scrollbars appear when the widget is larger than the scroll pane.
 * <p>
 * The scroll pane's preferred size is that of the child widget. At this size, the child widget will not need to scroll, so the
 * scroll pane is typically sized by ignoring the preferred size in one or both directions.
 * @author mzechner 
 * @author Nathan Sweet */
public class ScrollPane extends WidgetGroup {
	private ScrollPaneStyle style;
	private Element widget;

	final Rectangle hScrollBounds = new Rectangle();
	final Rectangle vScrollBounds = new Rectangle();
	final Rectangle hKnobBounds = new Rectangle();
	final Rectangle vKnobBounds = new Rectangle();
	private final Rectangle widgetAreaBounds = new Rectangle();
	private final Rectangle widgetCullingArea = new Rectangle();
	private final Rectangle scissorBounds = new Rectangle();
	private ActorGestureListener flickScrollListener;

	boolean scrollX, scrollY;
	boolean vScrollOnRight = true;
	boolean hScrollOnBottom = true;
	float amountX, amountY;
	float visualAmountX, visualAmountY;
	float maxX, maxY;
	boolean touchScrollH, touchScrollV;
	final Vector2 lastPoint = new Vector2();
	float areaWidth, areaHeight;
	private boolean fadeScrollBars = false, smoothScrolling = true;
	float fadeAlpha, fadeAlphaSeconds = 1, fadeDelay, fadeDelaySeconds = 1;
	boolean cancelTouchFocus = true;

	boolean flickScroll = true;
	float velocityX, velocityY;
	float flingTimer;
	private boolean overscrollX = true, overscrollY = true;
	float flingTime = 1f;
	private float overscrollDistance = 50, overscrollSpeedMin = 30, overscrollSpeedMax = 200;
	private boolean forceScrollX, forceScrollY;
	protected boolean disableX, disableY;
	private boolean clamp = true;
	private boolean scrollbarsOnTop;
	private boolean variableSizeKnobs = true;
	int draggingPointer = -1;


	/** @param widget May be null. */
	public ScrollPane (Element widget) {
		this(widget, skin.get(ScrollPaneStyle.class));
	}

	/** @param widget May be null. */
	public ScrollPane (Element widget, String styleName) {
		this(widget, skin.get(styleName, ScrollPaneStyle.class));
	}

	/** @param widget May be null. */
	public ScrollPane (Element widget, ScrollPaneStyle style) {
		if (style == null) throw new IllegalArgumentException("style cannot be null.");
		this.style = style;
		setWidget(widget);
		setSize(150, 150);

		addCaptureListener(new InputListener() {
			private float handlePosition;

			public boolean touchDown (InputEvent event, float x, float y, int pointer, int button) {
				if (draggingPointer != -1) return false;
				if (pointer == 0 && button != 0) return false;
				getScene().setScrollFocus(ScrollPane.this);

				if (!flickScroll) resetFade();

				if (fadeAlpha == 0) return false;

				if (scrollX && hScrollBounds.contains(x, y)) {
					event.stop();
					resetFade();
					if (hKnobBounds.contains(x, y)) {
						lastPoint.set(x, y);
						handlePosition = hKnobBounds.x;
						touchScrollH = true;
						draggingPointer = pointer;
						return true;
					}
					setScrollX(amountX + areaWidth * (x < hKnobBounds.x ? -1 : 1));
					return true;
				}
				if (scrollY && vScrollBounds.contains(x, y)) {
					event.stop();
					resetFade();
					if (vKnobBounds.contains(x, y)) {
						lastPoint.set(x, y);
						handlePosition = vKnobBounds.y;
						touchScrollV = true;
						draggingPointer = pointer;
						return true;
					}
					setScrollY(amountY + areaHeight * (y < vKnobBounds.y ? 1 : -1));
					return true;
				}
				return false;
			}

			public void touchUp (InputEvent event, float x, float y, int pointer, int button) {
				if (pointer != draggingPointer) return;
				cancel();
			}

			public void touchDragged (InputEvent event, float x, float y, int pointer) {
				if (pointer != draggingPointer) return;
				if (touchScrollH) {
					float delta = x - lastPoint.x;
					float scrollH = handlePosition + delta;
					handlePosition = scrollH;
					scrollH = Math.max(hScrollBounds.x, scrollH);
					scrollH = Math.min(hScrollBounds.x + hScrollBounds.width - hKnobBounds.width, scrollH);
					float total = hScrollBounds.width - hKnobBounds.width;
					if (total != 0) setScrollPercentX((scrollH - hScrollBounds.x) / total);
					lastPoint.set(x, y);
				} else if (touchScrollV) {
					float delta = y - lastPoint.y;
					float scrollV = handlePosition + delta;
					handlePosition = scrollV;
					scrollV = Math.max(vScrollBounds.y, scrollV);
					scrollV = Math.min(vScrollBounds.y + vScrollBounds.height - vKnobBounds.height, scrollV);
					float total = vScrollBounds.height - vKnobBounds.height;
					if (total != 0) setScrollPercentY(1 - ((scrollV - vScrollBounds.y) / total));
					lastPoint.set(x, y);
				}
			}

			public boolean mouseMoved (InputEvent event, float x, float y) {
				if (!flickScroll) resetFade();
				return false;
			}
		});

		flickScrollListener = new ActorGestureListener() {
			public void pan (InputEvent event, float x, float y, float deltaX, float deltaY) {
				resetFade();
				amountX -= deltaX;
				amountY += deltaY;
				clamp();
				if (cancelTouchFocus && ((scrollX && deltaX != 0) || (scrollY && deltaY != 0))) cancelTouchFocus();
			}

			public void fling (InputEvent event, float x, float y, int button) {
				if (Math.abs(x) > 150 && scrollX) {
					flingTimer = flingTime;
					velocityX = x;
					if (cancelTouchFocus) cancelTouchFocus();
				}
				if (Math.abs(y) > 150 && scrollY) {
					flingTimer = flingTime;
					velocityY = -y;
					if (cancelTouchFocus) cancelTouchFocus();
				}
			}

			public boolean handle (Event event) {
				if (super.handle(event)) {
					if (((InputEvent)event).getType() == InputEvent.Type.touchDown) flingTimer = 0;
					return true;
				}
				return false;
			}
		};
		addListener(flickScrollListener);

		addListener(new InputListener() {
			public boolean scrolled (InputEvent event, float x, float y, int amount) {
				resetFade();
				if (scrollY)
					setScrollY(amountY + getMouseWheelY() * amount);
				else if (scrollX) //
					setScrollX(amountX + getMouseWheelX() * amount);
				else
					return false;
				return true;
			}
		});
	}

	void resetFade () {
		fadeAlpha = fadeAlphaSeconds;
		fadeDelay = fadeDelaySeconds;
	}

	/** Cancels the stage's touch focus for all listeners except this scroll pane's flick scroll listener. This causes any widgets
	 * inside the scrollpane that have received touchDown to receive touchUp.
	 * @see #setCancelTouchFocus(boolean) */
	public void cancelTouchFocus () {
		Scene stage = getScene();
		if (stage != null) stage.cancelTouchFocusExcept(flickScrollListener, this);
	}

	/** If currently scrolling by tracking a touch down, stop scrolling. */
	public void cancel () {
		draggingPointer = -1;
		touchScrollH = false;
		touchScrollV = false;
		flickScrollListener.getGestureDetector().cancel();
	}

	void clamp () {
		if (!clamp) return;
		scrollX(overscrollX ? MathUtils.clamp(amountX, -overscrollDistance, maxX + overscrollDistance)
			: MathUtils.clamp(amountX, 0, maxX));
		scrollY(overscrollY ? MathUtils.clamp(amountY, -overscrollDistance, maxY + overscrollDistance)
			: MathUtils.clamp(amountY, 0, maxY));
	}

	public void setStyle (ScrollPaneStyle style) {
		if (style == null) throw new IllegalArgumentException("style cannot be null.");
		this.style = style;
		invalidateHierarchy();
	}

	/** Returns the scroll pane's style. Modifying the returned style may not have an effect until
	 * {@link #setStyle(ScrollPaneStyle)} is called. */
	public ScrollPaneStyle getStyle () {
		return style;
	}

	public void act (float delta) {
		super.act(delta);

		boolean panning = flickScrollListener.getGestureDetector().isPanning();
		boolean animating = false;

		if (fadeAlpha > 0 && fadeScrollBars && !panning && !touchScrollH && !touchScrollV) {
			fadeDelay -= delta;
			if (fadeDelay <= 0) fadeAlpha = Math.max(0, fadeAlpha - delta);
			animating = true;
		}

		if (flingTimer > 0) {
			resetFade();

			float alpha = flingTimer / flingTime;
			amountX -= velocityX * alpha * delta;
			amountY -= velocityY * alpha * delta;
			clamp();

			// Stop fling if hit overscroll distance.
			if (amountX == -overscrollDistance) velocityX = 0;
			if (amountX >= maxX + overscrollDistance) velocityX = 0;
			if (amountY == -overscrollDistance) velocityY = 0;
			if (amountY >= maxY + overscrollDistance) velocityY = 0;

			flingTimer -= delta;
			if (flingTimer <= 0) {
				velocityX = 0;
				velocityY = 0;
			}

			animating = true;
		}

		if (smoothScrolling && flingTimer <= 0 && !panning && //
		// Scroll smoothly when grabbing the scrollbar if one pixel of scrollbar movement is > 10% of the scroll area.
			((!touchScrollH || (scrollX && maxX / (hScrollBounds.width - hKnobBounds.width) > areaWidth * 0.1f)) //
				&& (!touchScrollV || (scrollY && maxY / (vScrollBounds.height - vKnobBounds.height) > areaHeight * 0.1f))) //
		) {
			if (visualAmountX != amountX) {
				if (visualAmountX < amountX)
					visualScrollX(Math.min(amountX, visualAmountX + Math.max(200 * delta, (amountX - visualAmountX) * 7 * delta)));
				else
					visualScrollX(Math.max(amountX, visualAmountX - Math.max(200 * delta, (visualAmountX - amountX) * 7 * delta)));
				animating = true;
			}
			if (visualAmountY != amountY) {
				if (visualAmountY < amountY)
					visualScrollY(Math.min(amountY, visualAmountY + Math.max(200 * delta, (amountY - visualAmountY) * 7 * delta)));
				else
					visualScrollY(Math.max(amountY, visualAmountY - Math.max(200 * delta, (visualAmountY - amountY) * 7 * delta)));
				animating = true;
			}
		} else {
			if (visualAmountX != amountX) visualScrollX(amountX);
			if (visualAmountY != amountY) visualScrollY(amountY);
		}

		if (!panning) {
			if (overscrollX && scrollX) {
				if (amountX < 0) {
					resetFade();
					amountX += (overscrollSpeedMin + (overscrollSpeedMax - overscrollSpeedMin) * -amountX / overscrollDistance)
						* delta;
					if (amountX > 0) scrollX(0);
					animating = true;
				} else if (amountX > maxX) {
					resetFade();
					amountX -= (overscrollSpeedMin
						+ (overscrollSpeedMax - overscrollSpeedMin) * -(maxX - amountX) / overscrollDistance) * delta;
					if (amountX < maxX) scrollX(maxX);
					animating = true;
				}
			}
			if (overscrollY && scrollY) {
				if (amountY < 0) {
					resetFade();
					amountY += (overscrollSpeedMin + (overscrollSpeedMax - overscrollSpeedMin) * -amountY / overscrollDistance)
						* delta;
					if (amountY > 0) scrollY(0);
					animating = true;
				} else if (amountY > maxY) {
					resetFade();
					amountY -= (overscrollSpeedMin
						+ (overscrollSpeedMax - overscrollSpeedMin) * -(maxY - amountY) / overscrollDistance) * delta;
					if (amountY < maxY) scrollY(maxY);
					animating = true;
				}
			}
		}

		if (animating) {
			Scene stage = getScene();
			if (stage != null && stage.getActionsRequestRendering()) Gdx.graphics.requestRendering();
		}
	}

	public void layout () {
		final Drawable bg = style.background;
		final Drawable hScrollKnob = style.hScrollKnob;
		final Drawable vScrollKnob = style.vScrollKnob;

		float bgLeftWidth = 0, bgRightWidth = 0, bgTopHeight = 0, bgBottomHeight = 0;
		if (bg != null) {
			bgLeftWidth = bg.getLeftWidth();
			bgRightWidth = bg.getRightWidth();
			bgTopHeight = bg.getTopHeight();
			bgBottomHeight = bg.getBottomHeight();
		}

		float width = getWidth();
		float height = getHeight();

		float scrollbarHeight = 0;
		if (hScrollKnob != null) scrollbarHeight = hScrollKnob.getMinHeight();
		if (style.hScroll != null) scrollbarHeight = Math.max(scrollbarHeight, style.hScroll.getMinHeight());
		float scrollbarWidth = 0;
		if (vScrollKnob != null) scrollbarWidth = vScrollKnob.getMinWidth();
		if (style.vScroll != null) scrollbarWidth = Math.max(scrollbarWidth, style.vScroll.getMinWidth());

		// Get available space size by subtracting background's padded area.
		areaWidth = width - bgLeftWidth - bgRightWidth;
		areaHeight = height - bgTopHeight - bgBottomHeight;

		if (widget == null) return;

		// Get widget's desired width.
		float widgetWidth, widgetHeight;
		if (widget instanceof Layout) {
			Layout layout = (Layout)widget;
			widgetWidth = layout.getPrefWidth();
			widgetHeight = layout.getPrefHeight();
		} else {
			widgetWidth = widget.getWidth();
			widgetHeight = widget.getHeight();
		}

		// Determine if horizontal/vertical scrollbars are needed.
		scrollX = forceScrollX || (widgetWidth > areaWidth && !disableX);
		scrollY = forceScrollY || (widgetHeight > areaHeight && !disableY);

		boolean fade = fadeScrollBars;
		if (!fade) {
			// Check again, now taking into account the area that's taken up by any enabled scrollbars.
			if (scrollY) {
				areaWidth -= scrollbarWidth;
				if (!scrollX && widgetWidth > areaWidth && !disableX) scrollX = true;
			}
			if (scrollX) {
				areaHeight -= scrollbarHeight;
				if (!scrollY && widgetHeight > areaHeight && !disableY) {
					scrollY = true;
					areaWidth -= scrollbarWidth;
				}
			}
		}

		// The bounds of the scrollable area for the widget.
		widgetAreaBounds.set(bgLeftWidth, bgBottomHeight, areaWidth, areaHeight);

		if (fade) {
			// Make sure widget is drawn under fading scrollbars.
			if (scrollX && scrollY) {
				areaHeight -= scrollbarHeight;
				areaWidth -= scrollbarWidth;
			}
		} else {
			if (scrollbarsOnTop) {
				// Make sure widget is drawn under non-fading scrollbars.
				if (scrollX) widgetAreaBounds.height += scrollbarHeight;
				if (scrollY) widgetAreaBounds.width += scrollbarWidth;
			} else {
				// Offset widget area y for horizontal scrollbar at bottom.
				if (scrollX && hScrollOnBottom) widgetAreaBounds.y += scrollbarHeight;
				// Offset widget area x for vertical scrollbar at left.
				if (scrollY && !vScrollOnRight) widgetAreaBounds.x += scrollbarWidth;
			}
		}

		// If the widget is smaller than the available space, make it take up the available space.
		widgetWidth = disableX ? areaWidth : Math.max(areaWidth, widgetWidth);
		widgetHeight = disableY ? areaHeight : Math.max(areaHeight, widgetHeight);

		maxX = widgetWidth - areaWidth;
		maxY = widgetHeight - areaHeight;
		if (fade) {
			// Make sure widget is drawn under fading scrollbars.
			if (scrollX && scrollY) {
				maxY -= scrollbarHeight;
				maxX -= scrollbarWidth;
			}
		}
		scrollX(MathUtils.clamp(amountX, 0, maxX));
		//scrollY(MathUtils.clamp(amountY, 0, maxY));

		// Set the bounds and scroll knob sizes if scrollbars are needed.
		if (scrollX) {
			if (hScrollKnob != null) {
				float hScrollHeight = style.hScroll != null ? style.hScroll.getMinHeight() : hScrollKnob.getMinHeight();
				// The corner gap where the two scroll bars intersect might have to flip from right to left.
				float boundsX = vScrollOnRight ? bgLeftWidth : bgLeftWidth + scrollbarWidth;
				// Scrollbar on the top or bottom.
				float boundsY = hScrollOnBottom ? bgBottomHeight : height - bgTopHeight - hScrollHeight;
				hScrollBounds.set(boundsX, boundsY, areaWidth, hScrollHeight);
				if (variableSizeKnobs)
					hKnobBounds.width = Math.max(hScrollKnob.getMinWidth(), (int)(hScrollBounds.width * areaWidth / widgetWidth));
				else
					hKnobBounds.width = hScrollKnob.getMinWidth();

				hKnobBounds.height = hScrollKnob.getMinHeight();

				hKnobBounds.x = hScrollBounds.x + (int)((hScrollBounds.width - hKnobBounds.width) * getScrollPercentX());
				hKnobBounds.y = hScrollBounds.y;
			} else {
				hScrollBounds.set(0, 0, 0, 0);
				hKnobBounds.set(0, 0, 0, 0);
			}
		}
		if (scrollY) {
			if (vScrollKnob != null) {
				float vScrollWidth = style.vScroll != null ? style.vScroll.getMinWidth() : vScrollKnob.getMinWidth();
				// the small gap where the two scroll bars intersect might have to flip from bottom to top
				float boundsX, boundsY;
				if (hScrollOnBottom) {
					boundsY = height - bgTopHeight - areaHeight;
				} else {
					boundsY = bgBottomHeight;
				}
				// bar on the left or right
				if (vScrollOnRight) {
					boundsX = width - bgRightWidth - vScrollWidth;
				} else {
					boundsX = bgLeftWidth;
				}
				vScrollBounds.set(boundsX, boundsY, vScrollWidth, areaHeight);
				vKnobBounds.width = vScrollKnob.getMinWidth();
				if (variableSizeKnobs)
					vKnobBounds.height = Math.max(vScrollKnob.getMinHeight(), (int)(vScrollBounds.height * areaHeight / widgetHeight));
				else
					vKnobBounds.height = vScrollKnob.getMinHeight();

				if (vScrollOnRight) {
					vKnobBounds.x = width - bgRightWidth - vScrollKnob.getMinWidth();
				} else {
					vKnobBounds.x = bgLeftWidth;
				}
				vKnobBounds.y = vScrollBounds.y + (int)((vScrollBounds.height - vKnobBounds.height) * (1 - getScrollPercentY()));
			} else {
				vScrollBounds.set(0, 0, 0, 0);
				vKnobBounds.set(0, 0, 0, 0);
			}
		}

		widget.setSize(widgetWidth, widgetHeight);
		if (widget instanceof Layout) widget.validate();
	}

	@Override
	public void draw (Batch batch, float parentAlpha) {
		if (widget == null) return;

		validate();

		// Setup transform for this group.
		applyTransform(batch, computeTransform());

		if (scrollX) hKnobBounds.x = hScrollBounds.x + (int)((hScrollBounds.width - hKnobBounds.width) * getVisualScrollPercentX());
		if (scrollY)
			vKnobBounds.y = vScrollBounds.y + (int)((vScrollBounds.height - vKnobBounds.height) * (1 - getVisualScrollPercentY()));

		// Calculate the widget's position depending on the scroll state and available widget area.
		float y = widgetAreaBounds.y;
		if (!scrollY)
			y -= (int)maxY;
		else
			y -= (int)(maxY - visualAmountY);

		float x = widgetAreaBounds.x;
		if (scrollX) x -= (int)visualAmountX;

		if (!fadeScrollBars && scrollbarsOnTop) {
			if (scrollX && hScrollOnBottom) {
				float scrollbarHeight = 0;
				if (style.hScrollKnob != null) scrollbarHeight = style.hScrollKnob.getMinHeight();
				if (style.hScroll != null) scrollbarHeight = Math.max(scrollbarHeight, style.hScroll.getMinHeight());
				y += scrollbarHeight;
			}
			if (scrollY && !vScrollOnRight) {
				float scrollbarWidth = 0;
				if (style.hScrollKnob != null) scrollbarWidth = style.hScrollKnob.getMinWidth();
				if (style.hScroll != null) scrollbarWidth = Math.max(scrollbarWidth, style.hScroll.getMinWidth());
				x += scrollbarWidth;
			}
		}

		widget.setPosition(x, y);

		if (widget instanceof Cullable) {
			widgetCullingArea.x = -widget.getX() + widgetAreaBounds.x;
			widgetCullingArea.y = -widget.getY() + widgetAreaBounds.y;
			widgetCullingArea.width = widgetAreaBounds.width;
			widgetCullingArea.height = widgetAreaBounds.height;
			((Cullable)widget).setCullingArea(widgetCullingArea);
		}

		// Draw the background ninepatch.
		Color color = getColor();
		batch.setColor(color.r, color.g, color.b, color.a * parentAlpha);
		if (style.background != null) style.background.draw(batch, 0, 0, getWidth(), getHeight());

		// Caculate the scissor bounds based on the batch transform, the available widget area and the camera transform. We need to
		// project those to screen coordinates for OpenGL ES to consume.
		getScene().calculateScissors(widgetAreaBounds, scissorBounds);

		// Enable scissors for widget area and draw the widget.
		batch.flush();
		if (ScissorStack.pushScissors(scissorBounds)) {
			drawChildren(batch, parentAlpha);
			batch.flush();
			ScissorStack.popScissors();
		}

		// Render scrollbars and knobs on top.
		batch.setColor(color.r, color.g, color.b, color.a * parentAlpha * Interpolation.fade.apply(fadeAlpha / fadeAlphaSeconds));
		if (scrollX && scrollY) {
			if (style.corner != null) {
				style.corner.draw(batch, hScrollBounds.x + hScrollBounds.width, hScrollBounds.y, vScrollBounds.width,
					vScrollBounds.y);
			}
		}
		if (scrollX) {
			if (style.hScroll != null)
				style.hScroll.draw(batch, hScrollBounds.x, hScrollBounds.y, hScrollBounds.width, hScrollBounds.height);
			if (style.hScrollKnob != null)
				style.hScrollKnob.draw(batch, hKnobBounds.x, hKnobBounds.y, hKnobBounds.width, hKnobBounds.height);
		}
		if (scrollY) {
			if (style.vScroll != null)
				style.vScroll.draw(batch, vScrollBounds.x, vScrollBounds.y, vScrollBounds.width, vScrollBounds.height);
			if (style.vScrollKnob != null)
				style.vScrollKnob.draw(batch, vKnobBounds.x, vKnobBounds.y, vKnobBounds.width, vKnobBounds.height);
		}

		resetTransform(batch);
	}

	/** Generate fling gesture.
	 * @param flingTime Time in seconds for which you want to fling last.
	 * @param velocityX Velocity for horizontal direction.
	 * @param velocityY Velocity for vertical direction. */
	public void fling (float flingTime, float velocityX, float velocityY) {
		this.flingTimer = flingTime;
		this.velocityX = velocityX;
		this.velocityY = velocityY;
	}

	public float getPrefWidth () {
		if (widget instanceof Layout) {
			float width = ((Layout)widget).getPrefWidth();
			if (style.background != null) width += style.background.getLeftWidth() + style.background.getRightWidth();
			if (forceScrollY) {
				float scrollbarWidth = 0;
				if (style.vScrollKnob != null) scrollbarWidth = style.vScrollKnob.getMinWidth();
				if (style.vScroll != null) scrollbarWidth = Math.max(scrollbarWidth, style.vScroll.getMinWidth());
				width += scrollbarWidth;
			}
			return width;
		}
		return 150;
	}

	public float getPrefHeight () {
		if (widget instanceof Layout) {
			float height = ((Layout)widget).getPrefHeight();
			if (style.background != null) height += style.background.getTopHeight() + style.background.getBottomHeight();
			if (forceScrollX) {
				float scrollbarHeight = 0;
				if (style.hScrollKnob != null) scrollbarHeight = style.hScrollKnob.getMinHeight();
				if (style.hScroll != null) scrollbarHeight = Math.max(scrollbarHeight, style.hScroll.getMinHeight());
				height += scrollbarHeight;
			}
			return height;
		}
		return 150;
	}

	public float getMinWidth () {
		return 0;
	}

	public float getMinHeight () {
		return 0;
	}

	/** Sets the {@link Element} embedded in this scroll pane.
	 * @param widget May be null to remove any current actor. */
	public void setWidget (Element widget) {
		if (widget == this) throw new IllegalArgumentException("widget cannot be the ScrollPane.");
		if (this.widget != null) super.removeChild(this.widget);
		this.widget = widget;
		if (widget != null) super.addChild(widget);
	}

	/** Returns the actor embedded in this scroll pane, or null. */
	public Element getWidget () {
		return widget;
	}

	public boolean removeChild (Element actor) {
		if (actor == null) throw new IllegalArgumentException("actor cannot be null.");
		if (actor != widget) return false;
		setWidget(null);
		return true;
	}

	public boolean removeChild (Element actor, boolean unfocus) {
		if (actor == null) throw new IllegalArgumentException("actor cannot be null.");
		if (actor != widget) return false;
		this.widget = null;
		return super.removeChild(actor, unfocus);
	}

	public Element hit (float x, float y, boolean touchable) {
		if (x < 0 || x >= getWidth() || y < 0 || y >= getHeight()) return null;
		if (scrollX && hScrollBounds.contains(x, y)) return this;
		if (scrollY && vScrollBounds.contains(x, y)) return this;
		return super.hit(x, y, touchable);
	}

	/** Called whenever the x scroll amount is changed. */
	protected void scrollX (float pixelsX) {
		this.amountX = pixelsX;
	}

	/** Called whenever the y scroll amount is changed. */
	protected void scrollY (float pixelsY) {
		this.amountY = pixelsY;
	}

	/** Called whenever the visual x scroll amount is changed. */
	protected void visualScrollX (float pixelsX) {
		this.visualAmountX = pixelsX;
	}

	/** Called whenever the visual y scroll amount is changed. */
	protected void visualScrollY (float pixelsY) {
		this.visualAmountY = pixelsY;
	}

	/** Returns the amount to scroll horizontally when the mouse wheel is scrolled. */
	protected float getMouseWheelX () {
		return Math.min(areaWidth, Math.max(areaWidth * 0.9f, maxX * 0.1f) / 4);
	}

	/** Returns the amount to scroll vertically when the mouse wheel is scrolled. */
	protected float getMouseWheelY () {
		return Math.min(areaHeight, Math.max(areaHeight * 0.9f, maxY * 0.1f) / 4);
	}

	public void setScrollX (float pixels) {
		scrollX(MathUtils.clamp(pixels, 0, maxX));
	}

	/** Returns the x scroll position in pixels, where 0 is the left of the scroll pane. */
	public float getScrollX () {
		return amountX;
	}

	public void setScrollY (float pixels) {
		scrollY(MathUtils.clamp(pixels, 0, maxY));
	}

	/** Returns the y scroll position in pixels, where 0 is the top of the scroll pane. */
	public float getScrollY () {
		return amountY;
	}

	/** Sets the visual scroll amount equal to the scroll amount. This can be used when setting the scroll amount without
	 * animating. */
	public void updateVisualScroll () {
		visualAmountX = amountX;
		visualAmountY = amountY;
	}

	public float getVisualScrollX () {
		return !scrollX ? 0 : visualAmountX;
	}

	public float getVisualScrollY () {
		return !scrollY ? 0 : visualAmountY;
	}

	public float getVisualScrollPercentX () {
		return MathUtils.clamp(visualAmountX / maxX, 0, 1);
	}

	public float getVisualScrollPercentY () {
		return MathUtils.clamp(visualAmountY / maxY, 0, 1);
	}

	public float getScrollPercentX () {
		return MathUtils.clamp(amountX / maxX, 0, 1);
	}

	public void setScrollPercentX (float percentX) {
		scrollX(maxX * MathUtils.clamp(percentX, 0, 1));
	}

	public float getScrollPercentY () {
		return MathUtils.clamp(amountY / maxY, 0, 1);
	}

	public void setScrollPercentY (float percentY) {
		scrollY(maxY * MathUtils.clamp(percentY, 0, 1));
	}

	public void setFlickScroll (boolean flickScroll) {
		if (this.flickScroll == flickScroll) return;
		this.flickScroll = flickScroll;
		if (flickScroll)
			addListener(flickScrollListener);
		else
			removeListener(flickScrollListener);
		invalidate();
	}

	public void setFlickScrollTapSquareSize (float halfTapSquareSize) {
		flickScrollListener.getGestureDetector().setTapSquareSize(halfTapSquareSize);
	}

	/** Sets the scroll offset so the specified rectangle is fully in view, if possible. Coordinates are in the scroll pane
	 * widget's coordinate system. */
	public void scrollTo (float x, float y, float width, float height) {
		scrollTo(x, y, width, height, false, false);
	}

	/** Sets the scroll offset so the specified rectangle is fully in view, and optionally centered vertically and/or horizontally,
	 * if possible. Coordinates are in the scroll pane widget's coordinate system. */
	public void scrollTo (float x, float y, float width, float height, boolean centerHorizontal, boolean centerVertical) {
		float amountX = this.amountX;
		if (centerHorizontal) {
			amountX = x - areaWidth / 2 + width / 2;
		} else {
			if (x + width > amountX + areaWidth) amountX = x + width - areaWidth;
			if (x < amountX) amountX = x;
		}
		scrollX(MathUtils.clamp(amountX, 0, maxX));

		float amountY = this.amountY;
		if (centerVertical) {
			amountY = maxY - y + areaHeight / 2 - height / 2;
		} else {
			if (amountY > maxY - y - height + areaHeight) amountY = maxY - y - height + areaHeight;
			if (amountY < maxY - y) amountY = maxY - y;
		}
		scrollY(MathUtils.clamp(amountY, 0, maxY));
	}

	/** Returns the maximum scroll value in the x direction. */
	public float getMaxX () {
		return maxX;
	}

	/** Returns the maximum scroll value in the y direction. */
	public float getMaxY () {
		return maxY;
	}

	public float getScrollBarHeight () {
		if (!scrollX) return 0;
		float height = 0;
		if (style.hScrollKnob != null) height = style.hScrollKnob.getMinHeight();
		if (style.hScroll != null) height = Math.max(height, style.hScroll.getMinHeight());
		return height;
	}

	public float getScrollBarWidth () {
		if (!scrollY) return 0;
		float width = 0;
		if (style.vScrollKnob != null) width = style.vScrollKnob.getMinWidth();
		if (style.vScroll != null) width = Math.max(width, style.vScroll.getMinWidth());
		return width;
	}

	/** Returns the width of the scrolled viewport. */
	public float getScrollWidth () {
		return areaWidth;
	}

	/** Returns the height of the scrolled viewport. */
	public float getScrollHeight () {
		return areaHeight;
	}

	/** Returns true if the widget is larger than the scroll pane horizontally. */
	public boolean isScrollX () {
		return scrollX;
	}

	/** Returns true if the widget is larger than the scroll pane vertically. */
	public boolean isScrollY () {
		return scrollY;
	}

	/** Disables scrolling in a direction. The widget will be sized to the FlickScrollPane in the disabled direction. */
	public void setScrollingDisabled (boolean x, boolean y) {
		disableX = x;
		disableY = y;
	}

	public boolean isScrollingDisabledX () {
		return disableX;
	}

	public boolean isScrollingDisabledY () {
		return disableY;
	}

	public boolean isLeftEdge () {
		return !scrollX || amountX <= 0;
	}

	public boolean isRightEdge () {
		return !scrollX || amountX >= maxX;
	}

	public boolean isTopEdge () {
		return !scrollY || amountY <= 0;
	}

	public boolean isBottomEdge () {
		return !scrollY || amountY >= maxY;
	}

	public boolean isDragging () {
		return draggingPointer != -1;
	}

	public boolean isPanning () {
		return flickScrollListener.getGestureDetector().isPanning();
	}

	public boolean isFlinging () {
		return flingTimer > 0;
	}

	public void setVelocityX (float velocityX) {
		this.velocityX = velocityX;
	}

	/** Gets the flick scroll x velocity. */
	public float getVelocityX () {
		return velocityX;
	}

	public void setVelocityY (float velocityY) {
		this.velocityY = velocityY;
	}

	/** Gets the flick scroll y velocity. */
	public float getVelocityY () {
		return velocityY;
	}

	/** For flick scroll, if true the widget can be scrolled slightly past its bounds and will animate back to its bounds when
	 * scrolling is stopped. Default is true. */
	public void setOverscroll (boolean overscrollX, boolean overscrollY) {
		this.overscrollX = overscrollX;
		this.overscrollY = overscrollY;
	}

	/** For flick scroll, sets the overscroll distance in pixels and the speed it returns to the widget's bounds in seconds.
	 * Default is 50, 30, 200. */
	public void setupOverscroll (float distance, float speedMin, float speedMax) {
		overscrollDistance = distance;
		overscrollSpeedMin = speedMin;
		overscrollSpeedMax = speedMax;
	}

	/** Forces enabling scrollbars (for non-flick scroll) and overscrolling (for flick scroll) in a direction, even if the contents
	 * do not exceed the bounds in that direction. */
	public void setForceScroll (boolean x, boolean y) {
		forceScrollX = x;
		forceScrollY = y;
	}

	public boolean isForceScrollX () {
		return forceScrollX;
	}

	public boolean isForceScrollY () {
		return forceScrollY;
	}

	/** For flick scroll, sets the amount of time in seconds that a fling will continue to scroll. Default is 1. */
	public void setFlingTime (float flingTime) {
		this.flingTime = flingTime;
	}

	/** For flick scroll, prevents scrolling out of the widget's bounds. Default is true. */
	public void setClamp (boolean clamp) {
		this.clamp = clamp;
	}

	/** Set the position of the vertical and horizontal scroll bars. */
	public void setScrollBarPositions (boolean bottom, boolean right) {
		hScrollOnBottom = bottom;
		vScrollOnRight = right;
	}

	/** When true the scrollbars don't reduce the scrollable size and fade out after some time of not being used. */
	public void setFadeScrollBars (boolean fadeScrollBars) {
		if (this.fadeScrollBars == fadeScrollBars) return;
		this.fadeScrollBars = fadeScrollBars;
		if (!fadeScrollBars) fadeAlpha = fadeAlphaSeconds;
		invalidate();
	}

	public void setupFadeScrollBars (float fadeAlphaSeconds, float fadeDelaySeconds) {
		this.fadeAlphaSeconds = fadeAlphaSeconds;
		this.fadeDelaySeconds = fadeDelaySeconds;
	}

	public void setSmoothScrolling (boolean smoothScrolling) {
		this.smoothScrolling = smoothScrolling;
	}

	/** When false (the default), the widget is clipped so it is not drawn under the scrollbars. When true, the widget is clipped
	 * to the entire scroll pane bounds and the scrollbars are drawn on top of the widget. If {@link #setFadeScrollBars(boolean)}
	 * is true, the scroll bars are always drawn on top. */
	public void setScrollbarsOnTop (boolean scrollbarsOnTop) {
		this.scrollbarsOnTop = scrollbarsOnTop;
		invalidate();
	}

	public boolean getVariableSizeKnobs () {
		return variableSizeKnobs;
	}

	/** If true, the scroll knobs are sized based on {@link #getMaxX()} or {@link #getMaxY()}. If false, the scroll knobs are sized
	 * based on {@link Drawable#getMinWidth()} or {@link Drawable#getMinHeight()}. Default is true. */
	public void setVariableSizeKnobs (boolean variableSizeKnobs) {
		this.variableSizeKnobs = variableSizeKnobs;
	}

	/** When true (default) and flick scrolling begins, {@link #cancelTouchFocus()} is called. This causes any widgets inside the
	 * scrollpane that have received touchDown to receive touchUp when flick scrolling begins. */
	public void setCancelTouchFocus (boolean cancelTouchFocus) {
		this.cancelTouchFocus = cancelTouchFocus;
	}

	public void drawDebug (ShapeRenderer shapes) {
		shapes.flush();
		applyTransform(shapes, computeTransform());
		if (ScissorStack.pushScissors(scissorBounds)) {
			drawDebugChildren(shapes);
			ScissorStack.popScissors();
		}
		resetTransform(shapes);
	}

	/** The style for a scroll pane, see {@link ScrollPane}.
	 * @author mzechner
	 * @author Nathan Sweet */
	static public class ScrollPaneStyle {
		/** Optional. */
		public Drawable background, corner;
		/** Optional. */
		public Drawable hScroll, hScrollKnob;
		/** Optional. */
		public Drawable vScroll, vScrollKnob;

		public ScrollPaneStyle () {
		}

		public ScrollPaneStyle (Drawable background, Drawable hScroll, Drawable hScrollKnob, Drawable vScroll,
			Drawable vScrollKnob) {
			this.background = background;
			this.hScroll = hScroll;
			this.hScrollKnob = hScrollKnob;
			this.vScroll = vScroll;
			this.vScrollKnob = vScrollKnob;
		}

		public ScrollPaneStyle (ScrollPaneStyle style) {
			this.background = style.background;
			this.hScroll = style.hScroll;
			this.hScrollKnob = style.hScrollKnob;
			this.vScroll = style.vScroll;
			this.vScrollKnob = style.vScrollKnob;
		}
	}
}
