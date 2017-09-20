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
		}).pad(2).size(180, 44).units(Unit.dp);
	}
	
	private void setup(){
		content().clear();
		
		content().add("Select a save slot.").padBottom(2);
		content().row();
		
		for(int i = 0; i < Vars.saveSlots; i ++){
			final int slot = i;
			
			TextButton button = new TextButton("[orange]Slot " + (i+1));
			button.getLabelCell().top().left().growX();
			button.row();
			button.pad(Unit.dp.inPixels(10));
			button.add("[gray]" + (!SaveIO.isSaveValid(i) ? "<empty>" : "Last Saved: " + SaveIO.getTimeString(i))).padBottom(2);
			button.getLabel().setFontScale(0.75f);
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
			
			content().add(button).size(400, 78).units(Unit.dp).pad(2);
			content().row();
		}
		
	}
}
