package io.anuke.mindustry.ui;

import static io.anuke.mindustry.Vars.ui;

import com.badlogic.gdx.Application.ApplicationType;
import com.badlogic.gdx.Gdx;

import io.anuke.mindustry.Vars;
import io.anuke.mindustry.core.GameState;
import io.anuke.mindustry.core.GameState.State;
import io.anuke.ucore.scene.ui.ConfirmDialog;
import io.anuke.ucore.scene.ui.layout.Cell;
import io.anuke.ucore.scene.ui.layout.Unit;

public class MenuDialog extends FloatingDialog{
	private SaveDialog save = new SaveDialog();
	private LoadDialog load = new LoadDialog();
	public boolean wasPaused = false;

	public MenuDialog(){
		super("Paused");
		setup();
	}
	
	void setup(){
		content().defaults().width(220).height(50).units(Unit.dp);
		
		content().addButton("Back", ()->{
			hide();
			if(!wasPaused)
				GameState.set(State.playing);
		});
		
		shown(()->{
			wasPaused = GameState.is(State.paused);
			GameState.set(State.paused);
		});
		
		content().row();
		content().addButton("Settings", ()->{
			ui.showPrefs();
		});
		
		if(!Vars.android){
			content().row();
			content().addButton("Controls", ()->{
				ui.showControls();
			});
		}
		
		if(Gdx.app.getType() != ApplicationType.WebGL){
			content().row();
			content().addButton("Save Game", ()->{
				save.show();
			});
		
			content().row();
			content().addButton("Load Game", ()->{
				load.show();
			});
		}
		
		content().row();
		content().addButton("Back to menu", ()->{
			new ConfirmDialog("Confirm", "Are you sure you want to quit?", ()->{
				hide();
				GameState.set(State.menu);
			}){{
				for(Cell<?> cell : getButtonTable().getCells())
					cell.pad(3).size(180, 44).units(Unit.dp);
			}}.show();
		});
	}
}
