package io.anuke.mindustry.core;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Colors;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Align;
import io.anuke.mindustry.Mindustry;
import io.anuke.mindustry.Vars;
import io.anuke.mindustry.core.GameState.State;
import io.anuke.mindustry.mapeditor.MapEditor;
import io.anuke.mindustry.mapeditor.MapEditorDialog;
import io.anuke.mindustry.net.Net;
import io.anuke.mindustry.ui.*;
import io.anuke.mindustry.ui.fragments.*;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.Blocks;
import io.anuke.mindustry.world.blocks.types.Configurable;
import io.anuke.ucore.core.*;
import io.anuke.ucore.function.Consumer;
import io.anuke.ucore.function.Listenable;
import io.anuke.ucore.modules.SceneModule;
import io.anuke.ucore.scene.Element;
import io.anuke.ucore.scene.Skin;
import io.anuke.ucore.scene.actions.Actions;
import io.anuke.ucore.scene.builders.build;
import io.anuke.ucore.scene.builders.label;
import io.anuke.ucore.scene.builders.table;
import io.anuke.ucore.scene.event.Touchable;
import io.anuke.ucore.scene.ui.*;
import io.anuke.ucore.scene.ui.TextField.TextFieldFilter;
import io.anuke.ucore.scene.ui.TextField.TextFieldFilter.DigitsOnlyFilter;
import io.anuke.ucore.scene.ui.Window.WindowStyle;
import io.anuke.ucore.scene.ui.layout.Table;
import io.anuke.ucore.scene.ui.layout.Unit;
import io.anuke.ucore.util.Bundles;
import io.anuke.ucore.util.Strings;

import java.io.IOException;

import static io.anuke.mindustry.Vars.*;
import static io.anuke.ucore.scene.actions.Actions.*;

public class UI extends SceneModule{
	Table loadingtable, configtable;
	MindustrySettingsDialog prefs;
	MindustryKeybindDialog keys;
	MapEditorDialog editorDialog;
	Dialog about, restart, levels, upgrades, load, settingserror, gameerror, discord, join;
	MenuDialog menu;
	Tooltip tooltip;
	Tile configTile;
	MapEditor editor;
	String lastip = "localhost";
	int lastport = Vars.port;
	boolean wasPaused = false;
	
	private Fragment menufrag = new MenuFragment(),
			toolfrag = new ToolFragment(),
			hudfrag = new HudFragment(),
			placefrag = new PlacementFragment(),
			weaponfrag = new WeaponFragment();
	
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
		
		Settings.setErrorHandler(()-> Timers.run(1f, ()-> settingserror.show()));
		
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
	
	protected void loadSkin(){
		skin = new Skin(Gdx.files.internal("ui/uiskin.json"), Core.atlas);
	}
	
	void drawBackground(){
		int w = (int)screen.x;
		int h = (int)screen.y;
		
		Draw.color();
		
		TextureRegion back = Draw.region("background");
		float backscl = Unit.dp.scl(5f);
		
		Draw.alpha(0.7f);
		Core.batch.draw(back, w/2 - back.getRegionWidth()*backscl/2 +240f, h/2 - back.getRegionHeight()*backscl/2 + 250f, 
				back.getRegionWidth()*backscl, back.getRegionHeight()*backscl);
		
		float logoscl = (int)Unit.dp.scl(7);
		TextureRegion logo = skin.getRegion("logotext");
		float logow = logo.getRegionWidth()*logoscl;
		float logoh = logo.getRegionHeight()*logoscl;
		
		
		Draw.color();
		Core.batch.draw(logo, w/2 - logow/2, h - logoh + 15, logow, logoh);
		
	}

	@Override
	public void update(){
		if(Vars.debug && !Vars.showUI) return;
		
		if(GameState.is(State.menu)){
			scene.getBatch().getProjectionMatrix().setToOrtho2D(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
			scene.getBatch().begin();
			
			drawBackground();
			
			scene.getBatch().end();
		}
		
		scene.act();
		scene.draw();

		if(control.showCursor()) {
			Draw.color();
			float scl = Unit.dp.scl(3f);
			scene.getBatch().begin();
			Draw.rect("controller-cursor", Gdx.input.getX(), Gdx.graphics.getHeight() - Gdx.input.getY(), 16*scl, 16*scl);
			scene.getBatch().end();
		}
	}

	@Override
	public void init(){
		
		configtable = new Table();
		scene.add(configtable);

		if(!Vars.gwt){
			editor = new MapEditor();
			editorDialog = new MapEditorDialog(editor);
		}

		join = new FloatingDialog("$text.joingame.title");
		join.content().add("$text.joingame.ip").left();
		join.content().addField("localhost", text -> lastip = text).size(180f, 54f);
		join.content().row();
		join.content().add("$text.server.port").left();
		join.content().addField(Vars.port + "", new DigitsOnlyFilter(), text -> lastport = Strings.parseInt(text)).size(180f, 54f);
		join.buttons().defaults().size(140f, 60f).pad(4f);
		join.buttons().addButton("$text.cancel", join::hide);
		join.buttons().addButton("$text.ok", () -> {
			showLoading("$text.connecting");

			Timers.runTask(2f, () -> {
				try{
					Net.connect(lastip, lastport);
				}catch (IOException e) {
					showError(Bundles.format("text.connectfail", Strings.parseException(e, false)));
					hideLoading();
				}
			});
		}).disabled(b -> lastip.isEmpty() || lastport == Integer.MIN_VALUE);
		
		settingserror = new Dialog("Warning", "dialog");
		settingserror.content().add("[crimson]Failed to access local storage.\nSettings will not be saved.");
		settingserror.content().margin(10f);
		settingserror.getButtonTable().addButton("OK", settingserror::hide).size(80f, 55f).pad(4);
		
		gameerror = new Dialog("$text.error.crashtitle", "dialog");
		gameerror.content().labelWrap("$text.error.crashmessage").width(600f).pad(10f);
		gameerror.buttons().addButton("#text.ok", gameerror::hide).size(200f, 50);
		
		discord = new Dialog("Discord", "dialog");
		discord.content().margin(12f);
		discord.content().add("$text.discord");
		discord.content().row();
		discord.content().add("[orange]"+Vars.discordURL);
		discord.buttons().defaults().size(200f, 50);
		discord.buttons().addButton("$text.openlink", () -> Mindustry.platforms.openLink(Vars.discordURL));
		discord.buttons().addButton("$text.back", discord::hide);
		
		load = new LoadDialog();
		
		upgrades = new UpgradeDialog();
		
		levels = new LevelDialog();
		
		prefs = new MindustrySettingsDialog();
		prefs.setStyle(Core.skin.get("dialog", WindowStyle.class));
		
		menu = new MenuDialog();

		prefs.sound.volumePrefs();
		
		prefs.game.sliderPref("difficulty", 1, 0, 2, i -> Bundles.get("setting.difficulty." + (i == 0 ? "easy" : i == 1 ? "normal" : "hard")));
		prefs.game.screenshakePref();
		prefs.game.checkPref("smoothcam", true);
		prefs.game.checkPref("indicators", true);
		prefs.game.checkPref("effects", true);
		prefs.game.sliderPref("sensitivity", 100, 10, 300, i -> i + "%");
		prefs.game.sliderPref("saveinterval", 90, 15, 5*120, i -> Bundles.format("setting.seconds", i));

        prefs.graphics.checkPref("fps", false);
		prefs.graphics.checkPref("vsync", true, b -> Gdx.graphics.setVSync(b));
		prefs.graphics.checkPref("lasers", true);
		prefs.graphics.checkPref("healthbars", true);
		prefs.graphics.checkPref("pixelate", true, b->{
			if(b){
				Vars.renderer.pixelSurface.setScale(Core.cameraScale);
				Vars.renderer.shadowSurface.setScale(Core.cameraScale);
				Vars.renderer.shieldSurface.setScale(Core.cameraScale);
			}else{
				Vars.renderer.shadowSurface.setScale(1);
				Vars.renderer.shieldSurface.setScale(1);
			}
			renderer.setPixelate(b);
		});

		Gdx.graphics.setVSync(Settings.getBool("vsync"));
		
		prefs.hidden(()->{
			if(!GameState.is(State.menu)){
				if(!wasPaused || Net.active())
					GameState.set(State.playing);
			}
		});
		
		prefs.shown(()->{
			if(!GameState.is(State.menu)){
				wasPaused = GameState.is(State.paused);
				if(menu.getScene() != null){
					wasPaused = menu.wasPaused;
				}
				if(!Net.active()) GameState.set(State.paused);
				menu.hide();
			}
		});

		keys = new MindustryKeybindDialog();

		about = new FloatingDialog("About");
		about.addCloseButton();
		for(String text : aboutText){
			about.content().add(text).left();
			about.content().row();
		}
		
		restart = new Dialog("$text.gameover", "dialog");
		
		restart.shown(()->{
			restart.content().clearChildren();
			if(control.isHighScore()){
				restart.content().add("$text.highscore").pad(6);
				restart.content().row();
			}
			restart.content().add("$text.lasted").pad(12).get();
			restart.content().add("[GREEN]" + control.getWave());
			restart.pack();
		});
		
		restart.getButtonTable().addButton("$text.menu", ()->{
			restart.hide();
			GameState.set(State.menu);
			control.reset();
		}).size(200, 50).pad(3);
		
		build.begin(scene);
		
		weaponfrag.build();
		
		hudfrag.build();
		
		menufrag.build();
		
		placefrag.build();
		
		loadingtable = new table("loadDim"){{
			touchable(Touchable.enabled);
			get().addImage("white").growX()
			.height(3f).pad(4f).growX().get().setColor(Colors.get("accent"));
			row();
			new label("$text.loading"){{
				get().setName("namelabel");
			}}.pad(10);
			row();
			get().addImage("white").growX()
			.height(3f).pad(4f).growX().get().setColor(Colors.get("accent"));
		}}.end().get();
		
		loadingtable.setVisible(false);
		
		toolfrag.build();

		updateItems();

		build.end();

	}
	
	public void showGameError(){
		gameerror.show();
	}
	
	public void updateWeapons(){
		((WeaponFragment)weaponfrag).updateWeapons();
	}
	
	public void fadeRespawn(boolean in){
		((HudFragment)hudfrag).fadeRespawn(in);
	}
	
	public void showConfig(Tile tile){
		configTile = tile;
		
		configtable.setVisible(true);
		configtable.clear();
		((Configurable)tile.block()).buildTable(tile, configtable);
		configtable.pack();
		
		configtable.update(()->{
			Vector2 pos = Graphics.screen(tile.worldx(), tile.worldy());
			configtable.setPosition(pos.x, pos.y, Align.center);
			if(configTile == null || configTile.block() == Blocks.air){
				hideConfig();
			}
		});
	}
	
	public boolean hasConfigMouse(){
		Element e = scene.hit(Gdx.input.getX(), Gdx.graphics.getHeight() - Gdx.input.getY(), true);
		return e != null && (e == configtable || e.isDescendantOf(configtable));
	}
	
	public void hideConfig(){
		configtable.setVisible(false);
	}

	public void showTextInput(String title, String text, String def, TextFieldFilter filter, Consumer<String> confirmed){
		new Dialog(title, "dialog"){{
			content().margin(30);
			content().add(text).padRight(6f);
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

	public void showError(String text){
		new Dialog("$text.error.title", "dialog"){{
			content().margin(15);
			content().add(text);
			buttons().addButton("$text.ok", this::hide).size(90, 50).pad(4);
		}}.show();
	}

	public void showErrorClose(String text){
		new Dialog("$text.error.title", "dialog"){{
			content().margin(15);
			content().add(text);
			buttons().addButton("$text.quit", Gdx.app::exit).size(90, 50).pad(4);
		}}.show();
	}
	
	public void showLoading(){
		showLoading("$text.loading");
	}
	
	public void showLoading(String text){
		loadingtable.<Label>find("namelabel").setText(text);
		loadingtable.setVisible(true);
		loadingtable.toFront();
	}
	
	public void hideLoading(){
		loadingtable.setVisible(false);
	}
	
	public void showPrefs(){
		prefs.show();
	}
	
	public void showControls(){
		keys.show();
	}
	
	public void showLevels(){
		levels.show();
	}
	
	public void showLoadGame(){
		load.show();
	}

	public void showJoinGame(){
		join.show();
	}

	public void hideJoinGame(){
		join.hide();
	}
	
	public void showMenu(){
		menu.show();
	}
	
	public void hideMenu(){
		menu.hide();
		
		if(scene.getKeyboardFocus() != null && scene.getKeyboardFocus() instanceof Dialog){
			((Dialog)scene.getKeyboardFocus()).hide();
		}
	}
	
	public void showRestart(){
		restart.show();
	}
	
	public void hideTooltip(){
		if(tooltip != null)
			tooltip.hide();
	}

	public void showInfo(String info){
		scene.table().add(info).get().getParent().actions(Actions.fadeOut(4f), Actions.removeActor());
	}
	
	public void showAbout(){
		about.show();
	}
	
	public boolean onDialog(){
		return scene.getKeyboardFocus() instanceof Dialog;
	}
	
	public boolean isGameOver(){
		return restart.getScene() != null;
	}
	
	public void showUpgrades(){
		upgrades.show();
	}
	
	public void showDiscord(){
		discord.show();
	}
	
	public void showEditor(){
		editorDialog.show();
	}
	
	public MapEditorDialog getEditorDialog(){
		return editorDialog;
	}

	public ToolFragment getTools(){
		return (ToolFragment)toolfrag;
	}
	
	public MapEditor getEditor(){
		return editor;
	}
	
	public void reloadLevels(){
		((LevelDialog)levels).reload();
	}
	
	public boolean isEditing(){
		return editorDialog.getScene() != null;
	}

	public void updateItems(){
		((HudFragment)hudfrag).updateItems();
	}

	public MindustrySettingsDialog getPrefs() {
		return prefs;
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
