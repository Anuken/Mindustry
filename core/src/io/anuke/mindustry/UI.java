package io.anuke.mindustry;

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

import io.anuke.mindustry.GameState.State;
import io.anuke.mindustry.input.AndroidInput;
import io.anuke.mindustry.input.PlaceMode;
import io.anuke.mindustry.resource.*;
import io.anuke.mindustry.ui.*;
import io.anuke.mindustry.world.Map;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.Blocks;
import io.anuke.mindustry.world.blocks.types.Configurable;
import io.anuke.ucore.core.*;
import io.anuke.ucore.entities.Entities;
import io.anuke.ucore.function.StringSupplier;
import io.anuke.ucore.function.VisibilityProvider;
import io.anuke.ucore.modules.SceneModule;
import io.anuke.ucore.scene.Element;
import io.anuke.ucore.scene.Skin;
import io.anuke.ucore.scene.actions.Actions;
import io.anuke.ucore.scene.builders.*;
import io.anuke.ucore.scene.event.Touchable;
import io.anuke.ucore.scene.ui.*;
import io.anuke.ucore.scene.ui.Window.WindowStyle;
import io.anuke.ucore.scene.ui.layout.Stack;
import io.anuke.ucore.scene.ui.layout.Table;
import io.anuke.ucore.scene.ui.layout.Unit;
import io.anuke.ucore.util.Mathf;

public class UI extends SceneModule{
	Table itemtable, weapontable, tools, loadingtable, desctable, respawntable, configtable;
	MindustrySettingsDialog prefs;
	MindustryKeybindDialog keys;
	Dialog about, restart, levels, upgrades, load, settingserror;
	MenuDialog menu;
	Tooltip tooltip;
	Tile configTile;
	Array<String> statlist = new Array<>();
	boolean wasPaused = false;

	VisibilityProvider play = () -> !GameState.is(State.menu);
	VisibilityProvider nplay = () -> GameState.is(State.menu);

	private Array<Item> tempItems = new Array<>();
	
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
		
		Settings.setErrorHandler(()->{
			Timers.run(1f, ()->{
				settingserror.show();
			});
		});
		
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
		Colors.put("interact", Color.ORANGE);
	}
	
	protected void loadSkin(){
		skin = new Skin(Gdx.files.internal("ui/uiskin.json"), Core.atlas);
	}
	
	void drawBackground(){
		int w = (int)screen.x;
		int h = (int)screen.y;
		
		Draw.color();
		
		TextureRegion back = Draw.region("background");
		float backscl = 5.5f;
		
		Core.batch.draw(back, w/2 - back.getRegionWidth()*backscl/2, h/2 - back.getRegionHeight()*backscl/2, 
				back.getRegionWidth()*backscl, back.getRegionHeight()*backscl);
		
		float logoscl = (int)Unit.dp.inPixels(7);
		TextureRegion logo = skin.getRegion("logotext");
		float logow = logo.getRegionWidth()*logoscl;
		float logoh = logo.getRegionHeight()*logoscl;
		
		Draw.color();
		Core.batch.draw(logo, w/2 - logow/2, h - logoh + 15, logow, logoh);
		
		Draw.color();
		
	}

	@Override
	public void update(){

		if(nplay.visible()){
			scene.getBatch().getProjectionMatrix().setToOrtho2D(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
			scene.getBatch().begin();
			
			drawBackground();
			
			scene.getBatch().end();
		}
		
		super.update();
	}

	@Override
	public void init(){
		
		configtable = new Table();
		scene.add(configtable);
		
		settingserror = new Dialog("Warning", "dialog");
		settingserror.content().add("[crimson]Failed to access local storage.\nSettings will not be saved.");
		settingserror.content().pad(10f);
		settingserror.getButtonTable().addButton("OK", ()->{
			settingserror.hide();
		}).size(80f, 55f).pad(4);
		
		load = new LoadDialog();
		
		upgrades = new UpgradeDialog();
		
		levels = new LevelDialog();
		
		prefs = new MindustrySettingsDialog();
		prefs.setStyle(Core.skin.get("dialog", WindowStyle.class));
		
		menu = new MenuDialog();
		
		prefs.sliderPref("difficulty", "Difficulty", 1, 0, 2, i -> {
			return i == 0 ? "Easy" : i == 1 ? "Normal" : "Hard";
		});
		
		prefs.screenshakePref();
		prefs.volumePrefs();
		
		prefs.checkPref("fps", "Show FPS", false);
		prefs.checkPref("noshadows", "Disable shadows", false);
		prefs.checkPref("smoothcam", "Smooth Camera", true);
		prefs.checkPref("indicators", "Enemy Indicators", true);
		prefs.checkPref("pixelate", "Pixelate Screen", true, b->{
			if(b){
				Graphics.getSurface("pixel").setScale(Core.cameraScale);
				Graphics.getSurface("shadow").setScale(Core.cameraScale);
				Graphics.getSurface("shield").setScale(Core.cameraScale);
			}else{
				Graphics.getSurface("shadow").setScale(1);
				Graphics.getSurface("shield").setScale(1);
			}
			renderer.setPixelate(b);
		});
		
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
		
		if(!android){
			prefs.content().row();
			prefs.content().addButton("Controls", () -> {
				keys.show(scene);
			}).size(300f, 50f).pad(5f).units(Unit.dp);
		}

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
			restart.content().add("You lasted until wave [GREEN]" + control.getWave() + "[].").pad(12).units(Unit.dp).get();
			restart.pack();
		});
		
		restart.getButtonTable().addButton("Back to menu", ()->{
			restart.hide();
			GameState.set(State.menu);
			control.reset();
		}).size(200, 50).pad(3).units(Unit.dp);
		
		weapontable = fill();
		weapontable.bottom().left();
		weapontable.setVisible(play);
		
		if(android){
			weapontable.remove();
		}
		
		build.begin(scene);

		new table(){{
			abottom();
			aright();
			
			new table("button"){{
				visible(()->player.recipe != null);
				desctable = get();
				fillX();
			}}.end().uniformX();
			
			row();

			new table("pane"){{
				
				int rows = 4;
				int maxcol = 0;
				float size = 48;
				
				Stack stack = new Stack();
				ButtonGroup<ImageButton> group = new ButtonGroup<>();
				Array<Recipe> recipes = new Array<Recipe>();
				
				for(Section sec : Section.values()){
					recipes.clear();
					Recipe.getBy(sec, recipes);
					maxcol = Math.max((int)((float)recipes.size/rows+1), maxcol);
				}
				
				for(Section sec : Section.values()){
					recipes.clear();
					Recipe.getBy(sec, recipes);
					
					Table table = new Table();
					
					ImageButton button = new ImageButton("icon-"+sec.name(), "toggle");
					button.clicked(()->{
						if(!table.isVisible() && player.recipe != null){
							player.recipe = null;
						}
					});
					button.setName("sectionbutton" + sec.name());
					add(button).growX().height(54).padTop(sec.ordinal() <= 2 ? -10 : -5).units(Unit.dp);
					button.getImageCell().size(40).padBottom(4).padTop(2).units(Unit.dp);
					group.add(button);
					
					if(sec.ordinal() % 3 == 2 && sec.ordinal() > 0){
						row();
					}
					
					table.pad(4);
					table.top().left();
					
					int i = 0;
					
					for(Recipe r : recipes){
						TextureRegion region = Draw.hasRegion(r.result.name() + "-icon") ? 
								Draw.region(r.result.name() + "-icon") : Draw.region(r.result.name());
						ImageButton image = new ImageButton(region, "select");
						
						image.clicked(()->{
							if(player.recipe == r){
								player.recipe = null;
							}else{
								player.recipe = r;
								updateRecipe();
							}
						});
						
						table.add(image).size(size+8).pad(2).units(Unit.dp);
						image.getImageCell().size(size).units(Unit.dp);
						
						image.update(()->{
							
							boolean canPlace = !control.tutorial.active() || control.tutorial.canPlace();
							boolean has = control.hasItems(r.requirements) && canPlace;
							//image.setDisabled(!has);
							image.setChecked(player.recipe == r);
							image.setTouchable(canPlace ? Touchable.enabled : Touchable.disabled);
							image.getImage().setColor(has ? Color.WHITE : Color.DARK_GRAY);
						});
						
						if(i % rows == rows-1)
							table.row();
						
						i++;
					}
					
					table.setVisible(()-> button.isChecked());
					
					stack.add(table);
				}
				
				
				row();
				add(stack).colspan(Section.values().length);
				get().pad(10f);
				
				get().padLeft(0f);
				get().padRight(0f);
				
				end();
			}}.right().bottom().uniformX();
			
			visible(play);

		}}.end();

		new table(){{
			atop();
			aleft();
			
			new table(){{
				left();
				defaults().size(66).units(Unit.dp).left();
				float isize = Unit.dp.inPixels(40);
				
				new imagebutton("icon-menu", isize, ()->{
					showMenu();
				});
				
				new imagebutton("icon-settings", isize, ()->{
					prefs.show();
				});

				new imagebutton("icon-pause", isize, ()->{
					GameState.set(GameState.is(State.paused) ? State.playing : State.paused);
				}){{
					get().update(()->{
						get().getStyle().imageUp = Core.skin.getDrawable(GameState.is(State.paused) ? "icon-play" : "icon-pause");
					});
				}};
			}}.end();
			
			row();
			
			itemtable = new table("button").end().top().left().fillX().size(-1).get();

			get().setVisible(play);
			
			Label fps = new Label(()->(Settings.getBool("fps") ? (Gdx.graphics.getFramesPerSecond() + " FPS") : ""));
			row();
			add(fps).size(-1);
			
		}}.end();
		
		new table(){{
			control.tutorial.buildUI(this);
			
			visible(()->control.tutorial.active());
		}}.end();
		
		//paused table
		new table(){{
			visible(()->GameState.is(State.paused));
			atop();
			
			new table("pane"){{
				new label("[orange]< paused >").scale(Unit.dp.inPixels(0.75f)).pad(6).units(Unit.dp);
			}}.end();
		}}.end();

		//wave table...
		new table(){{
			atop();
			aright();

			new table("button"){{

				new label(()->"[orange]Wave " + control.getWave()).scale(fontscale*2f).left();

				row();

				new label(()-> control.getEnemiesRemaining() > 0 ?
						control.getEnemiesRemaining() + " Enemies remaining" : 
							control.tutorial.active() ? "waiting..." : "New wave in " + (int) (control.getWaveCountdown() / 60f))
				.minWidth(150);

				get().pad(Unit.dp.inPixels(12));
			}};

			get().setVisible(play);
		}}.end();
		
		if(!android){
			//menu table
			new table(){{
				
				new table(){{
					float scale = 4f;
					defaults().size(100*scale, 21*scale).pad(-10f).units(Unit.dp);
					
					add(new MenuButton("text-play", ()-> levels.show()));
					row();
					
					add(new MenuButton("text-tutorial", ()-> control.playMap(Map.tutorial)));
					row();
					
					if(!gwt){
						add(new MenuButton("text-load", ()-> load.show()));
						row();
					}
					
					add(new MenuButton("text-settings", ()-> prefs.show()));
					row();
					
					if(!gwt){
						add(new MenuButton("text-exit", ()-> Gdx.app.exit()));
					}
					get().pad(Unit.dp.inPixels(16));
				}};
	
				visible(nplay);
			}}.end();
		}else{
			new table(){{
				defaults().size(120f).pad(5).units(Unit.dp);
				float isize = Unit.dp.inPixels(14f*4);
				
				new imagebutton("icon-play-2", isize, () -> {
					levels.show();
				}).text("Play").padTop(4f);
				
				new imagebutton("icon-tutorial", isize, ()->{
					control.playMap(Map.tutorial);
				}).text("Tutorial").padTop(4f);
				
				new imagebutton("icon-load", isize, () -> {
					load.show();
				}).text("Load").padTop(4f);

				new imagebutton("icon-tools", isize, () -> {
					prefs.show(scene);
				}).text("Settings").padTop(4f);
				
				visible(nplay);
			}}.end();
		}
		
		//settings icon
		new table(){{
			atop().aright();
			new imagebutton("icon-info", Unit.dp.inPixels(30f), ()->{
				about.show();
			}).get().pad(14);
		}}.end().visible(nplay);
		
		if(debug){
			new table(){{
				abottom();
				aleft();
				new label((StringSupplier)()->"[purple]entities: " + Entities.amount()).left();
				row();
				new label("[red]DEBUG MODE").scale(0.5f).left();
			}}.end();
			
			new table(){{
				atop();
				new table("button"){{
					defaults().left().growX();
					atop();
					aleft();
					new label((StringSupplier)()->"[red]total: " 
					+ String.format("%.1f", (float)Profiler.total/Profiler.total*100f)+ "% - " + Profiler.total).left();
					row();
					new label((StringSupplier)()->"[yellow]draw: " 
					+ String.format("%.1f", (float)Profiler.draw/Profiler.total*100f)+ "% - " + Profiler.draw).left();
					row();
					new label((StringSupplier)()->"[green]blockDraw: " 
					+ String.format("%.1f", (float)Profiler.blockDraw/Profiler.total*100f)+ "% - " + Profiler.blockDraw).left();
					row();
					new label((StringSupplier)()->"[blue]entityDraw: " 
					+ String.format("%.1f", (float)Profiler.entityDraw/Profiler.total*100f)+ "% - " + Profiler.entityDraw).left();
					row();
					new label((StringSupplier)()->"[purple]entityUpdate: " 
					+ String.format("%.1f", (float)Profiler.entityUpdate/Profiler.total*100f)+ "% - " + Profiler.entityUpdate).left();
					row();
				}}.width(400f).end();
			}}.end();
		}
		
		//respawn background table
		new table("white"){{
			respawntable = get();
			respawntable.setColor(Color.CLEAR);
			
		}}.end();
		
		//respawn table
		new table(){{
			new table("pane"){{
				
				new label(()->"[orange]Respawning in " + (int)(control.getRespawnTime()/60)).scale(0.75f).pad(10);
				
				visible(()->control.getRespawnTime() > 0 && !GameState.is(State.menu));
				
			}}.end();
		}}.end();
		
		if(android){
			//placement table
			new table(){{
				visible(()->player.recipe != null && !GameState.is(State.menu));
				abottom();
				aleft();
				
				
				new table("pane"){{
					new label(()->"Placement Mode: [orange]" + AndroidInput.mode.name()).pad(4).units(Unit.dp);
					row();
					
					aleft();
					
					new table(){{
						aleft();
						ButtonGroup<ImageButton> group = new ButtonGroup<>();
						
						defaults().size(58, 62).pad(6).units(Unit.dp);
						
						for(PlaceMode mode : PlaceMode.values()){
							new imagebutton("icon-" + mode.name(), "toggle",  Unit.dp.inPixels(10*3), ()->{
								AndroidInput.mode = mode;
							}){{
								group.add(get());
							}};
						}
						
						new imagebutton("icon-cancel", Unit.dp.inPixels(14*3), ()->{
							player.recipe = null;
						}).visible(()->player.recipe != null && AndroidInput.mode == PlaceMode.touch);
						
						new imagebutton("icon-rotate-arrow", Unit.dp.inPixels(14*3), ()->{
							player.rotation ++;
							player.rotation %= 4;
						}).update(i->{
							i.getImage().setOrigin(Align.center);
							i.getImage().setRotation(player.rotation*90);
						}).visible(()->player.recipe != null && AndroidInput.mode == PlaceMode.touch 
								&& player.recipe.result.rotate);
						
					}}.left().end();
				}}.end();
			}}.end();
		
		}
		
		loadingtable = new table("loadDim"){{
			get().setTouchable(Touchable.enabled);
			//new table(){{
			get().addImage("white").growX()
			.height(3f).pad(4f).growX().units(Unit.dp).get().setColor(Color.ORANGE);
			row();
			new label("[orange]Loading..."){{
				get().setName("namelabel");
			}}.pad(10).units(Unit.dp);
			row();
			get().addImage("white").growX()
			.height(3f).pad(4f).growX().units(Unit.dp).get().setColor(Color.ORANGE);
			//}}.end();
		}}.end().get();
		
		loadingtable.setVisible(false);
		
		tools = new Table();
		tools.addIButton("icon-cancel", Unit.dp.inPixels(42), ()->{
			player.recipe = null;
		});
		
		tools.addIButton("icon-rotate", Unit.dp.inPixels(42), ()->{
			player.rotation ++;
			player.rotation %= 4;
		});
		
		tools.addIButton("icon-check", Unit.dp.inPixels(42), ()->{
			AndroidInput.place();
		});
		
		scene.add(tools);
		
		tools.setVisible(()->
			!GameState.is(State.menu) && android && player.recipe != null && control.hasItems(player.recipe.requirements) &&
			AndroidInput.mode == PlaceMode.cursor
		);
		
		tools.update(()->{
			tools.setPosition(AndroidInput.mousex, Gdx.graphics.getHeight()-AndroidInput.mousey-15*Core.cameraScale, Align.top);
		});

		updateItems();

		build.end();
	}
	
	public void fadeRespawn(boolean in){
		respawntable.addAction(Actions.color(in ? new Color(0, 0, 0, 0.3f) : Color.CLEAR, 0.3f));
	}
	
	void updateRecipe(){
		Recipe recipe = player.recipe;
		desctable.clear();
		
		desctable.defaults().left();
		desctable.left();
		desctable.pad(Unit.dp.inPixels(12));
		
		Table header = new Table();
		
		desctable.add(header).left();
		
		desctable.row();
		
		TextureRegion region = Draw.hasRegion(recipe.result.name() + "-icon") ? 
				Draw.region(recipe.result.name() + "-icon") : Draw.region(recipe.result.name());
		
		header.addImage(region).size(8*5).padTop(4).units(Unit.dp);
		Label nameLabel = new Label(recipe.result.formalName);
		nameLabel.setWrap(true);
		header.add(nameLabel).padLeft(4).width(135f).units(Unit.dp);
		
		//extra info
		if(recipe.result.fullDescription != null){
			header.addButton("?", ()->{
				statlist.clear();
				recipe.result.getStats(statlist);
				
				Label desclabel = new Label(recipe.result.fullDescription);
				desclabel.setWrap(true);
				
				boolean wasPaused = GameState.is(State.paused);
				GameState.set(State.paused);
				
				FloatingDialog d = new FloatingDialog("Block Info");
				Table top = new Table();
				top.left();
				top.add(new Image(Draw.region(recipe.result.name))).size(8*5 * recipe.result.width).units(Unit.dp);
				top.add("[orange]"+recipe.result.formalName).padLeft(6f).units(Unit.dp);
				d.content().add(top).fill().left();
				d.content().row();
				d.content().add(desclabel).width(600).units(Unit.dp);
				d.content().row();
				
				if(statlist.size > 0){
					d.content().add("[coral][[extra block info]:").padTop(6).padBottom(5).left();
					d.content().row();
				}
				
				for(String s : statlist){
					d.content().add(s).left();
					d.content().row();
				}
				
				d.buttons().addButton("OK", ()->{
					if(!wasPaused) GameState.set(State.playing);
					d.hide();
				}).size(110, 50).pad(10f).units(Unit.dp);
				
				d.show();
			}).expandX().padLeft(4).top().right().size(36f, 40f).units(Unit.dp);
		}
		
		
		desctable.add().pad(2).units(Unit.dp);
		
		Table requirements = new Table();
		
		desctable.row();
		
		desctable.add(requirements);
		desctable.left();
		
		for(ItemStack stack : recipe.requirements){
			ItemStack fs = stack;
			requirements.addImage(Draw.region("icon-"+stack.item.name())).size(8*3).units(Unit.dp);
			Label reqlabel = new Label("");
			
			reqlabel.update(()->{
				int current = control.getAmount(fs.item);
				String text = Mathf.clamp(current, 0, stack.amount) + "/" + stack.amount;
				
				reqlabel.setColor(current < stack.amount ? Colors.get("missingitems") : Color.WHITE);
				
				reqlabel.setText(text);
			});
			
			requirements.add(reqlabel).left();
			requirements.row();
		}
		
		desctable.row();
		
		Label label = new Label("[health]health: " + recipe.result.health + (recipe.result.description == null ?
				"" : ("\n[]" + recipe.result.description)));
		label.setWrap(true);
		desctable.add(label).width(200).padTop(4).padBottom(2).units(Unit.dp);
		
	}
	
	public void updateWeapons(){
		weapontable.clearChildren();
		
		ButtonGroup group = new ButtonGroup();
		
		weapontable.defaults().size(58, 62);
		
		for(Weapon weapon : control.getWeapons()){
			ImageButton button = new ImageButton(Draw.region(weapon.name()), "toggle");
			button.getImageCell().size(8*5);
			
			group.add(button);
			
			button.clicked(()->{
				if(weapon == player.weapon) return;
				player.weapon = weapon;
				button.setChecked(true);
			});
			
			button.setChecked(weapon == player.weapon);
			
			weapontable.add(button);
			
			Table tiptable = new Table();
			String description = weapon.description;
				
			tiptable.background("button");
			tiptable.add("" + weapon.name(), 0.5f).left().padBottom(3f);
				
			tiptable.row();
			tiptable.row();
			tiptable.add("[GRAY]" + description).left();
			tiptable.pad(14f);
			
			Tooltip tip = new Tooltip(tiptable);
			
			tip.setInstant(true);

			button.addListener(tip);
			
		}
		
		weapontable.addIButton("icon-menu", 8*4, ()->{
			upgrades.show();
		});
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
			content().pad(Unit.dp.inPixels(15));
			content().add(text);
			getButtonTable().addButton("OK", ()->{
				hide();
			}).size(90, 50).pad(4).units(Unit.dp);
		}}.show();
	}
	
	public void showLoading(){
		showLoading("[orange]Loading..");
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

	public void updateItems(){
		itemtable.clear();
		itemtable.left();
		
		tempItems.clear();
		for(Item item : control.getItems().keys()){
			tempItems.add(item);
		}
		tempItems.sort();

		for(Item stack : tempItems){
			Image image = new Image(Draw.region("icon-" + stack.name()));
			Label label = new Label("" + Mindustry.formatter.format(control.getAmount(stack)));
			label.setFontScale(fontscale*1.5f);
			itemtable.add(image).size(8*3).units(Unit.dp);
			itemtable.add(label).left();
			itemtable.row();
		}
	}
	
}
