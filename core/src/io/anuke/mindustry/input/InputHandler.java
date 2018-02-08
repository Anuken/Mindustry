package io.anuke.mindustry.input;

import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import io.anuke.mindustry.entities.Player;
import io.anuke.mindustry.game.SpawnPoint;
import io.anuke.mindustry.graphics.Fx;
import io.anuke.mindustry.net.Net;
import io.anuke.mindustry.net.NetEvents;
import io.anuke.mindustry.resource.ItemStack;
import io.anuke.mindustry.resource.Recipe;
import io.anuke.mindustry.resource.Recipes;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.Blocks;
import io.anuke.mindustry.world.blocks.ProductionBlocks;
import io.anuke.ucore.core.Effects;
import io.anuke.ucore.core.Graphics;
import io.anuke.ucore.core.Sounds;
import io.anuke.ucore.entities.Entities;
import io.anuke.ucore.entities.SolidEntity;
import io.anuke.ucore.util.Mathf;

import static io.anuke.mindustry.Vars.*;

public abstract class InputHandler extends InputAdapter{
	public float breaktime = 0;
	public Recipe recipe;
	public int rotation;
	public PlaceMode placeMode = android ? PlaceMode.cursor : PlaceMode.hold;
	public PlaceMode breakMode = android ? PlaceMode.none : PlaceMode.holdDelete;
	public PlaceMode lastPlaceMode = placeMode;
	public PlaceMode lastBreakMode = breakMode;

	private Rectangle rect = new Rectangle();

	public abstract void update();
	public abstract float getCursorX();
	public abstract float getCursorY();
	public abstract float getCursorEndX();
	public abstract float getCursorEndY();
	public int getBlockX(){ return Mathf.sclb(Graphics.world(getCursorX(), getCursorY()).x, tilesize, round2()); }
	public int getBlockY(){ return Mathf.sclb(Graphics.world(getCursorX(), getCursorY()).y, tilesize, round2()); }
	public int getBlockEndX(){ return Mathf.sclb(Graphics.world(getCursorEndX(), getCursorEndY()).x, tilesize, round2()); }
	public int getBlockEndY(){ return Mathf.sclb(Graphics.world(getCursorEndX(), getCursorEndY()).y, tilesize, round2()); }
	public void resetCursor(){}
	public boolean drawPlace(){ return true; }
	
	public boolean onConfigurable(){
		Tile tile = world.tile(getBlockX(), getBlockY());
		return tile != null && (tile.block().isConfigurable(tile) || (tile.isLinked() && tile.getLinked().block().isConfigurable(tile)));
	}
	
	public boolean cursorNear(){
		return Vector2.dst(player.x, player.y, getBlockX() * tilesize, getBlockY() * tilesize) <= placerange;
	}
	
	public boolean tryPlaceBlock(int x, int y, boolean sound){
		if(recipe != null && 
				validPlace(x, y, recipe.result) && !ui.hasMouse() && cursorNear() &&
				state.inventory.hasItems(recipe.requirements)){
			
			placeBlock(x, y, recipe.result, rotation, true, sound);
			
			for(ItemStack stack : recipe.requirements){
				state.inventory.removeItem(stack);
			}

			return true;
		}
		return false;
	}
	
	public boolean tryDeleteBlock(int x, int y, boolean sound){
		if(cursorNear() && validBreak(x, y)){
			breakBlock(x, y, sound);
			return true;
		}
		return false;
	}
	
	public boolean round2(){
		return !(recipe != null && recipe.result.isMultiblock() && recipe.result.height % 2 == 0);
	}
	
	public boolean validPlace(int x, int y, Block type){
		
		for(SpawnPoint spawn : world.getSpawns()){
			if(Vector2.dst(x * tilesize, y * tilesize, spawn.start.worldx(), spawn.start.worldy()) < enemyspawnspace){
				return false;
			}
		}
		
		rect.setSize(type.width * tilesize, type.height * tilesize);
		Vector2 offset = type.getPlaceOffset();
		rect.setCenter(offset.x + x * tilesize, offset.y + y * tilesize);

		for(SolidEntity e : Entities.getNearby(enemyGroup, x * tilesize, y * tilesize, tilesize * 2f)){
			Rectangle rect = e.hitbox.getRect(e.x, e.y);

			if(this.rect.overlaps(rect)){
				return false;
			}
		}

		if(type.solid || type.solidifes) {
			for (Player player : playerGroup.all()) {
				if (!player.isAndroid && rect.overlaps(player.hitbox.getRect(player.x, player.y))) {
					return false;
				}
			}
		}
		
		Tile tile = world.tile(x, y);
		
		if(tile == null || (isSpawnPoint(tile) && (type.solidifes || type.solid))) return false;
		
		if(!type.isMultiblock() && control.tutorial().active() &&
				control.tutorial().showBlock()){
			
			GridPoint2 point = control.tutorial().getPlacePoint();
			int rotation = control.tutorial().getPlaceRotation();
			Block block = control.tutorial().getPlaceBlock();
			
			if(type != block || point.x != x - world.getCore().x || point.y != y - world.getCore().y
					|| (rotation != -1 && rotation != this.rotation)){
				return false;
			}
		}else if(control.tutorial().active()){
			return false;
		}
		
		if(type.isMultiblock()){
			int offsetx = -(type.width-1)/2;
			int offsety = -(type.height-1)/2;
			for(int dx = 0; dx < type.width; dx ++){
				for(int dy = 0; dy < type.height; dy ++){
					Tile other = world.tile(x + dx + offsetx, y + dy + offsety);
					if(other == null || (other.block() != Blocks.air && !other.block().alwaysReplace) || isSpawnPoint(other)){
						return false;
					}
				}
			}
			return true;
		}else{
			if(tile.block() != type && (type.canReplace(tile.block()) || tile.block().alwaysReplace) && tile.block().isMultiblock() == type.isMultiblock()){
				return true;
			}
			return tile.block() == Blocks.air;
		}
	}

	public boolean isSpawnPoint(Tile tile){
		return tile != null && tile.x == world.getCore().x && tile.y == world.getCore().y - 2;
	}
	
	public boolean validBreak(int x, int y){
		Tile tile = world.tile(x, y);
		
		if(tile == null || tile.block() == ProductionBlocks.core) return false;
		
		if(tile.isLinked() && tile.getLinked().block() == ProductionBlocks.core){
			return false;
		}
		
		if(control.tutorial().active()){
			
			if(control.tutorial().showBlock()){
				GridPoint2 point = control.tutorial().getPlacePoint();
				int rotation = control.tutorial().getPlaceRotation();
				Block block = control.tutorial().getPlaceBlock();
			
				if(block != Blocks.air || point.x != x - world.getCore().x || point.y != y - world.getCore().y
						|| (rotation != -1 && rotation != this.rotation)){
					return false;
				}
			}else{
				return false;
			}
		}
		
		return tile.breakable();
	}
	
	public void placeBlock(int x, int y, Block result, int rotation, boolean effects, boolean sound){

		placeBlockInternal(x, y, result, rotation, effects, sound);

		if(Net.active() && result != ProductionBlocks.core){
			NetEvents.handlePlace(x, y, result, rotation);
		}
	}

	public void placeBlockInternal(int x, int y, Block result, int rotation, boolean effects, boolean sound){
		Tile tile = world.tile(x, y);

		//just in case
		if(tile == null)
			return;

		tile.setBlock(result, rotation);

		if(result.isMultiblock()){
			int offsetx = -(result.width-1)/2;
			int offsety = -(result.height-1)/2;

			for(int dx = 0; dx < result.width; dx ++){
				for(int dy = 0; dy < result.height; dy ++){
					int worldx = dx + offsetx + x;
					int worldy = dy + offsety + y;
					if(!(worldx == x && worldy == y)){
						Tile toplace = world.tile(worldx, worldy);
						if(toplace != null)
							toplace.setLinked((byte)(dx + offsetx), (byte)(dy + offsety));
					}

					if(effects) Effects.effect(Fx.place, worldx * tilesize, worldy * tilesize);
				}
			}
		}else{
			if(effects) Effects.effect(Fx.place, x * tilesize, y * tilesize);
		}

		if(effects && sound) Sounds.play("place");

		result.placed(tile);
	}
	
	public void breakBlock(int x, int y, boolean sound){
		breakBlockInternal(x, y, sound);

		if(Net.active()){
			NetEvents.handleBreak(x, y);
		}
	}

	public void breakBlockInternal(int x, int y, boolean sound){
		Tile tile = world.tile(x, y);

		if(tile == null) return;

		Block block = tile.isLinked() ? tile.getLinked().block() : tile.block();
		Recipe result = null;

		for(Recipe recipe : Recipes.all()){
			if(recipe.result == block){
				result = recipe;
				break;
			}
		}

		if(result != null){
			for(ItemStack stack : result.requirements){
				state.inventory.addItem(stack.item, (int)(stack.amount * breakDropAmount));
			}
		}

		if(tile.block().drops != null){
			state.inventory.addItem(tile.block().drops.item, tile.block().drops.amount);
		}

		//Effects.shake(3f, 1f, player);
		if(sound) Sounds.play("break");

		if(!tile.block().isMultiblock() && !tile.isLinked()){
			tile.setBlock(Blocks.air);
			Effects.effect(Fx.breakBlock, tile.worldx(), tile.worldy());
		}else{
			Tile target = tile.isLinked() ? tile.getLinked() : tile;
			Array<Tile> removals = target.getLinkedTiles();
			for(Tile toremove : removals){
				//note that setting a new block automatically unlinks it
				toremove.setBlock(Blocks.air);
				Effects.effect(Fx.breakBlock, toremove.worldx(), toremove.worldy());
			}
		}
	}
}
