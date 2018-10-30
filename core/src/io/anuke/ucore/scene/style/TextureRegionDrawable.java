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
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion;
import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasSprite;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

import io.anuke.ucore.scene.ui.layout.Unit;

/** Drawable for a {@link TextureRegion}.
 * @author Nathan Sweet */
public class TextureRegionDrawable extends BaseDrawable implements TransformDrawable {
	private TextureRegion region;

	/** Creates an uninitialized TextureRegionDrawable. The texture region must be set before use. */
	public TextureRegionDrawable () {
	}

	public TextureRegionDrawable (TextureRegion region) {
		setRegion(region);
	}

	public TextureRegionDrawable (TextureRegionDrawable drawable) {
		super(drawable);
		setRegion(drawable.region);
	}

	public void draw (Batch batch, float x, float y, float width, float height) {
		batch.draw(region, x, y, width, height);
	}

	public void draw (Batch batch, float x, float y, float originX, float originY, float width, float height, float scaleX,
		float scaleY, float rotation) {
		batch.draw(region, x, y, originX, originY, width, height, scaleX, scaleY, rotation);
	}

	public void setRegion (TextureRegion region) {
		this.region = region;
		setMinWidth(Unit.dp.scl(region.getRegionWidth()));
		setMinHeight(Unit.dp.scl(region.getRegionHeight()));
	}

	public TextureRegion getRegion () {
		return region;
	}

	/** Creates a new drawable that renders the same as this drawable tinted the specified color. */
	public Drawable tint (Color tint) {
		Sprite sprite;
		if (region instanceof AtlasRegion)
			sprite = new AtlasSprite((AtlasRegion)region);
		else
			sprite = new Sprite(region);
		sprite.setColor(tint);
		sprite.setSize(getMinWidth(), getMinHeight());
		SpriteDrawable drawable = new SpriteDrawable(sprite);
		drawable.setLeftWidth(getLeftWidth());
		drawable.setRightWidth(getRightWidth());
		drawable.setTopHeight(getTopHeight());
		drawable.setBottomHeight(getBottomHeight());
		return drawable;
	}
}
