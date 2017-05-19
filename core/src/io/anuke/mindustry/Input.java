package io.anuke.mindustry;

import static io.anuke.mindustry.Vars.*;

import com.badlogic.gdx.Input.Buttons;
import com.badlogic.gdx.Input.Keys;

import io.anuke.mindustry.entities.Weapon;
import io.anuke.mindustry.resource.ItemStack;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.Blocks;
import io.anuke.mindustry.world.blocks.ProductionBlocks;
import io.anuke.ucore.core.Effects;
import io.anuke.ucore.core.Inputs;
import io.anuke.ucore.core.Sounds;
import io.anuke.ucore.scene.utils.Cursors;
import io.anuke.ucore.util.Mathf;

public class Input{
	
	public static void doInput(){
		//player is dead
		if(player.health <= 0) return;
		
		if(Inputs.scrolled()){
			Weapon[] val = Weapon.values();
			int index = 0;
			for(int i = 0; i < val.length; i ++)
				if(val[i] == currentWeapon){
					index = i;
					break;
				}
			
			for(int i = 0; i < val.length; i ++){
				index += Inputs.scroll();
				if(index >= 0 && index < val.length){
					if(weapons.get(val[index])){
						currentWeapon = (val[index]);
						break;
					}
				}else{
					break;
				}
			}
			
			ui.updateWeapons();
		}

		if(Inputs.keyUp("rotate"))
			rotation++;

		rotation %= 4;

		if(recipe != null && !Inventory.hasItems(recipe.requirements)){
			recipe = null;
			Cursors.restoreCursor();
		}
		
		for(int i = 0; i < 9; i ++)
			if(Inputs.keyUp(Keys.valueOf(""+(i+1))) && getWeapon(i) != null){
				currentWeapon = getWeapon(i);
				ui.updateWeapons();
			}
		
		if(Inputs.buttonUp(Buttons.LEFT) && recipe != null && 
				World.validPlace(World.tilex(), World.tiley(), recipe.result) && !ui.hasMouse()){
			Tile tile = World.tile(World.tilex(), World.tiley());
			
			if(tile == null)
				return; //just in case
			
			tile.setBlock(recipe.result);
			tile.rotation = rotation;

			Effects.effect("place", World.roundx(), World.roundy());
			Effects.shake(2f, 2f);
			Sounds.play("place");

			for(ItemStack stack : recipe.requirements){
				Inventory.removeItem(stack);
			}

			if(!Inventory.hasItems(recipe.requirements)){
				recipe = null;
				Cursors.restoreCursor();
			}
		}

		if(recipe != null && Inputs.buttonUp(Buttons.RIGHT)){
			recipe = null;
			Cursors.restoreCursor();
		}
		
		Tile cursor = World.cursorTile();

		//block breaking
		if(Inputs.buttonDown(Buttons.RIGHT) && World.cursorNear() && cursor.breakable()
				&& cursor.block() != ProductionBlocks.core){
			Tile tile = cursor;
			breaktime += Mathf.delta();
			if(breaktime >= tile.block().breaktime){
				Effects.effect("break", tile.worldx(), tile.worldy());
				Effects.shake(3f, 1f);
				tile.setBlock(Blocks.air);
				breaktime = 0f;
				Sounds.play("break");
			}
		}else{
			breaktime = 0f;
		}

	}
	
	public static int currentWeapons(){
		int i = 0;
		
		for(Weapon w : Weapon.values())
			if(weapons.get(w))
				i ++;
		
		return i;
	}
	
	public static Weapon getWeapon(int id){
		int i = 0;
		
		for(Weapon w : Weapon.values())
			if(weapons.get(w))
				if(i ++ == id)
					return w;
		
		return null;
	}
}
