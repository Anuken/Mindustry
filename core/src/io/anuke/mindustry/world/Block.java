package io.anuke.mindustry.world;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;

import io.anuke.mindustry.World;
import io.anuke.mindustry.entities.TileEntity;
import io.anuke.mindustry.resource.Item;
import io.anuke.ucore.core.Draw;
import io.anuke.ucore.graphics.Caches;
import io.anuke.ucore.util.Mathf;

public class Block{
	private static int lastid;
	
	protected static Vector2 vector = new Vector2();
	protected static Vector2 vector2 = new Vector2();
	protected static TextureRegion temp = new TextureRegion();
	
	public final String name;
	public boolean solid, update, rotate, breakable;
	public int health = 40;
	public String shadow = "shadow";
	public float breaktime = 30;
	public final int id;
	//edge fallback, used for ores
	public String edge = "stone";
	//whether to have 3 variants
	public boolean vary = true;

	public Block(String name) {
		this.name = name;
		this.solid = false;
		this.id = lastid++;
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

	public void handleItem(Tile tile, Item item, Tile source){
		tile.entity.addItem(item, 1);
	}
	
	public boolean accept(Item item, Tile dest, Tile source){
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
			if(other != null && other.block().accept(item, other, tile)
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
					
					if(tile.entity.hasItem(item) && other != null && other.block().accept(item, other, tile) &&
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
		if(other != null && other.block().accept(item, other, tile)){
			other.block().handleItem(other, item, tile);
			//other.entity.addCovey(item, ch == 1 ? 0.5f : ch ==2 ? 1f : 0f);
			return true;
		}
		return false;
	}
	
	public void drawCache(Tile tile){
		MathUtils.random.setSeed(tile.id());
		
		Caches.draw(vary ? (name() + MathUtils.random(1, 3))  : name(), tile.worldx(), tile.worldy());
		
		for(int dx = -1; dx <= 1; dx ++){
			for(int dy = -1; dy <= 1; dy ++){
				if(dx == 0 && dy == 0) continue;
				
				Tile other = World.tile(tile.x+dx, tile.y+dy);
				
				if(other == null) continue;
				
				Block floor = other.floor();
				
				if(floor.id <= this.id) continue;
				
				TextureRegion region = Draw.region(floor.name() + "edge");
				
				if(region == null)
					region = Draw.region(floor.edge + "edge");
				
				int sx = -dx*8+2, sy = -dy*8+2;
				int x = Mathf.clamp(sx, 0, 12);
				int y = Mathf.clamp(sy, 0, 12);
				int w = Mathf.clamp(sx+8, 0, 12)-x, h = Mathf.clamp(sy+8, 0, 12)-y;
				
				float rx = Mathf.clamp(dx*8, 0, 8-w);
				float ry = Mathf.clamp(dy*8, 0, 8-h);
				
				temp.setTexture(region.getTexture());
				temp.setRegion(region.getRegionX()+x, region.getRegionY()+y+h, w, -h);
				
				Caches.draw(temp, tile.worldx()-4 + rx, tile.worldy()-4 + ry, w, h);
			}
		}
	}

	public void draw(Tile tile){
		if(tile.floor() == this){
			throw new RuntimeException("Rendering non-cached tiles is disabled.");
		}else{
			Draw.rect(name(), tile.worldx(), tile.worldy(), rotate ? tile.rotation * 90 : 0);
		}
	}

}
