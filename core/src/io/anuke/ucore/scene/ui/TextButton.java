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

import io.anuke.ucore.scene.style.Drawable;
import io.anuke.ucore.scene.ui.Label.LabelStyle;
import io.anuke.ucore.scene.ui.layout.Cell;
/** A button with a child {@link Label} to display text.
 * @author Nathan Sweet */
public class TextButton extends Button {
	protected final Label label;
	private TextButtonStyle style;

	public TextButton (String text) {
		this(text, skin.get(TextButtonStyle.class));
	}

	public TextButton (String text, String styleName) {
		this(text, skin.get(styleName, TextButtonStyle.class));
	}

	public TextButton (String text, TextButtonStyle style) {
		super();
		setStyle(style);
		this.style = style;
		label = new Label(text, new LabelStyle(style.font, style.fontColor));
		label.setAlignment(Align.center);
		add(label).expand().fill();
		setSize(getPrefWidth(), getPrefHeight());
	}

	public void setStyle (ButtonStyle style) {
		if (style == null) throw new NullPointerException("style cannot be null");
		if (!(style instanceof TextButtonStyle)) throw new IllegalArgumentException("style must be a TextButtonStyle.");
		super.setStyle(style);
		this.style = (TextButtonStyle)style;
		if (label != null) {
			TextButtonStyle textButtonStyle = (TextButtonStyle)style;
			LabelStyle labelStyle = label.getStyle();
			labelStyle.font = textButtonStyle.font;
			labelStyle.fontColor = textButtonStyle.fontColor;
			label.setStyle(labelStyle);
		}
	}

	public TextButtonStyle getStyle () {
		return style;
	}

	public void draw (Batch batch, float parentAlpha) {
		Color fontColor;
		if (isDisabled() && style.disabledFontColor != null)
			fontColor = style.disabledFontColor;
		else if (isPressed() && style.downFontColor != null)
			fontColor = style.downFontColor;
		else if (isChecked && style.checkedFontColor != null)
			fontColor = (isOver() && style.checkedOverFontColor != null) ? style.checkedOverFontColor : style.checkedFontColor;
		else if (isOver() && style.overFontColor != null)
			fontColor = style.overFontColor;
		else
			fontColor = style.fontColor;
		if (fontColor != null) label.getStyle().fontColor = fontColor;
		super.draw(batch, parentAlpha);
	}

	public Label getLabel () {
		return label;
	}

	public Cell<Label> getLabelCell () {
		return getCell(label);
	}

	public void setText (String text) {
		label.setText(text);
	}

	public CharSequence getText () {
		return label.getText();
	}

	/** The style for a text button, see {@link TextButton}.
	 * @author Nathan Sweet */
	static public class TextButtonStyle extends ButtonStyle {
		public BitmapFont font;
		/** Optional. */
		public Color fontColor, downFontColor, overFontColor, checkedFontColor, checkedOverFontColor, disabledFontColor;

		public TextButtonStyle () {
		}

		public TextButtonStyle (Drawable up, Drawable down, Drawable checked, BitmapFont font) {
			super(up, down, checked);
			this.font = font;
		}

		public TextButtonStyle (TextButtonStyle style) {
			super(style);
			this.font = style.font;
			if (style.fontColor != null) this.fontColor = new Color(style.fontColor);
			if (style.downFontColor != null) this.downFontColor = new Color(style.downFontColor);
			if (style.overFontColor != null) this.overFontColor = new Color(style.overFontColor);
			if (style.checkedFontColor != null) this.checkedFontColor = new Color(style.checkedFontColor);
			if (style.checkedOverFontColor != null) this.checkedFontColor = new Color(style.checkedOverFontColor);
			if (style.disabledFontColor != null) this.disabledFontColor = new Color(style.disabledFontColor);
		}
	}
}
