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
import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasSprite;

/** Drawable for a {@link Sprite}.
 * @author Nathan Sweet */
public class SpriteDrawable extends BaseDrawable implements TransformDrawable {
	private Sprite sprite;

	/** Creates an uninitialized SpriteDrawable. The sprite must be set before use. */
	public SpriteDrawable () {
	}

	public SpriteDrawable (Sprite sprite) {
		setSprite(sprite);
	}

	public SpriteDrawable (SpriteDrawable drawable) {
		super(drawable);
		setSprite(drawable.sprite);
	}

	public void draw (Batch batch, float x, float y, float width, float height) {
		Color spriteColor = sprite.getColor();
		float batchColor = batch.getPackedColor();
		sprite.setColor(batch.getColor().mul(spriteColor));

		sprite.setRotation(0);
		sprite.setScale(1, 1);
		sprite.setBounds(x, y, width, height);
		sprite.draw(batch);

		sprite.setColor(spriteColor);
		batch.setColor(batchColor);
	}

	public void draw (Batch batch, float x, float y, float originX, float originY, float width, float height, float scaleX,
		float scaleY, float rotation) {

		Color spriteColor = sprite.getColor();
		float batchColor = batch.getPackedColor();
		sprite.setColor(batch.getColor().mul(spriteColor));

		sprite.setOrigin(originX, originY);
		sprite.setRotation(rotation);
		sprite.setScale(scaleX, scaleY);
		sprite.setBounds(x, y, width, height);
		sprite.draw(batch);

		sprite.setColor(spriteColor);
		batch.setColor(batchColor);
	}

	public void setSprite (Sprite sprite) {
		this.sprite = sprite;
		setMinWidth(sprite.getWidth());
		setMinHeight(sprite.getHeight());
	}

	public Sprite getSprite () {
		return sprite;
	}

	/** Creates a new drawable that renders the same as this drawable tinted the specified color. */
	public SpriteDrawable tint (Color tint) {
		Sprite newSprite;
		if (sprite instanceof AtlasSprite)
			newSprite = new AtlasSprite((AtlasSprite)sprite);
		else
			newSprite = new Sprite(sprite);
		newSprite.setColor(tint);
		newSprite.setSize(getMinWidth(), getMinHeight());
		SpriteDrawable drawable = new SpriteDrawable(newSprite);
		drawable.setLeftWidth(getLeftWidth());
		drawable.setRightWidth(getRightWidth());
		drawable.setTopHeight(getTopHeight());
		drawable.setBottomHeight(getBottomHeight());
		return drawable;
	}
}
