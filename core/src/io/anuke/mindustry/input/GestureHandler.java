package io.anuke.mindustry.input;

import static io.anuke.mindustry.Vars.*;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.input.GestureDetector.GestureAdapter;
import com.badlogic.gdx.math.Vector2;

import io.anuke.mindustry.Inventory;
import io.anuke.mindustry.Vars;
import io.anuke.ucore.core.Core;
import io.anuke.ucore.scene.ui.layout.Unit;

public class GestureHandler extends GestureAdapter{
	Vector2 pinch1 = new Vector2(-1, -1), pinch2 = pinch1.cpy();
	Vector2 vector = new Vector2();
	float initzoom = -1;
	boolean zoomed = false;
	
	@Override
	public boolean longPress(float x, float y){
		/*
		Tile tile = World.cursorTile();
		player.breaktime += Timers.delta();
		if(!GameState.is(State.menu) && player.breaktime >= tile.block().breaktime){
			Effects.effect("break", tile.worldx(), tile.worldy());
			Effects.shake(3f, 1f);
			tile.setBlock(Blocks.air);
			player.breaktime = 0f;
			Sounds.play("break");
		}*/
		return false;
	}
	
	@Override
	public boolean tap (float x, float y, int count, int button) {
		if(AndroidInput.mode == PlaceMode.touch && !ui.hasMouse() && player.recipe != null &&
				Inventory.hasItems(player.recipe.requirements) && !Vars.ui.hasMouse() && !AndroidInput.brokeBlock){
			AndroidInput.mousex = x;
			AndroidInput.mousey = y;
			AndroidInput.place();
			return true;
		}
		return false;
	}
	
	@Override
	public boolean pan(float x, float y, float deltaX, float deltaY){
		if(player.recipe == null || !Inventory.hasItems(player.recipe.requirements) || AndroidInput.mode == PlaceMode.touch){
			player.x -= deltaX*Core.camera.zoom/Core.cameraScale;
			player.y += deltaY*Core.camera.zoom/Core.cameraScale;
		}else if(AndroidInput.mode == PlaceMode.cursor){
			AndroidInput.mousex += deltaX;
			AndroidInput.mousey += deltaY;
		}
		
		return false;
	}
	
	@Override
	public boolean pinch (Vector2 initialPointer1, Vector2 initialPointer2, Vector2 pointer1, Vector2 pointer2) {
		if(player.recipe == null)
			return false;
		
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
		
		if(Math.abs(distance - initzoom) > Unit.dp.inPixels(100f) && !zoomed){
			int amount = (distance > initzoom ? 1 : -1);
			renderer.scaleCamera(Math.round(Unit.dp.inPixels(amount)));
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
	
	int touches(){
		int sum = 0;
		for(int i = 0; i < 10; i ++){
			if(Gdx.input.isTouched(i)) sum++;
		}
		return sum;
	}
}
