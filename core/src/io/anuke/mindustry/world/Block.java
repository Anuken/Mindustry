package io.anuke.mindustry.world;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.reflect.ClassReflection;
import io.anuke.mindustry.core.GameState.State;
import io.anuke.mindustry.entities.TileEntity;
import io.anuke.mindustry.entities.Unit;
import io.anuke.mindustry.entities.effect.DamageArea;
import io.anuke.mindustry.entities.effect.Puddle;
import io.anuke.mindustry.entities.effect.Rubble;
import io.anuke.mindustry.graphics.DrawLayer;
import io.anuke.mindustry.graphics.Layer;
import io.anuke.mindustry.graphics.Palette;
import io.anuke.mindustry.net.Net;
import io.anuke.mindustry.net.NetEvents;
import io.anuke.mindustry.resource.Item;
import io.anuke.mindustry.resource.ItemStack;
import io.anuke.mindustry.resource.Liquid;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.graphics.Draw;
import io.anuke.ucore.graphics.Hue;
import io.anuke.ucore.graphics.Lines;
import io.anuke.ucore.scene.ui.layout.Table;
import io.anuke.ucore.util.Bundles;
import io.anuke.ucore.util.EnumSet;
import io.anuke.ucore.util.Mathf;

import static io.anuke.mindustry.Vars.*;

public class Block extends BaseBlock {
	private static int lastid;
	private static Array<Block> blocks = new Array<>();
	private static ObjectMap<String, Block> map = new ObjectMap<>();

	protected Array<Tile> tempTiles = new Array<>();
	protected Vector2 tempVector = new Vector2();
	protected Color tempColor = new Color();

	/**internal name*/
	public final String name;
	/**internal ID*/
	public final int id;
	/**display name*/
	public final String formalName;
	/**whether this block has a tile entity that updates*/
	public boolean update;
	/**whether this block has health and can be destroyed*/
	public boolean destructible;
	/**if true, this block cannot be broken by normal means.*/
	public boolean unbreakable;
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
	/**whether this floor can be placed on.*/
	public boolean placeableOn = true;
	/**time it takes to break*/
	public float breaktime = 18;
	/**tile entity health*/
	public int health = 40;
	/**base block explosiveness*/
	public float baseExplosiveness = 0f;
	/**the shadow drawn under the block. use 'null' to indicate the default shadow for this block.*/
	public String shadow = null;
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
	/**Draw layer. Only used for 'cached' rendering.*/
	public DrawLayer drawLayer = DrawLayer.normal;
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
	/**List of block flags. Used for AI indexing.*/
	public EnumSet<BlockFlag> flags;
	/**Whether to automatically set the entity to 'sleeping' when created.*/
	public boolean autoSleep;

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
	public void unitOn(Tile tile, Unit unit){}

	/**Called after all blocks are created.*/
	public void init(){
		setStats();
		setBars();
	}

	/**Called after texture atlas is loaded.*/
	public void load(){

	}

	public void tapped(Tile tile){}
	public void buildTable(Tile tile, Table table) {}
	public void configure(Tile tile, byte data){}

	public void setConfigure(Tile tile, byte data){
		configure(tile, data);
		if(Net.active()) NetEvents.handleBlockConfig(tile, data);
		configure(tile, data);
	}

	/**Called when another tile is tapped while this block is selected.
	 * Returns whether or not this block should be deselected.*/
	public boolean onConfigureTileTapped(Tile tile, Tile other){
		return true;
	}

	public void drawConfigure(Tile tile){
		Draw.color("accent");
		Lines.stroke(1f);
		Lines.square(tile.drawx(), tile.drawy(),
				tile.block().size * tilesize / 2f + 1f);
		Draw.reset();
	}

	public boolean isConfigurable(Tile tile){
		return false;
	}
	
	public void setStats(){
		stats.add("size", size);
		stats.add("health", health);

		if(hasPower) stats.add("powercapacity", powerCapacity);
		if(hasLiquids) stats.add("liquidcapacity", liquidCapacity);
		if(hasItems) stats.add("capacity", itemCapacity);
	}

	//TODO make this easier to config.
	public void setBars(){
		if(hasPower) bars.add(new BlockBar(BarType.power, true, tile -> tile.entity.power.amount / powerCapacity));
		if(hasLiquids) bars.add(new BlockBar(BarType.liquid, true, tile -> tile.entity.liquids.amount / liquidCapacity));
		if(hasItems) bars.add(new BlockBar(BarType.inventory, true, tile -> (float)tile.entity.items.totalItems() / itemCapacity));
	}
	
	public String name(){
		return name;
	}
	
	public boolean isSolidFor(Tile tile){
		return false;
	}
	
	public boolean canReplace(Block other){
		return (other != this || rotate) && this.group != BlockGroup.none && other.group == this.group;
	}
	
	public float handleDamage(Tile tile, float amount){
		return amount;
	}
	
	public void update(Tile tile){}

	public boolean isAccessible(){
		return (hasItems && itemCapacity > 0);
	}
	
	public void onDestroyed(Tile tile){
		float x = tile.worldx(), y = tile.worldy();
		float explosiveness = baseExplosiveness;
		float flammability = 0f;
		float heat = 0f;
		float power = 0f;
		int units = 1;
		tempColor.set(Palette.darkFlame);

		if(hasItems){
			for(Item item : Item.getAllItems()){
				int amount = tile.entity.items.getItem(item);
				explosiveness += item.explosiveness*amount;
				flammability += item.flammability*amount;

				if(item.flammability*amount > 0.5){
					units ++;
					Hue.addu(tempColor, item.flameColor);
				}
			}
		}

		if(hasLiquids){
			float amount = tile.entity.liquids.amount;
			explosiveness += tile.entity.liquids.liquid.explosiveness*amount/2f;
			flammability += tile.entity.liquids.liquid.flammability*amount/2f;
			heat += Mathf.clamp(tile.entity.liquids.liquid.temperature-0.5f)*amount/2f;

			if(tile.entity.liquids.liquid.flammability*amount > 2f){
				units ++;
				Hue.addu(tempColor, tile.entity.liquids.liquid.flameColor);
			}
		}

		if(hasPower){
			power += tile.entity.power.amount;
		}

		tempColor.mul(1f/units);

		if(hasLiquids) {

			Liquid liquid = tile.entity.liquids.liquid;
			float splash = Mathf.clamp(tile.entity.liquids.amount / 4f, 0f, 10f);

			for (int i = 0; i < Mathf.clamp(tile.entity.liquids.amount / 5, 0, 30); i++) {
				Timers.run(i / 2, () -> {
					Tile other = world.tile(tile.x + Mathf.range(size / 2), tile.y + Mathf.range(size / 2));
					if (other != null) {
						Puddle.deposit(other, liquid, splash);
					}
				});
			}
		}

		DamageArea.dynamicExplosion(x, y, flammability, explosiveness, power, tilesize * size/2f, tempColor);
		if(!tile.floor().solid && !tile.floor().liquid){
			Rubble.create(tile.drawx(), tile.drawy(), size);
		}
	}

	/**Returns the flammability of the tile. Used for fire calculations.
	 * Takes flammability of floor liquid into account.*/
	public float getFlammability(Tile tile){
		if(!hasItems || tile.entity == null){
			if(tile.floor().liquid && !solid){
				return tile.floor().liquidDrop.flammability;
			}
			return 0;
		}else{
			float result = 0f;
			for(Item item : Item.getAllItems()){
				int amount = tile.entity.items.getItem(item);
				result += item.flammability*amount;
			}
			if(hasLiquids){
				result += tile.entity.liquids.amount * tile.entity.liquids.liquid.flammability/3f;
			}
			return result;
		}
	}

	public TextureRegion[] getIcon(){
		if(Draw.hasRegion(name + "-icon")){
			return new TextureRegion[]{Draw.region(name + "-icon")};
		}else if(Draw.hasRegion(name + "1")){
			return new TextureRegion[]{Draw.region(name+ "1")};
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

    public boolean hasEntity(){
		return destructible || update;
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

	public void drawNonLayer(Tile tile){}
	
	public void drawShadow(Tile tile){
		
		if(varyShadow && variants > 0 && shadow != null) {
			Draw.rect(shadow + (Mathf.randomSeed(tile.id(), 1, variants)), tile.worldx(), tile.worldy());
		}else if(shadow != null){
			Draw.rect(shadow, tile.drawx(), tile.drawy());
		}else{
			Draw.rect("shadow-" + size, tile.drawx(), tile.drawy());
		}
	}
	
	/**Offset for placing and drawing multiblocks.*/
	public Vector2 getPlaceOffset(){
		return tempVector.set(((size + 1) % 2) * tilesize/2, ((size + 1) % 2) * tilesize/2);
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
	    if(id < 0){
	        id += 256;
        }
        if(id >= blocks.size || id < 0){
	    	throw new RuntimeException("No block with ID '" + id + "' found!");
		}
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
				"entity.items.total", hasItems ? tile.entity.items.totalItems() : null
		);
	}

	@Override
	public String toString(){
		return name;
	}
}
