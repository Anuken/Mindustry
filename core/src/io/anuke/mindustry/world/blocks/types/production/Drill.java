package io.anuke.mindustry.world.blocks.types.production;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Array;
import io.anuke.mindustry.content.Liquids;
import io.anuke.mindustry.entities.TileEntity;
import io.anuke.mindustry.graphics.Fx;
import io.anuke.mindustry.graphics.Layer;
import io.anuke.mindustry.resource.Item;
import io.anuke.mindustry.resource.Liquid;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.BlockGroup;
import io.anuke.mindustry.world.Tile;
import io.anuke.ucore.core.Effects;
import io.anuke.ucore.core.Effects.Effect;
import io.anuke.ucore.core.Graphics;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.graphics.Draw;
import io.anuke.ucore.util.Mathf;
import io.anuke.ucore.util.Strings;

public class Drill extends Block{
	protected final static float hardnessDrillMultiplier = 50f;
	protected final int timerDump = timers++;

	protected final Array<Tile> drawTiles = new Array<>();
	protected final Array<Item> toAdd = new Array<>();

	/**Maximum tier of blocks this drill can mine.*/
	protected int tier;
	/**Base time to drill one ore, in frames.*/
	protected float drillTime = 300;
	/**power use per frame.*/
	public float powerUse = 0.08f;
	/**liquid use per frame.*/
	protected float liquidUse = 0.05f;
	/**Input liquid. Set hasLiquids to true so this is used.*/
	protected Liquid inputLiquid = Liquids.water;
	/**Whether the liquid is required to drill. If false, then it will be used as a speed booster.*/
	protected boolean liquidRequired = false;
	/**How many times faster the drill will progress when booster by liquid.*/
	protected float liquidBoostIntensity = 1.3f;
	/**Speed at which the drill speeds up.*/
	protected float warmupSpeed = 0.02f;

	/**Effect played when an item is produced. This is colored.*/
	protected Effect drillEffect = Fx.mine;
	/**Speed the drill bit rotates at.*/
	protected float rotateSpeed = 2f;
	/**Effect randomly played while drilling.*/
	protected Effect updateEffect = Fx.pulverizeSmall;
	/**Chance the update effect will appear.*/
	protected float updateEffectChance = 0.02f;

	protected boolean drawRim = false;

	protected Color heatColor = Color.valueOf("ff5512");

	public Drill(String name) {
		super(name);
		update = true;
		solid = true;
		layer = Layer.overlay;
		itemCapacity = 5;
		group = BlockGroup.drills;
		hasLiquids = true;
		liquidCapacity = 5f;
	}

	@Override
	public void draw(Tile tile) {
		float s = 0.3f;
		float ts = 0.6f;

		DrillEntity entity = tile.entity();

		Draw.rect(name, tile.drawx(), tile.drawy());

		if(drawRim) {
			Graphics.setAdditiveBlending();
			Draw.color(heatColor);
			Draw.alpha(entity.warmup * ts * (1f-s + Mathf.absin(Timers.time(), 3f, s)));
			Draw.rect(name + "-rim", tile.drawx(), tile.drawy());
			Draw.color();
			Graphics.setNormalBlending();
		}

		Draw.rect(name + "-rotator", tile.drawx(), tile.drawy(), entity.drillTime * rotateSpeed);

		Draw.rect(name + "-top", tile.drawx(), tile.drawy());

		if(!isMultiblock() && isValid(tile)) {
			Draw.color(tile.floor().drops.item.color);
			Draw.rect("blank", tile.worldx(), tile.worldy(), 2f, 2f);
			Draw.color();
		}
	}

	@Override
	public TextureRegion[] getIcon() {
		return new TextureRegion[]{Draw.region(name), Draw.region(name + "-rotator"), Draw.region(name + "-top")};
	}
	
	@Override
	public void setStats(){
		super.setStats();
		//TODO this is misleading, change it
		stats.add("secondsitem", Strings.toFixed(drillTime/60, 1));
	}
	
	@Override
	public void update(Tile tile){
		toAdd.clear();

		DrillEntity entity = tile.entity();

		float multiplier = 0f;
		float totalHardness = 0f;

		for(Tile other : tile.getLinkedTiles(tempTiles)){
			if(isValid(other)){
				toAdd.add(other.floor().drops.item);
				totalHardness += other.floor().drops.item.hardness;
				multiplier += 1f;
			}
		}

		if(entity.timer.get(timerDump, 15)){
			tryDump(tile);
		}

		entity.drillTime += entity.warmup * Timers.delta();

		float powerUsed = Math.min(powerCapacity, powerUse * Timers.delta());
		float liquidUsed = Math.min(liquidCapacity, liquidUse * Timers.delta());

		if(entity.inventory.totalItems() < itemCapacity && toAdd.size > 0 &&
				(!hasPower || entity.power.amount >= powerUsed) &&
				(!liquidRequired || entity.liquid.amount >= liquidUsed)){

			if(hasPower) entity.power.amount -= powerUsed;
			if(liquidRequired) entity.liquid.amount -= liquidUsed;

			float speed = 1f;

			if(entity.liquid.amount >= liquidUsed && !liquidRequired){
				entity.liquid.amount -= liquidUsed;
				speed = liquidBoostIntensity;
			}

			entity.warmup = Mathf.lerpDelta(entity.warmup, speed, warmupSpeed);
			entity.progress += Timers.delta() * multiplier * speed;

			if(Mathf.chance(Timers.delta() * updateEffectChance))
				Effects.effect(updateEffect, entity.x + Mathf.range(size*2f), entity.y + Mathf.range(size*2f));
		}else{
			entity.warmup = Mathf.lerpDelta(entity.warmup, 0f, warmupSpeed);
			return;
		}

		if(toAdd.size > 0 && entity.progress >= drillTime + hardnessDrillMultiplier*totalHardness
				&& tile.entity.inventory.totalItems() < itemCapacity){

			int index = entity.index % toAdd.size;
			offloadNear(tile, toAdd.get(index));

			entity.index ++;
			entity.progress = 0f;

			Effects.effect(drillEffect, toAdd.get(index).color, tile.drawx(), tile.drawy());
		}
	}

	@Override
	public boolean isLayer(Tile tile){
		if(isMultiblock()){
			for(Tile other : tile.getLinkedTiles(drawTiles)){
				if(isValid(other)){
					return false;
				}
			}
			return true;
		}else{
			return !isValid(tile);
		}
	}
	
	@Override
	public void drawLayer(Tile tile){
		Draw.colorl(0.85f + Mathf.absin(Timers.time(), 6f, 0.15f));
		Draw.rect("cross-" + size, tile.drawx(), tile.drawy());
		Draw.color();
	}

	@Override
	public boolean acceptLiquid(Tile tile, Tile source, Liquid liquid, float amount) {
		return super.acceptLiquid(tile, source, liquid, amount) && liquid == inputLiquid;
	}

	@Override
	public TileEntity getEntity() {
		return new DrillEntity();
	}

	public static class DrillEntity extends TileEntity{
		public float progress;
		public int index;
		public float warmup;
		public float drillTime;
	}

	protected boolean isValid(Tile tile){
		return tile.floor().drops != null && tile.floor().drops.item.hardness <= tier;
	}

}
