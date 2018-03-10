package io.anuke.mindustry.world.blocks.types;

import io.anuke.mindustry.entities.TileEntity;
import io.anuke.mindustry.world.Block;

public abstract class PowerBlock extends Block{
	public float powerCapacity = 10f;
	public float voltage = 0.001f;
	
	public PowerBlock(String name) {
		super(name);
		update = true;
		solid = true;
		hasPower = true;
	}
	
	@Override
	public TileEntity getEntity(){
		return new PowerEntity();
	}
	
	public static class PowerEntity extends TileEntity{
		public float time; //generator time. this is a bit of a hack
	}
}
