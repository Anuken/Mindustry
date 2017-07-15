package io.anuke.mindustry.ui;

import static io.anuke.mindustry.Vars.ui;

import io.anuke.mindustry.GameState;
import io.anuke.mindustry.GameState.State;
import io.anuke.ucore.scene.ui.ConfirmDialog;
import io.anuke.ucore.scene.ui.Dialog;
import io.anuke.ucore.scene.ui.layout.Unit;

public class MenuDialog extends Dialog{

	public MenuDialog(){
		super("Paused", "dialog");
		setup();
	}
	
	void setup(){
		content().addButton("Back", ()->{
			hide();
			GameState.set(State.playing);
		}).width(200).units(Unit.dp);
		
		content().row();
		content().addButton("Settings", ()->{
			ui.showPrefs();
		}).width(200).units(Unit.dp);
		
		content().row();
		content().addButton("Controls", ()->{
			ui.showControls();
		}).width(200).units(Unit.dp);
		
		content().row();
		content().addButton("Back to menu", ()->{
			new ConfirmDialog("Confirm", "Are you sure you want to quit?", ()->{
				hide();
				GameState.set(State.menu);
			}).show();
		}).width(200).units(Unit.dp);
	}
}
