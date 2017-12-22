package io.anuke.mindustry.core;

import static io.anuke.mindustry.Vars.*;
import static io.anuke.ucore.scene.actions.Actions.*;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Colors;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;

import io.anuke.mindustry.Mindustry;
import io.anuke.mindustry.Vars;
import io.anuke.mindustry.core.GameState.State;
import io.anuke.mindustry.mapeditor.MapEditor;
import io.anuke.mindustry.mapeditor.MapEditorDialog;
import io.anuke.mindustry.ui.*;
import io.anuke.mindustry.ui.fragments.*;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.Blocks;
import io.anuke.mindustry.world.blocks.types.Configurable;
import io.anuke.ucore.core.*;
import io.anuke.ucore.function.Listenable;
import io.anuke.ucore.function.VisibilityProvider;
import io.anuke.ucore.modules.SceneModule;
import io.anuke.ucore.scene.Element;
import io.anuke.ucore.scene.Skin;
import io.anuke.ucore.scene.builders.build;
import io.anuke.ucore.scene.builders.label;
import io.anuke.ucore.scene.builders.table;
import io.anuke.ucore.scene.event.Touchable;
import io.anuke.ucore.scene.ui.*;
import io.anuke.ucore.scene.ui.Window.WindowStyle;
import io.anuke.ucore.scene.ui.layout.Table;
import io.anuke.ucore.scene.ui.layout.Unit;

public class UI extends SceneModule{
	Table loadingtable, desctable, configtable;
	MindustrySettingsDialog prefs;
	MindustryKeybindDialog keys;
	MapEditorDialog editorDialog;
	Dialog about, restart, levels, upgrades, load, settingserror, gameerror, discord;
	MenuDialog menu;
	Tooltip tooltip;
	Tile configTile;
	Array<String> statlist = new Array<>();
	MapEditor editor = new MapEditor();
	boolean wasPaused = false;
	
	private Fragment blockfrag = new BlocksFragment(),
			menufrag = new MenuFragment(),
			toolfrag = new ToolFragment(),
			hudfrag = new HudFragment(),
			placefrag = new PlacementFragment(),
			weaponfrag = new WeaponFragment();

	VisibilityProvider play = () -> !GameState.is(State.menu);
	VisibilityProvider nplay = () -> GameState.is(State.menu);
	
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
		
		if(nplay.visible()){
			scene.getBatch().getProjectionMatrix().setToOrtho2D(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
			scene.getBatch().begin();
			
			drawBackground();
			
			scene.getBatch().end();
		}
		
		scene.act();
		scene.draw();
	}

	@Override
	public void init(){
		
		configtable = new Table();
		scene.add(configtable);

		editorDialog = new MapEditorDialog(editor);
		
		settingserror = new Dialog("Warning", "dialog");
		settingserror.content().add("[crimson]Failed to access local storage.\nSettings will not be saved.");
		settingserror.content().margin(10f);
		settingserror.getButtonTable().addButton("OK", ()->{
			settingserror.hide();
		}).size(80f, 55f).pad(4);
		
		gameerror = new Dialog("An error has occured", "dialog");
		gameerror.content().add(new Label("[SCARLET]An unexpected error has occured, which would have caused a crash. "
				+ "[]Please report the exact circumstances under which this error occured to the developer: "
				+ "\n[ORANGE]anukendev@gmail.com[]"){{
					setWrap(true);
				}}).width(600f).pad(10f);
		gameerror.buttons().addButton("OK", gameerror::hide).size(200f, 50);
		
		discord = new Dialog("Discord", "dialog");
		discord.content().margin(12f);
		discord.content().add("Join the mindustry discord!\n[orange]" + Vars.discordURL);
		discord.buttons().defaults().size(200f, 50);
		discord.buttons().addButton("Open link", () -> Mindustry.platforms.openLink(Vars.discordURL));
		discord.buttons().addButton("Back", discord::hide);
		
		load = new LoadDialog();
		
		upgrades = new UpgradeDialog();
		
		levels = new LevelDialog();
		
		prefs = new MindustrySettingsDialog();
		prefs.setStyle(Core.skin.get("dialog", WindowStyle.class));
		
		menu = new MenuDialog();

		prefs.sound.volumePrefs();
		
		prefs.game.sliderPref("difficulty", "Difficulty", 1, 0, 2, i -> i == 0 ? "Easy" : i == 1 ? "Normal" : "Hard");
		prefs.game.screenshakePref();
		prefs.game.checkPref("smoothcam", "Smooth Camera", true);
		prefs.game.checkPref("indicators", "Enemy Indicators", true);
		prefs.game.checkPref("effects", "Display Effects", true);

        prefs.graphics.checkPref("fps", "Show FPS", false);
		prefs.graphics.checkPref("vsync", "VSync", true, b -> Gdx.graphics.setVSync(b));
		prefs.graphics.checkPref("lasers", "Show Power Lasers", true);
		prefs.graphics.checkPref("healthbars", "Show Entity Health bars", true);
		prefs.graphics.checkPref("pixelate", "Pixelate Screen", true, b->{
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
				if(!wasPaused)
					GameState.set(State.playing);
			}
		});
		
		prefs.shown(()->{
			if(!GameState.is(State.menu)){
				wasPaused = GameState.is(State.paused);
				if(menu.getScene() != null){
					wasPaused = menu.wasPaused;
				}
				GameState.set(State.paused);
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
		
		restart = new Dialog("The core was destroyed.", "dialog");
		
		restart.shown(()->{
			restart.content().clearChildren();
			if(control.isHighScore()){
				restart.content().add("[YELLOW]New highscore!").pad(6);
				restart.content().row();
			}
			restart.content().add("You lasted until wave [GREEN]" + control.getWave() + "[].").pad(12).get();
			restart.pack();
		});
		
		restart.getButtonTable().addButton("Back to menu", ()->{
			restart.hide();
			GameState.set(State.menu);
			control.reset();
		}).size(200, 50).pad(3);
		
		build.begin(scene);
		
		weaponfrag.build();

		blockfrag.build();
		
		hudfrag.build();
		
		menufrag.build();
		
		placefrag.build();
		
		loadingtable = new table("loadDim"){{
			get().setTouchable(Touchable.enabled);
			get().addImage("white").growX()
			.height(3f).pad(4f).growX().get().setColor(Colors.get("accent"));
			row();
			new label("[accent]Loading..."){{
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
	
	void invalidateAll(){
		for(Element e : scene.getElements()){
			if(e instanceof Table){
				((Table)e).invalidateHierarchy();
			}
		}
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
	
	public void showError(String text){
		new Dialog("[crimson]An error has occured", "dialog"){{
			content().margin(15);
			content().add(text);
			getButtonTable().addButton("OK", this::hide).size(90, 50).pad(4);
		}}.show();
	}

	public void showErrorClose(String text){
		new Dialog("[crimson]A critical error has occured", "dialog"){{
			content().margin(15);
			content().add(text);
			getButtonTable().addButton("Exit", Gdx.app::exit).size(90, 50).pad(4);
		}}.show();
	}
	
	public void showLoading(){
		showLoading("[accent]Loading..");
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
	
	public void showConfirm(String title, String text, Listenable confirmed){
		FloatingDialog dialog = new FloatingDialog(title);
		dialog.content().add(text).pad(4f);
		dialog.buttons().defaults().size(200f, 54f).pad(2f);
		dialog.buttons().addButton("Cancel", dialog::hide);
		dialog.buttons().addButton("OK", () -> {
			dialog.hide();
			confirmed.listen();
		});
		dialog.show();
	}
	
}
