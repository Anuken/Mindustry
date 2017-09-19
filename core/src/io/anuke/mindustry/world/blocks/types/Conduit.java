package io.anuke.mindustry.world.blocks.types;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import io.anuke.mindustry.entities.TileEntity;
import io.anuke.mindustry.resource.Liquid;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Tile;
import io.anuke.ucore.core.Draw;
import io.anuke.ucore.core.Timers;

public class Conduit extends Block{
	protected float capacity = 10f;
	protected float flowfactor = 4f;
	
	public Conduit(String name) {
		super(name);
		rotate = true;
		update = true;
	}
	
	@Override
	public void draw(Tile tile){
		ConduitEntity entity = tile.entity();
		
		Draw.rect(name() + "bottom", tile.worldx(), tile.worldy(), tile.rotation * 90);
		if(entity.liquid != null && entity.amount > 0.01f){
			Draw.color(entity.liquid.color);
			Draw.alpha(entity.amount / capacity);
			Draw.rect("conduitliquid", tile.worldx(), tile.worldy(), tile.rotation * 90);
			Draw.color();
		}
		Draw.rect(name() + "top", tile.worldx(), tile.worldy(), tile.rotation * 90);
		
	}
	
	@Override
	public TileEntity getEntity(){
		return new ConduitEntity();
	}
	
	@Override
	public void update(Tile tile){
		ConduitEntity entity = tile.entity();
		
		if(entity.amount > 0.01f && Timers.get(entity, "flow", 3)){
			tryMoveLiquid(tile, tile.getNearby()[tile.rotation]);
		}
		
	}
	
	public void tryDumpLiquid(Tile tile){
		ConduitEntity entity = tile.entity();
		
		if(entity.amount > 0.01f){
			tryMoveLiquid(tile, tile.getNearby()[tile.dump]);
			tile.dump ++;
			tile.dump %= 4;
		}
	}
	
	public void tryMoveLiquid(Tile tile, Tile next){
		ConduitEntity entity = tile.entity();
		
		Liquid liquid = entity.liquid;
		
		if(next != null && next.block() instanceof Conduit && entity.amount > 0.01f){
			Conduit other = (Conduit)next.block();
			ConduitEntity otherentity = next.entity();
			
			float flow = Math.min(other.capacity - otherentity.amount - 0.001f, Math.min(entity.amount/flowfactor, entity.amount));
			
			if(flow <= 0f || entity.amount < flow) return;
			
			if(other.accept(next, tile, liquid, flow)){
				other.addLiquid(next, tile, liquid, flow);
				entity.amount -= flow;
			}
		}
	}
	
	public boolean accept(Tile tile, Tile source, Liquid liquid, float amount){
		ConduitEntity entity = tile.entity();
		
		return entity.amount + amount < capacity && (entity.liquid == liquid || entity.amount <= 0.01f);
	}
	
	public void addLiquid(Tile tile, Tile source, Liquid liquid, float amount){
		ConduitEntity entity = tile.entity();
		entity.liquid = liquid;
		entity.amount += amount;
	}
	
	static class ConduitEntity extends TileEntity{
		Liquid liquid;
		float amount;
		
		@Override
		public void write(DataOutputStream stream) throws IOException{
			stream.writeByte(liquid.ordinal());
			stream.writeByte((byte)(amount));
		}
		
		@Override
		public void read(DataInputStream stream) throws IOException{
			liquid = Liquid.values()[stream.readByte()];
			amount = stream.readByte();
		}
	}
}
