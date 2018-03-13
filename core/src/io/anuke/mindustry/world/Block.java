package io.anuke.mindustry.world;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.reflect.ClassReflection;
import io.anuke.mindustry.core.GameState.State;
import io.anuke.mindustry.entities.TileEntity;
import io.anuke.mindustry.graphics.Fx;
import io.anuke.mindustry.net.Net;
import io.anuke.mindustry.net.NetEvents;
import io.anuke.mindustry.resource.Item;
import io.anuke.mindustry.resource.ItemStack;
import io.anuke.mindustry.resource.Liquid;
import io.anuke.ucore.core.Effects;
import io.anuke.ucore.core.Effects.Effect;
import io.anuke.ucore.graphics.Draw;
import io.anuke.ucore.scene.ui.layout.Table;
import io.anuke.ucore.util.Bundles;
import io.anuke.ucore.util.Mathf;

import static io.anuke.mindustry.Vars.*;

public class Block{
	private static int lastid;
	private static Array<Block> blocks = new Array<>();
	private static ObjectMap<String, Block> map = new ObjectMap<>();
	
	protected static TextureRegion temp = new TextureRegion();
	protected Vector2 offset = new Vector2();

	/**internal name*/
	public final String name;
	/**internal ID*/
	public final int id;
	/**display name*/
	public final String formalName;
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
	/**whether this block CAN be solid.*/
	public boolean solidifes;
	/**whether this is rotateable*/
	public boolean rotate;
	/**whether you can break this with rightclick*/
	public boolean breakable;
	/**whether this block can be drowned in*/
	public boolean liquid;
	/**time it takes to break*/
	public float breaktime = 18;
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
	public final String description;
	/**Detailed description of the block. Can be as long as necesary.*/
	public final String fullDescription;
	/**Whether to draw this block in the expanded draw range.*/
	public boolean expanded = false;
	/**Max of timers used.*/
	public int timers = 0;
	/**Layer to draw extra stuff on.*/
	public Layer layer = null;
	/**Extra layer to draw extra extra stuff on.*/
	public Layer layer2 = null;
	/**list of displayed block status bars. Defaults to health bar.*/
	public Array<BlockBar> bars = Array.with(new BlockBar(Color.RED, false, tile -> tile.entity.health / (float)tile.block().health));
	/**whether this block can be replaced in all cases*/
	public boolean alwaysReplace = false;
	/**whether this block has instant transfer checking. used for calculations to prevent infinite loops.*/
	public boolean instantTransfer = false;

	public Block(String name) {
		this.name = name;
		this.formalName = Bundles.get("block." + name + ".name", name);
		this.description = Bundles.getOrNull("block." + name + ".description");
		this.fullDescription = Bundles.getOrNull("block." + name + ".fulldescription");
		this.solid = false;
		this.id = lastid++;

		if(map.containsKey(name)){
			throw new RuntimeException("Two blocks cannot have the same names! Problematic block: " + name);
		}

		map.put(name, this);
		blocks.add(this);
	}

	public boolean isLayer(Tile tile){return true;}
	public boolean isLayer2(Tile tile){return true;}
	public void drawLayer(Tile tile){}
	public void drawLayer2(Tile tile){}
	public void drawSelect(Tile tile){}
	public void drawPlace(int x, int y, int rotation, boolean valid){}
	public void placed(Tile tile){}
	public void init(){}

	public void tapped(Tile tile){}
	public void buildTable(Tile tile, Table table) {}
	public void configure(Tile tile, byte data){}

	public void setConfigure(Tile tile, byte data){
		configure(tile, data);
		if(Net.active()) NetEvents.handleBlockConfig(tile, data);
	}

	public boolean isConfigurable(Tile tile){
		return false;
	}
	
	public void getStats(Array<String> list){
		list.add("[gray]size: " + width + "x" + height);
		list.add("[healthstats]health: " + health);
	}
	
	public String name(){
		return name;
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
		if(tile.entity == null) return;

		tile.entity.addItem(item, 1);
	}
	
	public boolean acceptItem(Item item, Tile tile, Tile source){
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

	public boolean syncEntity(){
		return true;
	}

	/**
	 * Tries to put this item into a nearby container, if there are no available
	 * containers, it gets added to the block's inventory.*/
	public void offloadNear(Tile tile, Item item){
		if(Net.client() && syncBlockState){
			handleItem(item, tile, tile);
			return;
		}

		byte i = tile.getDump();
		byte pdump = (byte)(i % 4);
		
		for(int j = 0; j < 4; j ++){
			Tile other = tile.getNearby(i);
			if(other != null && other.block().acceptItem(item, other, tile)){
				other.block().handleItem(item, other, tile);
				tile.setDump((byte)((i+1)%4));
				if(Net.server() && syncBlockState) NetEvents.handleTransfer(tile, i, item);
				return;
			}
			i++;
			i %= 4;
		}
		tile.setDump(pdump);
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
		if(Net.client() && syncBlockState) return false;

		int i = tile.getDump()%4;
		
		for(int j = 0; j < 4; j ++){
			Tile other = tile.getNearby(i);
			
			if(i == direction || direction == -1){
				for(Item item : Item.getAllItems()){
					
					if(todump != null && item != todump) continue;
					
					if(tile.entity.hasItem(item) && other != null && other.block().acceptItem(item, other, tile)){
						other.block().handleItem(item, other, tile);
						tile.entity.removeItem(item, 1);
						tile.setDump((byte)((i+1)%4));
						if(Net.server() && syncBlockState) NetEvents.handleTransfer(tile, (byte)i, item);
						return true;
					}
				}
			}
			i++;
			i %= 4;
		}

		return false;
	}

	/**
	 * Try offloading an item to a nearby container in its facing direction. Returns true if success.
	 */
	protected boolean offloadDir(Tile tile, Item item){
		Tile other = tile.getNearby(tile.getRotation());
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
			Draw.rect(name(), tile.drawx(), tile.drawy());
		}
		
		//update the tile entity through the draw method, only if it's an entity without updating
		if(destructible && !update && !state.is(State.paused)){
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
		return offset.set(((width + 1) % 2) * tilesize/2, ((height + 1) % 2) * tilesize/2);
	}
	
	public boolean isMultiblock(){
		return width != 1 || height != 1;
	}
	
	public static Array<Block> getAllBlocks(){
		return blocks;
	}

	public static Block getByName(String name){
		return map.get(name);
	}
	
	public static Block getByID(int id){
		return blocks.get(id);
	}

	public Array<Object> getDebugInfo(Tile tile){
		return Array.with(
				"block", tile.block().name,
				"floor", tile.floor().name,
				"x", tile.x,
				"y", tile.y,
				"entity.name", ClassReflection.getSimpleName(tile.entity.getClass()),
				"entity.x", tile.entity.x,
				"entity.y", tile.entity.y,
				"entity.id", tile.entity.id,
				"entity.items.total", tile.entity.totalItems()
		);
	}

	@Override
	public String toString(){
		return name;
	}
}
