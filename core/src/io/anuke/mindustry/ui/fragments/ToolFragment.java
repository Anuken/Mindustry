package io.anuke.mindustry.ui.fragments;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Align;
import static io.anuke.mindustry.Vars.*;
import io.anuke.mindustry.core.GameState.State;
import io.anuke.mindustry.input.InputHandler;
import io.anuke.mindustry.input.PlaceMode;
import io.anuke.ucore.core.Core;
import io.anuke.ucore.core.Graphics;
import io.anuke.ucore.scene.ui.layout.Table;

public class ToolFragment implements Fragment{
	private Table tools;
	public int px, py, px2, py2;
	public boolean confirming;
	
	public void build(){
		InputHandler input = control.input();
		
		float isize = 14*3;
		
		tools = new Table();
		
		tools.addImageButton("icon-cancel", isize, () -> {
			if(input.placeMode == PlaceMode.areaDelete && confirming){
				confirming = false;
			}else{
				input.recipe = null;
			}
		});
		
		tools.addImageButton("icon-rotate", isize, () -> {
			input.rotation++;
			input.rotation %= 4;
		});
		
		tools.addImageButton("icon-check", isize, () -> {
			if(input.placeMode == PlaceMode.areaDelete && confirming){
				input.placeMode.released(px, py, px2, py2);
				confirming = false;
			}else{
				input.placeMode.tapped(control.input().getBlockX(), control.input().getBlockY());
			}
		});
		
		Core.scene.add(tools);
		
		tools.setVisible(() ->
			!state.is(State.menu) && mobile && ((input.recipe != null && state.inventory.hasItems(input.recipe.requirements) &&
			input.placeMode == PlaceMode.cursor) || confirming)
		);
		
		tools.update(() -> {
			if(confirming){
				Vector2 v = Graphics.screen((px + px2)/2f * tilesize, Math.min(py, py2) * tilesize - tilesize*1.5f);
				tools.setPosition(v.x, v.y, Align.top);

			}else{
				tools.setPosition(control.input().getCursorX(),
						Gdx.graphics.getHeight() - control.input().getCursorY() - 15*Core.cameraScale, Align.top);
			}

			if(input.placeMode != PlaceMode.areaDelete){
				confirming = false;
			}
		});
	}
}
