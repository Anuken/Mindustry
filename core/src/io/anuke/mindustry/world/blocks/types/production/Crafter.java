package io.anuke.mindustry.world.blocks.types.production;

import java.util.Arrays;
import java.util.Map;

import com.badlogic.gdx.utils.Array;

import io.anuke.mindustry.entities.effect.Fx;
import io.anuke.mindustry.resource.Item;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Tile;
import io.anuke.ucore.core.Effects;

public class Crafter extends Block{
	protected final int timerDump = timers++;

	protected Map<Item,Item[]> recipe;

	public Crafter(String name) {
		super(name);
		update = true;
		solid = true;
	}
	
	@Override
	public void getStats(Array<String> list){
		super.getStats(list);

		for(Item item : recipe.keySet()) {
		    list.add("[craftinfo]Input: " + Arrays.toString(recipe.get(item)));
		    list.add("[craftinfo]Output: " + item);
		}

	}
	
	@Override
	public void update(Tile tile){
	    boolean recipeFilled;
		for(Item result : recipe.keySet()) {
		    recipeFilled = true;
            if (tile.entity.timer.get(timerDump, 20) && tile.entity.hasItem(result)) {
                tryDump(tile, -1, result);
            }

            for (Item item : recipe.get(result)) {
                if (!tile.entity.hasItem(item)) {
                    recipeFilled = false;
                    break;
                }
            }
            if(recipeFilled) {
                for (Item item : recipe.get(result)) {
                    tile.entity.removeItem(item, 1);
                }

                offloadNear(tile, result);
                Effects.effect(Fx.smelt, tile.entity);
            }
        }
	}

	@Override
	public boolean acceptItem(Item item, Tile dest, Tile source){
		boolean craft = false;
		for(Item key : recipe.keySet()) {
            for (Item req : recipe.get(key)) {
                if (item == req) {
                    craft = true;
                    break;
                }
            }
        }
		return craft;
	}
}
