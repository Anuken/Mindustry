package io.anuke.mindustry.world.blocks.types;

import io.anuke.mindustry.world.Tile;

public interface PowerAcceptor{
	/**Attempts to add some power to this block; returns the amount of power <i>not</i> accepted.
	 * To add no power, you would return amount.*/
	public float addPower(Tile tile, float amount);
	public boolean acceptsPower(Tile tile);
}
