package io.anuke.mindustry.input;

import static io.anuke.mindustry.Vars.*;

import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.math.Vector2;

import io.anuke.mindustry.Vars;
import io.anuke.mindustry.resource.ItemStack;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.types.Configurable;
import io.anuke.ucore.core.Graphics;
import io.anuke.ucore.scene.utils.Cursors;
import io.anuke.ucore.util.Mathf;

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
	
	public boolean onConfigurable(){
		Tile tile = Vars.world.tile(getBlockX(), getBlockY());
		return tile != null && (tile.block() instanceof Configurable || (tile.isLinked() && tile.getLinked().block() instanceof Configurable));
	}
	
	public boolean cursorNear(){
		return Vector2.dst(player.x, player.y, getBlockX() * tilesize, getBlockY() * tilesize) <= placerange;
	}
	
	public void tryPlaceBlock(int x, int y){
		if(player.recipe != null && 
				Vars.world.validPlace(x, y, player.recipe.result) && !ui.hasMouse() && cursorNear() &&
				Vars.control.hasItems(player.recipe.requirements)){
			
			Vars.world.placeBlock(x, y, player.recipe.result, player.rotation, true);
			
			for(ItemStack stack : player.recipe.requirements){
				Vars.control.removeItem(stack);
			}
			
			if(!Vars.control.hasItems(player.recipe.requirements)){
				Cursors.restoreCursor();
			}
		}
	}
	
	public boolean round2(){
		return !(player.recipe != null && player.recipe.result.isMultiblock() && player.recipe.result.height % 2 == 0);
	}
}
