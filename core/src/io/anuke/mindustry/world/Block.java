package io.anuke.mindustry.world;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;

import io.anuke.mindustry.entities.TileEntity;
import io.anuke.mindustry.resource.Item;
import io.anuke.ucore.core.Draw;

public class Block{
	protected static Vector2 vector = new Vector2();
	protected static Vector2 vector2 = new Vector2();
	
	public final String name;
	public boolean solid, update, rotate;
	public int health = 40;

	public Block(String name) {
		this.name = name;
		solid = false;
	}
	
	public void drawOver(Tile tile){}
	public void drawPixelOverlay(Tile tile){}
	public void drawOverlay(Tile tile){}
	
	public String name(){
		return name;
	}
	
	public String description(){
		return "[no description]";
	}
	
	public String errorMessage(Tile tile){
		return null;
	}

	protected void handleItem(Tile tile, Item item, Tile source){
		tile.entity.addItem(item, 1);
	}
	
	public boolean accept(Item item){
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
			if(other != null && other.block().accept(item)
					//don't output to things facing this thing
					&& !(other.block().rotate && (other.rotation + 2) % 4 == i)){
				
				other.block().handleItem(other, item, tile);
				tile.dump = (i+1)%4;
				return;
			}
			i++;
			i %= 4;
		}
		tile.dump = pdump;
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
					
					if(tile.entity.hasItem(item) && other != null && other.block().accept(item) &&
					//don't output to things facing this thing
							!(other.block().rotate && (other.rotation + 2) % 4 == i)){
						other.block().handleItem(other, item, tile);
						tile.entity.removeItem(item, 1);
						tile.dump = (i+1)%4;
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
		if(other != null && other.block().accept(item)){
			other.block().handleItem(other, item, tile);
			//other.entity.addCovey(item, ch == 1 ? 0.5f : ch ==2 ? 1f : 0f);
			return true;
		}
		return false;
	}

	public void draw(Tile tile){
		if(tile.floor() == this){
			MathUtils.random.setSeed(tile.id());
			Draw.rect(name() + MathUtils.random(1, 3), tile.worldx(), tile.worldy(), rotate ? tile.rotation * 90 : 0);
		}else{
			Draw.rect(name(), tile.worldx(), tile.worldy(), rotate ? tile.rotation * 90 : 0);
		}
	}

}
