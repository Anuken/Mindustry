package io.anuke.mindustry.ui.dialogs;

import com.badlogic.gdx.utils.reflect.ClassReflection;
import io.anuke.mindustry.Vars;
import io.anuke.mindustry.core.GameState;
import io.anuke.mindustry.core.GameState.State;
import io.anuke.mindustry.io.Saves.SaveSlot;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.scene.ui.TextButton;
import io.anuke.ucore.util.Bundles;

public class SaveDialog extends LoadDialog{

	public SaveDialog() {
		super("$text.savegame");

		update(() -> {
			if(GameState.is(State.menu) && isShown()){
				hide();
			}
		});
	}

	public void addSetup(){
		if(!Vars.control.getSaves().canAddSave()){
			return;
		}

		slots.row();
		slots.addImageTextButton("$text.save.new", "icon-add", "clear", 14*3, () ->
			Vars.ui.showTextInput("$text.save", "$text.save.newslot", "", text -> {
				Vars.control.getSaves().addSave(text);
				setup();
			})
		).fillX().margin(10f).minWidth(300f).height(70f).pad(4f).padRight(-4);
	}

	@Override
	public void modifyButton(TextButton button, SaveSlot slot){
		button.clicked(() -> {
			if(button.childrenPressed()) return;

			Vars.ui.showConfirm("$text.overwrite", "$text.save.overwrite", () -> save(slot));
		});
	}

	void save(SaveSlot slot){

		Vars.ui.loadfrag.show("$text.saveload");

		Timers.runTask(5f, () -> {
			hide();
			Vars.ui.loadfrag.hide();
			try{
				slot.save();
			}catch(Throwable e){
				e = (e.getCause() == null ? e : e.getCause());

				Vars.ui.showError("[orange]"+Bundles.get("text.savefail")+"\n[white]" + ClassReflection.getSimpleName(e.getClass()) + ": " + e.getMessage() + "\n" + "at " + e.getStackTrace()[0].getFileName() + ":" + e.getStackTrace()[0].getLineNumber());
			}
		});
	}

}
