package io.anuke.mindustry.ui;

import com.badlogic.gdx.utils.Timer;
import com.badlogic.gdx.utils.Timer.Task;

import io.anuke.mindustry.GameState;
import io.anuke.mindustry.GameState.State;
import io.anuke.mindustry.Vars;
import io.anuke.mindustry.io.SaveIO;
import io.anuke.ucore.scene.ui.Dialog;
import io.anuke.ucore.scene.ui.TextButton;
import io.anuke.ucore.scene.ui.layout.Unit;

public class LoadDialog extends Dialog{

	public LoadDialog() {
		super("Load Game");
		setup();
		
		shown(()->{
			setup();
		});
		
		getButtonTable().addButton("Back", ()->{
			hide();
		}).pad(8).size(180, 50);
	}
	
	private void setup(){
		content().clear();
		
		content().add("Select a save slot.").padBottom(4);
		content().row();
		
		for(int i = 0; i < Vars.saveSlots; i ++){
			final int slot = i;
			
			TextButton button = new TextButton("[yellow]Slot " + i);
			button.getLabelCell().top().left().growX();
			button.row();
			button.pad(12);
			button.add("[gray]" + (!SaveIO.isSaveValid(i) ? "<empty>" : "Last Saved: " + SaveIO.getTimeString(i)));
			button.getLabel().setFontScale(1f);
			button.setDisabled(!SaveIO.isSaveValid(i) );
			
			button.clicked(()->{
				if(!button.isDisabled()){
					Vars.ui.showLoading();
					
					Timer.schedule(new Task(){
						public void run(){
							SaveIO.loadFromSlot(slot);
							Vars.ui.hideLoading();
							hide();
							Vars.ui.hideMenu();
							GameState.set(State.playing);
						}
					}, 2f/60f);
				}
			});
			
			content().add(button).size(400, 90).units(Unit.dp).pad(10);
			content().row();
		}
		
	}
}
