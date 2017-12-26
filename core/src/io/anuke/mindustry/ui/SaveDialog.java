package io.anuke.mindustry.ui;

import com.badlogic.gdx.utils.reflect.ClassReflection;

import io.anuke.mindustry.Vars;
import io.anuke.mindustry.io.SaveIO;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.scene.ui.ConfirmDialog;
import io.anuke.ucore.scene.ui.TextButton;
import io.anuke.ucore.scene.ui.layout.Cell;
import io.anuke.ucore.util.Bundles;

public class SaveDialog extends LoadDialog{

	public SaveDialog() {
		super("$text.savegame");
	}

	@Override
	public void modifyButton(TextButton button, int slot){
		button.clicked(() -> {
			if(SaveIO.isSaveValid(slot)){
				new ConfirmDialog("$text.overwrite", "$text.save.overwrite", () -> {
					save(slot);
				}){
					{
						content().margin(16);
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
		Vars.ui.showLoading("text.saveload");

		Timers.runTask(5f, () -> {
			hide();
			Vars.ui.hideLoading();
			try{
				SaveIO.saveToSlot(slot);
			}catch(Throwable e){
				e = (e.getCause() == null ? e : e.getCause());

				Vars.ui.showError("[orange]"+Bundles.get("text.savefail")+"\n[white]" + ClassReflection.getSimpleName(e.getClass()) + ": " + e.getMessage() + "\n" + "at " + e.getStackTrace()[0].getFileName() + ":" + e.getStackTrace()[0].getLineNumber());
			}
		});
	}

}
