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

import static io.anuke.ucore.core.Core.skin;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Scaling;

import io.anuke.ucore.scene.style.Drawable;
import io.anuke.ucore.scene.ui.layout.Cell;
/** A checkbox is a button that contains an image indicating the checked or unchecked state and a label.
 * @author Nathan Sweet */
public class CheckBox extends TextButton {
	private Image image;
	private Cell imageCell;
	private CheckBoxStyle style;

	public CheckBox (String text) {
		this(text, skin.get(CheckBoxStyle.class));
	}

	public CheckBox (String text, String styleName) {
		this(text, skin.get(styleName, CheckBoxStyle.class));
	}

	public CheckBox (String text, CheckBoxStyle style) {
		super(text, style);
		clearChildren();
		Label label = getLabel();
		imageCell = add(image = new Image(style.checkboxOff, Scaling.stretch));
		add(label).padLeft(4);
		label.setAlignment(Align.left);
		setSize(getPrefWidth(), getPrefHeight());
	}

	public void setStyle (ButtonStyle style) {
		if (!(style instanceof CheckBoxStyle)) throw new IllegalArgumentException("style must be a CheckBoxStyle.");
		super.setStyle(style);
		this.style = (CheckBoxStyle)style;
	}

	/** Returns the checkbox's style. Modifying the returned style may not have an effect until {@link #setStyle(ButtonStyle)} is
	 * called. */
	public CheckBoxStyle getStyle () {
		return style;
	}

	public void draw (Batch batch, float parentAlpha) {
		Drawable checkbox = null;
		if (isDisabled()) {
			if (isChecked && style.checkboxOnDisabled != null)
				checkbox = style.checkboxOnDisabled;
			else
				checkbox = style.checkboxOffDisabled;
		}
		if (checkbox == null) {
			if (isChecked && isOver() && style.checkboxOnOver != null)
				checkbox = style.checkboxOnOver;
			else if (isChecked && style.checkboxOn != null)
				checkbox = style.checkboxOn;
			else if (isOver() && style.checkboxOver != null && !isDisabled())
				checkbox = style.checkboxOver;
			else
				checkbox = style.checkboxOff;
		}
		image.setDrawable(checkbox);
		super.draw(batch, parentAlpha);
	}

	public Image getImage () {
		return image;
	}

	public Cell getImageCell () {
		return imageCell;
	}

	/** The style for a select box, see {@link CheckBox}.
	 * @author Nathan Sweet */
	static public class CheckBoxStyle extends TextButtonStyle {
		public Drawable checkboxOn, checkboxOff;
		/** Optional. */
		public Drawable checkboxOver, checkboxOnDisabled, checkboxOffDisabled, checkboxOnOver;

		public CheckBoxStyle () {
		}

		public CheckBoxStyle (Drawable checkboxOff, Drawable checkboxOn, BitmapFont font, Color fontColor) {
			this.checkboxOff = checkboxOff;
			this.checkboxOn = checkboxOn;
			this.font = font;
			this.fontColor = fontColor;
		}

		public CheckBoxStyle (CheckBoxStyle style) {
			super(style);
			this.checkboxOff = style.checkboxOff;
			this.checkboxOn = style.checkboxOn;
			this.checkboxOver = style.checkboxOver;
			this.checkboxOffDisabled = style.checkboxOffDisabled;
			this.checkboxOnDisabled = style.checkboxOnDisabled;
			this.checkboxOnOver = style.checkboxOnOver;
		}
	}
}
