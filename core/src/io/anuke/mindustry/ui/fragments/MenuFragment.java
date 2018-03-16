package io.anuke.mindustry.ui.fragments;

import com.badlogic.gdx.Gdx;
import io.anuke.mindustry.core.GameState.State;
import io.anuke.mindustry.io.Platform;
import io.anuke.mindustry.io.Version;
import io.anuke.mindustry.ui.MenuButton;
import io.anuke.mindustry.ui.dialogs.FloatingDialog;
import io.anuke.ucore.scene.builders.imagebutton;
import io.anuke.ucore.scene.builders.label;
import io.anuke.ucore.scene.builders.table;

import static io.anuke.mindustry.Vars.*;

public class MenuFragment implements Fragment{
	
	public void build(){
		new table(){{
			visible(() -> state.is(State.menu));

			if(!android){
				new table(){{

					float w = 200f;
					float bw = w * 2f + 10f;

					defaults().size(w, 70f).padTop(5).padRight(5);

					add(new MenuButton("icon-play-2", "$text.play", MenuFragment.this::showPlaySelect)).width(bw).colspan(2);

					row();

					add(new MenuButton("icon-editor", "$text.editor", () -> {
						if(gwt){
							ui.showInfo("$text.editor.web");
						}else{
							ui.editor.show();
						}
					}));
					
					add(new MenuButton("icon-tools", "$text.settings", ui.settings::show));

					row();

					add(new MenuButton("icon-info", "$text.about.button", ui.about::show));

					add(new MenuButton("icon-menu", "$text.changelog.title", ui.changelog::show));

					row();
					
					if(!gwt){
						add(new MenuButton("icon-exit", "$text.quit", Gdx.app::exit)).width(bw).colspan(2);
					}

					get().margin(16);
				}}.end();

			}else {
				new table() {{
					defaults().size(120f).pad(5);
					float isize = 14f * 4;

					new imagebutton("icon-play-2", isize, ui.levels::show).text("$text.play").padTop(4f);

					new imagebutton("icon-tutorial", isize, () -> control.playMap(world.maps().getMap("tutorial"))).text("$text.tutorial").padTop(4f);

					new imagebutton("icon-load", isize, ui.load::show).text("$text.load").padTop(4f);

					new imagebutton("icon-add", isize, ui.join::show).text("$text.joingame").padTop(4f);

					row();

					new imagebutton("icon-editor", isize, ui.editor::show).text("$text.editor").padTop(4f);

					new imagebutton("icon-tools", isize, ui.settings::show).text("$text.settings").padTop(4f);

					new imagebutton("icon-info", isize, ui.about::show).text("$text.about.button").padTop(4f);

					new imagebutton("icon-donate", isize, Platform.instance::openDonations).text("$text.donate").padTop(4f);
				}}.end();
			}
		}}.end();

		//discord icon in top right
		if(Platform.instance.hasDiscord()) {
			new table() {{
				abottom().atop().aright();
				get().addButton("", "discord", ui.discord::show);
			}}.end().visible(() -> state.is(State.menu));
		}

		//version info
		new table(){{
			visible(() -> state.is(State.menu));
			abottom().aleft();
			new label("Mindustry " + Version.code + " " + Version.type + " / " + Version.buildName);
		}}.end();
	}

	private void showPlaySelect(){
		float w = 200f;
		float bw = w * 2f + 10f;

		FloatingDialog dialog = new FloatingDialog("$text.play");
		dialog.addCloseButton();
		dialog.content().defaults().height(70f).width(w).padRight(5f);

		dialog.content().add(new MenuButton("icon-play-2", "$text.newgame", () -> {
			dialog.hide();
			ui.levels.show();
		})).width(bw).colspan(2);
		dialog.content().row();

		dialog.content().add(new MenuButton("icon-add", "$text.joingame", () -> {
			if(Platform.instance.canJoinGame()){
				ui.join.show();
				dialog.hide();
			}else{
				ui.showInfo("$text.multiplayer.web");
			}
		}));
		dialog.content().add(new MenuButton("icon-tutorial", "$text.tutorial", ()-> {
			control.playMap(world.maps().getMap("tutorial"));
			dialog.hide();
		}));

		dialog.content().row();

		dialog.content().add(new MenuButton("icon-load", "$text.loadgame", () -> {
			ui.load.show();
			dialog.hide();
		})).width(bw).colspan(2);

		dialog.show();
	}
}
