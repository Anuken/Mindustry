package io.anuke.mindustry.input;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Buttons;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import io.anuke.mindustry.core.GameState.State;
import io.anuke.mindustry.entities.Player;
import io.anuke.mindustry.graphics.Palette;
import io.anuke.mindustry.input.PlaceUtils.NormalizeResult;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Tile;
import io.anuke.ucore.core.Inputs;
import io.anuke.ucore.core.Inputs.DeviceType;
import io.anuke.ucore.core.KeyBinds;
import io.anuke.ucore.core.Settings;
import io.anuke.ucore.graphics.Draw;
import io.anuke.ucore.graphics.Lines;
import io.anuke.ucore.scene.ui.layout.Unit;
import io.anuke.ucore.scene.utils.Cursors;
import io.anuke.ucore.util.Mathf;

import static io.anuke.mindustry.Vars.*;

public class DesktopInput extends InputHandler{
	//controller info
	private float controlx, controly;
	private boolean controlling;
    private boolean handCursor;
    private final String section;

    /**Position where the player started dragging a line.*/
    private int selectX, selectY;
    /**Wehther selecting mode is active.*/
    private boolean selecting;
    /**Animation scale for line.*/
    private float selectScale;

	public DesktopInput(Player player){
	    super(player);
	    this.section = "player_" + (player.playerIndex + 1);
    }

    void tileTapped(Tile tile){

		//check if tapped block is configurable
		if(tile.block().isConfigurable(tile)){
			if((!frag.config.isShown() //if the config fragment is hidden, show
					//alternatively, the current selected block can 'agree' to switch config tiles
					|| frag.config.getSelectedTile().block().onConfigureTileTapped(frag.config.getSelectedTile(), tile))) {
				frag.config.showConfig(tile);
			}
			//otherwise...
		}else if(!frag.config.hasConfigMouse()){ //make sure a configuration fragment isn't on the cursor
			//then, if it's shown and the current block 'agrees' to hide, hide it.
			if(frag.config.isShown() && frag.config.getSelectedTile().block().onConfigureTileTapped(frag.config.getSelectedTile(), tile)) {
				frag.config.hideConfig();
			}
		}

		//TODO network event!
		//call tapped event
		tile.block().tapped(tile, player);
	}

	void drawPlace(int x, int y, Block block, int rotation){
        if(validPlace(x, y, block, rotation)){
            Draw.color();

            TextureRegion[] regions = block.getBlockIcon();

            for(TextureRegion region : regions){
                Draw.rect(region, x *tilesize + block.offset(), y * tilesize + block.offset(),
                        region.getRegionWidth() * selectScale, region.getRegionHeight() * selectScale, block.rotate ? rotation * 90 : 0);
            }
        }else{
            Draw.color(Palette.remove);
            Lines.square(x*tilesize + block.offset(), y*tilesize + block.offset(), block.size * tilesize/2f);
        }
    }

    @Override
    public void drawBottom(){
        Tile cursor = tileAt(control.gdxInput().getX(), control.gdxInput().getY());

        if(cursor == null) return;

	    //draw selection
	    if(selecting){
            NormalizeResult result = PlaceUtils.normalizeArea(selectX, selectY, cursor.x, cursor.y, rotation, true, maxLength);

            for(int i = 0; i <= result.getLength(); i += recipe.result.size){
                int x = selectX + i * Mathf.sign(cursor.x - selectX) * Mathf.bool(result.isX());
                int y = selectY + i * Mathf.sign(cursor.y - selectY) * Mathf.bool(!result.isX());

                if(i + recipe.result.size > result.getLength() && recipe.result.rotate){
                    Draw.color(!validPlace(x, y, recipe.result, result.rotation) ? Palette.remove : Palette.placeRotate);
                    Draw.grect("place-arrow", x * tilesize + recipe.result.offset(),
                            y * tilesize + recipe.result.offset(), result.rotation * 90 - 90);
                }

                drawPlace(x, y, recipe.result, result.rotation);
            }

            Draw.reset();
	    }else if(isPlacing()){
	        if(recipe.result.rotate){
	            Draw.color(!validPlace(cursor.x, cursor.y, recipe.result, rotation) ? Palette.remove : Palette.placeRotate);
	            Draw.grect("place-arrow", cursor.worldx() + recipe.result.offset(),
                        cursor.worldy() + recipe.result.offset(), rotation * 90 - 90);
            }
            drawPlace(cursor.x, cursor.y, recipe.result, rotation);
        }

        Draw.reset();
    }

	@Override
	public void update(){
		if(player.isDead() || state.is(State.menu) || ui.hasDialog()) return;

		//deslect if not placing
		if(!isPlacing()){
		    selecting = false;
        }

        if(isPlacing()){
            handCursor = true;
            selectScale = Mathf.lerpDelta(selectScale, 1f, 0.2f);
        }else{
		    selectScale = 0f;
        }

		boolean controller = KeyBinds.getSection(section).device.type == DeviceType.controller;

		//zoom and rotate things
		if(Inputs.getAxisActive("zoom") && (Inputs.keyDown(section,"zoom_hold") || controller)){
		    renderer.scaleCamera((int) Inputs.getAxisTapped(section, "zoom"));
		}

		renderer.minimap().zoomBy(-(int)Inputs.getAxisTapped(section,"zoom_minimap"));
		rotation = Mathf.mod(rotation + (int)Inputs.getAxisTapped(section,"rotate"), 4);
		
		Tile cursor = tileAt(control.gdxInput().getX(), control.gdxInput().getY());

		if(cursor != null && cursor.block().isConfigurable(cursor)){
            handCursor = true;
        }

		if(!ui.hasMouse()) {
			if (handCursor) {
				Cursors.setHand();
			}else {
				Cursors.restoreCursor();
			}
		}

        handCursor = false;
	}

	@Override
    public boolean touchDown (int screenX, int screenY, int pointer, int button) {
        if(player.isDead() || state.is(State.menu) || ui.hasDialog()) return false;

        Tile cursor = tileAt(screenX, screenY);

        if(cursor == null) return false;

        if(isPlacing()) {
            selectX = cursor.x;
            selectY = cursor.y;
            selecting = true;
        }else {
            tileTapped(cursor);
        }

        return false;
    }

    @Override
    public boolean touchUp (int screenX, int screenY, int pointer, int button) {
        if(player.isDead() || state.is(State.menu) || ui.hasDialog()) return false;

        Tile cursor = tileAt(screenX, screenY);

        if(cursor == null){
            selecting = false;
            return false;
        }

        if(selecting){
            NormalizeResult result = PlaceUtils.normalizeArea(selectX, selectY, cursor.x, cursor.y, rotation, true, maxLength);

            for(int i = 0; i <= result.getLength(); i += recipe.result.size){
                int x = selectX + i * Mathf.sign(cursor.x - selectX) * Mathf.bool(result.isX());
                int y = selectY + i * Mathf.sign(cursor.y - selectY) * Mathf.bool(!result.isX());

                rotation = result.rotation;

                tryPlaceBlock(x, y);
            }
        }

        selecting = false;

        return false;
    }

    @Override
    public float getMouseX() {
        return !controlling ? control.gdxInput().getX() : controlx;
    }

    @Override
    public float getMouseY() {
        return !controlling ? control.gdxInput().getY() : controly;
    }

    @Override
    public boolean isCursorVisible() {
        return controlling;
    }

    @Override
    public void updateController(){
	    boolean mousemove = Gdx.input.getDeltaX() > 1 || Gdx.input.getDeltaY() > 1;

        if(KeyBinds.getSection(section).device.type == DeviceType.controller && (!mousemove || player.playerIndex > 0)){
            if(player.playerIndex > 0){
                controlling = true;
            }

            if(Inputs.keyTap(section,"select")){
                Inputs.getProcessor().touchDown((int)getMouseX(), (int)getMouseY(), player.playerIndex, Buttons.LEFT);
            }

            if(Inputs.keyRelease(section,"select")){
                Inputs.getProcessor().touchUp((int)getMouseX(), (int)getMouseY(), player.playerIndex, Buttons.LEFT);
            }

            float xa = Inputs.getAxis(section, "cursor_x");
            float ya = Inputs.getAxis(section, "cursor_y");

            if(Math.abs(xa) > controllerMin || Math.abs(ya) > controllerMin) {
                float scl = Settings.getInt("sensitivity")/100f * Unit.dp.scl(1f);
                controlx += xa*baseControllerSpeed*scl;
                controly -= ya*baseControllerSpeed*scl;
                controlling = true;

                if(player.playerIndex == 0){
                    Gdx.input.setCursorCatched(true);
                }

                Inputs.getProcessor().touchDragged((int)getMouseX(), (int)getMouseY(), player.playerIndex);
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
}
