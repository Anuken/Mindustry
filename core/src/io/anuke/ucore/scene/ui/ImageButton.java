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
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Scaling;
import io.anuke.ucore.scene.style.Drawable;
import io.anuke.ucore.scene.style.TextureRegionDrawable;
import io.anuke.ucore.scene.ui.layout.Cell;

import static io.anuke.ucore.core.Core.skin;
/** A button with a child {@link Image} to display an image. This is useful when the button must be larger than the image and the
 * image centered on the button. If the image is the size of the button, a {@link Button} without any children can be used, where
 * the {@link Button.ButtonStyle#up}, {@link Button.ButtonStyle#down}, and {@link Button.ButtonStyle#checked} nine patches define
 * the image.
 * @author Nathan Sweet */
public class ImageButton extends Button {
	private final Image image;
	private ImageButtonStyle style;

	public ImageButton () {
		this(skin.get(ImageButtonStyle.class));
	}

	public ImageButton (String icon) {
		this(skin.get(ImageButtonStyle.class));
		ImageButtonStyle style = new ImageButtonStyle(skin.get(ImageButtonStyle.class));
		style.imageUp = skin.getDrawable(icon);
		
		setStyle(style);
	}
	
	public ImageButton (String icon, String stylen) {
		this(skin.get(stylen, ImageButtonStyle.class));
		ImageButtonStyle style = new ImageButtonStyle(skin.get(stylen, ImageButtonStyle.class));
		style.imageUp = skin.getDrawable(icon);
		
		setStyle(style);
	}
	
	public ImageButton (TextureRegion region) {
		this(skin.get(ImageButtonStyle.class));
		ImageButtonStyle style = new ImageButtonStyle(skin.get(ImageButtonStyle.class));
		style.imageUp = new TextureRegionDrawable(region);
		
		setStyle(style);
	}
	
	public ImageButton (TextureRegion region, String stylen) {
		this(skin.get(ImageButtonStyle.class));
		ImageButtonStyle style = new ImageButtonStyle(skin.get(stylen, ImageButtonStyle.class));
		style.imageUp = new TextureRegionDrawable(region);
		
		setStyle(style);
	}

	public ImageButton (ImageButtonStyle style) {
		super(style);
		image = new Image();
		image.setScaling(Scaling.fit);
		add(image);
		setStyle(style);
		setSize(getPrefWidth(), getPrefHeight());
	}

	public ImageButton (Drawable imageUp) {
		this(new ImageButtonStyle(null, null, null, imageUp, null, null));
		
		ImageButtonStyle style = new ImageButtonStyle(skin.get(ImageButtonStyle.class));
		style.imageUp = imageUp;
		setStyle(style);
	}

	public ImageButton (Drawable imageUp, Drawable imageDown) {
		this(new ImageButtonStyle(null, null, null, imageUp, imageDown, null));
	}

	public ImageButton (Drawable imageUp, Drawable imageDown, Drawable imageChecked) {
		this(new ImageButtonStyle(null, null, null, imageUp, imageDown, imageChecked));
	}

	public void setStyle (ButtonStyle style) {
		if (!(style instanceof ImageButtonStyle)) throw new IllegalArgumentException("style must be an ImageButtonStyle.");
		super.setStyle(style);
		this.style = (ImageButtonStyle)style;
		if (image != null) updateImage();
	}

	public ImageButtonStyle getStyle () {
		return style;
	}

	/** Updates the Image with the appropriate Drawable from the style before it is drawn. */
	protected void updateImage () {
		Drawable drawable = null;
		if (isDisabled() && style.imageDisabled != null)
			drawable = style.imageDisabled;
		else if (isPressed() && style.imageDown != null)
			drawable = style.imageDown;
		else if (isChecked && style.imageChecked != null)
			drawable = (style.imageCheckedOver != null && isOver()) ? style.imageCheckedOver : style.imageChecked;
		else if (isOver() && style.imageOver != null)
			drawable = style.imageOver;
		else if (style.imageUp != null) //
			drawable = style.imageUp;

		Color color = image.getColor();

		if(isDisabled && style.imageDisabledColor != null)
			color = style.imageDisabledColor;
		else if(isPressed() && style.imageDownColor != null)
			color = style.imageDownColor;
		else if(isChecked() && style.imageCheckedColor != null)
			color = style.imageCheckedColor;
		else if(style.imageUpColor != null)
			color = style.imageUpColor;

		image.setDrawable(drawable);
		image.setColor(color);
	}

	public void draw (Batch batch, float parentAlpha) {
		updateImage();
		super.draw(batch, parentAlpha);
	}

	public Image getImage () {
		return image;
	}

	public Cell getImageCell () {
		return getCell(image);
	}
	
	public void resizeImage(float size){
		getImageCell().size(size);
	}

	/** The style for an image button, see {@link ImageButton}.
	 * @author Nathan Sweet */
	static public class ImageButtonStyle extends ButtonStyle {
		/** Optional. */
		public Drawable imageUp, imageDown, imageOver, imageChecked, imageCheckedOver, imageDisabled;
		public Color imageUpColor, imageCheckedColor, imageDownColor, imageDisabledColor;

		public ImageButtonStyle () {
		}

		public ImageButtonStyle (Drawable up, Drawable down, Drawable checked, Drawable imageUp, Drawable imageDown,
			Drawable imageChecked) {
			super(up, down, checked);
			this.imageUp = imageUp;
			this.imageDown = imageDown;
			this.imageChecked = imageChecked;
		}

		public ImageButtonStyle (ImageButtonStyle style) {
			super(style);
			this.imageUp = style.imageUp;
			this.imageDown = style.imageDown;
			this.imageOver = style.imageOver;
			this.imageChecked = style.imageChecked;
			this.imageCheckedOver = style.imageCheckedOver;
			this.imageDisabled = style.imageDisabled;
			this.imageUpColor = style.imageUpColor;
			this.imageDownColor = style.imageDownColor;
			this.imageCheckedColor = style.imageCheckedColor;
			this.imageDisabledColor = style.imageDisabledColor;
		}

		public ImageButtonStyle (ButtonStyle style) {
			super(style);
		}
	}
}
