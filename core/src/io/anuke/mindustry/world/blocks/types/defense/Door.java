package io.anuke.mindustry.world.blocks.types.defense;

import static io.anuke.mindustry.Vars.*;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;

import io.anuke.mindustry.Vars;
import io.anuke.mindustry.entities.TileEntity;
import io.anuke.mindustry.entities.effect.Fx;
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
	protected Effect openfx = Fx.dooropen;
	protected Effect closefx = Fx.doorclose;

	public Door(String name) {
		super(name);
		solid = false;
	}
	
	@Override
	public void draw(Tile tile){
		DoorEntity entity = tile.entity();
		
		Vector2 offset = getPlaceOffset();
		
		if(!entity.open){
			Draw.rect(name, tile.worldx() + offset.x, tile.worldy() + offset.y);
		}else{
			Draw.rect(name + "-open", tile.worldx() + offset.x, tile.worldy() + offset.y);
		}
	}
	
	@Override
	public boolean isSolidFor(Tile tile){
		DoorEntity entity = tile.entity();
		return !entity.open;
	}

	@Override
	public void buildTable(Tile tile, Table table){
		DoorEntity entity = tile.entity();
		
		if(anyEntities(tile) && entity.open){
			return;
		}
		
		Vector2 offset = getPlaceOffset();
		
		entity.open = !entity.open;
		if(!entity.open){
			Effects.effect(closefx, tile.worldx() + offset.x, tile.worldy() + offset.y);
		}else{
			Effects.effect(openfx, tile.worldx() + offset.x, tile.worldy() + offset.y);
		}
	}
	
	boolean anyEntities(Tile tile){
		int x = tile.x, y = tile.y;
		Block type = tile.block();
		Tmp.r2.setSize(type.width * Vars.tilesize, type.height * Vars.tilesize);
		Vector2 offset = type.getPlaceOffset();
		Tmp.r2.setCenter(offset.x + x * Vars.tilesize, offset.y + y * Vars.tilesize);
		
		for(SolidEntity e : Entities.getNearby(Vars.control.enemyGroup, x * tilesize, y * tilesize, tilesize * 2f)){
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
	
	@Override
	public TileEntity getEntity(){
		return new DoorEntity();
	}
	
	public class DoorEntity extends TileEntity{
		public boolean open = false;
		
		@Override
		public void write(DataOutputStream stream) throws IOException{
			super.write(stream);
			stream.writeBoolean(open);
		}
		
		@Override
		public void read(DataInputStream stream) throws IOException{
			super.read(stream);
			open = stream.readBoolean();
		}
	}

}
