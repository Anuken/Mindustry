package io.anuke.mindustry.ui;

import com.badlogic.gdx.utils.reflect.ClassReflection;

import io.anuke.mindustry.Vars;
import io.anuke.mindustry.io.SaveIO;
import io.anuke.mindustry.io.Saves;
import io.anuke.mindustry.io.Saves.SaveSlot;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.scene.builders.button;
import io.anuke.ucore.scene.ui.ConfirmDialog;
import io.anuke.ucore.scene.ui.Dialog;
import io.anuke.ucore.scene.ui.TextButton;
import io.anuke.ucore.scene.ui.layout.Cell;
import io.anuke.ucore.util.Bundles;

public class SaveDialog extends LoadDialog{

	public SaveDialog() {
		super("$text.savegame");
	}

	public void addSetup(){
		if(!Vars.control.getSaves().canAddSave()){
			return;
		}

		slots.row();
		slots.addImageTextButton("$text.save.new", "icon-add", "clear", 14*3, () -> {
			Vars.ui.showTextInput("$text.save", "$text.save.newslot", "", text -> {
				Vars.control.getSaves().addSave(text);
				setup();
			});
		}).fillX().margin(10f).minWidth(300f).height(70f).pad(4f).padRight(-4);
	}

	@Override
	public void modifyButton(TextButton button, SaveSlot slot){
		button.clicked(() -> {
			if(button.childrenPressed()) return;

			Vars.ui.showConfirm("$text.overwrite", "$text.save.overwrite", () -> {
				save(slot);
			});
		});
	}

	void save(SaveSlot slot){

		Vars.ui.showLoading("$text.saveload");

		Timers.runTask(5f, () -> {
			hide();
			Vars.ui.hideLoading();
			try{
				slot.save();
			}catch(Throwable e){
				e = (e.getCause() == null ? e : e.getCause());

				Vars.ui.showError("[orange]"+Bundles.get("text.savefail")+"\n[white]" + ClassReflection.getSimpleName(e.getClass()) + ": " + e.getMessage() + "\n" + "at " + e.getStackTrace()[0].getFileName() + ":" + e.getStackTrace()[0].getLineNumber());
			}
		});
	}

}
