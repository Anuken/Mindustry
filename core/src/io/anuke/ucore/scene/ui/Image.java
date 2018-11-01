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
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.NinePatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Scaling;

import io.anuke.ucore.core.Core;
import io.anuke.ucore.graphics.Draw;
import io.anuke.ucore.scene.Element;
import io.anuke.ucore.scene.Skin;
import io.anuke.ucore.scene.style.*;

/** Displays a {@link Drawable}, scaled various way within the widgets bounds. The preferred size is the min size of the drawable.
 * Only when using a {@link TextureRegionDrawable} will the actor's scale, rotation, and origin be used when drawing.
 * @author Nathan Sweet */
public class Image extends Element {
	private Scaling scaling;
	private int align = Align.center;
	private Drawable drawable;
	protected float imageX, imageY, imageWidth, imageHeight;

	/** Creates an image with no region or patch, stretched, and aligned center. */
	public Image () {
		this((Drawable)null);
	}
	
	public Image (String name) {
		this(Draw.getPatch(name));
	}

	/** Creates an image stretched, and aligned center.
	 * @param patch May be null. */
	public Image (NinePatch patch) {
		this(new NinePatchDrawable(patch), Scaling.stretch, Align.center);
	}

	/** Creates an image stretched, and aligned center.
	 * @param region May be null. */
	public Image (TextureRegion region) {
		this(new TextureRegionDrawable(region), Scaling.stretch, Align.center);
	}

	/** Creates an image stretched, and aligned center. */
	public Image (Texture texture) {
		this(new TextureRegionDrawable(new TextureRegion(texture)));
	}

	/** Creates an image stretched, and aligned center. */
	public Image (Skin skin, String drawableName) {
		this(skin.getDrawable(drawableName), Scaling.stretch, Align.center);
	}

	/** Creates an image stretched, and aligned center.
	 * @param drawable May be null. */
	public Image (Drawable drawable) {
		this(drawable, Scaling.stretch, Align.center);
	}

	/** Creates an image aligned center.
	 * @param drawable May be null. */
	public Image (Drawable drawable, Scaling scaling) {
		this(drawable, scaling, Align.center);
	}

	/** @param drawable May be null. */
	public Image (Drawable drawable, Scaling scaling, int align) {
		setDrawable(drawable);
		this.scaling = scaling;
		this.align = align;
		setSize(getPrefWidth(), getPrefHeight());
	}

	public void layout () {
		if (drawable == null) return;

		float regionWidth = drawable.getMinWidth();
		float regionHeight = drawable.getMinHeight();
		float width = getWidth();
		float height = getHeight();

		Vector2 size = scaling.apply(regionWidth, regionHeight, width, height);
		imageWidth = size.x;
		imageHeight = size.y;

		if ((align & Align.left) != 0)
			imageX = 0;
		else if ((align & Align.right) != 0)
			imageX = (int)(width - imageWidth);
		else
			imageX = (int)(width / 2 - imageWidth / 2);

		if ((align & Align.top) != 0)
			imageY = (int)(height - imageHeight);
		else if ((align & Align.bottom) != 0)
			imageY = 0;
		else
			imageY = (int)(height / 2 - imageHeight / 2);
	}

	public void draw (Batch batch, float parentAlpha) {
		validate();

		Color color = getColor();
		batch.setColor(color.r, color.g, color.b, color.a * parentAlpha);

		float x = getX();
		float y = getY();
		float scaleX = getScaleX();
		float scaleY = getScaleY();

		if (drawable instanceof TransformDrawable) {
			float rotation = getRotation();
			if (scaleX != 1 || scaleY != 1 || rotation != 0) {
				((TransformDrawable)drawable).draw(batch, x + imageX, y + imageY, getOriginX() - imageX, getOriginY() - imageY,
					imageWidth, imageHeight, scaleX, scaleY, rotation);
				return;
			}
		}
		if (drawable != null) drawable.draw(batch, x + imageX, y + imageY, imageWidth * scaleX, imageHeight * scaleY);
	}

	public void setDrawable (String drawableName) {
		setDrawable(Core.skin.getDrawable(drawableName));
	}

	/** @param drawable May be null. */
	public void setDrawable (Drawable drawable) {
		if (this.drawable == drawable) return;
		if (drawable != null) {
			if (getPrefWidth() != drawable.getMinWidth() || getPrefHeight() != drawable.getMinHeight()) invalidateHierarchy();
		} else
			invalidateHierarchy();
		this.drawable = drawable;
	}

	/** @return May be null. */
	public Drawable getDrawable () {
		return drawable;
	}

	public void setScaling (Scaling scaling) {
		if (scaling == null) throw new IllegalArgumentException("scaling cannot be null.");
		this.scaling = scaling;
		invalidate();
	}

	public void setAlign (int align) {
		this.align = align;
		invalidate();
	}

	public float getMinWidth () {
		return 0;
	}

	public float getMinHeight () {
		return 0;
	}

	public float getPrefWidth () {
		if (drawable != null) return drawable.getMinWidth();
		return 0;
	}

	public float getPrefHeight () {
		if (drawable != null) return drawable.getMinHeight();
		return 0;
	}

	public float getImageX () {
		return imageX;
	}

	public float getImageY () {
		return imageY;
	}

	public float getImageWidth () {
		return imageWidth;
	}

	public float getImageHeight () {
		return imageHeight;
	}
}
