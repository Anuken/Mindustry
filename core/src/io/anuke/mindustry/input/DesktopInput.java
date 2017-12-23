package io.anuke.mindustry.input;

import static io.anuke.mindustry.Vars.*;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Buttons;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.math.Vector2;

import io.anuke.mindustry.core.GameState;
import io.anuke.mindustry.core.GameState.State;
import io.anuke.mindustry.resource.Weapon;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.types.Configurable;
import io.anuke.ucore.UCore;
import io.anuke.ucore.core.Graphics;
import io.anuke.ucore.core.Inputs;
import io.anuke.ucore.core.Inputs.DeviceType;
import io.anuke.ucore.core.KeyBinds;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.scene.utils.Cursors;
import io.anuke.ucore.util.InputProxy;
import io.anuke.ucore.util.Mathf;

public class DesktopInput extends InputHandler{
	int mousex, mousey;
	int endx, endy;
	private boolean enableHold = false;
	private boolean beganBreak;

	public DesktopInput(){

	}
	
	@Override public float getCursorEndX(){ return endx; }
	@Override public float getCursorEndY(){ return endy; }
	@Override public float getCursorX(){ return (int)(Graphics.screen(mousex, mousey).x + 2); }
	@Override public float getCursorY(){ return (int)(Gdx.graphics.getHeight() - 1 - Graphics.screen(mousex, mousey).y); }
	@Override public boolean drawPlace(){ return !beganBreak; }
	
	@Override
	public boolean touchDown (int screenX, int screenY, int pointer, int button){
		if((button == Buttons.LEFT && player.recipe != null) || (button == Buttons.RIGHT)){
			Vector2 vec = Graphics.world(screenX, screenY);
			mousex = (int)vec.x;
			mousey = (int)vec.y;
		}
		return false;
	}
	
	public boolean touchUp(int screenX, int screenY, int pointer, int button){
		if(button == Buttons.LEFT){
			player.placeMode.released(getBlockX(), getBlockY(), getBlockEndX(), getBlockEndY());
		}else if(button == Buttons.RIGHT && !beganBreak){
			player.breakMode.released(getBlockX(), getBlockY(), getBlockEndX(), getBlockEndY());
		}
		return false;
	}
	
	@Override
	public void update(){
		if(player.isDead()) return;

		if(!Inputs.buttonDown(Buttons.LEFT) && !Inputs.buttonDown(Buttons.RIGHT)){
			mousex = (int)Graphics.mouseWorld().x;
			mousey = (int)Graphics.mouseWorld().y;
		}
		
		endx = Gdx.input.getX();
		endy = Gdx.input.getY();
		
		if(Inputs.getAxisActive("zoom") && Inputs.keyDown("zoom_hold") && !GameState.is(State.menu) && !ui.onDialog()){
			renderer.scaleCamera((int)Inputs.getAxis("zoom"));
		}

		player.rotation += Inputs.getAxis("rotate_alt");
		player.rotation += Inputs.getAxis("rotate");
		player.rotation = Mathf.mod(player.rotation, 4);
		
		if(Inputs.buttonDown(Buttons.RIGHT)){
			player.breakMode = PlaceMode.areaDelete;
		}else{
			player.breakMode = PlaceMode.hold;
		}
		
		for(int i = 1; i <= 6 && i < control.getWeapons().size; i ++){
			if(Inputs.keyTap("weapon_" + i) && i < control.getWeapons().size){
				player.weapon = control.getWeapons().get(i - 1);
				ui.updateWeapons();
			}
		}
		
		Tile cursor = world.tile(tilex(), tiley());
		
		if(Inputs.buttonUp(Buttons.LEFT) && cursor != null && !ui.hasMouse()){
			Tile linked = cursor.isLinked() ? cursor.getLinked() : cursor;
			if(linked != null && linked.block() instanceof Configurable){
				ui.showConfig(linked);
			}else if(!ui.hasConfigMouse()){
				ui.hideConfig();
			}
		}
		
		if(Inputs.buttonUp(Buttons.RIGHT)){
			ui.hideConfig();
		}
		
		if(Inputs.buttonRelease(Buttons.RIGHT)){
			beganBreak = false;
		}

		if(player.recipe != null && Inputs.buttonUp(Buttons.RIGHT)){
			beganBreak = true;
			player.recipe = null;
			Cursors.restoreCursor();
		}

		//block breaking
		if(enableHold && Inputs.buttonDown(Buttons.RIGHT) && cursor != null && validBreak(tilex(), tiley())){
			Tile tile = cursor;
			player.breaktime += Timers.delta();
			if(player.breaktime >= tile.getBreakTime()){
				breakBlock(cursor.x, cursor.y, true);
				player.breaktime = 0f;
			}
		}else{
			player.breaktime = 0f;
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
