package io.anuke.mindustry.ui.fragments;

import static io.anuke.mindustry.Vars.*;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.Align;

import io.anuke.mindustry.GameState;
import io.anuke.mindustry.GameState.State;
import io.anuke.mindustry.input.AndroidInput;
import io.anuke.mindustry.input.PlaceMode;
import io.anuke.ucore.core.Core;
import io.anuke.ucore.scene.ui.layout.Table;
import io.anuke.ucore.scene.ui.layout.Unit;

public class ToolFragment implements Fragment{
	private Table tools;
	
	public void build(){
		tools = new Table();
		tools.addIButton("icon-cancel", Unit.dp.inPixels(42), ()->{
			player.recipe = null;
		});
		
		tools.addIButton("icon-rotate", Unit.dp.inPixels(42), ()->{
			player.rotation ++;
			player.rotation %= 4;
		});
		
		tools.addIButton("icon-check", Unit.dp.inPixels(42), ()->{
			AndroidInput.place();
		});
		
		Core.scene.add(tools);
		
		tools.setVisible(()->
			!GameState.is(State.menu) && android && player.recipe != null && control.hasItems(player.recipe.requirements) &&
			AndroidInput.mode == PlaceMode.cursor
		);
		
		tools.update(()->{
			tools.setPosition(AndroidInput.mousex, Gdx.graphics.getHeight()-AndroidInput.mousey-15*Core.cameraScale, Align.top);
		});
	}
}
