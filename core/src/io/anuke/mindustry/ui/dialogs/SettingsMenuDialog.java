package io.anuke.mindustry.ui.dialogs;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.graphics.Colors;
import com.badlogic.gdx.utils.Align;
import io.anuke.mindustry.Vars;
import io.anuke.mindustry.core.GameState.State;
import io.anuke.mindustry.net.Net;
import io.anuke.ucore.core.Core;
import io.anuke.ucore.core.Graphics;
import io.anuke.ucore.core.Settings;
import io.anuke.ucore.function.Consumer;
import io.anuke.ucore.scene.Element;
import io.anuke.ucore.scene.event.InputEvent;
import io.anuke.ucore.scene.event.InputListener;
import io.anuke.ucore.scene.ui.Image;
import io.anuke.ucore.scene.ui.ScrollPane;
import io.anuke.ucore.scene.ui.SettingsDialog;
import io.anuke.ucore.scene.ui.Slider;
import io.anuke.ucore.scene.ui.layout.Table;
import io.anuke.ucore.util.Bundles;
import io.anuke.ucore.util.Mathf;

import static io.anuke.mindustry.Vars.*;

public class SettingsMenuDialog extends SettingsDialog{
	public SettingsTable graphics;
	public SettingsTable game;
	public SettingsTable sound;

	private Table prefs;
	private Table menu;
	private boolean wasPaused;
	
	public SettingsMenuDialog(){
		setStyle(Core.skin.get("dialog", WindowStyle.class));

		hidden(() -> {
			if(!state.is(State.menu)){
				if(!wasPaused || Net.active())
					state.set(State.playing);
			}
		});

		shown(() -> {
			if(!state.is(State.menu)){
				wasPaused = state.is(State.paused);
				if(ui.paused.getScene() != null){
					wasPaused = ui.paused.wasPaused;
				}
				if(!Net.active()) state.set(State.paused);
				ui.paused.hide();
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
		if(!Vars.mobile) {
			menu.row();
			menu.addButton("$text.settings.controls", ui.controls::show);
		}
		menu.row();
		menu.addButton("$text.settings.language", ui.language::show);

		prefs.clearChildren();
		prefs.add(menu);

		ScrollPane pane = new ScrollPane(prefs, "clear");
		pane.addCaptureListener(new InputListener() {
			@Override
			public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
				Element actor = pane.hit(x, y, true);
				if (actor instanceof Slider) {
					pane.setFlickScroll(false);
					return true;
				}

				return super.touchDown(event, x, y, pointer, button);
			}

			@Override
			public void touchUp(InputEvent event, float x, float y, int pointer, int button) {
				pane.setFlickScroll(true);
				super.touchUp(event, x, y, pointer, button);
			}
		});
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

		game.screenshakePref();
		game.checkPref("smoothcam", true);
		game.checkPref("effects", true);
		game.sliderPref("sensitivity", 100, 10, 300, i -> i + "%");
		game.sliderPref("saveinterval", 90, 10, 5*120, i -> Bundles.format("setting.seconds", i));

		if(!gwt){
			graphics.checkPref("multithread", false, threads::setEnabled);

			if(Settings.getBool("multithread")){
				threads.setEnabled(true);
			}
		}

		if(!mobile && !gwt) {
			graphics.checkPref("vsync", true, b -> Gdx.graphics.setVSync(b));
			graphics.checkPref("fullscreen", false, b -> {
				if (b) {
					Gdx.graphics.setFullscreenMode(Gdx.graphics.getDisplayMode());
				} else {
					Gdx.graphics.setWindowedMode(600, 480);
				}
			});

			Gdx.graphics.setVSync(Settings.getBool("vsync"));
			if(Settings.getBool("fullscreen")){
				Gdx.graphics.setFullscreenMode(Gdx.graphics.getDisplayMode());
			}
		}

		graphics.checkPref("fps", false);
		graphics.checkPref("lasers", true);
        graphics.sliderPref("previewopacity", 50, 0, 100, i -> i + "%");
		graphics.checkPref("indicators", true);
		graphics.checkPref("healthbars", true);
		graphics.checkPref("pixelate", true, b -> {
			if(b){
				renderer.pixelSurface.setScale(Core.cameraScale);
				renderer.shadowSurface.setScale(Core.cameraScale);
				renderer.shieldSurface.setScale(Core.cameraScale);
				Graphics.getEffects1().setScale(Core.cameraScale);
				Graphics.getEffects2().setScale(Core.cameraScale);
			}else{
				renderer.shadowSurface.setScale(1);
				renderer.shieldSurface.setScale(1);
				Graphics.getEffects1().setScale(1);
				Graphics.getEffects2().setScale(1);
			}
			renderer.setPixelate(b);
		});
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
