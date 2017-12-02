package io.anuke.mindustry.ui;

import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Timer;
import com.badlogic.gdx.utils.Timer.Task;

import io.anuke.mindustry.Vars;
import io.anuke.mindustry.core.GameState;
import io.anuke.mindustry.core.GameState.State;
import io.anuke.mindustry.io.SaveIO;
import io.anuke.ucore.core.Core;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.scene.ui.Label;
import io.anuke.ucore.scene.ui.ScrollPane;
import io.anuke.ucore.scene.ui.TextButton;
import io.anuke.ucore.scene.ui.layout.Table;
import io.anuke.ucore.scene.ui.layout.Unit;

//TODO unified save/load dialogs
public class LoadDialog extends FloatingDialog{
	ScrollPane pane;
	
	public LoadDialog(){
		this("Load Game");
	}
	
	public LoadDialog(String title) {
		super(title);
		setup();
		

		shown(() -> {
			setup();
			Timers.runTask(2f, ()-> Core.scene.setScrollFocus(pane));
		});

		addCloseButton();
	}

	private void setup(){
		content().clear();

		content().add("Select a save slot.").padBottom(2);
		content().row();
	
		Table slots = new Table();
		pane = new ScrollPane(slots);
		pane.setFadeScrollBars(false);
		
		slots.padRight(Unit.dp.inPixels(24));

		for(int i = 0; i < Vars.saveSlots; i++){
			final int slot = i;

			TextButton button = new TextButton("[orange]Slot " + (i + 1));
			button.pad(Unit.dp.inPixels(12));
			button.getLabelCell().top().left().growX();
			
			button.row();
			
			Label info = new Label("[gray]" + (!SaveIO.isSaveValid(i) ? "<empty>" : SaveIO.getMode(slot) + ", " + 
					SaveIO.getMap(slot) + 
			", Wave " + SaveIO.getWave(slot) + "\nLast Saved: " + SaveIO.getTimeString(i)));
			info.setAlignment(Align.center, Align.center);
			
			button.add(info).padBottom(3).padTop(7);
			button.row();
			//button.addImage("white", Color.GRAY)
			//.growX().height(3f).pad(4f).units(Unit.dp);
			button.row();
			modifyButton(button, slot);

			slots.add(button).size(404, 104).pad(4).units(Unit.dp);
			slots.row();
		}
		
		content().add(pane);

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
