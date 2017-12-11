package io.anuke.mindustry.world;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;

import io.anuke.mindustry.Vars;
import io.anuke.mindustry.core.GameState;
import io.anuke.mindustry.core.GameState.State;
import io.anuke.mindustry.entities.TileEntity;
import io.anuke.mindustry.entities.effect.Fx;
import io.anuke.mindustry.resource.Item;
import io.anuke.mindustry.resource.ItemStack;
import io.anuke.mindustry.resource.Liquid;
import io.anuke.ucore.core.Draw;
import io.anuke.ucore.core.Effects;
import io.anuke.ucore.core.Effects.Effect;
import io.anuke.ucore.util.Mathf;
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
	public Effect explosionEffect = Fx.blockexplosion;
	/**played on destroy*/
	public String explosionSound = "break";
	/**whether this block has a tile entity that updates*/
	public boolean update;
	/**whether this block has health and can be destroyed*/
	public boolean destructible;
	/**whether this is solid*/
	public boolean solid;
	/**whethe this block CAN be solid.*/
	public boolean solidifes;
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
	/**whether to display a different shadow per variant*/
	public boolean varyShadow = false;
	/**edge fallback, used mainly for ores*/
	public String edge = "stone";
	/**number of block variants, 0 to disable*/
	public int variants = 0;
	/**stuff that drops when broken*/
	public ItemStack drops = null;
	/**liquids that drop from this block, used for pumps*/
	public Liquid liquidDrop = null;
	/**multiblock width/height*/
	public int width = 1, height = 1;
	/**Brief block description. Should be short enough fit in the place menu.*/
	public String description;
	/**Detailed description of the block. Can be as long as necesary.*/
	public String fullDescription;
	/**Whether to draw this block in the expanded draw range.*/
	public boolean expanded = false;
	/**Max of timers used.*/
	public int timers = 0;
	/**Layer to draw extra stuff on.*/
	public Layer layer = Layer.overlay;
	/**Extra layer to draw extra extra stuff on.*/
	public Layer layer2 = Layer.overlay;

	public Block(String name) {
		blocks.add(this);
		
		this.name = name;
		this.formalName = name;
		this.solid = false;
		this.id = lastid++;
	}
	
	
	public void drawLayer(Tile tile){}
	public void drawLayer2(Tile tile){}
	public void drawSelect(Tile tile){}
	public void drawPlace(int x, int y, int rotation, boolean valid){}
	public void postInit(){}
	
	public void getStats(Array<String> list){
		list.add("[gray]size: " + width + "x" + height);
		list.add("[healthstats]health: " + health);
	}
	
	public String name(){
		return name;
	}
	
	public void onReset(){
		
	}
	
	public boolean isSolidFor(Tile tile){
		return false;
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
	 * containers, it gets added to the block's inventory.*/
	protected void offloadNear(Tile tile, Item item){
		byte i = tile.getDump();
		byte pdump = tile.getDump();
		
		Tile[] tiles = tile.getNearby();
		
		for(int j = 0; j < 4; j ++){
			Tile other = tiles[i];
			if(other != null && other.block().acceptItem(item, other, tile)
					//don't output to things facing this thing
					&& !(other.block().rotate && (other.getRotation() + 2) % 4 == i)){
				
				other.block().handleItem(item, other, tile);
				tile.setDump((byte)((i+1)%4));
				return;
			}
			i++;
			i %= 4;
		}
		tile.setDump((byte)pdump);
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
		int i = tile.getDump()%4;
		
		Tile[] tiles = tile.getNearby();
		
		for(int j = 0; j < 4; j ++){
			Tile other = tiles[i];
			
			if(i == direction || direction == -1)
				for(Item item : Item.values()){
					
					if(todump != null && item != todump) continue;
					
					if(tile.entity.hasItem(item) && other != null && other.block().acceptItem(item, other, tile)
					//don't output to things facing this thing
							/*!(other.block().rotate && (other.getRotation() + 2) % 4 == i)*/){
						other.block().handleItem(item, other, tile);
						tile.entity.removeItem(item, 1);
						tile.setDump((byte)((i+1)%4));
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
		Tile other = tile.getNearby()[tile.getRotation()];
		if(other != null && other.block().acceptItem(item, other, tile)){
			other.block().handleItem(item, other, tile);
			return true;
		}
		return false;
	}
	
	public void draw(Tile tile){
		//note: multiblocks do not support rotation
		if(!isMultiblock()){
			Draw.rect(variants > 0 ? (name() + Mathf.randomSeed(tile.id(), 1, variants))  : name(), 
					tile.worldx(), tile.worldy(), rotate ? tile.getRotation() * 90 : 0);
		}else{
			//if multiblock, make sure to draw even block sizes offset, since the core block is at the BOTTOM LEFT
			Vector2 offset = getPlaceOffset();
			
			Draw.rect(name(), tile.worldx() + offset.x, tile.worldy() + offset.y);
		}
		
		//update the tile entity through the draw method, only if it's an entity without updating
		if(destructible && !update && !GameState.is(State.paused)){
			tile.entity.update();
		}
	}
	
	public void drawShadow(Tile tile){
		
		if(varyShadow && variants > 0){
			Draw.rect(shadow + (Mathf.randomSeed(tile.id(), 1, variants)), tile.worldx(), tile.worldy());
		}else{
			Draw.rect(shadow, tile.worldx(), tile.worldy());
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
