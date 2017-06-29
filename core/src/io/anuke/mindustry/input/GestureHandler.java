package io.anuke.mindustry.input;

import static io.anuke.mindustry.Vars.*;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.input.GestureDetector.GestureAdapter;
import com.badlogic.gdx.math.Vector2;

import io.anuke.mindustry.World;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.Blocks;
import io.anuke.ucore.core.Effects;
import io.anuke.ucore.core.Sounds;
import io.anuke.ucore.util.Mathf;

public class GestureHandler extends GestureAdapter{
	Vector2 pinch1 = new Vector2(-1, -1), pinch2 = pinch1.cpy();
	Vector2 vector = new Vector2();
	float initzoom = -1;
	
	@Override
	public boolean longPress(float x, float y){
		Tile tile = World.cursorTile();
		breaktime += Mathf.delta();
		if(breaktime >= tile.block().breaktime){
			Effects.effect("break", tile.worldx(), tile.worldy());
			Effects.shake(3f, 1f);
			tile.setBlock(Blocks.air);
			breaktime = 0f;
			Sounds.play("break");
		}
		return false;
	}
	
	@Override
	public boolean pan(float x, float y, float deltaX, float deltaY){
		if(recipe == null){
			player.x -= deltaX*control.camera.zoom/control.cameraScale;
			player.y += deltaY*control.camera.zoom/control.cameraScale;
		}else{
			AndroidInput.mousex += deltaX;
			AndroidInput.mousey += deltaY;
		}
		
		return false;
	}
	
	
	
	@Override
	public boolean pinch (Vector2 initialPointer1, Vector2 initialPointer2, Vector2 pointer1, Vector2 pointer2) {
		if(recipe == null)
			return false;
		
		if(pinch1.x < 0){
			pinch1.set(initialPointer1);
			pinch2.set(initialPointer2);
		}
		
		Vector2 vec = (vector.set(pointer1).add(pointer2).scl(0.5f)).sub(pinch1.add(pinch2).scl(0.5f));
		
		player.x -= vec.x*control.camera.zoom/control.cameraScale;
		player.y += vec.y*control.camera.zoom/control.cameraScale;
		
		pinch1.set(pointer1);
		pinch2.set(pointer2);
		
		return false;
	}
	
	@Override
	public boolean zoom(float initialDistance, float distance){
		
		if(initzoom <= 0)
			initzoom = initialDistance;
		
		control.targetzoom /= (distance/initzoom);
		control.clampZoom();
		control.camera.update();
		
		initzoom = distance;
		
		return false;
	}
	
	@Override
	public void pinchStop () {
		initzoom = -1;
		pinch2.set(pinch1.set(-1, -1));
	}
	
	int touches(){
		int sum = 0;
		for(int i = 0; i < 10; i ++){
			if(Gdx.input.isTouched(i)) sum++;
		}
		return sum;
	}
}
