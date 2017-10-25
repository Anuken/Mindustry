package io.anuke.mindustry.world;

import static io.anuke.mindustry.Vars.tilesize;

import com.badlogic.gdx.utils.Array;

import io.anuke.mindustry.entities.TileEntity;
import io.anuke.mindustry.world.blocks.Blocks;
import io.anuke.ucore.util.Bits;


public class Tile{
	private static final Array<Tile> tmpArray = new Array<>();
	
	private Block floor = Blocks.air;
	private Block block = Blocks.air;
	/**The coordinates of the core tile this is linked to, in the form of two bytes packed into one.
	 * This is relative to the block it is linked to; negate coords to find the link.*/
	public byte link = 0;
	public TileEntity entity;
	public short x, y;
	public byte rotation, dump;
	
	public Tile(int x, int y){
		this.x = (short)x;
		this.y = (short)y;
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
		return x + y * World.worldsize;
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
	
	/**Returns the breaktime of the block, <i>or</i> the breaktime of the linked block, if this tile is linked.*/
	public float getBreakTime(){
		return link == 0 ? block.breaktime : getLinked().block.breaktime;
	}
	
	public void setBlock(Block type, int rotation){
		if(rotation < 0) rotation = (-rotation + 2);
		rotation %= 4;
		this.block = type;
		this.rotation = (byte)rotation;
		this.link = 0;
		changed();
	}
	
	public void setBlock(Block type){
		this.block = type;
		this.link = 0;
		changed();
	}
	
	public void setFloor(Block type){
		this.floor = type;
	}
	
	public boolean passable(){
		return isLinked() || !(floor.solid || (block.solid && !block.update));
	}
	
	public boolean solid(){
		return block.solid || floor.solid;
	}
	
	public boolean breakable(){
		if(link == 0){
			return (block.update || block.breakable);
		}else{
			return getLinked().breakable();
		}
	}
	
	public boolean isLinked(){
		return link != 0;
	}
	
	/**Sets this to a linked tile, which sets the block to a blockpart. dx and dy can only be -8-7.*/
	public void setLinked(byte dx, byte dy){
		setBlock(Blocks.blockpart);
		link = Bits.packByte((byte)(dx + 8), (byte)(dy + 8));
	}
	
	/**Returns the list of all tiles linked to this multiblock, or an empty array if it's not a multiblock.
	 * This array contains only linked tiles, not this tile itself.*/
	public Array<Tile> getLinkedTiles(){
		tmpArray.clear();
		if(!(block.width == 1 && block.health == 1)){
			int offsetx = -(block.width-1)/2;
			int offsety = -(block.height-1)/2;
			for(int dx = 0; dx < block.width; dx ++){
				for(int dy = 0; dy < block.height; dy ++){
					Tile other = World.tile(x + dx - offsetx, y + dy - offsety);
					tmpArray.add(other);
				}
			}
		}
		return tmpArray;
	}
	
	/**Returns the block the multiblock is linked to, or null if it is not linked to any block.*/
	public Tile getLinked(){
		if(link == 0){
			return null;
		}else{
			byte dx = Bits.getLeftByte(link);
			byte dy = Bits.getRightByte(link);
			return World.tile(x - (dx - 8), y - (dy - 8));
		}
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
		return floor.name() + ":" + block.name() + 
				(link != 0 ? " link=[" + (Bits.getLeftByte(link) - 8) + ", " + (Bits.getRightByte(link) - 8) +  "]" : "");
	}
}
