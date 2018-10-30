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
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.BitmapFont.Glyph;
import com.badlogic.gdx.graphics.g2d.BitmapFontCache;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.StringBuilder;
import io.anuke.ucore.core.Core;
import io.anuke.ucore.function.Supplier;
import io.anuke.ucore.scene.Element;
import io.anuke.ucore.scene.style.Drawable;
import io.anuke.ucore.util.Bundles;

import static io.anuke.ucore.core.Core.font;
import static io.anuke.ucore.core.Core.skin;

/** A text label, with optional word wrapping.
 * <p>
 * The preferred size of the label is determined by the actual text bounds, unless {@link #setWrap(boolean) word wrap} is enabled.
 * @author Nathan Sweet */
public class Label extends Element {
	static private final Color tempColor = new Color();
	static private final GlyphLayout prefSizeLayout = new GlyphLayout();

	private LabelStyle style;
	private final GlyphLayout layout = new GlyphLayout();
	private final Vector2 prefSize = new Vector2();
	private final StringBuilder text = new StringBuilder();
	private BitmapFontCache cache;
	private int labelAlign = Align.left;
	private int lineAlign = Align.left;
	private boolean wrap;
	private float lastPrefHeight;
	private boolean prefSizeInvalid = true;
	private float fontScaleX = 1, fontScaleY = 1;
	private boolean fontScaleChanged = false;
	private String ellipsis;
	private boolean fallback = true;
	
	public Label(Supplier<String> sup){
		this("", new LabelStyle(skin.get(LabelStyle.class)));
		update(() -> setText(sup.get()));
		try{
			setText(sup.get());
		}catch (Exception e){}
	}

	public Label (CharSequence text) {
		this(text, skin.get(LabelStyle.class));
	}

	public Label (CharSequence text, String styleName) {
		this(text, skin.get(styleName, LabelStyle.class));
	}

	/** Creates a label, using a {@link LabelStyle} that has a BitmapFont with the specified name from the skin and the specified
	 * color. */
	public Label (CharSequence text, String fontName, Color color) {
		this(text, new LabelStyle(skin.getFont(fontName), color));
	}

	/** Creates a label, using a {@link LabelStyle} that has a BitmapFont with the specified name and the specified color from the
	 * skin. */
	public Label (CharSequence text, String fontName, String colorName) {
		this(text, new LabelStyle(skin.getFont(fontName), skin.getColor(colorName)));
	}

	public Label (CharSequence text, LabelStyle style) {
		setStyle(new LabelStyle(style));
		if (text != null) setText(text);
		if (text != null && text.length() > 0) setSize(getPrefWidth(), getPrefHeight());
	}

	public void setStyle (LabelStyle style) {
		if (style == null) throw new IllegalArgumentException("style cannot be null.");
		if (style.font == null) throw new IllegalArgumentException("Missing LabelStyle font.");
		this.style = style;
		cache = style.font.newFontCache();
		invalidateHierarchy();
	}

	/** Returns the label's style. Modifying the returned style may not have an effect until {@link #setStyle(LabelStyle)} is
	 * called. */
	public LabelStyle getStyle () {
		return style;
	}

	/** @param newText May be null, "" will be used. */
	public void setText (CharSequence newText) {
		if(Bundles.enabled() && newText != null && newText.length() > 0 && newText.charAt(0) == '$'){
			String out = newText.toString().substring(1);
			setTextInternal(Bundles.get(out, out));
		}else{
			setTextInternal(newText);
		}
	}

	private void setTextInternal(CharSequence newText){
		if (newText == null) newText = "";
		if (newText instanceof StringBuilder) {
			if (text.equals(newText)) return;
			text.setLength(0);
			text.append((StringBuilder)newText);
			if(fallback) {
				checkFallback();
			}
		} else {
			if (textEquals(newText)) return;
			text.setLength(0);
			text.append(newText);
			if(fallback) {
				checkFallback();
			}
		}
		invalidateHierarchy();
	}

	private void checkFallback(){

		for (int i = 0; i < text.length; i++) {
			if(isControl((int)text.charAt(i))) continue;
			Glyph g =font.getData().getGlyph(text.charAt(i));
			if (g == null || g == font.getData().missingGlyph) {
				for (BitmapFont font : Core.skin.getAll(BitmapFont.class).values()) {
					if(font.getData().getGlyph(text.charAt(i)) != null){
						this.style.font = font;
						setStyle(this.style);
						return;
					}
				}
			}
		}
	}

	private boolean isControl(int var0){
		return var0 <= 159 && (var0 >= 127 || var0 >>> 5 == 0);
	}

	public boolean textEquals (CharSequence other) {
		int length = text.length;
		char[] chars = text.chars;
		if (length != other.length()) return false;
		for (int i = 0; i < length; i++)
			if (chars[i] != other.charAt(i)) return false;
		return true;
	}

	public StringBuilder getText () {
		return text;
	}

	public void invalidate () {
		super.invalidate();
		prefSizeInvalid = true;
	}

	private void scaleAndComputePrefSize () {
		BitmapFont font = cache.getFont();
		float oldScaleX = font.getScaleX();
		float oldScaleY = font.getScaleY();
		if (fontScaleChanged) font.getData().setScale(fontScaleX, fontScaleY);

		computePrefSize();

		if (fontScaleChanged) font.getData().setScale(oldScaleX, oldScaleY);
	}

	private void computePrefSize () {
		prefSizeInvalid = false;
		GlyphLayout prefSizeLayout = Label.prefSizeLayout;
		if (wrap && ellipsis == null) {
			float width = getWidth();
			if (style.background != null) width -= style.background.getLeftWidth() + style.background.getRightWidth();
			prefSizeLayout.setText(cache.getFont(), text, Color.WHITE, width, Align.left, true);
		} else
			prefSizeLayout.setText(cache.getFont(), text);
		prefSize.set(prefSizeLayout.width, prefSizeLayout.height);
	}

	public void layout () {
		BitmapFont font = cache.getFont();
		float oldScaleX = font.getScaleX();
		float oldScaleY = font.getScaleY();
		if (fontScaleChanged) font.getData().setScale(fontScaleX, fontScaleY);

		boolean wrap = this.wrap && ellipsis == null;
		if (wrap) {
			float prefHeight = getPrefHeight();
			if (prefHeight != lastPrefHeight) {
				lastPrefHeight = prefHeight;
				invalidateHierarchy();
			}
		}

		float width = getWidth(), height = getHeight();
		Drawable background = style.background;
		float x = 0, y = 0;
		if (background != null) {
			x = background.getLeftWidth();
			y = background.getBottomHeight();
			width -= background.getLeftWidth() + background.getRightWidth();
			height -= background.getBottomHeight() + background.getTopHeight();
		}

		GlyphLayout layout = this.layout;
		float textWidth, textHeight;
		if (wrap || text.indexOf("\n") != -1) {
			// If the text can span multiple lines, determine the text's actual size so it can be aligned within the label.
			layout.setText(font, text, 0, text.length, Color.WHITE, width, lineAlign, wrap, ellipsis);
			textWidth = layout.width;
			textHeight = layout.height;

			if ((labelAlign & Align.left) == 0) {
				if ((labelAlign & Align.right) != 0)
					x += width - textWidth;
				else
					x += (width - textWidth) / 2;
			}
		} else {
			textWidth = width;
			textHeight = font.getData().capHeight;
		}

		if ((labelAlign & Align.top) != 0) {
			y += cache.getFont().isFlipped() ? 0 : height - textHeight;
			y += style.font.getDescent();
		} else if ((labelAlign & Align.bottom) != 0) {
			y += cache.getFont().isFlipped() ? height - textHeight : 0;
			y -= style.font.getDescent();
		} else {
			y += (height - textHeight) / 2;
		}
		if (!cache.getFont().isFlipped()) y += textHeight;

		layout.setText(font, text, 0, text.length, Color.WHITE, textWidth, lineAlign, wrap, ellipsis);
		cache.setText(layout, x, y);

		if (fontScaleChanged) font.getData().setScale(oldScaleX, oldScaleY);
	}

	public void draw (Batch batch, float parentAlpha) {
		validate();
		Color color = tempColor.set(getColor());
		color.a *= parentAlpha;
		if (style.background != null) {
			batch.setColor(color.r, color.g, color.b, color.a);
			style.background.draw(batch, getX(), getY(), getWidth(), getHeight());
		}
		if (style.fontColor != null) color.mul(style.fontColor);
		cache.tint(color);
		cache.setPosition(getX(), getY());
		cache.draw(batch);
	}

	public float getPrefWidth () {
		if (wrap) return 0;
		if (prefSizeInvalid) scaleAndComputePrefSize();
		float width = prefSize.x;
		Drawable background = style.background;
		if (background != null) width += background.getLeftWidth() + background.getRightWidth();
		return width;
	}

	public float getPrefHeight () {
		if (prefSizeInvalid) scaleAndComputePrefSize();
		float descentScaleCorrection = 1;
		if (fontScaleChanged) descentScaleCorrection = fontScaleY / style.font.getScaleY();
		float height = prefSize.y - style.font.getDescent() * descentScaleCorrection * 2;
		Drawable background = style.background;
		if (background != null) height += background.getTopHeight() + background.getBottomHeight();
		return height;
	}

	public GlyphLayout getGlyphLayout () {
		return layout;
	}

	/** If false, the text will only wrap where it contains newlines (\n). The preferred size of the label will be the text bounds.
	 * If true, the text will word wrap using the width of the label. The preferred width of the label will be 0, it is expected
	 * that something external will set the width of the label. Wrapping will not occur when ellipsis is enabled. Default is false.
	 * <p>
	 * When wrap is enabled, the label's preferred height depends on the width of the label. In some cases the parent of the label
	 * will need to layout twice: once to set the width of the label and a second time to adjust to the label's new preferred
	 * height. */
	public void setWrap (boolean wrap) {
		this.wrap = wrap;
		invalidateHierarchy();
	}

	public int getLabelAlign () {
		return labelAlign;
	}

	public int getLineAlign () {
		return lineAlign;
	}

	/** @param alignment Aligns all the text within the label (default left center) and each line of text horizontally (default
	 *           left).
	 * @see Align */
	public void setAlignment (int alignment) {
		setAlignment(alignment, alignment);
	}

	/** @param labelAlign Aligns all the text within the label (default left center).
	 * @param lineAlign Aligns each line of text horizontally (default left).
	 * @see Align */
	public void setAlignment (int labelAlign, int lineAlign) {
		this.labelAlign = labelAlign;

		if ((lineAlign & Align.left) != 0)
			this.lineAlign = Align.left;
		else if ((lineAlign & Align.right) != 0)
			this.lineAlign = Align.right;
		else
			this.lineAlign = Align.center;

		invalidate();
	}

	public void setFontScale (float fontScale) {
		setFontScale(fontScale, fontScale);
	}

	public void setFontScale (float fontScaleX, float fontScaleY) {
		fontScaleChanged = true;
		this.fontScaleX = fontScaleX;
		this.fontScaleY = fontScaleY;
		invalidateHierarchy();
	}

	public float getFontScaleX () {
		return fontScaleX;
	}

	public void setFontScaleX (float fontScaleX) {
		setFontScale(fontScaleX, fontScaleY);
	}

	public float getFontScaleY () {
		return fontScaleY;
	}

	public void setFontScaleY (float fontScaleY) {
		setFontScale(fontScaleX, fontScaleY);
	}

	/** When non-null the text will be truncated "..." if it does not fit within the width of the label. Wrapping will not occur
	 * when ellipsis is enabled. Default is false. */
	public void setEllipsis (String ellipsis) {
		this.ellipsis = ellipsis;
	}

	/** When true the text will be truncated "..." if it does not fit within the width of the label. Wrapping will not occur when
	 * ellipsis is true. Default is false. */
	public void setEllipsis (boolean ellipsis) {
		if (ellipsis)
			this.ellipsis = "...";
		else
			this.ellipsis = null;
	}

	/** Allows subclasses to access the cache in {@link #draw(Batch, float)}. */
	protected BitmapFontCache getBitmapFontCache () {
		return cache;
	}

	public String toString () {
		return super.toString() + ": " + text;
	}

	/** The style for a label, see {@link Label}.
	 * @author Nathan Sweet */
	static public class LabelStyle {
		public BitmapFont font;
		/** Optional. */
		public Color fontColor;
		/** Optional. */
		public Drawable background;

		public LabelStyle () {
		}

		public LabelStyle (BitmapFont font, Color fontColor) {
			this.font = font;
			this.fontColor = fontColor;
		}

		public LabelStyle (LabelStyle style) {
			this.font = style.font;
			if (style.fontColor != null) fontColor = new Color(style.fontColor);
			background = style.background;
		}
	}
}
