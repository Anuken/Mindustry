package io.anuke.mindustry.ui;

import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Timer;
import com.badlogic.gdx.utils.Timer.Task;

import io.anuke.mindustry.Vars;
import io.anuke.mindustry.core.GameState;
import io.anuke.mindustry.core.GameState.State;
import io.anuke.mindustry.io.SaveIO;
import io.anuke.ucore.scene.ui.Label;
import io.anuke.ucore.scene.ui.TextButton;
import io.anuke.ucore.scene.ui.layout.Unit;

//TODO unified save/load dialogs
public class LoadDialog extends FloatingDialog{

	public LoadDialog(){
		this("Load Game");
	}
	
	public LoadDialog(String title) {
		super(title);
		setup();
		

		shown(() -> {
			setup();
		});

		addCloseButton();
	}

	private void setup(){
		content().clear();

		content().add("Select a save slot.").padBottom(2);
		content().row();

		for(int i = 0; i < Vars.saveSlots; i++){
			final int slot = i;

			TextButton button = new TextButton("[orange]Slot " + (i + 1));
			button.pad(Unit.dp.inPixels(10));
			button.getLabelCell().top().left().growX();
			
			button.row();
			
			Label info = new Label("[gray]" + (!SaveIO.isSaveValid(i) ? "<empty>" : "Wave " +
					SaveIO.getWave(slot)+"\nLast Saved: " + SaveIO.getTimeString(i)));
			info.setAlignment(Align.center, Align.center);
			
			button.add(info).padBottom(2).padTop(6);
			button.row();
			//button.addImage("white", Color.GRAY)
			//.growX().height(3f).pad(4f).units(Unit.dp);
			button.row();
			modifyButton(button, slot);

			content().add(button).size(400, 86).units(Unit.dp).pad(2);
			content().row();
		}

	}
	
	public void modifyButton(TextButton button, int slot){
		button.setDisabled(!SaveIO.isSaveValid(slot));
		button.clicked(() -> {
			if(!button.isDisabled()){
				Vars.ui.showLoading();

				Timer.schedule(new Task(){
					@Override
					public void run(){
						Vars.ui.hideLoading();
						hide();
						try{
							SaveIO.loadFromSlot(slot);
							GameState.set(State.playing);
							Vars.ui.hideMenu();
						}catch(Exception e){
							e.printStackTrace();
							Vars.ui.hideMenu();
							GameState.set(State.menu);
							Vars.control.reset();
							Vars.ui.showError("[orange]Save file corrupted or invalid!");
							return;
						}

					}
				}, 3f/60f);
			}
		});
	}
}
