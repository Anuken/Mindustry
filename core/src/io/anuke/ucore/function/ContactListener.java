package io.anuke.ucore.function;

import io.anuke.ucore.aabb.Collider;

public interface ContactListener{
	public void onContact(Collider a, Collider b);
}
