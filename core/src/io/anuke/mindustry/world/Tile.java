package io.anuke.mindustry.world;

import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.reflect.ClassReflection;
import io.anuke.mindustry.content.blocks.Blocks;
import io.anuke.mindustry.entities.TileEntity;
import io.anuke.mindustry.game.Team;
import io.anuke.mindustry.type.Recipe;
import io.anuke.mindustry.world.blocks.types.Floor;
import io.anuke.mindustry.world.blocks.types.modules.InventoryModule;
import io.anuke.mindustry.world.blocks.types.modules.LiquidModule;
import io.anuke.mindustry.world.blocks.types.modules.PowerModule;
import io.anuke.ucore.function.Consumer;
import io.anuke.ucore.util.Bits;
import io.anuke.ucore.util.Position;

import static io.anuke.mindustry.Vars.tilesize;
import static io.anuke.mindustry.Vars.world;


public class Tile implements Position{
	public static final Object tileSetLock = new Object();
	
	/**Block ID data.*/
	private byte floor, wall;
	private byte rotation;
	private byte dump;
	private byte team;
	/**The coordinates of the core tile this is linked to, in the form of two bytes packed into one.
	 * This is relative to the block it is linked to; negate coords to find the link.*/
	public byte link = 0;
	public short x, y;
	/**Tile traversal cost*/
	public float cost = 1f;
	public TileEntity entity;
	
	public Tile(int x, int y){
		this.x = (short)x;
		this.y = (short)y;
	}

	public Tile(int x, int y, byte floor, byte wall){
		this(x, y);
		this.floor = floor;
		this.wall = wall;
		changed();
	}
	
	public Tile(int x, int y, byte floor, byte wall, byte rotation, byte team){
		this(x, y);
		this.floor = floor;
		this.wall = wall;
		this.rotation = rotation;
		changed();
		this.team = team;
	}

	public int packedPosition(){
		return x + y * world.width();
	}
	
	public byte getWallID(){
		return wall;
	}
	
	public byte getFloorID(){
		return floor;
	}
	
	/**Return relative rotation to a coordinate. Returns -1 if the coordinate is not near this tile.*/
	public byte relativeTo(int cx, int cy){
		if(x == cx && y == cy - 1) return 1;
		if(x == cx && y == cy + 1) return 3;
		if(x == cx - 1 && y == cy) return 0;
		if(x == cx + 1 && y == cy) return 2;
		return -1;
	}

	public byte absoluteRelativeTo(int cx, int cy){
		if(x == cx && y <= cy - 1) return 1;
		if(x == cx && y >= cy + 1) return 3;
		if(x <= cx - 1 && y == cy) return 0;
		if(x >= cx + 1 && y == cy) return 2;
		return -1;
	}

	public byte sizedRelativeTo(int cx, int cy){
		if(x == cx && y == cy - 1 - block().size/2) return 1;
		if(x == cx && y == cy + 1 + block().size/2) return 3;
		if(x == cx - 1 - block().size/2 && y == cy) return 0;
		if(x == cx + 1 + block().size/2 && y == cy) return 2;
		return -1;
	}
	
	public <T extends TileEntity> T entity(){
		return (T)entity;
	}
	
	public int id(){
		return x + y * world.width();
	}
	
	public float worldx(){
		return x * tilesize;
	}
	
	public float worldy(){
		return y * tilesize;
	}

	public float drawx(){
		return block().getPlaceOffset().x + worldx();
	}

	public float drawy(){
		return block().getPlaceOffset().y + worldy();
	}
	
	public Floor floor(){
		return (Floor)Block.getByID(getFloorID());
	}
	
	public Block block(){
		return Block.getByID(getWallID());
	}

	public Team getTeam(){
		return Team.values()[team];
	}

	public void setTeam(Team team){
		this.team = (byte)team.ordinal();
	}
	
	/**Returns the break time of the block, <i>or</i> the breaktime of the linked block, if this tile is linked.*/
	public float getBreakTime(){
		Block block = target().block();
		if(Recipe.getByResult(block) != null){
			return Recipe.getByResult(block).cost;
		}else{
			return 15f;
		}
	}
	
	public void setBlock(Block type, int rotation){
		synchronized (tileSetLock) {
			if(rotation < 0) rotation = (-rotation + 2);
			this.wall = (byte)type.id;
			this.link = 0;
			setRotation((byte) (rotation % 4));
			changed();
		}
	}
	
	public void setBlock(Block type){
		synchronized (tileSetLock) {
			this.wall = (byte)type.id;
			this.link = 0;
			changed();
		}
	}
	
	public void setFloor(Block type){
		this.floor = (byte)type.id;
	}
	
	public void setRotation(byte rotation){
		this.rotation = rotation;
	}
	
	public void setDump(byte dump){
		this.dump = dump;
	}
	
	public byte getRotation(){
		return rotation;
	}
	
	public byte getDump(){
		return dump;
	}

	public boolean passable(){
		Block block = block();
		Block floor = floor();
		return isLinked() || !((floor.solid && (block == Blocks.air || block.solidifes)) || (block.solid && (!block.destructible && !block.update)));
	}

	public boolean synthetic(){
		Block block = block();
		return block.update || block.destructible;
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
	public synchronized Array<Tile> getLinkedTiles(Array<Tile> tmpArray){
		Block block = block();
		tmpArray.clear();
		if(block.isMultiblock()){
			int offsetx = -(block.size-1)/2;
			int offsety = -(block.size-1)/2;
			for(int dx = 0; dx < block.size; dx ++){
				for(int dy = 0; dy < block.size; dy ++){
					Tile other = world.tile(x + dx + offsetx, y + dy + offsety);
					tmpArray.add(other);
				}
			}
		}else{
			tmpArray.add(this);
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
			return world.tile(x - (dx - 8), y - (dy - 8));
		}
	}

	public void allNearby(Consumer<Tile> cons){
		for(GridPoint2 point : Edges.getEdges(block().size)){
			Tile tile = world.tile(x + point.x, y + point.y);
			if(tile != null){
				cons.accept(tile.target());
			}
		}
	}

	public void allInside(Consumer<Tile> cons){
		for(GridPoint2 point : Edges.getInsideEdges(block().size)){
			Tile tile = world.tile(x + point.x, y + point.y);
			if(tile != null){
				cons.accept(tile);
			}
		}
	}

	public Tile target(){
		Tile link = getLinked();
		return link == null ? this : link;
	}

	public Tile getNearby(GridPoint2 relative){
		return world.tile(x + relative.x, y + relative.y);
	}

	public Tile getNearby(int dx, int dy){
		return world.tile(x + dx, y + dy);
	}

	public Tile getNearby(int rotation){
		if(rotation == 0) return world.tile(x + 1, y);
		if(rotation == 1) return world.tile(x, y + 1);
		if(rotation == 2) return world.tile(x - 1, y);
		if(rotation == 3) return world.tile(x, y - 1);
		return null;
	}

	public Tile[] getNearby(Tile[] temptiles){
		temptiles[0] = world.tile(x+1, y);
		temptiles[1] = world.tile(x, y+1);
		temptiles[2] = world.tile(x-1, y);
		temptiles[3] = world.tile(x, y-1);
		return temptiles;
	}

	public void updateOcclusion(){
		cost = 0.5f;
		boolean occluded = false;
		outer:
		for(int dx = -1; dx <= 1; dx ++){
			for(int dy = -1; dy <= 1; dy ++){
				Tile tile = world.tile(x + dx, y + dy);
				if(tile != null && tile.solid()){
					occluded = true;
					break outer;
				}
			}
		}
		if(occluded){
			cost += 0.5f;
		}
	}
	
	public void changed(){

		synchronized (tileSetLock) {
			if (entity != null) {
				entity.remove();
				entity = null;
			}

			team = 0;

			Block block = block();

			if (block.hasEntity()) {
				entity = block.getEntity().init(this, block.update);
				if(block.hasItems) entity.items = new InventoryModule();
				if(block.hasLiquids) entity.liquids = new LiquidModule();
				if(block.hasPower) entity.power = new PowerModule();
			}

			updateOcclusion();
		}

		world.notifyChanged(this);
	}

	@Override
	public float getX() {
		return drawx();
	}

	@Override
	public float getY() {
		return drawy();
	}

	@Override
	public String toString(){
		Block block = block();
		Block floor = floor();
		
		return floor.name() + ":" + block.name() + "[" + x + "," + y + "] " + "entity=" + (entity == null ? "null" : ClassReflection.getSimpleName(entity.getClass())) +
				(link != 0 ? " link=[" + (Bits.getLeftByte(link) - 8) + ", " + (Bits.getRightByte(link) - 8) +  "]" : "");
	}
}
