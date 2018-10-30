package io.anuke.ucore.function;

import com.badlogic.gdx.math.Rectangle;

public interface BoundingBoxProvider<T>{
	public void getBoundingBox(T type, Rectangle out);
}
