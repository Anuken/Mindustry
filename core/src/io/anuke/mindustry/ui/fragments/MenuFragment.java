package io.anuke.mindustry.ui.fragments;

import com.badlogic.gdx.Gdx;
import io.anuke.mindustry.core.GameState.State;
import io.anuke.mindustry.core.Platform;
import io.anuke.mindustry.game.EventType.ResizeEvent;
import io.anuke.mindustry.io.Version;
import io.anuke.mindustry.ui.MenuButton;
import io.anuke.mindustry.ui.MobileButton;
import io.anuke.mindustry.ui.dialogs.FloatingDialog;
import io.anuke.ucore.core.Events;
import io.anuke.ucore.scene.Group;
import io.anuke.ucore.scene.builders.build;
import io.anuke.ucore.scene.builders.label;
import io.anuke.ucore.scene.builders.table;
import io.anuke.ucore.scene.ui.layout.Table;

import static io.anuke.mindustry.Vars.*;

public class MenuFragment extends Fragment{
	private Table mobileContainer;

	@Override
	public void build(Group parent){
		new table(){{
			visible(() -> state.is(State.menu));

			if(!mobile){
				buildDesktop();
			}else{
				buildMobile();

				Events.on(ResizeEvent.class, () -> buildMobile());
			}
		}}.end();

		//discord icon in top right
		if(Platform.instance.hasDiscord()) {
			new table() {{
				abottom().atop().aright();
				get().addButton("", "discord", ui.discord::show).size(81, 42);
			}}.end().visible(() -> state.is(State.menu));
		}

		//info icon
		if(mobile) {
			new table() {{
				abottom().atop().aleft();
				get().addButton("", "info", ui.about::show).size(81, 42);
			}}.end().visible(() -> state.is(State.menu));
		}

		//version info
		new table(){{
			visible(() -> state.is(State.menu));
			abottom().aleft();
			new label("Mindustry " + Version.code + " " + Version.type + " / " + Version.buildName);
		}}.end();
	}

	private void buildMobile(){
		if(mobileContainer == null){
			mobileContainer = build.getTable();
		}

		mobileContainer.clear();
		mobileContainer.setSize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

		float size = 120f;
		float isize = 14f * 4;
		mobileContainer.defaults().size(size).pad(5).padTop(4f);

		MobileButton
				play = new MobileButton("icon-play-2", isize, "$text.play", ui.levels::show),
				maps = new MobileButton("icon-map", isize, "$text.maps", ui.maps::show),
				load = new MobileButton("icon-load", isize, "$text.load", ui.load::show),
				join = new MobileButton("icon-add", isize, "$text.joingame", ui.join::show),
				editor = new MobileButton("icon-editor", isize, "$text.editor", () -> ui.loadAnd(ui.editor::show)),
				tools = new MobileButton("icon-tools", isize, "$text.settings", ui.settings::show),
				unlocks = new MobileButton("icon-unlocks", isize, "$text.unlocks", ui.unlocks::show),
				donate = new MobileButton("icon-donate", isize, "$text.donate", Platform.instance::openDonations);

		if(Gdx.graphics.getWidth() > Gdx.graphics.getHeight()){
			mobileContainer.add(play);
			mobileContainer.add(join);
			mobileContainer.add(load);
			mobileContainer.add(maps);
			mobileContainer.row();

			mobileContainer.table(table -> {
				table.defaults().set(mobileContainer.defaults());

				table.add(editor);
				table.add(tools);
				table.add(unlocks);

				if(Platform.instance.canDonate()) table.add(donate);
			}).colspan(4);
		}else{
			mobileContainer.add(play);
			mobileContainer.add(maps);
			mobileContainer.row();
			mobileContainer.add(load);
			mobileContainer.add(join);
			mobileContainer.row();
			mobileContainer.add(editor);
			mobileContainer.add(tools);
			mobileContainer.row();

			mobileContainer.table(table -> {
				table.defaults().set(mobileContainer.defaults());

				table.add(unlocks);

				if(Platform.instance.canDonate()) table.add(donate);
			}).colspan(2);
		}
	}

	private void buildDesktop(){
		new table(){{

			float w = 200f;
			float bw = w * 2f + 10f;

			defaults().size(w, 66f).padTop(5).padRight(5);

			add(new MenuButton("icon-play-2", "$text.play", MenuFragment.this::showPlaySelect)).width(bw).colspan(2);

			row();

			add(new MenuButton("icon-editor", "$text.editor", () -> ui.loadAnd(ui.editor::show)));

			add(new MenuButton("icon-map", "$text.maps", ui.maps::show));

			row();

			add(new MenuButton("icon-info", "$text.about.button", ui.about::show));

			add(new MenuButton("icon-tools", "$text.settings", ui.settings::show));

			row();

			add(new MenuButton("icon-menu", "$text.changelog.title", ui.changelog::show));

			add(new MenuButton("icon-unlocks", "$text.unlocks", ui.unlocks::show));

			row();

			if(!gwt){
				add(new MenuButton("icon-exit", "$text.quit", Gdx.app::exit)).width(bw).colspan(2);
			}

			get().margin(16);
		}}.end();
	}

	private void showPlaySelect(){
		float w = 200f;
		float bw = w * 2f + 10f;

		FloatingDialog dialog = new FloatingDialog("$text.play");
		dialog.addCloseButton();
		dialog.content().defaults().height(66f).width(w).padRight(5f);

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

		dialog.content().add(new MenuButton("icon-tutorial", "$text.tutorial", ()-> ui.showInfo("The tutorial is currently not yet implemented.")));

		dialog.content().row();

		dialog.content().add(new MenuButton("icon-load", "$text.loadgame", () -> {
			ui.load.show();
			dialog.hide();
		})).width(bw).colspan(2);

		dialog.show();
	}
}
