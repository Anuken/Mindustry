package io.anuke.mindustry.input;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.input.GestureDetector;
import com.badlogic.gdx.input.GestureDetector.GestureListener;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import io.anuke.mindustry.content.fx.Fx;
import io.anuke.mindustry.core.GameState.State;
import io.anuke.mindustry.entities.Player;
import io.anuke.mindustry.graphics.Palette;
import io.anuke.mindustry.graphics.Shaders;
import io.anuke.mindustry.input.PlaceUtils.NormalizeDrawResult;
import io.anuke.mindustry.input.PlaceUtils.NormalizeResult;
import io.anuke.mindustry.type.Recipe;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.types.BuildBlock;
import io.anuke.mindustry.world.blocks.types.BuildBlock.BuildEntity;
import io.anuke.ucore.core.Core;
import io.anuke.ucore.core.Effects;
import io.anuke.ucore.core.Graphics;
import io.anuke.ucore.core.Inputs;
import io.anuke.ucore.graphics.Draw;
import io.anuke.ucore.graphics.Lines;
import io.anuke.ucore.scene.Group;
import io.anuke.ucore.scene.builders.imagebutton;
import io.anuke.ucore.scene.builders.table;
import io.anuke.ucore.scene.ui.layout.Unit;
import io.anuke.ucore.util.Mathf;

import static io.anuke.mindustry.Vars.*;

public class AndroidInput extends InputHandler implements GestureListener{
    private static Rectangle r1 = new Rectangle(), r2 = new Rectangle();

    /**Maximum line length.*/
    private static final int maxLength = 100;
    /**Maximum speed the player can pan.*/
    private static final float maxPanSpeed = 1.3f;
    /**Distance to edge of screen to start panning.*/
    private final float edgePan = Unit.dp.scl(60f);

    //gesture data
    private Vector2 pinch1 = new Vector2(-1, -1), pinch2 = pinch1.cpy();
    private Vector2 vector = new Vector2();
    private float initzoom = -1;
    private boolean zoomed = false;

    /**Position where the player started dragging a line.*/
    private int lineStartX, lineStartY;

    /**Animation scale for line.*/
    private float lineScale;

    /**List of currently selected tiles to place.*/
    private Array<PlaceRequest> placement = new Array<>();
    /**Place requests to be removed.*/
    private Array<PlaceRequest> removals = new Array<>();
    /**Whether or not the player is currently shifting all placed tiles.*/
    private boolean selecting;
    /**Whether the player is currently in line-place mode.*/
    private boolean lineMode;
	
	public AndroidInput(Player player){
	    super(player);
		Inputs.addProcessor(new GestureDetector(20, 0.5f, 0.4f, 0.15f, this));
	}

	/**Returns whether this tile is in the list of requests, or at least colliding with one.*/
	boolean hasRequest(Tile tile){
        return getRequest(tile) != null;
    }

    /**Returns whether this block overlaps any placement requests.*/
    boolean checkOverlapPlacement(int x, int y, Block block){
        r2.setSize(block.size * tilesize);
        r2.setCenter(x * tilesize + block.offset(), y * tilesize + block.offset());

        for(PlaceRequest req : placement){
            Tile other = req.tile();

            if(other == null) continue;

            r1.setSize(req.recipe.result.size * tilesize);
            r1.setCenter(other.worldx() + req.recipe.result.offset(), other.worldy() + req.recipe.result.offset());

            if(r2.overlaps(r1)){
                return true;
            }
        }
	    return false;
    }

    /**Returns the placement request that overlaps this tile, or null.*/
    PlaceRequest getRequest(Tile tile){
	    r2.setSize(tilesize);
	    r2.setCenter(tile.worldx(), tile.worldy());

        for(PlaceRequest req : placement){
            Tile other = req.tile();

            if(other == null) continue;

            r1.setSize(req.recipe.result.size * tilesize);
            r1.setCenter(other.worldx() + req.recipe.result.offset(), other.worldy() + req.recipe.result.offset());

            if(r2.overlaps(r1)){
                return req;
            }
        }
        return null;
    }

    Tile tileAt(float x, float y){
        Vector2 vec = Graphics.world(x, y);
        return world.tileWorld(vec.x, vec.y);
    }

    void removeRequest(PlaceRequest request){
        placement.removeValue(request, true);
        removals.add(request);
    }

    void drawRequest(PlaceRequest request){
        Tile tile = request.tile();

        float offset = request.recipe.result.offset();
        TextureRegion[] regions = request.recipe.result.getBlockIcon();

        Draw.alpha(Mathf.clamp((1f - request.scale) / 0.5f));
        Draw.tint(Color.WHITE, Palette.breakInvalid, request.redness);

        for(TextureRegion region : regions){
            Draw.rect(region, tile.worldx() + offset, tile.worldy() + offset,
                    region.getRegionWidth() * request.scale, region.getRegionHeight() * request.scale,
                    request.recipe.result.rotate ? request.rotation * 90 : 0);
        }
    }

    @Override
    public void buildUI(Group group) {

	    //Create confirm/cancel table
        new table(){{
            abottom().aleft();
            defaults().size(60f);

            //Add a cancel button, which clears the selection.
            new imagebutton("icon-cancel", 14*2f, () -> recipe = null);

            //Add an accept button, which places everything.
            new imagebutton("icon-check", 14*2f, () -> {
                for(PlaceRequest request : placement){
                    Tile tile = request.tile();

                    if(tile != null){
                        rotation = request.rotation;
                        recipe = request.recipe;
                        tryPlaceBlock(tile.x, tile.y);
                    }
                }

                removals.addAll(placement);
                placement.clear();
                selecting = false;
            });
        }}.visible(() -> isPlacing() && placement.size > 0).end();
    }

    @Override
	public void drawBottom(){

        Shaders.mix.color.set(Palette.accent);
        Graphics.shader(Shaders.mix);

        //draw removals
        for(PlaceRequest request : removals){
            Tile tile = request.tile();

            if(tile == null) continue;

            request.scale = Mathf.lerpDelta(request.scale, 0f, 0.2f);
            request.redness = Mathf.lerpDelta(request.redness, 0f, 0.2f);

            drawRequest(request);
        }

        //draw normals
        for(PlaceRequest request : placement){
            Tile tile = request.tile();

            if(tile == null) continue;

            if(validPlace(tile.x, tile.y, request.recipe.result)){
                request.scale = Mathf.lerpDelta(request.scale, 1f, 0.2f);
                request.redness = Mathf.lerpDelta(request.redness, 0f, 0.2f);
            }else{
                request.scale = Mathf.lerpDelta(request.scale, 0.5f, 0.1f);
                request.redness = Mathf.lerpDelta(request.redness, 1f, 0.2f);
            }

            drawRequest(request);
        }

        Graphics.shader();

        Draw.color(Palette.accent);

        //Draw lines
        if(lineMode){
            Tile tile = tileAt(control.gdxInput().getX(), control.gdxInput().getY());

            if(tile != null){
                NormalizeDrawResult dresult = PlaceUtils.normalizeDrawArea(recipe.result, lineStartX, lineStartY, tile.x, tile.y, true, maxLength, lineScale);

                Lines.rect(dresult.x, dresult.y, dresult.x2 - dresult.x, dresult.y2 - dresult.y);

                NormalizeResult result = PlaceUtils.normalizeArea(lineStartX, lineStartY, tile.x, tile.y, rotation, true, maxLength);

                //go through each cell and draw the block to place if valid
                for(int i = 0; i <= result.getLength(); i += recipe.result.size){
                    int x = lineStartX + i * Mathf.sign(tile.x - lineStartX) * Mathf.bool(result.isX());
                    int y = lineStartY + i * Mathf.sign(tile.y - lineStartY) * Mathf.bool(!result.isX());

                    if(!checkOverlapPlacement(x, y, recipe.result) && validPlace(x, y, recipe.result)){
                        Draw.color();

                        TextureRegion[] regions = recipe.result.getBlockIcon();

                        for(TextureRegion region : regions){
                            Draw.rect(region, x *tilesize + recipe.result.offset(), y * tilesize + recipe.result.offset(),
                                    region.getRegionWidth() * lineScale, region.getRegionHeight() * lineScale, recipe.result.rotate ? rotation * 90 : 0);
                        }
                    }else{
                        Draw.color(Palette.breakInvalid);
                        Lines.square(x*tilesize + recipe.result.offset(), y*tilesize + recipe.result.offset(), recipe.result.size * tilesize/2f);
                    }
                }
            }
        }

        Draw.color();
    }

	@Override
	public boolean touchDown(int screenX, int screenY, int pointer, int button){
		if(state.is(State.menu)) return false;

        //get tile on cursor
        Tile cursor = tileAt(screenX, screenY);

        //ignore off-screen taps
        if(cursor == null || ui.hasMouse(screenX, screenY)) return false;

        //only begin selecting if the tapped block is a request
        selecting = hasRequest(cursor) && isPlacing();

        Tile linked = cursor.target();

        //show-hide configuration fragment for blocks
        if(linked.block().isConfigurable(linked)){
            frag.config.showConfig(linked);
        }else if(!frag.config.hasConfigMouse()){
            frag.config.hideConfig();
        }

        //when tapping a build block, select it
        if(linked.block() instanceof BuildBlock){
            BuildEntity entity = linked.entity();

            player.replaceBuilding(linked.x, linked.y, linked.getRotation(), entity.recipe);
        }

        //call tapped() event
        //TODO net event for block tapped
        linked.block().tapped(linked, player);

		return false;
	}

    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button){

        //place down a line if in line mode
        if(lineMode) {
            Tile tile = tileAt(screenX, screenY);

            if(tile == null) return false;

            //normalize area
            NormalizeResult result = PlaceUtils.normalizeArea(lineStartX, lineStartY, tile.x, tile.y, rotation, true, 100);

            rotation = result.rotation;

            //place blocks on line
            for(int i = 0; i <= result.getLength(); i += recipe.result.size){
                int x = lineStartX + i * Mathf.sign(tile.x - lineStartX) * Mathf.bool(result.isX());
                int y = lineStartY + i * Mathf.sign(tile.y - lineStartY) * Mathf.bool(!result.isX());

                if(!checkOverlapPlacement(x, y, recipe.result) && validPlace(x, y, recipe.result)){
                    PlaceRequest request = new PlaceRequest(x * tilesize, y * tilesize, recipe, result.rotation);
                    request.scale = 1f;
                    placement.add(request);
                }
            }

            lineMode = false;
        }
        return false;
    }

    @Override
    public boolean longPress(float x, float y) {
        if(state.is(State.menu)) return false;

        //get tile on cursor
        Tile cursor = tileAt(x, y);

        //ignore off-screen taps
        if(cursor == null || ui.hasMouse(x, y)) return false;

        //remove request if it's there
        //long pressing enables line mode otherwise
        lineStartX = cursor.x;
        lineStartY = cursor.y;
        lineMode = true;

        Effects.effect(Fx.tapBlock, cursor.worldx() + recipe.result.offset(), cursor.worldy() + recipe.result.offset(), recipe.result.size);

	    return false;
	}

    @Override
    public boolean tap(float x, float y, int count, int button) {
        if(state.is(State.menu) || lineMode) return false;

        //get tile on cursor
        Tile cursor = tileAt(x, y);

        //ignore off-screen taps
        if(cursor == null || ui.hasMouse(x, y)) return false;

        //remove if request present
        if(hasRequest(cursor)) {
            removeRequest(getRequest(cursor));
        }else if(isPlacing() && validPlace(cursor.x, cursor.y, recipe.result) && !checkOverlapPlacement(cursor.x, cursor.y, recipe.result)){
            //add to placement queue if it's a valid place position
            placement.add(new PlaceRequest(cursor.worldx(), cursor.worldy(), recipe, rotation));
        }

        return false;
    }

	@Override
	public void update(){

        //reset state when not placing
	    if(!isPlacing()){
	        selecting = false;
	        lineMode = false;
	        removals.addAll(placement);
	        placement.clear();
        }

        if(lineMode){
	        lineScale = Mathf.lerpDelta(lineScale, 1f, 0.1f);

	        //When in line mode, pan when near screen edges automatically
            if(Gdx.input.isTouched(0) && lineMode){
                float screenX = Graphics.mouse().x, screenY = Graphics.mouse().y;

                float panX = 0, panY = 0;

                if(screenX <= edgePan){
                    panX = -(edgePan - screenX);
                }

                if(screenX >= Gdx.graphics.getWidth() - edgePan){
                    panX = (screenX - Gdx.graphics.getWidth()) + edgePan;
                }

                if(screenY <= edgePan){
                    panY = -(edgePan - screenY);
                }

                if(screenY >= Gdx.graphics.getHeight() - edgePan){
                    panY = (screenY - Gdx.graphics.getHeight()) + edgePan;
                }

                vector.set(panX, panY).scl((Core.camera.viewportWidth * Core.camera.zoom) / Gdx.graphics.getWidth());
                vector.limit(maxPanSpeed);

                player.x += vector.x;
                player.y += vector.y;
                player.targetAngle = vector.angle();
            }
        }else{
	        lineScale = 0f;
        }

        //remove place requests that have disappeared
        for(int i = removals.size - 1; i >= 0; i --){
            PlaceRequest request = removals.get(i);

            if(request.scale <= 0.0001f){
                removals.removeIndex(i);
                i --;
            }
        }
	}

    @Override
    public boolean pan(float x, float y, float deltaX, float deltaY){
        //can't pan in line mode with one finger!
        if(lineMode && !Gdx.input.isTouched(1)){
            return false;
        }

        float dx = deltaX * Core.camera.zoom / Core.cameraScale, dy = deltaY * Core.camera.zoom / Core.cameraScale;

	    if(selecting){
            for(PlaceRequest req : placement){
                req.x += dx;
                req.y -= dy;
            }
        }else{
            player.x -= dx;
            player.y += dy;
            player.targetAngle = Mathf.atan2(dx, -dy) + 180f;
        }

        return false;
    }

    @Override
    public boolean panStop(float x, float y, int pointer, int button) {
        return false;
    }

    @Override
    public boolean pinch (Vector2 initialPointer1, Vector2 initialPointer2, Vector2 pointer1, Vector2 pointer2) {
        if(pinch1.x < 0){
            pinch1.set(initialPointer1);
            pinch2.set(initialPointer2);
        }

        Vector2 vec = (vector.set(pointer1).add(pointer2).scl(0.5f)).sub(pinch1.add(pinch2).scl(0.5f));

        player.x -= vec.x*Core.camera.zoom/Core.cameraScale;
        player.y += vec.y*Core.camera.zoom/Core.cameraScale;

        pinch1.set(pointer1);
        pinch2.set(pointer2);

        return false;
    }

    @Override
    public boolean zoom(float initialDistance, float distance){
        if(initzoom < 0){
            initzoom = initialDistance;
        }

        if(Math.abs(distance - initzoom) > Unit.dp.scl(100f) && !zoomed){
            int amount = (distance > initzoom ? 1 : -1);
            renderer.scaleCamera(Math.round(Unit.dp.scl(amount)));
            initzoom = distance;
            zoomed = true;
            return true;
        }

        return false;
    }

    @Override
    public void pinchStop () {
        initzoom = -1;
        pinch2.set(pinch1.set(-1, -1));
        zoomed = false;
    }

    @Override public boolean touchDown(float x, float y, int pointer, int button) { return false; }
    @Override public boolean fling(float velocityX, float velocityY, int button) { return false; }

    class PlaceRequest{
	    float x, y;
	    Recipe recipe;
	    int rotation;

	    //animation variables
	    float scale;
	    float redness;

	    PlaceRequest(float x, float y, Recipe recipe, int rotation) {
            this.x = x;
            this.y = y;
            this.recipe = recipe;
            this.rotation = rotation;
        }

        Tile tile(){
	        return world.tileWorld(x - recipe.result.offset(), y - recipe.result.offset());
        }
    }
}
