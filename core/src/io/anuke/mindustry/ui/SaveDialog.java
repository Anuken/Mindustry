package io.anuke.mindustry.ui;

import com.badlogic.gdx.utils.Timer;
import com.badlogic.gdx.utils.Timer.Task;

import io.anuke.mindustry.Vars;
import io.anuke.mindustry.io.SaveIO;
import io.anuke.ucore.scene.ui.ConfirmDialog;
import io.anuke.ucore.scene.ui.Dialog;
import io.anuke.ucore.scene.ui.TextButton;
import io.anuke.ucore.scene.ui.layout.Cell;
import io.anuke.ucore.scene.ui.layout.Unit;

public class SaveDialog extends Dialog{

	public SaveDialog() {
		super("Save Game");
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
			button.add((!SaveIO.isSaveValid(i) ? "[gray]<empty>" : "[LIGHT_GRAY]Last Saved: " + SaveIO.getTimeString(i)));
			button.getLabel().setFontScale(1f);
			
			button.clicked(()->{
				if(SaveIO.isSaveValid(slot)){
					new ConfirmDialog("Overwrite", "Are you sure you want to overwrite\nthis save slot?", ()->{
						save(slot);
					}){{
						content().pad(16);
						for(Cell<?> cell : getButtonTable().getCells())
							cell.size(110, 45).pad(4);
					}}.show();
				}else{
					save(slot);
				}
			});
			
			content().add(button).size(400, 90).units(Unit.dp).pad(8);
			content().row();
		}
	}
	
	void save(int slot){
		Vars.ui.showLoading("[yellow]Saving...");
		
		Timer.schedule(new Task(){
			@Override
			public void run(){
				SaveIO.saveToSlot(slot);
				hide();
				Vars.ui.hideLoading();
			}
		}, 2f/60f);
	}

}
