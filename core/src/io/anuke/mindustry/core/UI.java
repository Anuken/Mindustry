package io.anuke.mindustry.core;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Colors;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.utils.Align;
import io.anuke.mindustry.Vars;
import io.anuke.mindustry.io.Platform;
import io.anuke.mindustry.mapeditor.MapEditorDialog;
import io.anuke.mindustry.ui.dialogs.*;
import io.anuke.mindustry.ui.fragments.*;
import io.anuke.ucore.core.Core;
import io.anuke.ucore.core.Graphics;
import io.anuke.ucore.core.Settings;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.function.Consumer;
import io.anuke.ucore.function.Listenable;
import io.anuke.ucore.graphics.Draw;
import io.anuke.ucore.modules.SceneModule;
import io.anuke.ucore.scene.Skin;
import io.anuke.ucore.scene.builders.build;
import io.anuke.ucore.scene.ui.Dialog;
import io.anuke.ucore.scene.ui.TextField;
import io.anuke.ucore.scene.ui.TextField.TextFieldFilter;
import io.anuke.ucore.scene.ui.TooltipManager;
import io.anuke.ucore.scene.ui.layout.Unit;
import io.anuke.ucore.util.Mathf;

import java.util.Locale;

import static io.anuke.mindustry.Vars.control;
import static io.anuke.ucore.scene.actions.Actions.*;

public class UI extends SceneModule{
	public AboutDialog about;
	public RestartDialog restart;
	public LevelDialog levels;
	public LoadDialog load;
	public DiscordDialog discord;
	public JoinDialog join;
	public HostDialog host;
	public PausedDialog paused;
	public SettingsMenuDialog settings;
	public ControlsDialog controls;
	public MapEditorDialog editor;
	public LanguageDialog language;
	public BansDialog bans;
	public AdminsDialog admins;
	public TraceDialog traces;
	public ChangelogDialog changelog;

	public final MenuFragment menufrag = new MenuFragment();
    public final ToolFragment toolfrag = new ToolFragment();
    public final HudFragment hudfrag = new HudFragment();
    public final PlacementFragment placefrag = new PlacementFragment();
    public final ChatFragment chatfrag = new ChatFragment();
    public final PlayerListFragment listfrag = new PlayerListFragment();
    public final BackgroundFragment backfrag = new BackgroundFragment();
    public final LoadingFragment loadfrag = new LoadingFragment();
    public final BlockConfigFragment configfrag = new BlockConfigFragment();
    public final DebugFragment debugfrag = new DebugFragment();

    private Locale lastLocale;
	
	public UI() {
		Dialog.setShowAction(()-> sequence(
			alpha(0f),
			originCenter(),
			moveToAligned(Gdx.graphics.getWidth()/2, Gdx.graphics.getHeight()/2, Align.center), 
			scaleTo(0.0f, 1f),
			parallel(
				scaleTo(1f, 1f, 0.1f, Interpolation.fade), 
				fadeIn(0.1f, Interpolation.fade)
			)
		));
		
		Dialog.setHideAction(()-> sequence(
			parallel(
				scaleTo(0.01f, 0.01f, 0.1f, Interpolation.fade), 
				fadeOut(0.1f, Interpolation.fade)
			)
		));
		
		TooltipManager.getInstance().animations = false;
		
		Settings.setErrorHandler(()-> Timers.run(1f, ()-> showError("[crimson]Failed to access local storage.\nSettings will not be saved.")));
		
		Settings.defaults("pixelate", true);
		
		Dialog.closePadR = -1;
		Dialog.closePadT = 5;
		
		Colors.put("description", Color.WHITE);
		Colors.put("turretinfo", Color.ORANGE);
		Colors.put("iteminfo", Color.LIGHT_GRAY);
		Colors.put("powerinfo", Color.YELLOW);
		Colors.put("liquidinfo", Color.ROYAL);
		Colors.put("craftinfo", Color.LIGHT_GRAY);
		Colors.put("missingitems", Color.SCARLET);
		Colors.put("health", Color.YELLOW);
		Colors.put("healthstats", Color.SCARLET);
		Colors.put("interact", Color.ORANGE);
		Colors.put("accent", Color.valueOf("f4ba6e"));
		Colors.put("place", Color.PURPLE);
		Colors.put("placeInvalid", Color.RED);
		Colors.put("placeRotate", Color.ORANGE);
		Colors.put("break", Color.CORAL);
		Colors.put("breakStart", Color.YELLOW);
		Colors.put("breakInvalid", Color.RED);
	}

	@Override
	protected void loadSkin(){
		skin = new Skin(Gdx.files.internal("ui/uiskin.json"), Core.atlas);
		Mathf.each(font -> {
			font.setUseIntegerPositions(false);
			font.getData().setScale(Vars.fontscale);
			font.getData().down += Unit.dp.scl(4f);
			font.getData().lineHeight -= Unit.dp.scl(2f);
		}, skin.font(), skin.getFont("default-font-chat"), skin.getFont("korean"));
	}

	@Override
	public synchronized void update(){
		if(Vars.debug && !Vars.showUI) return;

		if(Graphics.drawing()) Graphics.end();
		
		act();

		if(control.showCursor()) {
			Draw.color();

			float scl = Unit.dp.scl(3f);

			Graphics.begin();
			Draw.rect("controller-cursor", Graphics.mouse().x, Graphics.mouse().y, 16*scl, 16*scl);
			Graphics.end();
		}
	}

	@Override
	public void init(){

		editor = new MapEditorDialog();
		controls = new ControlsDialog();
		restart = new RestartDialog();
		join = new JoinDialog();
		discord = new DiscordDialog();
		load = new LoadDialog();
		levels = new LevelDialog();
		language = new LanguageDialog();
		settings = new SettingsMenuDialog();
		paused = new PausedDialog();
		changelog = new ChangelogDialog();
		about = new AboutDialog();
		host = new HostDialog();
		bans = new BansDialog();
		admins = new AdminsDialog();
		traces = new TraceDialog();
		
		build.begin(scene);

		backfrag.build();
		hudfrag.build();
		configfrag.build();
		menufrag.build();
		placefrag.build();
		toolfrag.build();
		chatfrag.build();
		listfrag.build();
		debugfrag.build();
		loadfrag.build();

		build.end();
	}

	@Override
	public synchronized boolean hasMouse() {
		return super.hasMouse();
	}

	public Locale getLocale(){
		String loc = Settings.getString("locale");
		if(loc.equals("default")){
			return Locale.getDefault();
		}else{
			if(lastLocale == null || !lastLocale.toString().equals(loc)){
				if(loc.contains("_")){
					String[] split = loc.split("_");
					lastLocale = new Locale(split[0], split[1]);
				}else{
					lastLocale = new Locale(loc);
				}
			}

			return lastLocale;
		}
	}

	public void showTextInput(String title, String text, String def, TextFieldFilter filter, Consumer<String> confirmed){
		new Dialog(title, "dialog"){{
			content().margin(30).add(text).padRight(6f);
			TextField field = content().addField(def, t->{}).size(170f, 50f).get();
			field.setTextFieldFilter((f, c) -> field.getText().length() < 12 && filter.acceptChar(f, c));
			Platform.instance.addDialog(field);
			buttons().defaults().size(120, 54).pad(4);
			buttons().addButton("$text.ok", () -> {
				confirmed.accept(field.getText());
				hide();
			}).disabled(b -> field.getText().isEmpty());
			buttons().addButton("$text.cancel", this::hide);
		}}.show();
	}

	public void showTextInput(String title, String text, String def, Consumer<String> confirmed){
		showTextInput(title, text, def, (field, c) -> true, confirmed);
	}

	public void showInfo(String info){
		new Dialog("$text.info.title", "dialog"){{
			content().margin(15).add(info).width(600f).get().setWrap(true);
			buttons().addButton("$text.ok", this::hide).size(90, 50).pad(4);
		}}.show();
	}

	public void showError(String text){
		new Dialog("$text.error.title", "dialog"){{
			content().margin(15).add(text);
			buttons().addButton("$text.ok", this::hide).size(90, 50).pad(4);
		}}.show();
	}

	public void showConfirm(String title, String text, Listenable confirmed){
		FloatingDialog dialog = new FloatingDialog(title);
		dialog.content().add(text).pad(4f);
		dialog.buttons().defaults().size(200f, 54f).pad(2f);
		dialog.buttons().addButton("$text.cancel", dialog::hide);
		dialog.buttons().addButton("$text.ok", () -> {
			dialog.hide();
			confirmed.listen();
		});
		dialog.keyDown(Keys.ESCAPE, dialog::hide);
		dialog.keyDown(Keys.BACK, dialog::hide);
		dialog.show();
	}

	public void showConfirmListen(String title, String text, Consumer<Boolean> listener){
		FloatingDialog dialog = new FloatingDialog(title);
		dialog.content().add(text).pad(4f);
		dialog.buttons().defaults().size(200f, 54f).pad(2f);
		dialog.buttons().addButton("$text.cancel", () -> {
			dialog.hide();
			listener.accept(true);
		});
		dialog.buttons().addButton("$text.ok", () -> {
			dialog.hide();
			listener.accept(true);
		});
		dialog.show();
	}
	
}
