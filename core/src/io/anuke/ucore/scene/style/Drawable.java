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

package io.anuke.ucore.scene.style;

import com.badlogic.gdx.graphics.g2d.Batch;

/** A drawable knows how to draw itself at a given rectangular size. It provides border sizes and a minimum size so that other code
 * can determine how to size and position content.
 * @author Nathan Sweet */
public interface Drawable {
	/** Draws this drawable at the specified bounds. The drawable should be tinted with {@link Batch#getColor()}, possibly by mixing
	 * its own color. */
	public void draw (Batch batch, float x, float y, float width, float height);
	
	public void draw (Batch batch, float x, float y, float originX, float originY, float width, float height, float scaleX,
			float scaleY, float rotation);

	public float getLeftWidth ();

	public void setLeftWidth (float leftWidth);

	public float getRightWidth ();

	public void setRightWidth (float rightWidth);

	public float getTopHeight ();

	public void setTopHeight (float topHeight);

	public float getBottomHeight ();

	public void setBottomHeight (float bottomHeight);

	public float getMinWidth ();

	public void setMinWidth (float minWidth);

	public float getMinHeight ();

	public void setMinHeight (float minHeight);
}
