package io.anuke.mindustry.ui;

import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Timer;
import com.badlogic.gdx.utils.Timer.Task;

import io.anuke.mindustry.GameState;
import io.anuke.mindustry.GameState.State;
import io.anuke.mindustry.Vars;
import io.anuke.mindustry.io.SaveIO;
import io.anuke.ucore.scene.ui.Dialog;
import io.anuke.ucore.scene.ui.Label;
import io.anuke.ucore.scene.ui.TextButton;
import io.anuke.ucore.scene.ui.layout.Unit;

public class LoadDialog extends Dialog{

	public LoadDialog() {
		super("Load Game");
		setup();

		shown(() -> {
			setup();
		});

		getButtonTable().addButton("Back", () -> {
			hide();
		}).pad(2).size(180, 44).units(Unit.dp);
	}

	private void setup(){
		content().clear();

		content().add("Select a save slot.").padBottom(2);
		content().row();

		for(int i = 0; i < Vars.saveSlots; i++){
			final int slot = i;

			TextButton button = new TextButton("[orange]Slot " + (i + 1));
			button.getLabelCell().top().left().growX();
			button.row();
			button.pad(Unit.dp.inPixels(10));
			
			Label info = new Label("[gray]" + (!SaveIO.isSaveValid(i) ? "<empty>" : "Wave " +
					SaveIO.getWave(slot)+"\nLast Saved: " + SaveIO.getTimeString(i)));
			info.setAlignment(Align.center, Align.center);
			
			button.add(info).padBottom(2).padTop(6);
			button.getLabel().setFontScale(Unit.dp.inPixels(0.75f));
			button.setDisabled(!SaveIO.isSaveValid(i));

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
							}catch(Exception e){
								Vars.ui.showError("[orange]Save file corrupted or invalid!");
								return;
							}
							Vars.ui.hideMenu();
							GameState.set(State.playing);
						}
					}, 3f/60f);
				}
			});

			content().add(button).size(400, 80).units(Unit.dp).pad(2);
			content().row();
		}

	}
}
