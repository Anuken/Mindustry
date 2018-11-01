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

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.NinePatch;

/** Drawable for a {@link NinePatch}.
 * <p>
 * The drawable sizes are set when the ninepatch is set, but they are separate values. Eg, {@link Drawable#getLeftWidth()} could
 * be set to more than {@link NinePatch#getLeftWidth()} in order to provide more space on the left than actually exists in the
 * ninepatch.
 * <p>
 * The min size is set to the ninepatch total size by default. It could be set to the left+right and top+bottom, excluding the
 * middle size, to allow the drawable to be sized down as small as possible.
 * @author Nathan Sweet */
public class NinePatchDrawable extends BaseDrawable implements TransformDrawable {
	protected NinePatch patch;

	/** Creates an uninitialized NinePatchDrawable. The ninepatch must be {@link #setPatch(NinePatch) set} before use. */
	public NinePatchDrawable () {
	}

	public NinePatchDrawable (NinePatch patch) {
		setPatch(patch);
	}

	public NinePatchDrawable (NinePatchDrawable drawable) {
		super(drawable);
		setPatch(drawable.patch);
	}

	public void draw (Batch batch, float x, float y, float width, float height) {
		patch.draw(batch, x, y, width, height);
	}

	public void draw (Batch batch, float x, float y, float originX, float originY, float width, float height, float scaleX,
		float scaleY, float rotation) {
		patch.draw(batch, x, y, originX, originY, width, height, scaleX, scaleY, rotation);
	}

	public void setPatch (NinePatch patch) {
		this.patch = patch;
		setMinWidth(patch.getTotalWidth());
		setMinHeight(patch.getTotalHeight());
		setTopHeight(patch.getPadTop());
		setRightWidth(patch.getPadRight());
		setBottomHeight(patch.getPadBottom());
		setLeftWidth(patch.getPadLeft());
	}

	public NinePatch getPatch () {
		return patch;
	}

	/** Creates a new drawable that renders the same as this drawable tinted the specified color. */
	public NinePatchDrawable tint (Color tint) {
		NinePatchDrawable drawable = new NinePatchDrawable(this);
		drawable.setPatch(new NinePatch(drawable.getPatch(), tint));
		return drawable;
	}
}
