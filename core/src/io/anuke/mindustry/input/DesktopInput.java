package io.anuke.mindustry.input;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Buttons;
import com.badlogic.gdx.math.Vector2;
import io.anuke.mindustry.core.GameState.State;
import io.anuke.mindustry.entities.Player;
import io.anuke.mindustry.net.Net;
import io.anuke.mindustry.net.NetEvents;
import io.anuke.mindustry.resource.Weapon;
import io.anuke.mindustry.world.Tile;
import io.anuke.ucore.core.*;
import io.anuke.ucore.core.Inputs.DeviceType;
import io.anuke.ucore.scene.ui.layout.Unit;
import io.anuke.ucore.scene.utils.Cursors;
import io.anuke.ucore.util.Mathf;

import static io.anuke.mindustry.Vars.*;

public class DesktopInput extends InputHandler{
	float mousex, mousey;
	float endx, endy;
	private float controlx, controly;
	private boolean enableHold = false;
	private boolean beganBreak;
	private boolean controlling;
	private final int index;
	private final String section;

	public DesktopInput(Player player){
	    super(player);
	    this.index = player.playerIndex;
	    this.section = "player_" + (player.playerIndex + 1);
    }
	
	@Override public float getCursorEndX(){ return endx; }
	@Override public float getCursorEndY(){ return endy; }
	@Override public float getCursorX(){ return Graphics.screen(mousex, mousey).x; }
	@Override public float getCursorY(){ return Gdx.graphics.getHeight() - 1 - Graphics.screen(mousex, mousey).y; }
	@Override public boolean drawPlace(){ return !beganBreak; }

	@Override
	public void update(){

        updateController();

		if(player.isDead()) return;

		if(Inputs.keyRelease(section, "select")){
			placeMode.released(this, getBlockX(), getBlockY(), getBlockEndX(), getBlockEndY());
		}

		if(Inputs.keyRelease(section, "break") && !beganBreak){
			breakMode.released(this, getBlockX(), getBlockY(), getBlockEndX(), getBlockEndY());
		}

		if(!Inputs.keyDown(section, "select")){
			shooting = false;
		}

		boolean canBeginShoot = Inputs.keyTap(section, "select") && canShoot();

		if(Inputs.keyTap(section, "select") && recipe == null && player.inventory.hasItem()){
			Vector2 vec = Graphics.screen(player.x, player.y);
			if(vec.dst(Gdx.input.getX(), Gdx.graphics.getHeight() - Gdx.input.getY()) <= playerSelectRange){
				canBeginShoot = false;
				droppingItem = true;
			}
		}

		if((Inputs.keyTap(section, "select") && recipe != null) || Inputs.keyTap(section, "break")){
			Vector2 vec = Graphics.world(Gdx.input.getX(), Gdx.input.getY());
			mousex = vec.x;
			mousey = vec.y;
		}

		if(!Inputs.keyDown(section, "select") && !Inputs.keyDown(section, "break")){
			mousex = Graphics.mouseWorld().x;
			mousey = Graphics.mouseWorld().y;
		}
		
		endx = Gdx.input.getX();
		endy = Gdx.input.getY();

		boolean controller = KeyBinds.getSection(section).device.type == DeviceType.controller;
		
		if(Inputs.getAxisActive("zoom") && (Inputs.keyDown(section,"zoom_hold") || controller)
				&& !state.is(State.menu) && !ui.hasDialog()){
		    renderer.scaleCamera((int) Inputs.getAxisTapped(section, "zoom"));
		}

		renderer.minimap().zoomBy(-(int)Inputs.getAxisTapped(section,"zoom_minimap"));

		rotation += Inputs.getAxisTapped(section,"rotate_alt");
		rotation += Inputs.getAxis(section,"rotate");

		rotation = Mathf.mod(rotation, 4);
		
		if(Inputs.keyDown(section,"break")){
			breakMode = PlaceMode.areaDelete;
		}else{
			breakMode = PlaceMode.hold;
		}

		int keyIndex = 1;
		
		for(int i = 0; i < 6 && i < player.upgrades.size; i ++){
			if(!(player.upgrades.get(i) instanceof Weapon)){
				continue;
			}

			if(Inputs.keyTap("weapon_" + keyIndex)){
				player.weapon = (Weapon) player.upgrades.get(i);
                if(Net.active()) NetEvents.handleWeaponSwitch(player);
			}

			keyIndex ++;
		}
		
		Tile cursor = world.tile(tilex(), tiley());
		Tile target = cursor == null ? null : cursor.target();
		boolean showCursor = false;

		if(droppingItem && Inputs.keyRelease(section,"select") && !player.inventory.isEmpty() && target != null){
			dropItem(target, player.inventory.getItem());
		}

		if(droppingItem && (!Inputs.keyDown(section,"select") || player.inventory.isEmpty())){
			droppingItem = false;
		}

		if(recipe == null && target != null && !ui.hasMouse() && Inputs.keyDown(section,"block_info") && target.block().isAccessible()){
			showCursor = true;
			if(Inputs.keyTap(section,"select")){
				canBeginShoot = false;
				frag.inv.showFor(target);
                Cursors.restoreCursor();
            }
		}

		if(!ui.hasMouse() && (target == null || !target.block().isAccessible()) && Inputs.keyTap(section,"select")){
			frag.inv.hide();
		}

        if(target != null && target.block().isConfigurable(target)){
		    showCursor = true;
        }
		
		if(target != null && Inputs.keyTap(section,"select") && !ui.hasMouse()){
			if(target.block().isConfigurable(target)){
				if((!frag.config.isShown()
						|| frag.config.getSelectedTile().block().onConfigureTileTapped(frag.config.getSelectedTile(), cursor))) {
					frag.config.showConfig(target);
					canBeginShoot = false;
				}
			}else if(!frag.config.hasConfigMouse()){
				if(frag.config.isShown() && frag.config.getSelectedTile().block().onConfigureTileTapped(frag.config.getSelectedTile(), cursor)) {
					frag.config.hideConfig();
					canBeginShoot = false;
				}
			}

			target.block().tapped(target);
			if(Net.active()) NetEvents.handleBlockTap(target);
		}
		
		if(Inputs.keyTap(section,"break")){
			frag.config.hideConfig();
		}
		
		if(Inputs.keyRelease(section,"break")){
			beganBreak = false;
		}

		if(recipe != null && Inputs.keyTap(section,"break")){
			beganBreak = true;
			recipe = null;
		}

		//block breaking
		if(enableHold && Inputs.keyDown(section,"break") && cursor != null && validBreak(tilex(), tiley())){
			breaktime += Timers.delta();
			if(breaktime >= cursor.getBreakTime()){
				breakBlock(cursor.x, cursor.y, true);
				breaktime = 0f;
			}
		}else{
			breaktime = 0f;
		}

		if(recipe != null){
			showCursor = validPlace(tilex(), tiley(), recipe.result) && cursorNear();
		}

		if(canBeginShoot){
			shooting = true;
		}

		if(!ui.hasMouse()) {
			if (showCursor)
				Cursors.setHand();
			else
				Cursors.restoreCursor();
		}

	}

    @Override
    public float getMouseX() {
        return controlx;
    }

    @Override
    public float getMouseY() {
        return controly;
    }

    @Override
    public boolean isCursorVisible() {
        return controlling;
    }

    void updateController(){
	    boolean mousemove = Gdx.input.getDeltaX() > 1 || Gdx.input.getDeltaY() > 1;

        if(KeyBinds.getSection(section).device.type == DeviceType.controller && (!mousemove || player.playerIndex > 0)){
            if(player.playerIndex > 0){
                controlling = true;
            }

            if(Inputs.keyTap(section,"select")){
                Inputs.getProcessor().touchDown(Gdx.input.getX(), Gdx.input.getY(), player.playerIndex, Buttons.LEFT);
            }

            if(Inputs.keyRelease(section,"select")){
                Inputs.getProcessor().touchUp(Gdx.input.getX(), Gdx.input.getY(), player.playerIndex, Buttons.LEFT);
            }

            float xa = Inputs.getAxis(section, "cursor_x");
            float ya = Inputs.getAxis(section, "cursor_y");

            if(Math.abs(xa) > controllerMin || Math.abs(ya) > controllerMin) {
                float scl = Settings.getInt("sensitivity")/100f * Unit.dp.scl(1f);
                controlx += xa*baseControllerSpeed*scl;
                controly -= ya*baseControllerSpeed*scl;
                controlling = true;

                Gdx.input.setCursorCatched(true);

                Inputs.getProcessor().touchDragged(Gdx.input.getX(), Gdx.input.getY(), player.playerIndex);
            }

            controlx = Mathf.clamp(controlx, 0, Gdx.graphics.getWidth());
            controly = Mathf.clamp(controly, 0, Gdx.graphics.getHeight());
        }else{
            controlling = false;
            Gdx.input.setCursorCatched(false);
        }

        if(!controlling){
            controlx = control.gdxInput().getX();
            controly = control.gdxInput().getY();
        }
    }

	public int tilex(){
		return (recipe != null && recipe.result.isMultiblock() &&
				recipe.result.size % 2 == 0) ?
				Mathf.scl(Graphics.mouseWorld().x, tilesize) : Mathf.scl2(Graphics.mouseWorld().x, tilesize);
	}

	public int tiley(){
		return (recipe != null && recipe.result.isMultiblock() &&
				recipe.result.size % 2 == 0) ?
				Mathf.scl(Graphics.mouseWorld().y, tilesize) : Mathf.scl2(Graphics.mouseWorld().y, tilesize);
	}

	@Override
	public boolean keyDown(int keycode) {
		return super.keyDown(keycode);
	}
}
