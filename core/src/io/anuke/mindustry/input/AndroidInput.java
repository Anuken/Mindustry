package io.anuke.mindustry.input;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.input.GestureDetector;
import com.badlogic.gdx.math.Vector2;
import io.anuke.mindustry.core.GameState.State;
import io.anuke.mindustry.entities.Player;
import io.anuke.mindustry.net.Net;
import io.anuke.mindustry.net.NetEvents;
import io.anuke.mindustry.world.Tile;
import io.anuke.ucore.core.Graphics;
import io.anuke.ucore.core.Inputs;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.scene.ui.layout.Unit;
import io.anuke.ucore.util.Mathf;

import static io.anuke.mindustry.Vars.*;

public class AndroidInput extends InputHandler{
	public float lmousex, lmousey;
	public float mousex, mousey;
	public boolean brokeBlock = false;
	public boolean placing = false;
	
	private boolean enableHold = false;
	private float warmup;
	private float warmupDelay = 20;
	
	public AndroidInput(Player player){
	    super(player);
		Inputs.addProcessor(new GestureDetector(20, 0.5f, 2, 0.15f, new GestureHandler(this)));
	}
	
	@Override public float getCursorEndX(){ return Gdx.input.getX(0); }
	@Override public float getCursorEndY(){ return Gdx.input.getY(0); }
	@Override public float getCursorX(){ return mousex; }
	@Override public float getCursorY(){ return mousey; }
	@Override public boolean drawPlace(){ return (placing && !brokeBlock) || (placeMode.pan && recipe != null); }

	@Override
	public boolean touchUp(int screenX, int screenY, int pointer, int button){
		if(brokeBlock){
			brokeBlock = false;
			return false;
		}
		
		if(placing && pointer == 0 && !placeMode.pan && !breaking()){
			placeMode.released(this, getBlockX(), getBlockY(), getBlockEndX(), getBlockEndY());
		}else if(pointer == 0 && !breakMode.pan && breaking() && drawPlace()){
			breakMode.released(this, getBlockX(), getBlockY(), getBlockEndX(), getBlockEndY());
		}

		placing = false;
		return false;
	}

	@Override
	public boolean touchDown(int screenX, int screenY, int pointer, int button){
		if(ui.hasMouse()){
			brokeBlock = true;
			return false;
		}

		lmousex = screenX;
		lmousey = screenY;
		
		if((!placeMode.pan || breaking()) && pointer == 0){
			mousex = screenX;
			mousey = screenY;
		}

		placing = pointer == 0;
		
		warmup = 0;

		if(!state.is(State.menu)){
			Tile cursor = world.tile(Mathf.scl2(Graphics.mouseWorld().x, tilesize), Mathf.scl2(Graphics.mouseWorld().y, tilesize));
			if(cursor != null && !ui.hasMouse(screenX, screenY)){
				Tile linked = cursor.isLinked() ? cursor.getLinked() : cursor;
				if(linked != null && linked.block().isConfigurable(linked)){
					frag.config.showConfig(linked);
				}else if(!frag.config.hasConfigMouse()){
					frag.config.hideConfig();
				}

				if(linked != null) {
					linked.block().tapped(linked);
					if(Net.active()) NetEvents.handleBlockTap(linked);
				}
			}
		}
		return false;
	}
	
	@Override
	public void resetCursor(){
		mousex = Gdx.graphics.getWidth()/2;
		mousey = Gdx.graphics.getHeight()/2;
	}

	public Tile selected(){
		Vector2 vec = Graphics.world(mousex, mousey);
		return world.tile(Mathf.scl2(vec.x, tilesize), Mathf.scl2(vec.y, tilesize));
	}

	@Override
	public void update(){
		enableHold = breakMode == PlaceMode.holdDelete;

		float xa = Inputs.getAxis("move_x");
		float ya = Inputs.getAxis("move_y");
		if(Math.abs(xa) < controllerMin) xa = 0;
		if(Math.abs(ya) < controllerMin) ya = 0;

		player.x += xa * 4f;
		player.y += ya * 4f;

		rotation += Inputs.getAxis("rotate_alt");
		rotation += Inputs.getAxis("rotate");
		rotation = Mathf.mod(rotation, 4);

		if(enableHold && Gdx.input.isTouched(0) && Mathf.near2d(lmousex, lmousey, Gdx.input.getX(0), Gdx.input.getY(0), Unit.dp.scl(50))
				&& !ui.hasMouse()){
			warmup += Timers.delta();

			float lx = mousex, ly = mousey;

			mousex = Gdx.input.getX(0);
			mousey = Gdx.input.getY(0);

			Tile sel = selected();

			if(sel == null)
				return;

			mousex = lx;
			mousey = ly;
		}else{
			warmup = 0;
			breaktime = 0;

			mousex = Mathf.clamp(mousex, 0, Gdx.graphics.getWidth());
			mousey = Mathf.clamp(mousey, 0, Gdx.graphics.getHeight());
		}
	}

	public boolean breaking(){
		return recipe == null;
	}
}
