package io.anuke.mindustry.ui;

import static io.anuke.mindustry.Vars.ui;

import io.anuke.mindustry.GameState;
import io.anuke.mindustry.GameState.State;
import io.anuke.mindustry.Vars;
import io.anuke.ucore.scene.ui.ConfirmDialog;
import io.anuke.ucore.scene.ui.Dialog;
import io.anuke.ucore.scene.ui.layout.Unit;

public class MenuDialog extends Dialog{
	private SaveDialog save = new SaveDialog();

	public MenuDialog(){
		super("Paused", "dialog");
		setup();
	}
	
	void setup(){
		content().defaults().width(200).units(Unit.dp);
		
		content().addButton("Back", ()->{
			hide();
			GameState.set(State.playing);
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
		
		content().row();
		content().addButton("Save Game", ()->{
			save.show();
		});
		
		content().row();
		content().addButton("Load Game", ()->{
			
		});
		
		content().row();
		content().addButton("Back to menu", ()->{
			new ConfirmDialog("Confirm", "Are you sure you want to quit?", ()->{
				hide();
				GameState.set(State.menu);
			}).show();
		});
	}
}
