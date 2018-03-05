package io.anuke.mindustry.world.blocks.types.production;

import com.badlogic.gdx.utils.Array;
import io.anuke.mindustry.entities.TileEntity;
import io.anuke.mindustry.graphics.Fx;
import io.anuke.mindustry.resource.Item;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Layer;
import io.anuke.mindustry.world.Tile;
import io.anuke.ucore.core.Effects;
import io.anuke.ucore.core.Effects.Effect;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.graphics.Draw;
import io.anuke.ucore.util.Mathf;

public class Drill extends Block{
	protected final int timerDrill = timers++;
	protected final int timerDump = timers++;

	protected final Array<Tile> drawTiles = new Array<>();
	
	protected Block resource;
	protected Item result;
	protected float time = 5;
	protected int capacity = 5;
	protected Effect drillEffect = Fx.spark;

	public Drill(String name) {
		super(name);
		update = true;
		solid = true;
		layer = Layer.overlay;
	}
	
	@Override
	public void getStats(Array<String> list){
		super.getStats(list);
		list.add("[iteminfo]Capacity: " + capacity);
		list.add("[iteminfo]Seconds/item: " + time);
	}
	
	@Override
	public void update(Tile tile){
		TileEntity entity = tile.entity;

		int mines = 0;

		if(isMultiblock()){
			for(Tile other : tile.getLinkedTiles(tempTiles)){
				if(isValid(other)){
					mines ++;
				}
			}
		}else{
			if(isValid(tile)) mines = 1;
		}

		if(mines > 0 && entity.timer.get(timerDrill, 60 * time) && tile.entity.getItem(result) < capacity){
			for(int i = 0; i < mines; i ++) offloadNear(tile, result);
			Effects.effect(drillEffect, tile.drawx(), tile.drawy());
		}

		if(entity.timer.get(timerDump, 30)){
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

	boolean isValid(Tile tile){
		return tile.floor() == resource || (resource != null && resource.drops.equals(tile.floor().drops));
	}

}
