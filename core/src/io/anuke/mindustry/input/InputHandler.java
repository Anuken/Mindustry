package io.anuke.mindustry.input;

import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.math.Vector2;
import io.anuke.mindustry.entities.BlockBuilder.BuildRequest;
import io.anuke.mindustry.entities.ItemTransfer;
import io.anuke.mindustry.entities.Player;
import io.anuke.mindustry.type.ItemStack;
import io.anuke.mindustry.type.Recipe;
import io.anuke.mindustry.ui.fragments.OverlayFragment;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Build;
import io.anuke.mindustry.world.Tile;
import io.anuke.ucore.core.Core;
import io.anuke.ucore.core.Graphics;
import io.anuke.ucore.core.Inputs;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.scene.Group;
import io.anuke.ucore.scene.ui.layout.Unit;
import io.anuke.ucore.util.Angles;
import io.anuke.ucore.util.Mathf;
import io.anuke.ucore.util.Translator;

import static io.anuke.mindustry.Vars.*;

public abstract class InputHandler extends InputAdapter{
	/**Used for dropping items.*/
    final float playerSelectRange = Unit.dp.scl(40f);
	/**Maximum line length.*/
	final int maxLength = 100;
    final Translator stackTrns = new Translator();

	public final Player player;
	public final OverlayFragment frag = new OverlayFragment(this);

	public Recipe recipe;
	public int rotation;
	public boolean droppingItem, transferring;
	public boolean shooting;

	public InputHandler(Player player){
	    this.player = player;
	    Timers.run(1f, () -> frag.build(Core.scene.getRoot()));
    }

    //methods to override

	public void update(){

	}

	public float getMouseX(){
		return control.gdxInput().getX();
	}

    public float getMouseY(){
    	return control.gdxInput().getY();
    }

	public void resetCursor(){

	}

	public boolean isCursorVisible(){
		return false;
	}

	public void buildUI(Group group){

	}

	public void updateController(){

	}

	public void drawBottom(){

	}

	public void drawTop(){

	}

	/**Handles tile tap events that are not platform specific.*/
	public boolean tileTapped(Tile tile){
		tile = tile.target();

		boolean consumed = false;

		//check if tapped block is configurable
		if(tile.block().isConfigurable(tile)){
			consumed = true;
			if((!frag.config.isShown() //if the config fragment is hidden, show
					//alternatively, the current selected block can 'agree' to switch config tiles
					|| frag.config.getSelectedTile().block().onConfigureTileTapped(frag.config.getSelectedTile(), tile))) {
				frag.config.showConfig(tile);
			}
			//otherwise...
		}else if(!frag.config.hasConfigMouse()){ //make sure a configuration fragment isn't on the cursor
			//then, if it's shown and the current block 'agrees' to hide, hide it.
			if(frag.config.isShown() && frag.config.getSelectedTile().block().onConfigureTileTapped(frag.config.getSelectedTile(), tile)) {
				consumed = true;
				frag.config.hideConfig();
			}
		}

		//TODO network event!
		//call tapped event
		if(tile.block().tapped(tile, player)){
			consumed = true;
		}

		return consumed;
	}

	//utility methods

	/**Returns the tile at the specified MOUSE coordinates.*/
	Tile tileAt(float x, float y){
		Vector2 vec = Graphics.world(x, y);
		return world.tileWorld(vec.x, vec.y);
	}

	public boolean isPlacing(){
		return recipe != null;
	}

	public float mouseAngle(float x, float y){
        return Graphics.world(getMouseX(), getMouseY()).sub(x, y).angle();
    }

	public void remove(){
	    Inputs.removeProcessor(this);
	    frag.remove();
    }

	public boolean canShoot(){
		return recipe == null && !ui.hasMouse() && !onConfigurable() && !isDroppingItem();
	}

	public boolean isShooting(){
		return shooting;
	}
	
	public boolean onConfigurable(){
		return false;
	}

	public boolean isDroppingItem(){
		return droppingItem;
	}

	public void dropItem(Tile tile, ItemStack stack){
		int accepted = tile.block().acceptStack(stack.item, stack.amount, tile, player);

		if(accepted > 0){
			if(transferring) return;

			transferring = true;
			boolean clear = stack.amount == accepted;

			float backTrns = 3f;

			int sent = Mathf.clamp(accepted/4, 1, 8);
			int removed = accepted/sent;
			int[] remaining = {accepted, accepted};

			for(int i = 0; i < sent; i ++){
				boolean end = i == sent-1;
				Timers.run(i * 3, () -> {
					tile.block().getStackOffset(stack.item, tile, stackTrns);

					ItemTransfer.create(stack.item,
							player.x + Angles.trnsx(rotation + 180f, backTrns), player.y + Angles.trnsy(rotation + 180f, backTrns),
							new Translator(tile.drawx() + stackTrns.x, tile.drawy() + stackTrns.y), () -> {

						tile.block().handleStack(stack.item, removed, tile, player);
						remaining[1] -= removed;

						if(end && remaining[1] > 0) {
							tile.block().handleStack(stack.item, remaining[1], tile, player);
						}
					});

					stack.amount -= removed;
					remaining[0] -= removed;

					if(end){
						stack.amount -= remaining[0];
						if(clear) player.inventory.clear();
						transferring = false;
					}
				});
			}
		}
		//TODO create drop on the ground otherwise
	}

	public boolean cursorNear(){
		return true;
	}
	
	public void tryPlaceBlock(int x, int y){
		if(recipe != null && validPlace(x, y, recipe.result, rotation) && cursorNear()){
			placeBlock(x, y, recipe, rotation);
		}
	}
	
	public void tryBreakBlock(int x, int y){
		if(cursorNear() && validBreak(x, y)){
			breakBlock(x, y);
		}
	}
	
	public boolean validPlace(int x, int y, Block type, int rotation){
        for(Tile tile : state.teams.get(player.team).cores){
            if(tile.distanceTo(x * tilesize, y * tilesize) < coreBuildRange){
                return Build.validPlace(player.team, x, y, type, rotation) &&
                        Vector2.dst(player.x, player.y, x * tilesize, y * tilesize) < Player.placeDistance;
            }
        }

        return false;
	}
	
	public boolean validBreak(int x, int y){
		return Build.validBreak(player.team, x, y);
	}
	
	public void placeBlock(int x, int y, Recipe recipe, int rotation){
        //todo multiplayer support
        player.addBuildRequest(new BuildRequest(x, y, rotation, recipe));
	}

	public void breakBlock(int x, int y){

		//todo multiplayer support
    	Tile tile = world.tile(x, y).target();
		player.addBuildRequest(new BuildRequest(tile.x, tile.y));
	}

}
