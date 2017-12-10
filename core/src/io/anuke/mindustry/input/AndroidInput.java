package io.anuke.mindustry.input;

import static io.anuke.mindustry.Vars.*;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.math.Vector2;

import io.anuke.mindustry.Vars;
import io.anuke.mindustry.core.GameState;
import io.anuke.mindustry.core.GameState.State;
import io.anuke.mindustry.resource.ItemStack;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.types.Configurable;
import io.anuke.ucore.core.Graphics;
import io.anuke.ucore.core.Timers;
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
	public boolean keyDown(int keycode){
		if(keycode == Keys.E){
			place();
		}
		return false;
	}

	@Override
	public boolean touchUp(int screenX, int screenY, int pointer, int button){
		brokeBlock = false;
		return false;
	}

	@Override
	public boolean touchDown(int screenX, int screenY, int pointer, int button){
		ui.hideTooltip();
		if(pointer == 0){
			lmousex = screenX;
			lmousey = screenY;
		}
		warmup = 0;

		if(!GameState.is(State.menu)){
			Tile cursor = Vars.world.tile(Mathf.scl2(Graphics.mouseWorld().x, tilesize), Mathf.scl2(Graphics.mouseWorld().y, tilesize));
			if(cursor != null && !Vars.ui.hasMouse()){
				Tile linked = cursor.isLinked() ? cursor.getLinked() : cursor;
				if(linked != null && linked.block() instanceof Configurable){
					Vars.ui.showConfig(linked);
				}else if(!Vars.ui.hasConfigMouse()){
					Vars.ui.hideConfig();
				}
			}
		}
		return false;
	}

	public static Tile selected(){
		Vector2 vec = Graphics.world(mousex, mousey);
		return Vars.world.tile(Mathf.scl2(vec.x, tilesize), Mathf.scl2(vec.y, tilesize));
	}

	public static void breakBlock(){
		Tile tile = selected();
		player.breaktime += Timers.delta();

		if(player.breaktime >= tile.block().breaktime){
			brokeBlock = true;
			Vars.world.breakBlock(tile.x, tile.y);
			player.breaktime = 0f;
		}
	}

	public static void place(){
		Vector2 vec = Graphics.world(mousex, mousey);

		int tilex = Mathf.scl2(vec.x, tilesize);
		int tiley = Mathf.scl2(vec.y, tilesize);

		if(player.recipe != null && Vars.control.hasItems(player.recipe.requirements) && Vars.world.validPlace(tilex, tiley, player.recipe.result)){

			Vars.world.placeBlock(tilex, tiley, player.recipe.result, player.rotation);

			for(ItemStack stack : player.recipe.requirements){
				Vars.control.removeItem(stack);
			}
		}
	}

	public static void doInput(){

		if(Gdx.input.isTouched(0) && Mathf.near2d(lmousex, lmousey, Gdx.input.getX(0), Gdx.input.getY(0), Unit.dp.inPixels(50))
				&& !ui.hasMouse()){
			warmup += Timers.delta();

			float lx = mousex, ly = mousey;

			mousex = Gdx.input.getX(0);
			mousey = Gdx.input.getY(0);

			Tile sel = selected();

			if(sel == null)
				return;

			if(warmup > warmupDelay && Vars.world.validBreak(sel.x, sel.y)){
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
		for(int i = 0; i < 10; i++){
			if(Gdx.input.isTouched(i))
				sum++;
		}
		return sum;
	}
}
