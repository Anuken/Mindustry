package io.anuke.mindustry.world.blocks.types.distribution;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import io.anuke.mindustry.entities.TileEntity;
import io.anuke.mindustry.world.Block;

//TODO
public class Teleporter extends Block{
	public static final int colors = 4;
	
	public Teleporter(String name) {
		super(name);
	}
	
	@Override
	public TileEntity getEntity(){
		return new TeleporterEntity();
	}

	public static class TeleporterEntity extends TileEntity{
		public byte color = 0;
		
		@Override
		public void write(DataOutputStream stream) throws IOException{
			stream.writeByte(color);
		}
		
		@Override
		public void read(DataInputStream stream) throws IOException{
			color = stream.readByte();
		}
	}
}
