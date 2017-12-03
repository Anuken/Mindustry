package io.anuke.mindustry.input;

import static io.anuke.mindustry.Vars.*;

import com.badlogic.gdx.Input.Buttons;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.math.Vector2;

import io.anuke.mindustry.Vars;
import io.anuke.mindustry.core.GameState;
import io.anuke.mindustry.core.GameState.State;
import io.anuke.mindustry.resource.ItemStack;
import io.anuke.mindustry.resource.Weapon;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.types.Configurable;
import io.anuke.ucore.core.Graphics;
import io.anuke.ucore.core.Inputs;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.scene.utils.Cursors;
import io.anuke.ucore.util.Mathf;

public class Input{
	
	public static void doInput(){
		//player is dead
		if(player.health <= 0) return;
		
		if(Inputs.scrolled() && !GameState.is(State.menu) && !Vars.ui.onDialog()){
			Vars.renderer.scaleCamera(Inputs.scroll());
		}

		if(Inputs.keyUp("rotate"))
			player.rotation ++;
		
		if(Inputs.keyUp("rotate_back"))
			player.rotation --;

		if(player.rotation < 0)
			player.rotation += 4;
		
		player.rotation %= 4;
		
		for(int i = 0; i < 9; i ++){
			if(Inputs.keyUp(Keys.valueOf(""+(i+1))) && i < control.getWeapons().size){
				player.weapon = control.getWeapons().get(i);
				ui.updateWeapons();
			}
		}
		
		Tile cursor = Vars.world.tile(tilex(), tiley());
		
		if(Inputs.buttonUp(Buttons.LEFT) && player.recipe != null && 
				Vars.world.validPlace(tilex(), tiley(), player.recipe.result) && !ui.hasMouse() && cursorNear() &&
				Vars.control.hasItems(player.recipe.requirements)){
			
			Vars.world.placeBlock(tilex(), tiley(), player.recipe.result, player.rotation);
			
			for(ItemStack stack : player.recipe.requirements){
				Vars.control.removeItem(stack);
			}
			
			if(!Vars.control.hasItems(player.recipe.requirements)){
				Cursors.restoreCursor();
			}
			
		}else if(Inputs.buttonUp(Buttons.LEFT)){
			Tile linked = cursor.isLinked() ? cursor.getLinked() : cursor;
			if(linked != null && linked.block() instanceof Configurable){
				Vars.ui.showConfig(linked);
			}else if(!Vars.ui.hasConfigMouse()){
				Vars.ui.hideConfig();
			}
		}
		
		if(Inputs.buttonUp(Buttons.RIGHT)){
			Vars.ui.hideConfig();
		}

		if(player.recipe != null && Inputs.buttonUp(Buttons.RIGHT)){
			player.recipe = null;
			Cursors.restoreCursor();
		}

		//block breaking
		if(Inputs.buttonDown(Buttons.RIGHT) && cursor != null && Vars.world.validBreak(tilex(), tiley())){
			Tile tile = cursor;
			player.breaktime += Timers.delta();
			if(player.breaktime >= tile.getBreakTime()){
				Vars.world.breakBlock(cursor.x, cursor.y);
				player.breaktime = 0f;
			}
		}else{
			player.breaktime = 0f;
		}

	}
	
	public static boolean cursorNear(){
		return Vector2.dst(player.x, player.y, tilex() * tilesize, tiley() * tilesize) <= placerange;
	}
	
	public static boolean onConfigurable(){
		Tile tile = Vars.world.tile(tilex(), tiley());
		return tile != null && (tile.block() instanceof Configurable || (tile.isLinked() && tile.getLinked().block() instanceof Configurable));
	}

	public static int tilex(){
		return (player.recipe != null && player.recipe.result.isMultiblock() &&
				player.recipe.result.width % 2 == 0) ?
				Mathf.scl(Graphics.mouseWorld().x, tilesize) : Mathf.scl2(Graphics.mouseWorld().x, tilesize);
	}

	public static int tiley(){
		return (player.recipe != null && player.recipe.result.isMultiblock() &&
				player.recipe.result.height % 2 == 0) ?
				Mathf.scl(Graphics.mouseWorld().y, tilesize) : Mathf.scl2(Graphics.mouseWorld().y, tilesize);
	}
	
	public static int currentWeapon(){
		int i = 0;
		for(Weapon weapon : control.getWeapons()){
			if(player.weapon == weapon)
				return i;
			i ++;
		}
		return 0;
	}
}
