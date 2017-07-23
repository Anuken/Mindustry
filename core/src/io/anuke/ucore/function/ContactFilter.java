package io.anuke.ucore.function;

import io.anuke.ucore.aabb.Collider;

public interface ContactFilter{
	public boolean collide(Collider a, Collider b);
}
