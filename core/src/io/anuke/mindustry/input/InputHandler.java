package io.anuke.mindustry.input;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.Vector2;
import io.anuke.mindustry.entities.BlockPlacer.PlaceRequest;
import io.anuke.mindustry.entities.ItemAnimationEffect;
import io.anuke.mindustry.entities.Player;
import io.anuke.mindustry.net.Net;
import io.anuke.mindustry.net.NetEvents;
import io.anuke.mindustry.resource.ItemStack;
import io.anuke.mindustry.resource.Recipe;
import io.anuke.mindustry.ui.fragments.OverlayFragment;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Placement;
import io.anuke.mindustry.world.Tile;
import io.anuke.ucore.core.Core;
import io.anuke.ucore.core.Graphics;
import io.anuke.ucore.core.Inputs;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.scene.ui.layout.Unit;
import io.anuke.ucore.util.Angles;
import io.anuke.ucore.util.Mathf;
import io.anuke.ucore.util.Translator;

import static io.anuke.mindustry.Vars.*;

public abstract class InputHandler extends InputAdapter{
    public final static float playerSelectRange = Unit.dp.scl(60f);
    private final static Translator stackTrns = new Translator();

	public float breaktime = 0;
	public Recipe recipe;
	public int rotation;
	public Player player;
	public PlaceMode placeMode = mobile ? PlaceMode.cursor : PlaceMode.hold;
	public PlaceMode breakMode = mobile ? PlaceMode.none : PlaceMode.holdDelete;
	public PlaceMode lastPlaceMode = placeMode;
	public PlaceMode lastBreakMode = breakMode;
	public boolean droppingItem, transferring;
	public boolean shooting;
	public OverlayFragment frag = new OverlayFragment(this);

	public InputHandler(Player player){
	    this.player = player;
	    Timers.run(1f, () -> frag.build(Core.scene.getRoot()));
    }

	public abstract void update();
	public abstract float getCursorX();
	public abstract float getCursorY();
	public abstract float getCursorEndX();
	public abstract float getCursorEndY();
	public float getMouseX(){ return Gdx.input.getX(); };
    public float getMouseY(){ return Gdx.input.getY(); };
    public int getBlockX(){ return Mathf.sclb(Graphics.world(getCursorX(), getCursorY()).x, tilesize, round2()); }
	public int getBlockY(){ return Mathf.sclb(Graphics.world(getCursorX(), getCursorY()).y, tilesize, round2()); }
	public int getBlockEndX(){ return Mathf.sclb(Graphics.world(getCursorEndX(), getCursorEndY()).x, tilesize, round2()); }
	public int getBlockEndY(){ return Mathf.sclb(Graphics.world(getCursorEndX(), getCursorEndY()).y, tilesize, round2()); }
	public void resetCursor(){}
	public boolean isCursorVisible(){ return false; }

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

	public boolean drawPlace(){ return true; }
	
	public boolean onConfigurable(){
		Tile tile = world.tile(getBlockX(), getBlockY());
		return tile != null && (tile.block().isConfigurable(tile) || (tile.isLinked() && tile.getLinked().block().isConfigurable(tile)));
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

					new ItemAnimationEffect(stack.item,
							player.x + Angles.trnsx(rotation + 180f, backTrns), player.y + Angles.trnsy(rotation + 180f, backTrns),
							tile.drawx() + stackTrns.x, tile.drawy() + stackTrns.y, () -> {

						tile.block().handleStack(stack.item, removed, tile, player);
						remaining[1] -= removed;

						if(end && remaining[1] > 0) {
							tile.block().handleStack(stack.item, remaining[1], tile, player);
						}
					}).add();

					stack.amount -= removed;
					remaining[0] -= removed;

					if(end){
						stack.amount -= remaining[0];
						if(clear) player.inventory.clear();
						transferring = false;
					}
				});
			}
		}else{
			Vector2 vec = Graphics.screen(player.x, player.y);

			if(vec.dst(Gdx.input.getX(), Gdx.graphics.getHeight() - Gdx.input.getY()) > playerSelectRange) {
				int sent = Mathf.clamp(stack.amount / 4, 1, 8);
				float backTrns = 3f;
				for (int i = 0; i < sent; i++) {
					Timers.run(i, () -> {
						float x = player.x + Angles.trnsx(rotation + 180f, backTrns),
								y = player.y + Angles.trnsy(rotation + 180f, backTrns);

						ItemAnimationEffect e = new ItemAnimationEffect(stack.item,
								x, y, x + Mathf.range(20f), y + Mathf.range(20f), () -> {}).add();
						e.interp = Interpolation.pow3Out;
						e.endSize = 0.5f;
						e.lifetime = 20;
					});
				}
				player.inventory.clear();
			}
		}
	}

	public boolean cursorNear(){
		return true;
	}
	
	public boolean tryPlaceBlock(int x, int y, boolean sound){
		if(recipe != null && 
				validPlace(x, y, recipe.result) && !ui.hasMouse() && cursorNear() &&
				state.inventory.hasItems(recipe.requirements)){
			
			placeBlock(x, y, recipe, rotation, true, sound);
			
			for(ItemStack stack : recipe.requirements){
				state.inventory.removeItem(stack);
			}

			return true;
		}
		return false;
	}
	
	public boolean tryDeleteBlock(int x, int y, boolean sound){
		if(cursorNear() && validBreak(x, y)){
			breakBlock(x, y, sound);
			return true;
		}
		return false;
	}
	
	public boolean round2(){
		return !(recipe != null && recipe.result.isMultiblock() && recipe.result.size % 2 == 0);
	}
	
	public boolean validPlace(int x, int y, Block type){
        for(Tile tile : state.teams.get(player.team).cores){
            if(tile.distanceTo(x * tilesize, y * tilesize) < coreBuildRange){
                return Placement.validPlace(player.team, x, y, type, rotation) &&
                        Vector2.dst(player.x, player.y, x * tilesize, y * tilesize) < Player.placeDistance;
            }
        }

        return false;
	}
	
	public boolean validBreak(int x, int y){
		return Placement.validBreak(player.team, x, y);
	}
	
	public void placeBlock(int x, int y, Recipe recipe, int rotation, boolean effects, boolean sound){
        //todo multiplayer support
        player.addPlaceBlock(new PlaceRequest(x, y, rotation, recipe));
        /*
		if(!Net.client()){ //is server or singleplayer
			threads.run(() -> Placement.placeBlock(player.team, x, y, recipe, rotation, effects, sound));
		}

		if(Net.active()){
			NetEvents.handlePlace(player, x, y, recipe, rotation);
		}

		//todo fix this, call placed()
		if(!Net.client()){
			//Tile tile = world.tile(x, y);
			//if(tile != null) threads.run(() -> result.placed(tile));
		}*/
	}

	public void breakBlock(int x, int y, boolean sound){
		if(!Net.client()){
			threads.run(() -> Placement.breakBlock(player.team, x, y, true, sound));
		}

		if(Net.active()){
			NetEvents.handleBreak(x, y);
		}
	}
}
