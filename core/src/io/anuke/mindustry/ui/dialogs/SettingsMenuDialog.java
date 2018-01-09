package io.anuke.mindustry.ui.dialogs;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.graphics.Colors;
import com.badlogic.gdx.utils.Align;
import io.anuke.mindustry.Vars;
import io.anuke.mindustry.core.GameState;
import io.anuke.mindustry.core.GameState.State;
import io.anuke.mindustry.net.Net;
import io.anuke.ucore.core.Core;
import io.anuke.ucore.core.Settings;
import io.anuke.ucore.function.Consumer;
import io.anuke.ucore.scene.ui.Image;
import io.anuke.ucore.scene.ui.ScrollPane;
import io.anuke.ucore.scene.ui.SettingsDialog;
import io.anuke.ucore.scene.ui.layout.Table;
import io.anuke.ucore.util.Bundles;
import io.anuke.ucore.util.Mathf;

import static io.anuke.mindustry.Vars.renderer;

public class SettingsMenuDialog extends SettingsDialog{
	public SettingsTable graphics;
	public SettingsTable game;
	public SettingsTable sound;

	private Table prefs;
	private Table menu;
	private boolean wasPaused;
	
	public SettingsMenuDialog(){
		setStyle(Core.skin.get("dialog", WindowStyle.class));

		hidden(()->{
			if(!GameState.is(State.menu)){
				if(!wasPaused || Net.active())
					GameState.set(State.playing);
			}
		});

		shown(()->{
			if(!GameState.is(State.menu)){
				wasPaused = GameState.is(State.paused);
				if(menu.getScene() != null){
					wasPaused = ((PausedDialog)menu).wasPaused;
				}
				if(!Net.active()) GameState.set(State.paused);
				Vars.ui.paused.hide();
			}
		});

		setFillParent(true);
		title().setAlignment(Align.center);
		getTitleTable().row();
		getTitleTable().add(new Image("white"))
		.growX().height(3f).pad(4f).get().setColor(Colors.get("accent"));

		content().clearChildren();
		content().remove();
		buttons().remove();

		menu = new Table();

		Consumer<SettingsTable> s = table -> {
			table.row();
			table.addImageTextButton("$text.back", "icon-arrow-left", 10*3, this::back).size(240f, 60f).colspan(2).padTop(15f);
		};

		game = new SettingsTable(s);
		graphics = new SettingsTable(s);
		sound = new SettingsTable(s);

		prefs = new Table();
		prefs.top();
		prefs.margin(14f);

		menu.defaults().size(300f, 60f).pad(3f);
		menu.addButton("$text.settings.game", () -> visible(0));
		menu.row();
		menu.addButton("$text.settings.graphics", () -> visible(1));
		menu.row();
		menu.addButton("$text.settings.sound", () -> visible(2));

		if(!Vars.android) {
			menu.row();
			menu.addButton("$text.settings.controls", Vars.ui.controls::show);
		}

		prefs.clearChildren();
		prefs.add(menu);

		ScrollPane pane = new ScrollPane(prefs, "clear");
		pane.setFadeScrollBars(false);

		row();
		add(pane).grow().top();
		row();
		add(buttons()).fillX();

		hidden(this::back);

		addSettings();
	}

	void addSettings(){
		sound.volumePrefs();

		game.sliderPref("difficulty", 1, 0, 2, i -> Bundles.get("setting.difficulty." + (i == 0 ? "easy" : i == 1 ? "normal" : "hard")));
		game.screenshakePref();
		game.checkPref("smoothcam", true);
		game.checkPref("indicators", true);
		game.checkPref("effects", true);
		game.sliderPref("sensitivity", 100, 10, 300, i -> i + "%");
		game.sliderPref("saveinterval", 90, 10, 5*120, i -> Bundles.format("setting.seconds", i));

		graphics.checkPref("fps", false);
		graphics.checkPref("vsync", true, b -> Gdx.graphics.setVSync(b));
		graphics.checkPref("lasers", true);
		graphics.checkPref("healthbars", true);
		graphics.checkPref("pixelate", true, b -> {
			if(b){
				renderer.pixelSurface.setScale(Core.cameraScale);
				renderer.shadowSurface.setScale(Core.cameraScale);
				renderer.shieldSurface.setScale(Core.cameraScale);
			}else{
				renderer.shadowSurface.setScale(1);
				renderer.shieldSurface.setScale(1);
			}
			renderer.setPixelate(b);
		});

		Gdx.graphics.setVSync(Settings.getBool("vsync"));
	}

	private void back(){
		prefs.clearChildren();
		prefs.add(menu);
	}

	private void visible(int index){
	    prefs.clearChildren();
	    Table table = Mathf.select(index, game, graphics, sound);
        prefs.add(table);
	}
	
	@Override
	public void addCloseButton(){
		buttons().addImageTextButton("$text.menu", "icon-arrow-left", 30f, this::hide).size(230f, 64f);
		
		keyDown(key->{
			if(key == Keys.ESCAPE || key == Keys.BACK)
				hide();
		});
	}
}
