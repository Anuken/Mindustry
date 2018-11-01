
package io.anuke.ucore.scene.style;

import com.badlogic.gdx.graphics.g2d.Batch;

/** A drawable that supports scale and rotation. */
public interface TransformDrawable extends Drawable {
	public void draw (Batch batch, float x, float y, float originX, float originY, float width, float height, float scaleX,
		float scaleY, float rotation);
}
