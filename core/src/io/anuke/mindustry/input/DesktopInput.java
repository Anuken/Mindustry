package io.anuke.mindustry.input;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Vector2;
import io.anuke.mindustry.core.GameState.State;
import io.anuke.mindustry.net.Net;
import io.anuke.mindustry.net.NetEvents;
import io.anuke.mindustry.world.Tile;
import io.anuke.ucore.core.Graphics;
import io.anuke.ucore.core.Inputs;
import io.anuke.ucore.core.Inputs.DeviceType;
import io.anuke.ucore.core.KeyBinds;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.scene.utils.Cursors;
import io.anuke.ucore.util.Mathf;

import static io.anuke.mindustry.Vars.*;

public class DesktopInput extends InputHandler{
	float mousex, mousey;
	float endx, endy;
	private boolean enableHold = false;
	private boolean beganBreak;
	private boolean rotated = false, rotatedAlt, zoomed;
	
	@Override public float getCursorEndX(){ return endx; }
	@Override public float getCursorEndY(){ return endy; }
	@Override public float getCursorX(){ return Graphics.screen(mousex, mousey).x; }
	@Override public float getCursorY(){ return Gdx.graphics.getHeight() - 1 - Graphics.screen(mousex, mousey).y; }
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
			mousex = vec.x;
			mousey = vec.y;
		}

		if(!Inputs.keyDown("select") && !Inputs.keyDown("break")){
			mousex = Graphics.mouseWorld().x;
			mousey = Graphics.mouseWorld().y;
		}
		
		endx = Gdx.input.getX();
		endy = Gdx.input.getY();

		boolean controller = KeyBinds.getSection("default").device.type == DeviceType.controller;
		
		if(Inputs.getAxisActive("zoom") && (Inputs.keyDown("zoom_hold") || controller)
				&& !state.is(State.menu) && !ui.hasDialog()){
			if((!zoomed || !controller)) {
				renderer.scaleCamera((int) Inputs.getAxis("zoom"));
			}
			zoomed = true;
		}else{
			zoomed = false;
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
		
		for(int i = 1; i <= 6 && i <= control.upgrades().getWeapons().size; i ++){
			if(Inputs.keyTap("weapon_" + i)){
				player.weaponLeft = player.weaponRight = control.upgrades().getWeapons().get(i - 1);
                if(Net.active()) NetEvents.handleWeaponSwitch();
				ui.hudfrag.updateWeapons();
			}
		}
		
		Tile cursor = world.tile(tilex(), tiley());
		Tile target = cursor == null ? null : cursor.isLinked() ? cursor.getLinked() : cursor;
		boolean showCursor = false;

		if(recipe == null && target != null && !ui.hasMouse() && Inputs.keyDown("block_info")
				&& target.block().fullDescription != null){
			showCursor = true;
			if(Inputs.keyTap("select")){
			    ui.hudfrag.blockfrag.showBlockInfo(target.block());
                Cursors.restoreCursor();
            }
		}

        if(target != null && target.block().isConfigurable(target)){
		    showCursor = true;
        }
		
		if(target != null && Inputs.keyTap("select") && !ui.hasMouse()){
			if(target.block().isConfigurable(target)){
				ui.configfrag.showConfig(target);
			}else if(!ui.configfrag.hasConfigMouse()){
				ui.configfrag.hideConfig();
			}

			target.block().tapped(target);
			if(Net.active()) NetEvents.handleBlockTap(target);
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

		if(recipe != null){
			showCursor = validPlace(tilex(), tiley(), control.input().recipe.result) && control.input().cursorNear();
		}

		if(!ui.hasMouse()) {
			if (showCursor)
				Cursors.setHand();
			else
				Cursors.restoreCursor();
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

	@Override
	public boolean keyDown(int keycode) {
		return super.keyDown(keycode);
	}
}
