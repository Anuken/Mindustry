package io.anuke.moment.world;

import io.anuke.moment.Moment;
import io.anuke.moment.entities.TileEntity;

public class Tile{
	private static Tile[] tiles = new Tile[4];
	private TileType floor = TileType.air;
	private TileType block = TileType.air;
	public TileEntity entity;
	public int x, y, rotation;
	
	public Tile(int x, int y){
		this.x = x;
		this.y = y;
	}
	
	public Tile(int x, int y, TileType floor){
		this(x, y);
		this.floor = floor;
	}
	
	public int id(){
		return x + y * Moment.i.size;
	}
	
	public float worldx(){
		return x * TileType.tilesize;
	}
	
	public float worldy(){
		return y * TileType.tilesize;
	}
	
	public TileType floor(){
		return floor;
	}
	
	public TileType block(){
		return block;
	}
	
	public void setBlock(TileType type){
		this.block = type;
		changed();
	}
	
	public void setFloor(TileType type){
		this.floor = type;
	}
	
	public boolean artifical(){
		return block.update;
	}
	
	public Tile[] getNearby(){
		tiles[0] = Moment.i.tile(x+1, y);
		tiles[1] = Moment.i.tile(x, y+1);
		tiles[2] = Moment.i.tile(x-1, y);
		tiles[3] = Moment.i.tile(x, y-1);
		
		return tiles;
	}
	
	
	public void changed(){
		//TODO where do the items go?
		if(entity != null)
			entity.remove();
		
		if(block.update)
			entity = new TileEntity(this).add();
	}
}
