package io.anuke.mindustry;

import static io.anuke.mindustry.Vars.*;
import static io.anuke.ucore.scene.actions.Actions.*;

import com.badlogic.gdx.Application.ApplicationType;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Colors;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;

import io.anuke.mindustry.GameState.State;
import io.anuke.mindustry.input.AndroidInput;
import io.anuke.mindustry.input.PlaceMode;
import io.anuke.mindustry.resource.*;
import io.anuke.mindustry.ui.*;
import io.anuke.mindustry.world.Map;
import io.anuke.ucore.core.*;
import io.anuke.ucore.function.VisibilityProvider;
import io.anuke.ucore.modules.SceneModule;
import io.anuke.ucore.scene.Skin;
import io.anuke.ucore.scene.actions.Actions;
import io.anuke.ucore.scene.builders.*;
import io.anuke.ucore.scene.event.Touchable;
import io.anuke.ucore.scene.ui.*;
import io.anuke.ucore.scene.ui.Window.WindowStyle;
import io.anuke.ucore.scene.ui.layout.*;
import io.anuke.ucore.util.Mathf;

public class UI extends SceneModule{
	Table itemtable, weapontable, tools, loadingtable, desctable, respawntable;
	SettingsDialog prefs;
	KeybindDialog keys;
	Dialog about, menu, restart, levels, upgrades, load, settingserror;
	Tooltip tooltip;

	VisibilityProvider play = () -> !GameState.is(State.menu);
	VisibilityProvider nplay = () -> GameState.is(State.menu);

	private Array<Item> tempItems = new Array<>();
	
	public UI() {
		Dialog.setShowAction(()-> sequence(Actions.moveToAligned(Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), Align.center),
						parallel(Actions.moveToAligned(Gdx.graphics.getWidth()/2,
								Gdx.graphics.getHeight()/2, Align.center, 0.09f, Interpolation.fade), 
								
								Actions.fadeIn(0.09f, Interpolation.fade))));
		
		Dialog.setHideAction(()-> sequence(
					parallel(Actions.moveBy(0, -Gdx.graphics.getHeight()/2, 0.08f, Interpolation.fade), 
							Actions.fadeOut(0.08f, Interpolation.fade))));
		
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
		
		Dialog.closePadR = -1;
		Dialog.closePadT = 5;
		
		Colors.put("description", Color.WHITE);
		Colors.put("turretinfo", Color.ORANGE);
		Colors.put("missingitems", Color.SCARLET);
		Colors.put("health", Color.YELLOW);
	}
	
	protected void loadSkin(){
		skin = new Skin(Gdx.files.internal("ui/uiskin.json"), Core.atlas);
	}
	
	void drawBackground(){
		int w = (int)screen.x;
		int h = (int)screen.y;
		
		Draw.color();
		
		TextureRegion back = Draw.region("background");
		float backscl = 5;
		
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
		
		settingserror = new Dialog("Warning", "dialog");
		settingserror.content().add("[crimson]Failed to access local storage.\nSettings will not be saved.");
		settingserror.content().pad(10f);
		settingserror.getButtonTable().addButton("OK", ()->{
			settingserror.hide();
		}).size(80f, 55f).pad(4);
		
		load = new LoadDialog();
		
		upgrades = new UpgradeDialog();
		
		levels = new LevelDialog();
		
		prefs = new SettingsDialog();
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
		
		prefs.hidden(()->{
			if(!GameState.is(State.menu)){
				GameState.set(State.playing);
			}
		});
		
		prefs.shown(()->{
			if(!GameState.is(State.menu)){
				GameState.set(State.paused);
				menu.hide();
			}
		});

		keys = new KeybindDialog();

		about = new TextDialog("About", aboutText);
		
		for(Cell<?> cell : about.content().getCells())
			cell.left();
		
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
				float size = 46;
				
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
					GameState.set(State.paused);
					showMenu();
				});
				
				new imagebutton("icon-settings", isize, ()->{
					GameState.set(State.paused);
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

			new table(){{
				get().background("button");

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
		
		new table(){{
			control.tutorial.buildUI(this);
			
			visible(()->control.tutorial.active());
		}}.end();
	
		//menu table
		new table(){{
			
			new table("pane"){{
				defaults().size(220, 48).pad(3);
				
				new button("Play", () -> {
					levels.show();
				});
				
				row();
				
				new button("Tutorial", ()->{
					//TODO show loading, etc
					control.playMap(Map.tutorial);
				});
				
				if(Gdx.app.getType() != ApplicationType.WebGL){
					row();
				
					new button("Load Game", () -> {
						load.show();
					});
				}

				row();

				new button("Settings", () -> {
					prefs.show(scene);
				});

				row();
				
				if(!android){
					new button("Controls", () -> {
						keys.show(scene);
					});
					
					row();
				}

				new button("About", () -> {
					about.show(scene);
				});
				
				row();
				
				if(Gdx.app.getType() != ApplicationType.WebGL && !android){
					new button("Exit", () -> {
						Gdx.app.exit();
					});
				}
				
				get().pad(Unit.dp.inPixels(16));
			}};

			get().setVisible(nplay);
		}}.end();
		
		if(debug){
			new table(){{
				abottom();
				aleft();
				new label("[red]DEBUG MODE").scale(0.5f);
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
			new table("button"){{
				new label("[orange]Loading..."){{
					get().setName("namelabel");
				}}.scale(2f*Vars.fontscale).pad(Unit.dp.inPixels(10));
			}}.end();
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
		header.add(recipe.result.formalName).padLeft(4).units(Unit.dp);
		
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
		
		Label label = new Label("[health]health: " + recipe.result.health + (recipe.result.description() == null ?
				"" : ("\n[]" + recipe.result.description())));
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
