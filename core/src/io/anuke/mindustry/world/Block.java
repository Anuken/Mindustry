package io.anuke.mindustry.world;

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
import io.anuke.mindustry.resource.ItemStack;
import io.anuke.mindustry.resource.Liquid;
import io.anuke.mindustry.world.blocks.BaseBlock;
import io.anuke.ucore.core.Effects;
import io.anuke.ucore.core.Effects.Effect;
import io.anuke.ucore.graphics.Draw;
import io.anuke.ucore.scene.ui.layout.Table;
import io.anuke.ucore.util.Bundles;
import io.anuke.ucore.util.Mathf;

import static io.anuke.mindustry.Vars.state;
import static io.anuke.mindustry.Vars.tilesize;

public class Block extends BaseBlock {
	private static int lastid;
	private static Array<Block> blocks = new Array<>();
	private static ObjectMap<String, Block> map = new ObjectMap<>();

	protected Array<Tile> tempTiles = new Array<>();
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
	/**multiblock size*/
	public int size = 1;
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
	/**whether this block can be replaced in all cases*/
	public boolean alwaysReplace = false;
	/**whether this block has instant transfer checking. used for calculations to prevent infinite loops.*/
	public boolean instantTransfer = false;
	/**The block group. Unless {@link #canReplace} is overriden, blocks in the same group can replace each other.*/
	public BlockGroup group = BlockGroup.none;
	/**list of displayed block status bars. Defaults to health bar.*/
	public BlockBars bars = new BlockBars();
	/**List of block stats.*/
	public BlockStats stats = new BlockStats();

	public Block(String name) {
		this.name = name;
		this.formalName = Bundles.get("block." + name + ".name", name);
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
	/**Called after all blocks are created.*/
	public void init(){
		setStats();
		setBars();
	}

	public void tapped(Tile tile){}
	public void buildTable(Tile tile, Table table) {}
	public void configure(Tile tile, byte data){}

	public void setConfigure(Tile tile, byte data){
		if(Net.active()) NetEvents.handleBlockConfig(tile, data);
		configure(tile, data);
	}

	public boolean isConfigurable(Tile tile){
		return false;
	}
	
	public void setStats(){
		stats.add("size", size);
		stats.add("health", health);

		if(hasPower) stats.add("powercapacity", powerCapacity);
		if(hasLiquids) stats.add("liquidcapacity", liquidCapacity);
		if(hasInventory) stats.add("capacity", itemCapacity);
	}

	//TODO make this easier to config.
	public void setBars(){
		if(hasPower) bars.add(new BlockBar(BarType.power, true, tile -> tile.entity.power.amount / powerCapacity));
		if(hasLiquids) bars.add(new BlockBar(BarType.liquid, true, tile -> tile.entity.liquid.amount / liquidCapacity));
		if(hasInventory) bars.add(new BlockBar(BarType.inventory, true, tile -> (float)tile.entity.inventory.totalItems() / itemCapacity));
	}
	
	public String name(){
		return name;
	}
	
	public boolean isSolidFor(Tile tile){
		return false;
	}
	
	public boolean canReplace(Block other){
		return other != this && this.group != BlockGroup.none && other.group == this.group;
	}
	
	public int handleDamage(Tile tile, int amount){
		return amount;
	}
	
	public void update(Tile tile){}
	
	public void onDestroyed(Tile tile){
		float x = tile.worldx(), y = tile.worldy();
		
		Effects.shake(4f, 4f, x, y);
		Effects.effect(explosionEffect, x, y);
		Effects.sound(explosionSound, x, y);
	}

	public TextureRegion[] getIcon(){
		if(Draw.hasRegion(name + "-icon")){
			return new TextureRegion[]{Draw.region(name + "-icon")};
		}else{
			return new TextureRegion[]{Draw.region(name)};
		}
	}

	public TextureRegion[] getCompactIcon(){
		TextureRegion[] out = getIcon();
		for(int i = 0; i < out.length; i ++){
			out[i] = iconRegion(out[i]);
		}
		return out;
	}

	protected TextureRegion iconRegion(TextureRegion src){
        TextureRegion region = new TextureRegion(src);
        region.setRegionWidth(8);
        region.setRegionHeight(8);
        return region;
    }
	
	public TileEntity getEntity(){
		return new TileEntity();
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
		return offset.set(((size + 1) % 2) * tilesize/2, ((size + 1) % 2) * tilesize/2);
	}
	
	public boolean isMultiblock(){
		return size > 1;
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
				"entity.items.total", hasInventory ? tile.entity.inventory.totalItems() : null
		);
	}

	@Override
	public String toString(){
		return name;
	}
}
