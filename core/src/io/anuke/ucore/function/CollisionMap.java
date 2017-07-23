package io.anuke.ucore.function;

import io.anuke.ucore.aabb.Collider;

public interface CollisionMap{
	public boolean valid(Collider c, float x, float y);
}
