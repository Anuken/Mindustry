package io.anuke.mindustry.input;

import static io.anuke.mindustry.Vars.*;

import com.badlogic.gdx.Input.Buttons;
import com.badlogic.gdx.Input.Keys;

import io.anuke.mindustry.Inventory;
import io.anuke.mindustry.entities.Weapon;
import io.anuke.mindustry.resource.ItemStack;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.World;
import io.anuke.mindustry.world.blocks.Blocks;
import io.anuke.mindustry.world.blocks.ProductionBlocks;
import io.anuke.ucore.core.*;
import io.anuke.ucore.scene.utils.Cursors;
import io.anuke.ucore.util.Mathf;

public class Input{
	
	public static void doInput(){
		//player is dead
		if(player.health <= 0) return;
		
		if(Inputs.scrolled()){
			int index = currentWeapon();
			
			index -= Inputs.scroll();
			player.weapon = control.getWeapons().get(Mathf.clamp(index, 0, control.getWeapons().size-1));
			
			ui.updateWeapons();
		}

		if(Inputs.keyUp("rotate"))
			player.rotation++;

		player.rotation %= 4;

		//TODO restore cursor when requirements are back
		
		for(int i = 0; i < 9; i ++)
			if(Inputs.keyUp(Keys.valueOf(""+(i+1))) && i < control.getWeapons().size){
				player.weapon = control.getWeapons().get(i);
				ui.updateWeapons();
			}
		
		if(Inputs.buttonUp(Buttons.LEFT) && player.recipe != null && 
				World.validPlace(World.tilex(), World.tiley(), player.recipe.result) && !ui.hasMouse() &&
				Inventory.hasItems(player.recipe.requirements)){
			Tile tile = World.tile(World.tilex(), World.tiley());
			
			if(tile == null)
				return; //just in case
			
			tile.setBlock(player.recipe.result);
			tile.rotation = (byte)player.rotation;

			Effects.effect("place", World.roundx(), World.roundy());
			Effects.shake(2f, 2f);
			Sounds.play("place");

			for(ItemStack stack : player.recipe.requirements){
				Inventory.removeItem(stack);
			}
			
			if(!Inventory.hasItems(player.recipe.requirements)){
				Cursors.restoreCursor();
			}
		}

		if(player.recipe != null && Inputs.buttonUp(Buttons.RIGHT)){
			player.recipe = null;
			Cursors.restoreCursor();
		}
		
		Tile cursor = World.cursorTile();

		//block breaking
		if(Inputs.buttonDown(Buttons.RIGHT) && World.cursorNear() && cursor.breakable()
				&& cursor.block() != ProductionBlocks.core){
			Tile tile = cursor;
			player.breaktime += Timers.delta();
			if(player.breaktime >= tile.block().breaktime){
				if(tile.block().drops != null){
					Inventory.addItem(tile.block().drops.item, tile.block().drops.amount);
				}
				
				Effects.effect("break", tile.worldx(), tile.worldy());
				Effects.shake(3f, 1f);
				tile.setBlock(Blocks.air);
				player.breaktime = 0f;
				Sounds.play("break");
			}
		}else{
			player.breaktime = 0f;
		}

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
