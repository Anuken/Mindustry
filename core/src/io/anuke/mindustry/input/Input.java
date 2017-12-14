package io.anuke.mindustry.input;

import static io.anuke.mindustry.Vars.*;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Buttons;
import com.badlogic.gdx.Input.Keys;

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

public class Input extends InputHandler{
	float mousex, mousey;
	
	@Override public float getCursorEndX(){ return Gdx.input.getX(); }
	@Override public float getCursorEndY(){ return Gdx.input.getY(); }
	@Override public float getCursorX(){ return mousex; }
	@Override public float getCursorY(){ return mousey; }
	
	@Override
	public boolean touchDown (int screenX, int screenY, int pointer, int button) {
		if(button == Buttons.LEFT){
			mousex = screenX;
			mousey = screenY;
		}
		return false;
	}
	
	public boolean touchUp(int screenX, int screenY, int pointer, int button){
		player.placeMode.tapped(getBlockX(), getBlockY(), getBlockEndX(), getBlockEndY());
		return false;
	}
	
	@Override
	public void update(){
		if(player.isDead()) return;
		
		if(!Inputs.buttonDown(Buttons.LEFT)){
			mousex = Gdx.input.getX();
			mousey = Gdx.input.getY();
		}
		
		if(Inputs.scrolled() && Inputs.keyDown("zoom_hold") && !GameState.is(State.menu) && !Vars.ui.onDialog()){
			Vars.renderer.scaleCamera(Inputs.scroll());
		}
		
		if(Inputs.scrolled()){
			player.rotation += Inputs.scroll();
		}
		
		if(Inputs.keyUp("rotate_right")){
			player.rotation --;
		}
		
		if(Inputs.keyUp("rotate_left")){
			player.rotation ++;
		}
		
		player.rotation = Mathf.mod(player.rotation, 4);
		
		for(int i = 0; i < 9; i ++){
			if(Inputs.keyUp(Keys.valueOf(""+(i+1))) && i < control.getWeapons().size){
				player.weapon = control.getWeapons().get(i);
				ui.updateWeapons();
			}
		}
		
		Tile cursor = Vars.world.tile(tilex(), tiley());
		
		if(Inputs.buttonUp(Buttons.LEFT) && cursor != null){
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

	public int tilex(){
		return (player.recipe != null && player.recipe.result.isMultiblock() &&
				player.recipe.result.width % 2 == 0) ?
				Mathf.scl(Graphics.mouseWorld().x, tilesize) : Mathf.scl2(Graphics.mouseWorld().x, tilesize);
	}

	public int tiley(){
		return (player.recipe != null && player.recipe.result.isMultiblock() &&
				player.recipe.result.height % 2 == 0) ?
				Mathf.scl(Graphics.mouseWorld().y, tilesize) : Mathf.scl2(Graphics.mouseWorld().y, tilesize);
	}
	
	public int currentWeapon(){
		int i = 0;
		for(Weapon weapon : control.getWeapons()){
			if(player.weapon == weapon)
				return i;
			i ++;
		}
		return 0;
	}
}
