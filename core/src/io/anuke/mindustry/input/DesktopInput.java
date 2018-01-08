package io.anuke.mindustry.input;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Vector2;
import io.anuke.mindustry.Vars;
import io.anuke.mindustry.core.GameState;
import io.anuke.mindustry.core.GameState.State;
import io.anuke.mindustry.resource.Weapon;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.types.Configurable;
import io.anuke.ucore.core.Graphics;
import io.anuke.ucore.core.Inputs;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.scene.utils.Cursors;
import io.anuke.ucore.util.Mathf;

import static io.anuke.mindustry.Vars.*;

public class DesktopInput extends InputHandler{
	int mousex, mousey;
	int endx, endy;
	private boolean enableHold = false;
	private boolean beganBreak;
	private boolean rotated = false, rotatedAlt;
	
	@Override public float getCursorEndX(){ return endx; }
	@Override public float getCursorEndY(){ return endy; }
	@Override public float getCursorX(){ return (int)(Graphics.screen(mousex, mousey).x + 2); }
	@Override public float getCursorY(){ return (int)(Gdx.graphics.getHeight() - 1 - Graphics.screen(mousex, mousey).y); }
	@Override public boolean drawPlace(){ return !beganBreak; }

	@Override
	public void update(){
		if(player.isDead()) return;

		if(Inputs.keyRelease("select")){
			placeMode.released(getBlockX(), getBlockY(), getBlockEndX(), getBlockEndY());
		}

		if(Inputs.keyRelease("break") && !beganBreak){
			breakMode.released(getBlockX(), getBlockY(), getBlockEndX(), getBlockEndY());
		}

		if((Inputs.keyTap("select") && recipe != null) || Inputs.keyTap("break")){
			Vector2 vec = Graphics.world(Gdx.input.getX(), Gdx.input.getY());
			mousex = (int)vec.x;
			mousey = (int)vec.y;
		}

		if(!Inputs.keyDown("select") && !Inputs.keyDown("break")){
			mousex = (int)Graphics.mouseWorld().x;
			mousey = (int)Graphics.mouseWorld().y;
		}
		
		endx = Gdx.input.getX();
		endy = Gdx.input.getY();
		
		if(Inputs.getAxisActive("zoom") && Inputs.keyDown("zoom_hold") && !GameState.is(State.menu) && !ui.hasDialog()){
			renderer.scaleCamera((int)Inputs.getAxis("zoom"));
		}

		if(!rotated) {
			rotation += Inputs.getAxis("rotate_alt");
			rotated = true;
		}
		if(!Inputs.getAxisActive("rotate_alt")) rotated = false;

		if(!rotatedAlt) {
			rotation += Inputs.getAxis("rotate");
			rotatedAlt = true;
		}
		if(!Inputs.getAxisActive("rotate")) rotatedAlt = false;

		rotation = Mathf.mod(rotation, 4);
		
		if(Inputs.keyDown("break")){
			breakMode = PlaceMode.areaDelete;
		}else{
			breakMode = PlaceMode.hold;
		}
		
		for(int i = 1; i <= 6 && i <= control.getWeapons().size; i ++){
			if(Inputs.keyTap("weapon_" + i)){
				player.weaponLeft = player.weaponRight = control.getWeapons().get(i - 1);
                Vars.netClient.handleWeaponSwitch();
				Vars.ui.hudfrag.updateWeapons();
			}
		}
		
		Tile cursor = world.tile(tilex(), tiley());
		
		if(Inputs.keyTap("select") && cursor != null && !ui.hasMouse()){
			Tile linked = cursor.isLinked() ? cursor.getLinked() : cursor;
			if(linked != null && linked.block() instanceof Configurable){
				ui.configfrag.showConfig(linked);
			}else if(!ui.configfrag.hasConfigMouse()){
				ui.configfrag.hideConfig();
			}
		}
		
		if(Inputs.keyTap("break")){
			ui.configfrag.hideConfig();
		}
		
		if(Inputs.keyRelease("break")){
			beganBreak = false;
		}

		if(recipe != null && Inputs.keyTap("break")){
			beganBreak = true;
			recipe = null;
			Cursors.restoreCursor();
		}

		//block breaking
		if(enableHold && Inputs.keyDown("break") && cursor != null && validBreak(tilex(), tiley())){
			breaktime += Timers.delta();
			if(breaktime >= cursor.getBreakTime()){
				breakBlock(cursor.x, cursor.y, true);
				breaktime = 0f;
			}
		}else{
			breaktime = 0f;
		}

	}

	public int tilex(){
		return (recipe != null && recipe.result.isMultiblock() &&
				recipe.result.width % 2 == 0) ?
				Mathf.scl(Graphics.mouseWorld().x, tilesize) : Mathf.scl2(Graphics.mouseWorld().x, tilesize);
	}

	public int tiley(){
		return (recipe != null && recipe.result.isMultiblock() &&
				recipe.result.height % 2 == 0) ?
				Mathf.scl(Graphics.mouseWorld().y, tilesize) : Mathf.scl2(Graphics.mouseWorld().y, tilesize);
	}
	
	public int currentWeapon(){
		int i = 0;
		for(Weapon weapon : control.getWeapons()){
			if(weapon == weapon)
				return i;
			i ++;
		}
		return 0;
	}

	@Override
	public boolean keyDown(int keycode) {
		return super.keyDown(keycode);
	}
}
