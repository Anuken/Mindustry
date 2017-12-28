package io.anuke.mindustry.ui.fragments;

import static io.anuke.mindustry.Vars.*;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Align;

import io.anuke.mindustry.Vars;
import io.anuke.mindustry.core.GameState;
import io.anuke.mindustry.core.GameState.State;
import io.anuke.mindustry.input.PlaceMode;
import io.anuke.ucore.core.Core;
import io.anuke.ucore.core.Graphics;
import io.anuke.ucore.scene.ui.layout.Table;

public class ToolFragment implements Fragment{
	private Table tools;
	public int px, py, px2, py2;
	public boolean confirming;
	
	public void build(){
		float isize = 14*3;
		
		tools = new Table();
		
		tools.addIButton("icon-cancel", isize, () -> {
			if(player.placeMode == PlaceMode.areaDelete && confirming){
				confirming = false;
			}else{
				player.recipe = null;
			}
		});
		
		tools.addIButton("icon-rotate", isize, () -> {
			player.rotation ++;
			player.rotation %= 4;
		});
		
		tools.addIButton("icon-check", isize, () -> {
			if(player.placeMode == PlaceMode.areaDelete && confirming){
				player.placeMode.released(px, py, px2, py2);
				confirming = false;
			}else{
				player.placeMode.tapped(control.getInput().getBlockX(), control.getInput().getBlockY());
			}
		});
		
		Core.scene.add(tools);
		
		tools.setVisible(() ->
			!GameState.is(State.menu) && android && ((player.recipe != null && control.hasItems(player.recipe.requirements) &&
			player.placeMode == PlaceMode.cursor) || confirming)
		);
		
		tools.update(() -> {
			if(confirming){
				Vector2 v = Graphics.screen((px + px2)/2f * Vars.tilesize, Math.min(py, py2) * Vars.tilesize - Vars.tilesize*1.5f);
				tools.setPosition(v.x, v.y, Align.top);

			}else{
				tools.setPosition(control.getInput().getCursorX(),
						Gdx.graphics.getHeight() - control.getInput().getCursorY() - 15*Core.cameraScale, Align.top);
			}

			if(player.placeMode != PlaceMode.areaDelete){
				confirming = false;
			}
		});
	}
}
