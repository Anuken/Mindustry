package io.anuke.mindustry.world;

import static io.anuke.mindustry.Vars.*;

import io.anuke.mindustry.World;
import io.anuke.mindustry.entities.TileEntity;
import io.anuke.mindustry.world.blocks.Blocks;


public class Tile{
	private Block floor = Blocks.air;
	private Block block = Blocks.air;
	public TileEntity entity;
	public int x, y, rotation, dump;
	
	public Tile(int x, int y){
		this.x = x;
		this.y = y;
	}
	
	public Tile(int x, int y, Block floor){
		this(x, y);
		this.floor = floor;
	}
	
	public int relativeTo(int cx, int cy){
		if(x == cx && y == cy - 1) return 1;
		if(x == cx && y == cy + 1) return 3;
		if(x == cx - 1 && y == cy) return 0;
		if(x == cx + 1 && y == cy) return 2;
		return -1;
	}
	
	public <T extends TileEntity> T entity(){
		return (T)entity;
	}
	
	public int id(){
		return x + y * worldsize;
	}
	
	public float worldx(){
		return x * tilesize;
	}
	
	public float worldy(){
		return y * tilesize;
	}
	
	public Block floor(){
		return floor;
	}
	
	public Block block(){
		return block;
	}
	
	public void setBlock(Block type){
		this.block = type;
		changed();
	}
	
	public void setFloor(Block type){
		this.floor = type;
	}
	
	public boolean artifical(){
		return block.update;
	}
	
	public Tile[] getNearby(){
		return World.getNearby(x, y);
	}
	
	public void changed(){
		if(entity != null){
			entity.remove();
			entity = null;
		}
		
		if(block.update)
			entity = block.getEntity().init(this).add();
	}
	
	@Override
	public String toString(){
		return floor.name() + ":" + block.name();
	}
}
