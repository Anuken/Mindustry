package io.anuke.mindustry.input;

import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.math.Vector2;
import io.anuke.annotations.Annotations.Loc;
import io.anuke.annotations.Annotations.Remote;
import io.anuke.mindustry.content.blocks.Blocks;
import io.anuke.mindustry.entities.Player;
import io.anuke.mindustry.entities.effect.ItemDrop;
import io.anuke.mindustry.entities.effect.ItemTransfer;
import io.anuke.mindustry.entities.traits.BuilderTrait.BuildRequest;
import io.anuke.mindustry.gen.CallBlocks;
import io.anuke.mindustry.gen.CallEntity;
import io.anuke.mindustry.net.In;
import io.anuke.mindustry.net.Net;
import io.anuke.mindustry.net.ValidateException;
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
import io.anuke.ucore.util.Angles;
import io.anuke.ucore.util.Mathf;
import io.anuke.ucore.util.Translator;

import static io.anuke.mindustry.Vars.*;

public abstract class InputHandler extends InputAdapter{
	/**Used for dropping items.*/
    final static float playerSelectRange = mobile ? 17f : 11f;
	/**Maximum line length.*/
	final static int maxLength = 100;
    final static Translator stackTrns = new Translator();
    /**Distance on the back from where items originate.*/
	final static float backTrns = 3f;

	public final Player player;
	public final OverlayFragment frag = new OverlayFragment(this);

	public Recipe recipe;
	public int rotation;
	public boolean droppingItem;

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

	public void drawOutlined(){

	}

	public void drawTop(){

	}

	public boolean isDrawing(){
		return false;
	}

	/**Handles tile tap events that are not platform specific.*/
	boolean tileTapped(Tile tile){
		tile = tile.target();

		boolean consumed = false;
		boolean showedInventory = false;

		//check if tapped block is configurable
		if(tile.block().configurable && tile.getTeam() == player.getTeam()){
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

		//call tapped event
		CallBlocks.onTileTapped(player, tile);

		//consume tap event if necessary
		if(tile.getTeam() == player.getTeam() && tile.block().consumesTap){
			consumed = true;
		}else if(tile.getTeam() == player.getTeam() && tile.block().synthetic() && tile.block().hasItems && !consumed){
			frag.inv.showFor(tile);
			consumed = true;
			showedInventory = true;
		}

		if(!showedInventory){
			frag.inv.hide();
		}

		return consumed;
	}

	/**Tries to select the player to drop off items, returns true if successful.*/
	boolean tryTapPlayer(float x, float y){
		if(canTapPlayer(x, y)){
			droppingItem = true;
			return true;
		}
		return false;
	}

	boolean canTapPlayer(float x, float y){
		return Vector2.dst(x, y, player.x, player.y) <= playerSelectRange && player.inventory.hasItem();
	}

	/**Tries to begin mining a tile, returns true if successful.*/
	boolean tryBeginMine(Tile tile){
		if(canMine(tile)){
			//if a block is clicked twice, reset it
			player.setMineTile(player.getMineTile() == tile ? null : tile);
			return true;
		}
		return false;
	}

	boolean canMine(Tile tile){
		return tile.floor().drops != null && tile.floor().drops.item.hardness <= player.mech.drillPower
				&& player.inventory.canAcceptItem(tile.floor().drops.item)
				&& tile.block() == Blocks.air && player.distanceTo(tile.worldx(), tile.worldy()) <= Player.mineDistance;
	}

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
	
	public boolean onConfigurable(){
		return false;
	}

	public boolean isDroppingItem(){
		return droppingItem;
	}

	public void tryDropItems(Tile tile, float x, float y){
		if(!droppingItem || !player.inventory.hasItem() || !tile.block().hasItems || canTapPlayer(x, y)){
			droppingItem = false;
			return;
		}

		droppingItem = false;

		ItemStack stack = player.inventory.getItem();

		if(tile.block().acceptStack(stack.item, stack.amount, tile, player) > 0){
			CallBlocks.transferInventory(player, tile);
		}else{
			CallEntity.dropItem(player, player.angleTo(x, y));
		}
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
        for(Tile tile : state.teams.get(player.getTeam()).cores){
            if(tile.distanceTo(x * tilesize, y * tilesize) < coreBuildRange){
                return Build.validPlace(player.getTeam(), x, y, type, rotation) &&
                        Vector2.dst(player.x, player.y, x * tilesize, y * tilesize) < Player.placeDistance;
            }
        }

        return false;
	}
	
	public boolean validBreak(int x, int y){
		return Build.validBreak(player.getTeam(), x, y);
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

	@Remote(targets = Loc.client, called = Loc.both, in = In.entities)
	public static void setShooting(Player player, boolean on){
		player.isShooting = on;
	}

	@Remote(targets = Loc.both, called = Loc.server, in = In.entities)
	public static void dropItem(Player player, float angle){
		if(Net.server() && !player.inventory.hasItem()){
			throw new ValidateException(player, "Player cannot drop an item.");
		}

		ItemDrop.create(player.inventory.getItem().item, player.inventory.getItem().amount, player.x, player.y, angle);
		player.inventory.clearItem();
	}

	@Remote(targets = Loc.both, forward = true, called = Loc.server, in = In.blocks)
	public static void transferInventory(Player player, Tile tile){
		if(Net.server() && (!player.inventory.hasItem() || player.isTransferring)){
			throw new ValidateException(player, "Player cannot transfer an item.");
		}

		player.isTransferring = true;

		ItemStack stack = player.inventory.getItem();
		int accepted = tile.block().acceptStack(stack.item, stack.amount, tile, player);

		boolean clear = stack.amount == accepted;
		int sent = Mathf.clamp(accepted/4, 1, 8);
		int removed = accepted/sent;
		int[] remaining = {accepted, accepted};

		for(int i = 0; i < sent; i ++){
			boolean end = i == sent-1;
			Timers.run(i * 3, () -> {
				tile.block().getStackOffset(stack.item, tile, stackTrns);

				ItemTransfer.create(stack.item,
						player.x + Angles.trnsx(player.rotation + 180f, backTrns), player.y + Angles.trnsy(player.rotation + 180f, backTrns),
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
					if(clear){
						player.inventory.clearItem();
					}
					player.isTransferring = false;
				}
			});
		}

		//ItemDrop.create(player.inventory.getItem().item, player.inventory.getItem().amount, player.x, player.y, angle);
		//player.inventory.clearItem();
	}

	@Remote(targets = Loc.both, called = Loc.server, forward = true, in = In.blocks)
	public static void onTileTapped(Player player, Tile tile){
		tile.block().tapped(tile, player);
	}

}
