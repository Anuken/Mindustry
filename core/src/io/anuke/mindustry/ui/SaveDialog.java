package io.anuke.mindustry.ui;

import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Timer;
import com.badlogic.gdx.utils.Timer.Task;
import com.badlogic.gdx.utils.reflect.ClassReflection;

import io.anuke.mindustry.Vars;
import io.anuke.mindustry.io.SaveIO;
import io.anuke.ucore.scene.ui.*;
import io.anuke.ucore.scene.ui.layout.Cell;
import io.anuke.ucore.scene.ui.layout.Unit;

//TODO unified save/load dialogs
public class SaveDialog extends Dialog{

	public SaveDialog() {
		super("Save Game");
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
			
			Label info = new Label("[gray]" + (!SaveIO.isSaveValid(i) ? "<empty>" : "Wave " +
					SaveIO.getWave(slot)+"\nLast Saved: " + SaveIO.getTimeString(i)));
			info.setAlignment(Align.center, Align.center);
			
			button.add(info).padBottom(2).padTop(6);
			button.getLabel().setFontScale(Unit.dp.inPixels(0.75f));
			
			button.clicked(()->{
				if(SaveIO.isSaveValid(slot)){
					new ConfirmDialog("Overwrite", "Are you sure you want to overwrite\nthis save slot?", ()->{
						save(slot);
					}){{
						content().pad(16);
						for(Cell<?> cell : getButtonTable().getCells())
							cell.size(110, 45).pad(4).units(Unit.dp);
					}}.show();
				}else{
					save(slot);
				}
			});
			
			content().add(button).size(400, 80).units(Unit.dp).pad(2);
			content().row();
		}
	}
	
	void save(int slot){
		Vars.ui.showLoading("[orange]Saving...");
		
		Timer.schedule(new Task(){
			@Override
			public void run(){
				hide();
				Vars.ui.hideLoading();
				try{
					SaveIO.saveToSlot(slot);
				}catch (Throwable e){
					e = (e.getCause() == null ? e : e.getCause());
					
					Vars.ui.showError("[orange]Failed to save game!\n[white]" +
							ClassReflection.getSimpleName(e.getClass()) + ": " + e.getMessage() + "\n" +
							"at " + e.getStackTrace()[0].getFileName() + ":"+ e.getStackTrace()[0].getLineNumber());
				}
				
			}
		}, 5f/60f);
	}

}
