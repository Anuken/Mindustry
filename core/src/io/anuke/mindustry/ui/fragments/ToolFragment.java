package io.anuke.mindustry.ui.fragments;

import static io.anuke.mindustry.Vars.*;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.Align;

import io.anuke.mindustry.core.GameState;
import io.anuke.mindustry.core.GameState.State;
import io.anuke.mindustry.input.PlaceMode;
import io.anuke.ucore.core.Core;
import io.anuke.ucore.scene.ui.layout.Table;
import io.anuke.ucore.scene.ui.layout.Unit;

public class ToolFragment implements Fragment{
	private Table tools;
	
	public void build(){
		tools = new Table();
		tools.addIButton("icon-cancel", Unit.dp.scl(42), ()->{
			player.recipe = null;
		});
		
		tools.addIButton("icon-rotate", Unit.dp.scl(42), ()->{
			player.rotation ++;
			player.rotation %= 4;
		});
		
		tools.addIButton("icon-check", Unit.dp.scl(42), ()->{
			player.placeMode.tapped(control.getInput().getBlockX(), control.getInput().getBlockY());
		});
		
		Core.scene.add(tools);
		
		tools.setVisible(()->
			!GameState.is(State.menu) && android && player.recipe != null && control.hasItems(player.recipe.requirements) &&
			player.placeMode == PlaceMode.cursor
		);
		
		tools.update(()->{
			tools.setPosition(control.getInput().getCursorX(), Gdx.graphics.getHeight() - control.getInput().getCursorY() - 15*Core.cameraScale, Align.top);
		});
	}
}
