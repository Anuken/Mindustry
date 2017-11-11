package io.anuke.mindustry.world;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;

import io.anuke.mindustry.GameState;
import io.anuke.mindustry.GameState.State;
import io.anuke.mindustry.Vars;
import io.anuke.mindustry.entities.TileEntity;
import io.anuke.mindustry.resource.Item;
import io.anuke.mindustry.resource.ItemStack;
import io.anuke.mindustry.resource.Liquid;
import io.anuke.ucore.core.Draw;
import io.anuke.ucore.core.Effects;
import io.anuke.ucore.util.Tmp;
public class Block{
	private static int lastid;
	private static Array<Block> blocks = new Array<Block>();
	protected static TextureRegion temp = new TextureRegion();
	
	/**internal name*/
	public final String name;
	/**internal ID*/
	public final int id;
	/**display name*/
	public String formalName;
	/**played on destroy*/
	public String explosionEffect = "explosion";
	/**played on destroy*/
	public String explosionSound = "break";
	/**whether this block has a tile entity that updates*/
	public boolean update;
	/**whether this block has health and can be destroyed*/
	public boolean destructible;
	/**whether this is solid*/
	public boolean solid;
	/**whether this is rotateable*/
	public boolean rotate;
	/**whether you can break this with rightblick*/
	public boolean breakable;
	/**time it takes to break*/
	public float breaktime = 30;
	/**tile entity health*/
	public int health = 40;
	/**the shadow drawn under the block*/
	public String shadow = "shadow";
	/**edge fallback, used mainly for ores*/
	public String edge = "stone";
	/**whether this block has 3 variants*/
	public boolean vary = true;
	/**stuff that drops when broken*/
	public ItemStack drops = null;
	/**liquids that drop from this block, used for pumps*/
	public Liquid liquidDrop = null;
	/**multiblock width/height*/
	public int width = 1, height = 1;

	public Block(String name) {
		blocks.add(this);
		
		this.name = name;
		this.formalName = name;
		this.solid = false;
		this.id = lastid++;
	}
	
	public void drawOver(Tile tile){}
	public void drawPixelOverlay(Tile tile){}
	public void drawPlace(int x, int y, boolean valid){}
	
	public String name(){
		return name;
	}
	
	public String description(){
		return null;
	}
	
	public String errorMessage(Tile tile){
		return null;
	}
	
	public boolean canReplace(Block other){
		return false;
	}
	
	public int handleDamage(Tile tile, int amount){
		return amount;
	}

	public void handleItem(Item item, Tile tile, Tile source){
		tile.entity.addItem(item, 1);
	}
	
	public boolean acceptItem(Item item, Tile dest, Tile source){
		return false;
	}
	
	public void update(Tile tile){}
	
	public void onDestroyed(Tile tile){
		float x = tile.worldx(), y = tile.worldy();
		
		Effects.shake(4f, 4f, x, y);
		Effects.effect(explosionEffect, x, y);
		Effects.sound(explosionSound, x, y);
	}
	
	public TileEntity getEntity(){
		return new TileEntity();
	}

	/**
	 * Tries to put this item into a nearby container, if there are no available
	 * containers, it gets added to the block's inventory.
	 */
	protected void offloadNear(Tile tile, Item item){
		int i = tile.dump;
		int pdump = tile.dump;
		
		Tile[] tiles = tile.getNearby();
		
		for(int j = 0; j < 4; j ++){
			Tile other = tiles[i];
			if(other != null && other.block().acceptItem(item, other, tile)
					//don't output to things facing this thing
					&& !(other.block().rotate && (other.rotation + 2) % 4 == i)){
				
				other.block().handleItem(item, other, tile);
				tile.dump = (byte)((i+1)%4);
				return;
			}
			i++;
			i %= 4;
		}
		tile.dump = (byte)pdump;
		handleItem(item, tile, tile);
	}

	/** Try dumping any item near the tile. */
	protected boolean tryDump(Tile tile){
		return tryDump(tile, -1, null);
	}

	/**
	 * Try dumping any item near the tile. -1 = any direction
	 */
	protected boolean tryDump(Tile tile, int direction, Item todump){
		int i = tile.dump;
		
		Tile[] tiles = tile.getNearby();
		
		for(int j = 0; j < 4; j ++){
			Tile other = tiles[i];
			
			if(i == direction || direction == -1)
				for(Item item : Item.values()){
					
					if(todump != null && item != todump) continue;
					
					if(tile.entity.hasItem(item) && other != null && other.block().acceptItem(item, other, tile) &&
					//don't output to things facing this thing
							!(other.block().rotate && (other.rotation + 2) % 4 == i)){
						other.block().handleItem(item, other, tile);
						tile.entity.removeItem(item, 1);
						tile.dump = (byte)((i+1)%4);
						return true;
					}
				}
			i++;
			i %= 4;
		}

		return false;
	}

	/**
	 * Try offloading an item to a nearby container. Returns true if success.
	 */
	protected boolean offloadDir(Tile tile, Item item){
		Tile other = tile.getNearby()[tile.rotation];
		if(other != null && other.block().acceptItem(item, other, tile)){
			other.block().handleItem(item, other, tile);
			//other.entity.addCovey(item, ch == 1 ? 0.5f : ch ==2 ? 1f : 0f);
			return true;
		}
		return false;
	}
	
	public void drawCache(Tile tile){
		
	}
	
	public void draw(Tile tile){
		//note: multiblocks do not support rotation
		if(width == 1 && height == 1){
			Draw.rect(name(), tile.worldx(), tile.worldy(), rotate ? tile.rotation * 90 : 0);
		}else{
			//if multiblock, make sure to draw even block sizes offset, since the core block is at the BOTTOM LEFT
			Vector2 offset = getPlaceOffset();
			Draw.rect(name(), tile.worldx() + offset.x, tile.worldy() + offset.y);
		}
		
		//update the tile entity through the draw method, only if it's an entity without updating
		//TODO enable
		if(destructible && !update && !GameState.is(State.paused)){
		//	tile.entity.update();
		}
	}
	
	/**Offset for placing and drawing multiblocks.*/
	public Vector2 getPlaceOffset(){
		return Tmp.v3.set(((width + 1) % 2) * Vars.tilesize/2, ((height + 1) % 2) * Vars.tilesize/2);
	}
	
	public boolean isMultiblock(){
		return width != 1 || height != 1;
	}
	
	public static Array<Block> getAllBlocks(){
		return blocks;
	}
	
	public static Block getByID(int id){
		return blocks.get(id);
	}
}
