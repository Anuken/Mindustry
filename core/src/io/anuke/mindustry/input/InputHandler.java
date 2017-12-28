package io.anuke.mindustry.input;

import static io.anuke.mindustry.Vars.*;

import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;

import io.anuke.mindustry.Vars;
import io.anuke.mindustry.graphics.Fx;
import io.anuke.mindustry.resource.ItemStack;
import io.anuke.mindustry.resource.Recipe;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.SpawnPoint;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.Blocks;
import io.anuke.mindustry.world.blocks.ProductionBlocks;
import io.anuke.mindustry.world.blocks.types.Configurable;
import io.anuke.ucore.core.Effects;
import io.anuke.ucore.core.Graphics;
import io.anuke.ucore.core.Sounds;
import io.anuke.ucore.entities.Entities;
import io.anuke.ucore.entities.SolidEntity;
import io.anuke.ucore.scene.utils.Cursors;
import io.anuke.ucore.util.Mathf;
import io.anuke.ucore.util.Tmp;

public abstract class InputHandler extends InputAdapter{
	public abstract void update();
	public abstract float getCursorX();
	public abstract float getCursorY();
	public abstract float getCursorEndX();
	public abstract float getCursorEndY();
	public int getBlockX(){ return Mathf.sclb(Graphics.world(getCursorX(), getCursorY()).x, Vars.tilesize, round2()); }
	public int getBlockY(){ return Mathf.sclb(Graphics.world(getCursorX(), getCursorY()).y, Vars.tilesize, round2()); }
	public int getBlockEndX(){ return Mathf.sclb(Graphics.world(getCursorEndX(), getCursorEndY()).x, Vars.tilesize, round2()); }
	public int getBlockEndY(){ return Mathf.sclb(Graphics.world(getCursorEndX(), getCursorEndY()).y, Vars.tilesize, round2()); }
	public void resetCursor(){}
	public boolean drawPlace(){ return true; }
	
	public boolean onConfigurable(){
		Tile tile = Vars.world.tile(getBlockX(), getBlockY());
		return tile != null && (tile.block() instanceof Configurable || (tile.isLinked() && tile.getLinked().block() instanceof Configurable));
	}
	
	public boolean cursorNear(){
		return Vector2.dst(player.x, player.y, getBlockX() * tilesize, getBlockY() * tilesize) <= placerange;
	}
	
	public boolean tryPlaceBlock(int x, int y, boolean sound){
		if(player.recipe != null && 
				validPlace(x, y, player.recipe.result) && !ui.hasMouse() && cursorNear() &&
				Vars.control.hasItems(player.recipe.requirements)){
			
			placeBlock(x, y, player.recipe.result, player.rotation, true, sound);
			
			for(ItemStack stack : player.recipe.requirements){
				Vars.control.removeItem(stack);
			}
			
			if(!Vars.control.hasItems(player.recipe.requirements)){
				Cursors.restoreCursor();
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
		return !(player.recipe != null && player.recipe.result.isMultiblock() && player.recipe.result.height % 2 == 0);
	}
	
	public boolean validPlace(int x, int y, Block type){
		
		for(SpawnPoint spawn : control.getSpawnPoints()){
			if(Vector2.dst(x * tilesize, y * tilesize, spawn.start.worldx(), spawn.start.worldy()) < enemyspawnspace){
				return false;
			}
		}
		
		Tmp.r2.setSize(type.width * Vars.tilesize, type.height * Vars.tilesize);
		Vector2 offset = type.getPlaceOffset();
		Tmp.r2.setCenter(offset.x + x * Vars.tilesize, offset.y + y * Vars.tilesize);

		for(SolidEntity e : Entities.getNearby(control.enemyGroup, x * tilesize, y * tilesize, tilesize * 2f)){
			Rectangle rect = e.hitbox.getRect(e.x, e.y);

			if(Tmp.r2.overlaps(rect)){
				return false;
			}
		}
		
		if(!Vars.android && Tmp.r2.overlaps(player.hitbox.getRect(player.x, player.y))){
			return false;
		}
		
		Tile tile = world.tile(x, y);
		
		if(tile == null) return false;
		
		if(!type.isMultiblock() && Vars.control.getTutorial().active() &&
				Vars.control.getTutorial().showBlock()){
			
			GridPoint2 point = Vars.control.getTutorial().getPlacePoint();
			int rotation = Vars.control.getTutorial().getPlaceRotation();
			Block block = Vars.control.getTutorial().getPlaceBlock();
			
			if(type != block || point.x != x - control.getCore().x || point.y != y - control.getCore().y 
					|| (rotation != -1 && rotation != Vars.player.rotation)){
				return false;
			}
		}else if(Vars.control.getTutorial().active()){
			return false;
		}
		
		if(type.isMultiblock()){
			int offsetx = -(type.width-1)/2;
			int offsety = -(type.height-1)/2;
			for(int dx = 0; dx < type.width; dx ++){
				for(int dy = 0; dy < type.height; dy ++){
					Tile other = world.tile(x + dx + offsetx, y + dy + offsety);
					if(other == null || other.block() != Blocks.air){
						return false;
					}
				}
			}
			return true;
		}else{
			if(tile.block() != type && type.canReplace(tile.block()) && tile.block().isMultiblock() == type.isMultiblock()){
				return true;
			}
			return tile != null && tile.block() == Blocks.air;
		}
	}
	
	public boolean validBreak(int x, int y){
		Tile tile = world.tile(x, y);
		
		if(tile == null || tile.block() == ProductionBlocks.core) return false;
		
		if(tile.isLinked() && tile.getLinked().block() == ProductionBlocks.core){
			return false;
		}
		
		if(Vars.control.getTutorial().active()){
			
			if(Vars.control.getTutorial().showBlock()){
				GridPoint2 point = Vars.control.getTutorial().getPlacePoint();
				int rotation = Vars.control.getTutorial().getPlaceRotation();
				Block block = Vars.control.getTutorial().getPlaceBlock();
			
				if(block != Blocks.air || point.x != x - control.getCore().x || point.y != y - control.getCore().y 
						|| (rotation != -1 && rotation != Vars.player.rotation)){
					return false;
				}
			}else{
				return false;
			}
		}
		
		return tile.breakable();
	}
	
	public void placeBlock(int x, int y, Block result, int rotation, boolean effects, boolean sound){
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
					
					if(effects) Effects.effect(Fx.place, worldx * Vars.tilesize, worldy * Vars.tilesize);
				}
			}
		}else{
			if(effects) Effects.effect(Fx.place, x * Vars.tilesize, y * Vars.tilesize);
		}
		
		if(effects && sound) Sounds.play("place");

		result.placed(tile);
	}
	
	public void breakBlock(int x, int y, boolean sound){
		Tile tile = world.tile(x, y);
		
		if(tile == null) return;
		
		Block block = tile.isLinked() ? tile.getLinked().block() : tile.block();
		Recipe result = null;
		
		for(Recipe recipe : Recipe.values()){
			if(recipe.result == block){
				result = recipe;
				break;
			}
		}
		
		if(result != null){
			for(ItemStack stack : result.requirements){
				Vars.control.addItem(stack.item, (int)(stack.amount * Vars.breakDropAmount));
			}
		}
		
		if(tile.block().drops != null){
			Vars.control.addItem(tile.block().drops.item, tile.block().drops.amount);
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
