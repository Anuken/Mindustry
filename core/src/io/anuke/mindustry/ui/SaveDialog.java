package io.anuke.mindustry.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.reflect.ClassReflection;

import io.anuke.mindustry.Vars;
import io.anuke.mindustry.io.SaveIO;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.scene.ui.ConfirmDialog;
import io.anuke.ucore.scene.ui.TextButton;
import io.anuke.ucore.scene.ui.layout.Cell;

//TODO unified save/load dialogs
public class SaveDialog extends LoadDialog{

	public SaveDialog() {
		super("Save Game");
	}

	@Override
	public void modifyButton(TextButton button, int slot){
		button.clicked(() -> {
			if(SaveIO.isSaveValid(slot)){
				new ConfirmDialog("Overwrite", "Are you sure you want to overwrite\nthis save slot?", () -> {
					save(slot);
				}){
					{
						content().pad(16);
						for(Cell<?> cell : getButtonTable().getCells())
							cell.size(110, 45).pad(4);
					}
				}.show();
			}else{
				save(slot);
			}
		});
	}

	void save(int slot){
		Vars.ui.showLoading("[accent]Saving...");

		Timers.runTask(5f, () -> {
			hide();
			Vars.ui.hideLoading();
			if(Gdx.files.getLocalStoragePath().equals("C:\\Windows\\System32")){
				Vars.ui.showError("[orange]Invalid local storage directory![]\nAre you running the game from inside a zip file?");
				return;
			}
			try{
				SaveIO.saveToSlot(slot);
			}catch(Throwable e){
				e = (e.getCause() == null ? e : e.getCause());

				Vars.ui.showError("[orange]Failed to save game!\n[white]" + ClassReflection.getSimpleName(e.getClass()) + ": " + e.getMessage() + "\n" + "at " + e.getStackTrace()[0].getFileName() + ":" + e.getStackTrace()[0].getLineNumber());
			}
		});
	}

}
