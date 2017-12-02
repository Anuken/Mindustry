package io.anuke.mindustry.world.blocks.types.defense;

import static io.anuke.mindustry.Vars.*;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.ObjectMap;

import io.anuke.mindustry.Vars;
import io.anuke.mindustry.entities.effect.Fx;
import io.anuke.mindustry.entities.enemies.Enemy;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.types.Configurable;
import io.anuke.mindustry.world.blocks.types.Wall;
import io.anuke.ucore.core.Draw;
import io.anuke.ucore.core.Effects;
import io.anuke.ucore.core.Effects.Effect;
import io.anuke.ucore.entities.Entities;
import io.anuke.ucore.entities.SolidEntity;
import io.anuke.ucore.scene.ui.layout.Table;
import io.anuke.ucore.util.Tmp;

public class Door extends Wall implements Configurable{
	private ObjectMap<Tile, Boolean> open = new ObjectMap<>();
	
	protected Effect openfx = Fx.dooropen;
	protected Effect closefx = Fx.doorclose;

	public Door(String name) {
		super(name);
		solid = false;
	}
	
	@Override
	public void draw(Tile tile){
		if(open.get(tile, true)){
			Draw.rect(name, tile.worldx(), tile.worldy());
		}else{
			Draw.rect(name + "-open", tile.worldx(), tile.worldy());
		}
	}
	
	@Override
	public boolean isSolidFor(Tile tile){
		return open.get(tile, true);
	}

	@Override
	public void buildTable(Tile tile, Table table){
		if(anyEntities(tile) && !open.get(tile, true)){
			return;
		}
		
		open.put(tile, !open.get(tile, true));
		if(open.get(tile)){
			Effects.effect(closefx, tile.worldx(), tile.worldy());
		}else{
			Effects.effect(openfx, tile.worldx(), tile.worldy());
		}
	}
	
	boolean anyEntities(Tile tile){
		int x = tile.x, y = tile.y;
		Block type = tile.block();
		Tmp.r2.setSize(type.width * Vars.tilesize, type.height * Vars.tilesize);
		Vector2 offset = type.getPlaceOffset();
		Tmp.r2.setCenter(offset.x + x * Vars.tilesize, offset.y + y * Vars.tilesize);
		
		for(SolidEntity e : Entities.getNearby(Entities.getGroup(Enemy.class), x * tilesize, y * tilesize, tilesize * 2f)){
			Rectangle rect = e.hitbox.getRect(e.x, e.y);

			if(Tmp.r2.overlaps(rect)){
				return true;
			}
		}
		
		if(Tmp.r2.overlaps(player.hitbox.getRect(player.x, player.y))){
			return true;
		}
		
		return false;
	}

}
