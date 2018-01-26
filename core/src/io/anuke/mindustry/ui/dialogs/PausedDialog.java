package io.anuke.mindustry.ui.dialogs;

import com.badlogic.gdx.utils.reflect.ClassReflection;
import io.anuke.mindustry.Vars;
import io.anuke.mindustry.core.GameState;
import io.anuke.mindustry.core.GameState.State;
import io.anuke.mindustry.net.Net;
import io.anuke.mindustry.ui.PressGroup;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.scene.Element;
import io.anuke.ucore.scene.builders.build;
import io.anuke.ucore.scene.builders.imagebutton;
import io.anuke.ucore.scene.ui.ImageButton;
import io.anuke.ucore.util.Bundles;

import static io.anuke.mindustry.Vars.ui;

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
			if(GameState.is(State.menu) && isShown()){
				hide();
			}
		});

		shown(() -> {
			wasPaused = GameState.is(State.paused);
			if(!Net.active()) GameState.set(State.paused);
		});
		
		if(!Vars.android){
			content().defaults().width(220).height(50);

			content().addButton("$text.back", () -> {
				hide();
				if((!wasPaused || Net.active()) && !GameState.is(State.menu))
					GameState.set(State.playing);
			});

			content().row();
			content().addButton("$text.settings", ui.settings::show);

			content().row();
			content().addButton("$text.savegame", () -> {
				save.show();
			}).disabled(b -> Vars.world.getMap().id == -1);

			content().row();
			content().addButton("$text.loadgame", () -> {
				load.show();
			}).disabled(b -> Net.active());

			content().row();

			if(!Vars.gwt) {
				content().addButton("$text.hostserver", () -> {
					ui.host.show();
				}).disabled(b -> Net.active());
			}

            content().row();

			content().addButton("$text.quit", () -> {
                ui.showConfirm("$text.confirm", "$text.quit.confirm", () -> {
                	if(Net.active() && Net.client()) Vars.netClient.disconnectQuietly();
					runExitSave();
					hide();
				});
			});

		}else{
			build.begin(content());
			
			PressGroup group = new PressGroup();
			
			content().defaults().size(120f).pad(5);
			float isize = 14f*4;
			
			new imagebutton("icon-play-2", isize, () -> {
				hide();
				if(!wasPaused && !GameState.is(State.menu))
					GameState.set(State.playing);
			}).text("$text.back").padTop(4f);
			
			new imagebutton("icon-tools", isize, ui.settings::show).text("$text.settings").padTop(4f);
			
			imagebutton sa = new imagebutton("icon-save", isize, save::show);
			sa.text("$text.save").padTop(4f);
			sa.cell.disabled(b -> Vars.world.getMap().id == -1);

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
				Vars.ui.showConfirm("$text.confirm", "$text.quit.confirm", () -> {
					if(Net.active() && Net.client()) Vars.netClient.disconnectQuietly();
					runExitSave();
					hide();
				});
			}).text("Quit").padTop(4f);
			
			for(Element e : content().getChildren()){
				if(e instanceof ImageButton){
					group.add((ImageButton)e);
				}
			}
			
			build.end();
		}
	}

	private void runExitSave(){
		if(Vars.control.getSaves().getCurrent() == null ||
				!Vars.control.getSaves().getCurrent().isAutosave()){
			GameState.set(State.menu);
			Vars.control.getTutorial().reset();
			return;
		}

		Vars.ui.loadfrag.show("$text.saveload");

		Timers.runTask(5f, () -> {
			Vars.ui.loadfrag.hide();
			try{
				Vars.control.getSaves().getCurrent().save();
			}catch(Throwable e){
				e = (e.getCause() == null ? e : e.getCause());
				Vars.ui.showError("[orange]"+ Bundles.get("text.savefail")+"\n[white]" + ClassReflection.getSimpleName(e.getClass()) + ": " + e.getMessage() + "\n" + "at " + e.getStackTrace()[0].getFileName() + ":" + e.getStackTrace()[0].getLineNumber());
			}
			GameState.set(State.menu);
		});
	}
}
