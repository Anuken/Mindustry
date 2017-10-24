package io.anuke.mindustry.input;

import static io.anuke.mindustry.Vars.*;

import com.badlogic.gdx.Input.Buttons;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.math.Vector2;

import io.anuke.mindustry.Vars;
import io.anuke.mindustry.resource.Weapon;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.World;
import io.anuke.ucore.core.Graphics;
import io.anuke.ucore.core.Inputs;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.scene.utils.Cursors;
import io.anuke.ucore.util.Mathf;

public class Input{
	
	public static void doInput(){
		//player is dead
		if(player.health <= 0) return;
		
		if(Inputs.scrolled()){
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
		
		if(Inputs.buttonUp(Buttons.LEFT) && player.recipe != null && 
				World.validPlace(tilex(), tiley(), player.recipe.result) && !ui.hasMouse() && cursorNear() &&
				Vars.control.hasItems(player.recipe.requirements)){
			
			World.placeBlock(tilex(), tiley());
			
		}

		if(player.recipe != null && Inputs.buttonUp(Buttons.RIGHT)){
			player.recipe = null;
			Cursors.restoreCursor();
		}
		
		Tile cursor = World.tile(tilex(), tiley());

		//block breaking
		if(cursor != null && Inputs.buttonDown(Buttons.RIGHT) && World.validBreak(tilex(), tiley())){
			Tile tile = cursor;
			player.breaktime += Timers.delta();
			if(player.breaktime >= tile.getBreakTime()){
				World.breakBlock(cursor.x, cursor.y);
				player.breaktime = 0f;
			}
		}else{
			player.breaktime = 0f;
		}

	}
	
	public static boolean cursorNear(){
		return Vector2.dst(player.x, player.y, tilex() * tilesize, tiley() * tilesize) <= placerange;
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
