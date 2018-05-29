package io.anuke.mindustry.input;

import com.badlogic.gdx.input.GestureDetector;
import com.badlogic.gdx.input.GestureDetector.GestureListener;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import io.anuke.mindustry.core.GameState.State;
import io.anuke.mindustry.entities.Player;
import io.anuke.mindustry.graphics.Palette;
import io.anuke.mindustry.type.Recipe;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Tile;
import io.anuke.ucore.core.Core;
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

    //gesture data
    private Vector2 pinch1 = new Vector2(-1, -1), pinch2 = pinch1.cpy();
    private Vector2 vector = new Vector2();
    private float initzoom = -1;
    private boolean zoomed = false;

    /**List of currently selected tiles to place.*/
    private Array<PlaceRequest> placement = new Array<>();
    /**Whether or not the player is currently shifting all placed tiles.*/
    private boolean selecting;
	
	public AndroidInput(Player player){
	    super(player);
		Inputs.addProcessor(new GestureDetector(20, 0.5f, 2, 0.15f, this));
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

    @Override
    public void buildUI(Group group) {

	    //Create confirm/cancel table
        new table(){{
            abottom().aleft();
            defaults().size(60f);

            //Add a cancel button, which clears the selection.
            new imagebutton("icon-cancel", 14*2f, () -> {
                placement.clear();
                selecting = false;
            });

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

                placement.clear();
                selecting = false;
            });
        }}.visible(this::isPlacing).end();
    }

    @Override
	public void draw(){
        Draw.color(Palette.accent);

        //Draw all placement requests as squares
        for(PlaceRequest request : placement){
            Tile tile = request.tile();
            if(tile == null) continue;
            Lines.square(tile.worldx() + request.recipe.result.offset(), tile.worldy() + request.recipe.result.offset(),
                    request.recipe.result.size * tilesize/2f);
        }

        Draw.color();
    }

	@Override
	public boolean touchDown(int screenX, int screenY, int pointer, int button){
		if(state.is(State.menu)) return false;

        //get tile on cursor
        Tile cursor = world.tile(Mathf.scl2(Graphics.mouseWorld().x, tilesize), Mathf.scl2(Graphics.mouseWorld().y, tilesize));

        //ignore off-screen taps
        if(cursor == null || ui.hasMouse(screenX, screenY)) return false;

        //only begin selecting if the tapped block is a request
        selecting = hasRequest(cursor);

        Tile linked = cursor.target();

        //show-hide configuration fragment for blocks
        if(linked.block().isConfigurable(linked)){
            frag.config.showConfig(linked);
        }else if(!frag.config.hasConfigMouse()){
            frag.config.hideConfig();
        }

        //call tapped() event
        //TODO net event for block tapped
        linked.block().tapped(linked, player);

		return false;
	}

    @Override
    public boolean longPress(float x, float y) {
        if(state.is(State.menu)) return false;

        //get tile on cursor
        Tile cursor = world.tile(Mathf.scl2(Graphics.mouseWorld().x, tilesize), Mathf.scl2(Graphics.mouseWorld().y, tilesize));

        //ignore off-screen taps
        if(cursor == null || ui.hasMouse(x, y)) return false;

        //remove request if it's there
        if(hasRequest(cursor)){
            placement.removeValue(getRequest(cursor), true);
        }

	    return false;
	}

    @Override
    public boolean tap(float x, float y, int count, int button) {
        if(state.is(State.menu)) return false;

        //get tile on cursor
        Tile cursor = world.tile(Mathf.scl2(Graphics.mouseWorld().x, tilesize), Mathf.scl2(Graphics.mouseWorld().y, tilesize));

        //ignore off-screen taps
        if(cursor == null || ui.hasMouse(x, y)) return false;

        //add to placement queue if it's a valid place position
        if(isPlacing() && validPlace(cursor.x, cursor.y, recipe.result) && !checkOverlapPlacement(cursor.x, cursor.y, recipe.result)){
            placement.add(new PlaceRequest(cursor.worldx(), cursor.worldy(), recipe, rotation));
        }

        return false;
    }

	@Override
	public void update(){

	    if(!isPlacing()){
	        selecting = false;
	        placement.clear();
        }
	}

    @Override
    public boolean pan(float x, float y, float deltaX, float deltaY){
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
