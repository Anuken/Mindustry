package io.anuke.mindustry.ui.dialogs;

import com.badlogic.gdx.utils.reflect.ClassReflection;
import io.anuke.mindustry.core.GameState.State;
import io.anuke.mindustry.net.Net;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.scene.builders.build;
import io.anuke.ucore.scene.builders.imagebutton;
import io.anuke.ucore.util.Bundles;

import static io.anuke.mindustry.Vars.*;

public class PausedDialog extends FloatingDialog{
	private SaveDialog save = new SaveDialog();
	private LoadDialog load = new LoadDialog();
	public boolean wasPaused = false;

	public PausedDialog() {
		super("$text.menu");
		setup();
	}

	void setup(){
		update(() -> {
			if(state.is(State.menu) && isShown()){
				hide();
			}
		});

		shown(() -> {
			wasPaused = state.is(State.paused);
			if(!Net.active()) state.set(State.paused);
		});
		
		if(!mobile){
			content().defaults().width(220).height(50);

			content().addButton("$text.back", () -> {
				hide();
				if((!wasPaused || Net.active()) && !state.is(State.menu))
					state.set(State.playing);
			});

			content().row();
			content().addButton("$text.settings", ui.settings::show);

			content().row();
			content().addButton("$text.savegame", () -> {
				save.show();
			});

			content().row();
			content().addButton("$text.loadgame", () -> {
				load.show();
			}).disabled(b -> Net.active());

			//Local multiplayer is currently functional, but disabled.
			/*
            content().row();
            content().addButton("$text.addplayers", () -> {
                ui.localplayers.show();
            }).disabled(b -> Net.active());*/

			content().row();

			if(!gwt) {
				content().addButton("$text.hostserver", () -> {
					ui.host.show();
				}).disabled(b -> Net.active());
			}

            content().row();

			content().addButton("$text.quit", () -> {
                ui.showConfirm("$text.confirm", "$text.quit.confirm", () -> {
                	if(Net.client()) netClient.disconnectQuietly();
					runExitSave();
					hide();
				});
			});

		}else{
			build.begin(content());
			
			content().defaults().size(120f).pad(5);
			float isize = 14f*4;
			
			new imagebutton("icon-play-2", isize, () -> {
				hide();
				if(!wasPaused && !state.is(State.menu))
					state.set(State.playing);
			}).text("$text.back").padTop(4f);
			
			new imagebutton("icon-tools", isize, ui.settings::show).text("$text.settings").padTop(4f);
			
			imagebutton sa = new imagebutton("icon-save", isize, save::show);
			sa.text("$text.save").padTop(4f);

			content().row();
			
			imagebutton lo = new imagebutton("icon-load", isize, load::show);
			lo.text("$text.load").padTop(4f);
			lo.cell.disabled(b -> Net.active());

			imagebutton ho = new imagebutton("icon-host", isize, () -> {
				ui.host.show();
			});
			ho.text("$text.host").padTop(4f);
			ho.cell.disabled(b -> Net.active());
			
			new imagebutton("icon-quit", isize, () -> {
				ui.showConfirm("$text.confirm", "$text.quit.confirm", () -> {
					if(Net.client()) netClient.disconnectQuietly();
					runExitSave();
					hide();
				});
			}).text("Quit").padTop(4f);
			
			build.end();
		}
	}

	private void runExitSave(){
		if(control.getSaves().getCurrent() == null ||
				!control.getSaves().getCurrent().isAutosave()){
			state.set(State.menu);
			return;
		}

		ui.loadfrag.show("$text.saveload");

		Timers.runTask(5f, () -> {
			ui.loadfrag.hide();
			try{
				control.getSaves().getCurrent().save();
			}catch(Throwable e){
				e = (e.getCause() == null ? e : e.getCause());
				ui.showError("[orange]"+ Bundles.get("text.savefail")+"\n[white]" + ClassReflection.getSimpleName(e.getClass()) + ": " + e.getMessage() + "\n" + "at " + e.getStackTrace()[0].getFileName() + ":" + e.getStackTrace()[0].getLineNumber());
			}
			state.set(State.menu);
		});
	}
}
