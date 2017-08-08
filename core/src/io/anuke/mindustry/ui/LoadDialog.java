package io.anuke.mindustry.ui;

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
		}).pad(8).size(180, 60);
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
					SaveIO.loadFromSlot(slot);
					hide();
				}
			});
			
			content().add(button).size(400, 100).units(Unit.dp).pad(10);
			content().row();
		}
		
	}
}
