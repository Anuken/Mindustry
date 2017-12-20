package io.anuke.mindustry.world.blocks.types.distribution;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import com.badlogic.gdx.graphics.g2d.TextureRegion;

import io.anuke.mindustry.entities.TileEntity;
import io.anuke.mindustry.resource.Item;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.types.Configurable;
import io.anuke.ucore.core.Draw;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.scene.ui.layout.Table;
import io.anuke.ucore.util.Mathf;
import io.anuke.ucore.util.Tmp;

public class Sorter extends Junction implements Configurable{
	
	public Sorter(String name) {
		super(name);
	}
	
	@Override
	public void draw(Tile tile){
		super.draw(tile);
		
		SorterEntity entity = tile.entity();
		
		TextureRegion region = Draw.region("icon-" + entity.sortItem.name());
		Tmp.tr1.setRegion(region, 4, 4, 1, 1);
		
		Draw.rect(Tmp.tr1, tile.worldx(), tile.worldy(), 4f, 4f);
	}
	
	@Override
	public boolean acceptItem(Item item, Tile dest, Tile source){
		if(source.block() instanceof Sorter) return false;
		Tile to = getTileTarget(item, dest, source, false);
		
		return to != null && to.block().acceptItem(item, to, dest);
	}
	
	@Override
	public void handleItem(Item item, Tile tile, Tile source){
		Tile to = getTileTarget(item, tile, source, true);
		
		Timers.run(15, ()->{
			if(to == null || to.entity == null) return;
			to.block().handleItem(item, to, tile);
		});
		
	}
	
	Tile getTileTarget(Item item, Tile dest, Tile source, boolean flip){
		SorterEntity entity = dest.entity();
		
		int dir = source.relativeTo(dest.x, dest.y);
		if(dir == -1) return null;
		Tile to = null;
		
		if(item == entity.sortItem){
			to = dest.getNearby()[dir];
		}else{
			Tile a = dest.getNearby()[Mathf.mod(dir - 1, 4)];
			Tile b = dest.getNearby()[Mathf.mod(dir + 1, 4)];
			boolean ac = a.block().acceptItem(item, a, dest);
			boolean bc = b.block().acceptItem(item, b, dest);
			
			if(ac && !bc){
				to = a;
			}else if(bc && !ac){
				to = b;
			}else{
				if(dest.getDump() == 0){
					to = a;
					if(flip)
						dest.setDump((byte)1);
				}else{
					to = b;
					if(flip)
						dest.setDump((byte)0);
				}
			}
		}
		
		return to;
	}
	
	@Override
	public void buildTable(Tile tile, Table table){
		SorterEntity entity = tile.entity();
		
		table.addIButton("icon-arrow-left", 10*3, ()->{
			int color = entity.sortItem.ordinal();
			
			color --;
			if(color < 0)
				color += Item.values().length;
			
			entity.sortItem = Item.values()[color];
		});
		
		table.add().size(40f);
		
		table.addIButton("icon-arrow-right", 10*3, ()->{
			int color = entity.sortItem.ordinal();
			
			color ++;
			color %= Item.values().length;
			
			entity.sortItem = Item.values()[color];
		});
	}
	
	@Override
	public TileEntity getEntity(){
		return new SorterEntity();
	}

	public static class SorterEntity extends TileEntity{
		public Item sortItem = Item.iron;
		
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
