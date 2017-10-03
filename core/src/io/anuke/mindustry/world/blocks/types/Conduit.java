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
	protected float liquidCapacity = 10f;
	protected float flowfactor = 4f;
	
	public Conduit(String name) {
		super(name);
		rotate = true;
		update = true;
	}
	
	@Override
	public String description(){
		return "Transports liquids";
	}
	
	@Override
	public void draw(Tile tile){
		ConduitEntity entity = tile.entity();
		
		Draw.rect(name() + "bottom", tile.worldx(), tile.worldy(), tile.rotation * 90);
		
		if(entity.liquid != null && entity.liquidAmount > 0.01f){
			Draw.color(entity.liquid.color);
			Draw.alpha(entity.liquidAmount / liquidCapacity);
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
		
		if(entity.liquidAmount > 0.01f && Timers.get(entity, "flow", 3)){
			tryMoveLiquid(tile, tile.getNearby()[tile.rotation]);
		}
		
	}
	
	public void tryDumpLiquid(Tile tile){
		ConduitEntity entity = tile.entity();
		
		if(entity.liquidAmount > 0.01f){
			tryMoveLiquid(tile, tile.getNearby()[tile.dump]);
			tile.dump ++;
			tile.dump %= 4;
		}
	}
	
	public void tryMoveLiquid(Tile tile, Tile next){
		ConduitEntity entity = tile.entity();
		
		Liquid liquid = entity.liquid;
		
		if(next != null && next.block() instanceof Conduit && entity.liquidAmount > 0.01f){
			Conduit other = (Conduit)next.block();
			ConduitEntity otherentity = next.entity();
			
			float flow = Math.min(other.liquidCapacity - otherentity.liquidAmount - 0.001f, Math.min(entity.liquidAmount/flowfactor, entity.liquidAmount));
			
			if(flow <= 0f || entity.liquidAmount < flow) return;
			
			if(other.acceptLiquid(next, tile, liquid, flow)){
				other.handleLiquid(next, tile, liquid, flow);
				entity.liquidAmount -= flow;
			}
		}
	}
	
	public boolean acceptLiquid(Tile tile, Tile source, Liquid liquid, float amount){
		ConduitEntity entity = tile.entity();
		
		return entity.liquidAmount + amount < liquidCapacity && (entity.liquid == liquid || entity.liquidAmount <= 0.01f);
	}
	
	public void handleLiquid(Tile tile, Tile source, Liquid liquid, float amount){
		ConduitEntity entity = tile.entity();
		entity.liquid = liquid;
		entity.liquidAmount += amount;
	}
	
	static class ConduitEntity extends TileEntity{
		Liquid liquid;
		float liquidAmount;
		
		@Override
		public void write(DataOutputStream stream) throws IOException{
			stream.writeByte(liquid.ordinal());
			stream.writeByte((byte)(liquidAmount));
		}
		
		@Override
		public void read(DataInputStream stream) throws IOException{
			liquid = Liquid.values()[stream.readByte()];
			liquidAmount = stream.readByte();
		}
	}
}
