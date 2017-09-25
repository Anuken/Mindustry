package io.anuke.mindustry.input;

import static io.anuke.mindustry.Vars.*;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.math.Vector2;

import io.anuke.mindustry.Inventory;
import io.anuke.mindustry.resource.ItemStack;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.World;
import io.anuke.mindustry.world.blocks.Blocks;
import io.anuke.mindustry.world.blocks.ProductionBlocks;
import io.anuke.ucore.core.*;
import io.anuke.ucore.scene.ui.layout.Unit;
import io.anuke.ucore.util.Mathf;

public class AndroidInput extends InputAdapter{
	public static float mousex, mousey;
	public static PlaceMode mode = PlaceMode.cursor;
	public static boolean brokeBlock = false;
	private static float lmousex, lmousey;
	private static float warmup;
	private static float warmupDelay = 20;
	
	@Override
	public boolean keyDown (int keycode) {
		if(keycode == Keys.E){
			place();
		}
		return false;
	}
	
	@Override
	public boolean touchUp (int screenX, int screenY, int pointer, int button) {
		brokeBlock = false;
		return false;
	}
	
	@Override
	public boolean touchDown (int screenX, int screenY, int pointer, int button) {
		ui.hideTooltip();
		if(pointer == 0){
			lmousex = screenX;
			lmousey = screenY;
		}
		warmup = 0;
		return false;
	}
	
	public static Tile selected(){
		Vector2 vec = Graphics.world(mousex, mousey);
		return World.tile(Mathf.scl2(vec.x, tilesize), Mathf.scl2(vec.y, tilesize));
	}
	
	public static void breakBlock(){
		Tile tile = selected();
		player.breaktime += Timers.delta();
		
		if(player.breaktime >= tile.block().breaktime){
			brokeBlock = true;
			if(tile.block().drops != null){
				Inventory.addItem(tile.block().drops.item, tile.block().drops.amount);
			}
			
			Effects.effect("break", tile.worldx(), tile.worldy());
			Effects.shake(3f, 1f);
			tile.setBlock(Blocks.air);
			player.breaktime = 0f;
			Sounds.play("break");
		}
	}
	
	public static void place(){
		Vector2 vec = Graphics.world(mousex, mousey);
		
		int tilex = Mathf.scl2(vec.x, tilesize);
		int tiley = Mathf.scl2(vec.y, tilesize);
		
		if(player.recipe != null && 
				World.validPlace(tilex, tiley, player.recipe.result)){
			
			Tile tile = World.tile(tilex, tiley);
			
			if(tile == null)
				return; //just in case
			
			tile.setBlock(player.recipe.result);
			tile.rotation = (byte)player.rotation;

			Effects.effect("place", tilex*tilesize, tiley*tilesize);
			Effects.shake(2f, 2f);
			Sounds.play("place");

			for(ItemStack stack : player.recipe.requirements){
				Inventory.removeItem(stack);
			}
		}
	}
	
	public static void doInput(){
		if(Gdx.input.isTouched(0) 
				&& Mathf.near2d(lmousex, lmousey, Gdx.input.getX(0), Gdx.input.getY(0), Unit.dp.inPixels(50)) 
				&& !ui.hasMouse() /*&& (player.recipe == null || mode == PlaceMode.touch)*/){
			warmup += Timers.delta();
			
			float lx = mousex, ly = mousey;
			
			mousex = Gdx.input.getX(0);
			mousey = Gdx.input.getY(0);
			
			Tile sel = selected();
			
			if(sel == null) return;
			
			if(warmup > warmupDelay && sel.block() != ProductionBlocks.core && sel.breakable()){
				player.breaktime += Timers.delta();
				
				if(player.breaktime > selected().block().breaktime){
					breakBlock();
					player.breaktime = 0;
				}
			}
			
			mousex = lx;
			mousey = ly;
		}else{
			warmup = 0;
			//lmousex = Gdx.input.getX(0);
			//lmousey = Gdx.input.getY(0);
			player.breaktime = 0;
			
			mousex = Mathf.clamp(mousex, 0, Gdx.graphics.getWidth());
			mousey = Mathf.clamp(mousey, 0, Gdx.graphics.getHeight());
		}
	}
	
	public static int touches(){
		int sum = 0;
		for(int i = 0; i < 10; i ++){
			if(Gdx.input.isTouched(i))
				sum ++;
		}
		return sum;
	}
}
