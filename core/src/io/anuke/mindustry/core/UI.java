package io.anuke.mindustry.core;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Colors;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.utils.Align;
import io.anuke.mindustry.Mindustry;
import io.anuke.mindustry.Vars;
import io.anuke.mindustry.mapeditor.MapEditorDialog;
import io.anuke.mindustry.ui.dialogs.*;
import io.anuke.mindustry.ui.fragments.*;
import io.anuke.ucore.core.*;
import io.anuke.ucore.function.Consumer;
import io.anuke.ucore.function.Listenable;
import io.anuke.ucore.modules.SceneModule;
import io.anuke.ucore.scene.Skin;
import io.anuke.ucore.scene.actions.Actions;
import io.anuke.ucore.scene.builders.build;
import io.anuke.ucore.scene.ui.Dialog;
import io.anuke.ucore.scene.ui.TextField;
import io.anuke.ucore.scene.ui.TextField.TextFieldFilter;
import io.anuke.ucore.scene.ui.TooltipManager;
import io.anuke.ucore.scene.ui.layout.Unit;

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

	public final MenuFragment menufrag = new MenuFragment();
    public final ToolFragment toolfrag = new ToolFragment();
    public final HudFragment hudfrag = new HudFragment();
    public final PlacementFragment placefrag = new PlacementFragment();
    public final WeaponFragment weaponfrag = new WeaponFragment();
    public final ChatFragment chatfrag = new ChatFragment();
    public final PlayerListFragment listfrag = new PlayerListFragment();
    public final BackgroundFragment backfrag = new BackgroundFragment();
    public final LoadingFragment loadfrag = new LoadingFragment();
    public final BlockConfigFragment configfrag = new BlockConfigFragment();
	
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
		
		skin.font().setUseIntegerPositions(false);
		skin.font().getData().setScale(Vars.fontscale);
		skin.font().getData().down += 4f;
		skin.font().getData().lineHeight -= 2f;
		
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
	}

	@Override
	public void update(){
		if(Vars.debug && !Vars.showUI) return;
		
		scene.act();
		scene.draw();

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
		settings = new SettingsMenuDialog();
		paused = new PausedDialog();
		about = new AboutDialog();
		host = new HostDialog();
		
		build.begin(scene);

		backfrag.build();
		weaponfrag.build();
		hudfrag.build();
		configfrag.build();
		menufrag.build();
		placefrag.build();
		toolfrag.build();
		chatfrag.build();
		listfrag.build();
		loadfrag.build();

		build.end();
	}

	public void showTextInput(String title, String text, String def, TextFieldFilter filter, Consumer<String> confirmed){
		new Dialog(title, "dialog"){{
			content().margin(30).add(text).padRight(6f);
			TextField field = content().addField(def, t->{}).size(170f, 50f).get();
			field.setTextFieldFilter((f, c) -> field.getText().length() < 12 && filter.acceptChar(f, c));
			Mindustry.platforms.addDialog(field);
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
		scene.table().add("[accent]" + info).padBottom(Gdx.graphics.getHeight()/2+100f).get().getParent().actions(Actions.fadeOut(4f), Actions.removeActor());
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
		dialog.show();
	}
	
}
