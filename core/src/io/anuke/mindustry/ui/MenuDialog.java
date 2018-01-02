package io.anuke.mindustry.ui;

import com.badlogic.gdx.utils.reflect.ClassReflection;
import io.anuke.mindustry.Vars;
import io.anuke.mindustry.core.GameState;
import io.anuke.mindustry.core.GameState.State;
import io.anuke.mindustry.net.Net;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.scene.Element;
import io.anuke.ucore.scene.builders.build;
import io.anuke.ucore.scene.builders.imagebutton;
import io.anuke.ucore.scene.ui.ImageButton;
import io.anuke.ucore.scene.ui.TextField.TextFieldFilter.DigitsOnlyFilter;
import io.anuke.ucore.util.Bundles;
import io.anuke.ucore.util.Strings;

import java.io.IOException;

import static io.anuke.mindustry.Vars.ui;

public class MenuDialog extends FloatingDialog{
	private SaveDialog save = new SaveDialog();
	private LoadDialog load = new LoadDialog();
	public boolean wasPaused = false;

	public MenuDialog() {
		super("$text.menu");
		setup();
	}

	void setup(){
		shown(() -> {
			wasPaused = GameState.is(State.paused);
			if(!Net.active()) GameState.set(State.paused);
		});
		
		if(!Vars.android){
			content().defaults().width(220).height(50);

			content().addButton("$text.back", () -> {
				hide();
				if(!wasPaused || Net.active())
					GameState.set(State.playing);
			});

			content().row();
			content().addButton("$text.settings", () -> {
				ui.showPrefs();
			});

			if(!Vars.gwt){
				content().row();
				content().addButton("$text.savegame", () -> {
					save.show();
				});

				content().row();
				content().addButton("$text.loadgame", () -> {
					load.show();
				});
			}

			content().row();

			content().addButton("$text.hostserver", () -> {
				Vars.ui.showTextInput("$text.hostserver", "$text.server.port", Vars.port + "", new DigitsOnlyFilter(), text -> {
					int result = Strings.parseInt(text);
					if(result == Integer.MIN_VALUE || result >= 65535){
						Vars.ui.showError("$text.server.invalidport");
					}else{
						try{
							Net.host(result);
							GameState.set(State.playing);
						}catch (IOException e){
							Vars.ui.showError(Bundles.format("text.server.error", Strings.parseException(e, false)));
						}
					}
				});
			}).disabled(b -> Net.active() || (Net.active() && !Net.server()));

            content().row();

			content().addButton("$text.quit", () -> {
        Vars.ui.showConfirm("$text.confirm", "$text.quit.confirm", () -> {
					runSave();
					hide();
					GameState.set(State.menu);
				});
			});

		}else{
			build.begin(content());
			
			PressGroup group = new PressGroup();
			
			content().defaults().size(120f).pad(5);
			float isize = 14f*4;
			
			new imagebutton("icon-play-2", isize, () -> {
				hide();
				if(!wasPaused)
					GameState.set(State.playing);
			}).text("$text.back").padTop(4f);
			
			new imagebutton("icon-tools", isize, () -> ui.showPrefs()).text("$text.settings").padTop(4f);
			
			new imagebutton("icon-save", isize, ()-> save.show()).text("$text.save").padTop(4f);
			
			new imagebutton("icon-load", isize, () -> load.show()).text("$text.load").padTop(4f);
			
			new imagebutton("icon-quit", isize, () -> {
				Vars.ui.showConfirm("$text.confirm", "$text.quit.confirm", () -> {
					runSave();
					hide();
					GameState.set(State.menu);
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

	private void runSave(){
		if(Vars.control.getSaves().getCurrent() == null ||
				!Vars.control.getSaves().getCurrent().isAutosave()) return;

		Vars.ui.showLoading("$text.saveload");

		Timers.runTask(5f, () -> {
			Vars.ui.hideLoading();
			try{
				Vars.control.getSaves().getCurrent().save();
			}catch(Throwable e){
				e = (e.getCause() == null ? e : e.getCause());

				Vars.ui.showError("[orange]"+ Bundles.get("text.savefail")+"\n[white]" + ClassReflection.getSimpleName(e.getClass()) + ": " + e.getMessage() + "\n" + "at " + e.getStackTrace()[0].getFileName() + ":" + e.getStackTrace()[0].getLineNumber());
			}
		});
	}
}
