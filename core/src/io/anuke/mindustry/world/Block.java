package io.anuke.mindustry.world;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Array;

import io.anuke.mindustry.entities.TileEntity;
import io.anuke.mindustry.resource.Item;
import io.anuke.mindustry.resource.ItemStack;
import io.anuke.mindustry.resource.Liquid;
import io.anuke.ucore.core.Draw;

public class Block{
	private static int lastid;
	private static Array<Block> blocks = new Array<Block>();
	protected static TextureRegion temp = new TextureRegion();
	
	public final String name;
	public String formalName;
	public boolean solid, update, rotate, breakable;
	public int health = 40;
	public String shadow = "shadow";
	public float breaktime = 30;
	public final int id;
	//edge fallback, used for ores
	public String edge = "stone";
	//whether to have 3 variants
	public boolean vary = true;
	//stuff that drops when broken
	public ItemStack drops = null;
	public Liquid liquidDrop = null;
	public int width, height;

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

	public void handleItem(Tile tile, Item item, Tile source){
		tile.entity.addItem(item, 1);
	}
	
	public boolean acceptItem(Item item, Tile dest, Tile source){
		return false;
	}
	
	public void update(Tile tile){}
	
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
				
				other.block().handleItem(other, item, tile);
				tile.dump = (byte)((i+1)%4);
				return;
			}
			i++;
			i %= 4;
		}
		tile.dump = (byte)pdump;
		handleItem(tile, item, tile);
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
						other.block().handleItem(other, item, tile);
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
			other.block().handleItem(other, item, tile);
			//other.entity.addCovey(item, ch == 1 ? 0.5f : ch ==2 ? 1f : 0f);
			return true;
		}
		return false;
	}
	
	public void drawCache(Tile tile){
		
	}
	
	public void draw(Tile tile){
		Draw.rect(name(), tile.worldx(), tile.worldy(), rotate ? tile.rotation * 90 : 0);
	}
	
	
	public static Array<Block> getAllBlocks(){
		return blocks;
	}
	
	public static Block getByID(int id){
		return blocks.get(id);
	}
}
