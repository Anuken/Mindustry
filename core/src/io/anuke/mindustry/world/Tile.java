package io.anuke.mindustry.world;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import io.anuke.mindustry.Vars;
import io.anuke.mindustry.entities.TileEntity;
import io.anuke.mindustry.world.blocks.Blocks;
import io.anuke.ucore.util.Bits;
import io.anuke.ucore.util.Mathf;

import static io.anuke.mindustry.Vars.tilesize;


public class Tile{
	private static final Array<Tile> tmpArray = new Array<>();
	
	/**Packed block data. Left is floor, right is block.*/
	private short blocks;
	/**Packed data. Left is placerot, right is extra data, packed into two half-bytes: left is dump, right is extra.*/
	private short data;
	/**The coordinates of the core tile this is linked to, in the form of two bytes packed into one.
	 * This is relative to the block it is linked to; negate coords to find the link.*/
	public byte link = 0;
	public short x, y;
	public TileEntity entity;
	
	public Tile(int x, int y){
		this.x = (short)x;
		this.y = (short)y;
	}
	
	public Tile(int x, int y, Block floor){
		this(x, y);
		iSetFloor(floor);
	}

	public int packedPosition(){
		return x + y * Vars.world.width();
	}
	
	private void iSetFloor(Block floor){
		byte id = (byte)floor.id;
		blocks = Bits.packShort(id, getWallID());
	}
	
	private void iSetBlock(Block wall){
		byte id = (byte)wall.id;
		blocks = Bits.packShort(getFloorID(), id);
	}
	
	public byte getWallID(){
		return Bits.getRightByte(blocks);
	}
	
	public byte getFloorID(){
		return Bits.getLeftByte(blocks);
	}
	
	/**Return relative placerot to a coordinate. Returns -1 if the coordinate is not near this tile.*/
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
	
	public void damageNearby(int rad, int amount, float falloff){
		//TODO damage falloff?
		for(int dx = -rad; dx <= rad; dx ++){
			for(int dy = -rad; dy <= rad; dy ++){
				float dst = Vector2.dst(dx, dy, 0, 0);
				if(dst > rad || (dx == 0 && dy == 0)) continue;
				
				Tile other = Vars.world.tile(x + dx, y + dy);
				if(other != null && other.entity != null){
					other.entity.damage((int)(amount * Mathf.lerp(1f-dst/rad, 1f, falloff)));
				}
			}
		}
	}
	
	public int id(){
		return x + y * Vars.world.width();
	}
	
	public float worldx(){
		return x * tilesize;
	}
	
	public float worldy(){
		return y * tilesize;
	}
	
	public Block floor(){
		return Block.getByID(getFloorID());
	}
	
	public Block block(){
		return Block.getByID(getWallID());
	}
	
	/**Returns the breaktime of the block, <i>or</i> the breaktime of the linked block, if this tile is linked.*/
	public float getBreakTime(){
		Block block = block();
		return link == 0 ? block.breaktime : getLinked().block().breaktime;
	}
	
	public void setBlock(Block type, int rotation){
		if(rotation < 0) rotation = (-rotation + 2);
		rotation %= 4;
		iSetBlock(type);
		setRotation((byte)rotation);
		this.link = 0;
		changed();
	}
	
	public void setBlock(Block type){
		iSetBlock(type);
		this.link = 0;
		changed();
	}
	
	public void setFloor(Block type){
		iSetFloor(type);
	}
	
	public void setRotation(byte rotation){
		data = Bits.packShort(rotation, Bits.getRightByte(data));
	}
	
	public void setDump(byte dump){
		data = Bits.packShort(getRotation(), Bits.packByte(dump, getExtra()));
	}
	
	public void setExtra(byte extra){
		data = Bits.packShort(getRotation(), Bits.packByte(getDump(), extra));
	}
	
	public byte getRotation(){
		return Bits.getLeftByte(data);
	}
	
	public byte getDump(){
		return Bits.getLeftByte(Bits.getRightByte(data));
	}
	
	public byte getExtra(){
		return Bits.getRightByte(Bits.getRightByte(data));
	}

	public boolean passable(){
		Block block = block();
		Block floor = floor();
		return isLinked() || !((floor.solid && (block == Blocks.air || block.solidifes)) || (block.solid && (!block.destructible && !block.update)));
	}
	
	public boolean solid(){
		Block block = block();
		Block floor = floor();
		return block.solid || (floor.solid && (block == Blocks.air || block.solidifes)) || block.isSolidFor(this);
	}
	
	public boolean breakable(){
		Block block = block();
		if(link == 0){
			return (block.destructible || block.breakable || block.update);
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
	 * This array contains all linked tiles, including this tile itself.*/
	public Array<Tile> getLinkedTiles(){
		Block block = block();
		tmpArray.clear();
		if(!(block.width == 1 && block.height == 1)){
			int offsetx = -(block.width-1)/2;
			int offsety = -(block.height-1)/2;
			for(int dx = 0; dx < block.width; dx ++){
				for(int dy = 0; dy < block.height; dy ++){
					Tile other = Vars.world.tile(x + dx + offsetx, y + dy + offsety);
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
			return Vars.world.tile(x - (dx - 8), y - (dy - 8));
		}
	}
	
	public Tile[] getNearby(){
		return Vars.world.getNearby(x, y);
	}

	public Tile[] getNearby(Tile[] copy){
		return Vars.world.getNearby(x, y);
	}
	
	public void changed(){
		if(entity != null){
			entity.remove();
			entity = null;
		}
		
		Block block = block();
		
		if(block.destructible || block.update){
			entity = block.getEntity().init(this, block.update);
		}
	}
	
	@Override
	public String toString(){
		Block block = block();
		Block floor = floor();
		
		return floor.name() + ":" + block.name() + "[" + x + "," + y + "] " +
				(link != 0 ? " link=[" + (Bits.getLeftByte(link) - 8) + ", " + (Bits.getRightByte(link) - 8) +  "]" : "");
	}
}
