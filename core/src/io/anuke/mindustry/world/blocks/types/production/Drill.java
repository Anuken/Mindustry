package io.anuke.mindustry.world.blocks.types.production;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Array;
import io.anuke.mindustry.entities.TileEntity;
import io.anuke.mindustry.graphics.Fx;
import io.anuke.mindustry.graphics.Layer;
import io.anuke.mindustry.resource.Item;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.BlockGroup;
import io.anuke.mindustry.world.Tile;
import io.anuke.ucore.core.Effects;
import io.anuke.ucore.core.Effects.Effect;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.graphics.Draw;
import io.anuke.ucore.util.Mathf;
import io.anuke.ucore.util.Strings;

public class Drill extends Block{
	protected final static float hardnessDrillMultiplier = 40f;
	protected final int timerDrill = timers++;
	protected final int timerDump = timers++;

	protected final Array<Tile> drawTiles = new Array<>();
	
	protected int tier;
	protected float drillTime = 300;
	protected Effect drillEffect = Fx.mine;
	protected float rotateSpeed = 2f;
	protected Effect updateEffect = Fx.pulverizeSmall;
	protected float updateEffectChance = 0.02f;

	protected Array<Item> toAdd = new Array<>();

	public Drill(String name) {
		super(name);
		update = true;
		solid = true;
		layer = Layer.overlay;
		itemCapacity = 5;
		group = BlockGroup.drills;
	}

	@Override
	public void draw(Tile tile) {
		boolean valid = isMultiblock() || isValid(tile);

		Draw.rect(name, tile.drawx(), tile.drawy());
		Draw.rect(name + "-rotator", tile.drawx(), tile.drawy(), valid ? Timers.time() * rotateSpeed : 0f);
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
		stats.add("secondsitem", Strings.toFixed(drillTime/60, 1));
	}
	
	@Override
	public void update(Tile tile){
		toAdd.clear();

		TileEntity entity = tile.entity;

		float multiplier = 0f;
		float totalHardness = 0f;

		for(Tile other : tile.getLinkedTiles(tempTiles)){
			if(isValid(other)){
				toAdd.add(other.floor().drops.item);
				totalHardness += other.floor().drops.item.hardness;
				multiplier += 1f;
			}
		}

		if(toAdd.size > 0 && tile.entity.inventory.totalItems() < itemCapacity){

			if(entity.timer.get(timerDrill, drillTime/multiplier + totalHardness*hardnessDrillMultiplier)) {
				int extra = tile.getExtra() % toAdd.size;

				offloadNear(tile, toAdd.get(extra));

				tile.setExtra((byte)((extra + 1) % toAdd.size));
				Effects.effect(drillEffect, toAdd.get(extra).color, tile.drawx(), tile.drawy());
			}

			if(Mathf.chance(Timers.delta() * updateEffectChance))
				Effects.effect(updateEffect, entity.x + Mathf.range(size*2f), entity.y + Mathf.range(size*2f));
		}

		if(entity.timer.get(timerDump, 15)){
			tryDump(tile);
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

	protected boolean isValid(Tile tile){
		return tile.floor().drops != null && tile.floor().drops.item.hardness <= tier;
	}

}
