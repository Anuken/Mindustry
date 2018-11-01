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
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.utils.*;

import io.anuke.ucore.scene.Scene;
import io.anuke.ucore.scene.event.InputEvent;
import io.anuke.ucore.scene.event.InputListener;
import io.anuke.ucore.scene.style.Drawable;

/** A multiple-line text input field, entirely based on {@link TextField} */
public class TextArea extends TextField {

	/** Array storing lines breaks positions **/
	IntArray linesBreak;

	/** Last text processed. This attribute is used to avoid unnecessary computations while calculating offsets **/
	private String lastText;

	/** Current line for the cursor **/
	int cursorLine;

	/** Index of the first line showed by the text area **/
	int firstLineShowing;

	/** Number of lines showed by the text area **/
	private int linesShowing;

	/** Variable to maintain the x offset of the cursor when moving up and down. If it's set to -1, the offset is reset **/
	float moveOffset;

	private float prefRows;

	public TextArea (String text) {
		super(text);
	}

	public TextArea (String text, String styleName) {
		super(text, styleName);
	}

	public TextArea (String text, TextFieldStyle style) {
		super(text, style);
	}

	@Override
	protected void initialize () {
		super.initialize();
		writeEnters = true;
		linesBreak = new IntArray();
		cursorLine = 0;
		firstLineShowing = 0;
		moveOffset = -1;
		linesShowing = 0;
	}

	protected int letterUnderCursor (float x) {
		if (linesBreak.size > 0) {
			if (cursorLine * 2 >= linesBreak.size) {
				return text.length();
			} else {
				float[] glyphPositions = this.glyphPositions.items;
				int start = linesBreak.items[cursorLine * 2];
				x += glyphPositions[start];
				int end = linesBreak.items[cursorLine * 2 + 1];
				int i = start;
				for (; i < end; i++)
					if (glyphPositions[i] > x) break;
				if (glyphPositions[i] - x <= x - glyphPositions[i - 1]) return i;
				return Math.max(0, i - 1);
			}
		} else {
			return 0;
		}
	}

	/** Sets the preferred number of rows (lines) for this text area. Used to calculate preferred height */
	public void setPrefRows (float prefRows) {
		this.prefRows = prefRows;
	}

	@Override
	public float getPrefHeight () {
		if (prefRows <= 0) {
			return super.getPrefHeight();
		} else {
			float prefHeight = textHeight * prefRows;
			if (style.background != null) {
				prefHeight = Math.max(prefHeight + style.background.getBottomHeight() + style.background.getTopHeight(),
					style.background.getMinHeight());
			}
			return prefHeight;
		}
	}

	/** Returns total number of lines that the text occupies **/
	public int getLines () {
		return linesBreak.size / 2 + (newLineAtEnd() ? 1 : 0);
	}

	/** Returns if there's a new line at then end of the text **/
	public boolean newLineAtEnd () {
		return text.length() != 0
			&& (text.charAt(text.length() - 1) == ENTER_ANDROID || text.charAt(text.length() - 1) == ENTER_DESKTOP);
	}

	/** Moves the cursor to the given number line **/
	public void moveCursorLine (int line) {
		if (line < 0) {
			cursorLine = 0;
			cursor = 0;
			moveOffset = -1;
		} else if (line >= getLines()) {
			int newLine = getLines() - 1;
			cursor = text.length();
			if (line > getLines() || newLine == cursorLine) {
				moveOffset = -1;
			}
			cursorLine = newLine;
		} else if (line != cursorLine) {
			if (moveOffset < 0) {
				moveOffset = linesBreak.size <= cursorLine * 2 ? 0
					: glyphPositions.get(cursor) - glyphPositions.get(linesBreak.get(cursorLine * 2));
			}
			cursorLine = line;
			cursor = cursorLine * 2 >= linesBreak.size ? text.length() : linesBreak.get(cursorLine * 2);
			while (cursor < text.length() && cursor <= linesBreak.get(cursorLine * 2 + 1) - 1
				&& glyphPositions.get(cursor) - glyphPositions.get(linesBreak.get(cursorLine * 2)) < moveOffset) {
				cursor++;
			}
			showCursor();
		}
	}

	/** Updates the current line, checking the cursor position in the text **/
	void updateCurrentLine () {
		int index = calculateCurrentLineIndex(cursor);
		int line = index / 2;
		// Special case when cursor moves to the beginning of the line from the end of another and a word
		// wider than the box
		if (index % 2 == 0 || index + 1 >= linesBreak.size || cursor != linesBreak.items[index]
			|| linesBreak.items[index + 1] != linesBreak.items[index]) {
			if (line < linesBreak.size / 2 || text.length() == 0 || text.charAt(text.length() - 1) == ENTER_ANDROID
				|| text.charAt(text.length() - 1) == ENTER_DESKTOP) {
				cursorLine = line;
			}
		}
	}

	/** Scroll the text area to show the line of the cursor **/
	void showCursor () {
		updateCurrentLine();
		if (cursorLine != firstLineShowing) {
			int step = cursorLine >= firstLineShowing ? 1 : -1;
			while (firstLineShowing > cursorLine || firstLineShowing + linesShowing - 1 < cursorLine) {
				firstLineShowing += step;
			}
		}
	}

	/** Calculates the text area line for the given cursor position **/
	private int calculateCurrentLineIndex (int cursor) {
		int index = 0;
		while (index < linesBreak.size && cursor > linesBreak.items[index]) {
			index++;
		}
		return index;
	}

	// OVERRIDE from TextField

	@Override
	protected void sizeChanged () {
		lastText = null; // Cause calculateOffsets to recalculate the line breaks.

		// The number of lines showed must be updated whenever the height is updated
		BitmapFont font = style.font;
		Drawable background = style.background;
		float availableHeight = getHeight() - (background == null ? 0 : background.getBottomHeight() + background.getTopHeight());
		linesShowing = (int)Math.floor(availableHeight / font.getLineHeight());
	}

	@Override
	protected float getTextY (BitmapFont font, Drawable background) {
		float textY = getHeight();
		if (background != null) {
			textY = (int)(textY - background.getTopHeight());
		}
		return textY;
	}

	@Override
	protected void drawSelection (Drawable selection, Batch batch, BitmapFont font, float x, float y) {
		int i = firstLineShowing * 2;
		float offsetY = 0;
		int minIndex = Math.min(cursor, selectionStart);
		int maxIndex = Math.max(cursor, selectionStart);
		while (i + 1 < linesBreak.size && i < (firstLineShowing + linesShowing) * 2) {

			int lineStart = linesBreak.get(i);
			int lineEnd = linesBreak.get(i + 1);

			if (!((minIndex < lineStart && minIndex < lineEnd && maxIndex < lineStart && maxIndex < lineEnd)
				|| (minIndex > lineStart && minIndex > lineEnd && maxIndex > lineStart && maxIndex > lineEnd))) {

				int start = Math.max(linesBreak.get(i), minIndex);
				int end = Math.min(linesBreak.get(i + 1), maxIndex);

				float selectionX = glyphPositions.get(start) - glyphPositions.get(linesBreak.get(i));
				float selectionWidth = glyphPositions.get(end) - glyphPositions.get(start);

				selection.draw(batch, x + selectionX + fontOffset, y - textHeight - font.getDescent() - offsetY, selectionWidth,
					font.getLineHeight());
			}

			offsetY += font.getLineHeight();
			i += 2;
		}
	}

	@Override
	protected void drawText (Batch batch, BitmapFont font, float x, float y) {
		float offsetY = 0;
		for (int i = firstLineShowing * 2; i < (firstLineShowing + linesShowing) * 2 && i < linesBreak.size; i += 2) {
			font.draw(batch, displayText, x, y + offsetY, linesBreak.items[i], linesBreak.items[i + 1], 0, Align.left, false);
			offsetY -= font.getLineHeight();
		}
	}

	@Override
	protected void drawCursor (Drawable cursorPatch, Batch batch, BitmapFont font, float x, float y) {
		float textOffset = cursor >= glyphPositions.size || cursorLine * 2 >= linesBreak.size ? 0
			: glyphPositions.get(cursor) - glyphPositions.get(linesBreak.items[cursorLine * 2]);
		cursorPatch.draw(batch, x + textOffset + fontOffset + font.getData().cursorX,
			y - font.getDescent() / 2 - (cursorLine - firstLineShowing + 1) * font.getLineHeight(), cursorPatch.getMinWidth(),
			font.getLineHeight());
	}

	@Override
	protected void calculateOffsets () {
		super.calculateOffsets();
		if (!this.text.equals(lastText)) {
			this.lastText = text;
			BitmapFont font = style.font;
			float maxWidthLine = this.getWidth()
				- (style.background != null ? style.background.getLeftWidth() + style.background.getRightWidth() : 0);
			linesBreak.clear();
			int lineStart = 0;
			int lastSpace = 0;
			char lastCharacter;
			Pool<GlyphLayout> layoutPool = Pools.get(GlyphLayout.class);
			GlyphLayout layout = layoutPool.obtain();
			for (int i = 0; i < text.length(); i++) {
				lastCharacter = text.charAt(i);
				if (lastCharacter == ENTER_DESKTOP || lastCharacter == ENTER_ANDROID) {
					linesBreak.add(lineStart);
					linesBreak.add(i);
					lineStart = i + 1;
				} else {
					lastSpace = (continueCursor(i, 0) ? lastSpace : i);
					layout.setText(font, text.subSequence(lineStart, i + 1));
					if (layout.width > maxWidthLine) {
						if (lineStart >= lastSpace) {
							lastSpace = i - 1;
						}
						linesBreak.add(lineStart);
						linesBreak.add(lastSpace + 1);
						lineStart = lastSpace + 1;
						lastSpace = lineStart;
					}
				}
			}
			layoutPool.free(layout);
			// Add last line
			if (lineStart < text.length()) {
				linesBreak.add(lineStart);
				linesBreak.add(text.length());
			}
			showCursor();
		}
	}

	@Override
	protected InputListener createInputListener () {
		return new TextAreaListener();
	}

	@Override
	public void setSelection (int selectionStart, int selectionEnd) {
		super.setSelection(selectionStart, selectionEnd);
		updateCurrentLine();
	}

	@Override
	protected void moveCursor (boolean forward, boolean jump) {
		int count = forward ? 1 : -1;
		int index = (cursorLine * 2) + count;
		if (index >= 0 && index + 1 < linesBreak.size && linesBreak.items[index] == cursor
			&& linesBreak.items[index + 1] == cursor) {
			cursorLine += count;
			if (jump) {
				super.moveCursor(forward, jump);
			}
			showCursor();
		} else {
			super.moveCursor(forward, jump);
		}
		updateCurrentLine();

	}

	@Override
	protected boolean continueCursor (int index, int offset) {
		int pos = calculateCurrentLineIndex(index + offset);
		return super.continueCursor(index, offset) && (pos < 0 || pos >= linesBreak.size - 2 || (linesBreak.items[pos + 1] != index)
			|| (linesBreak.items[pos + 1] == linesBreak.items[pos + 2]));
	}

	public int getCursorLine () {
		return cursorLine;
	}

	public int getFirstLineShowing () {
		return firstLineShowing;
	}

	public int getLinesShowing () {
		return linesShowing;
	}

	public float getCursorX () {
		return textOffset + fontOffset + style.font.getData().cursorX;
	}

	public float getCursorY () {
		BitmapFont font = style.font;
		return -(-font.getDescent() / 2 - (cursorLine - firstLineShowing + 1) * font.getLineHeight());
	}

	/** Input listener for the text area **/
	public class TextAreaListener extends TextFieldClickListener {

		@Override
		protected void setCursorPosition (float x, float y) {
			moveOffset = -1;

			Drawable background = style.background;
			BitmapFont font = style.font;

			float height = getHeight();

			if (background != null) {
				height -= background.getTopHeight();
				x -= background.getLeftWidth();
			}
			x = Math.max(0, x);
			if (background != null) {
				y -= background.getTopHeight();
			}

			cursorLine = (int)Math.floor((height - y) / font.getLineHeight()) + firstLineShowing;
			cursorLine = Math.max(0, Math.min(cursorLine, getLines() - 1));

			super.setCursorPosition(x, y);
			updateCurrentLine();
		}

		@Override
		public boolean keyDown (InputEvent event, int keycode) {
			boolean result = super.keyDown(event, keycode);
			Scene stage = getScene();
			if (stage != null && stage.getKeyboardFocus() == TextArea.this) {
				boolean repeat = false;
				boolean shift = Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT) || Gdx.input.isKeyPressed(Input.Keys.SHIFT_RIGHT);
				if (keycode == Input.Keys.DOWN) {
					if (shift) {
						if (!hasSelection) {
							selectionStart = cursor;
							hasSelection = true;
						}
					} else {
						clearSelection();
					}
					moveCursorLine(cursorLine + 1);
					repeat = true;

				} else if (keycode == Input.Keys.UP) {
					if (shift) {
						if (!hasSelection) {
							selectionStart = cursor;
							hasSelection = true;
						}
					} else {
						clearSelection();
					}
					moveCursorLine(cursorLine - 1);
					repeat = true;

				} else {
					moveOffset = -1;
				}
				if (repeat) {
					scheduleKeyRepeatTask(keycode);
				}
				showCursor();
				return true;
			}
			return result;
		}

		@Override
		public boolean keyTyped (InputEvent event, char character) {
			boolean result = super.keyTyped(event, character);
			showCursor();
			return result;
		}

		@Override
		protected void goHome (boolean jump) {
			if (jump) {
				cursor = 0;
			} else if (cursorLine * 2 < linesBreak.size) {
				cursor = linesBreak.get(cursorLine * 2);
			}
		}

		@Override
		protected void goEnd (boolean jump) {
			if (jump || cursorLine >= getLines()) {
				cursor = text.length();
			} else if (cursorLine * 2 + 1 < linesBreak.size) {
				cursor = linesBreak.get(cursorLine * 2 + 1);
			}
		}
	}
}
