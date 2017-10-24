package io.anuke.mindustry.world.blocks.types.distribution;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import io.anuke.mindustry.entities.TileEntity;
import io.anuke.mindustry.resource.Item;
import io.anuke.mindustry.world.Tile;

//TODO
public class Sorter extends Junction{
	
	public Sorter(String name) {
		super(name);
	}
	
	public boolean acceptItem(Item item, Tile tile, Tile source){
		SorterEntity entity = tile.entity();
		return super.acceptItem(item, tile, source) && item == entity.sortItem;
	}
	
	@Override
	public TileEntity getEntity(){
		return new SorterEntity();
	}

	public static class SorterEntity extends TileEntity{
		public Item sortItem = Item.stone;
		
		@Override
		public void write(DataOutputStream stream) throws IOException{
			stream.writeByte(sortItem.ordinal());
		}
		
		@Override
		public void read(DataInputStream stream) throws IOException{
			sortItem = Item.values()[stream.readByte()];
		}
	}
}
