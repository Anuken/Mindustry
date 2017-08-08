package io.anuke.mindustry;

import static io.anuke.mindustry.Vars.*;
import static io.anuke.ucore.scene.actions.Actions.*;

import com.badlogic.gdx.Application.ApplicationType;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Timer;
import com.badlogic.gdx.utils.Timer.Task;

import io.anuke.mindustry.GameState.State;
import io.anuke.mindustry.entities.Weapon;
import io.anuke.mindustry.input.AndroidInput;
import io.anuke.mindustry.resource.*;
import io.anuke.mindustry.ui.*;
import io.anuke.ucore.core.Draw;
import io.anuke.ucore.core.Settings;
import io.anuke.ucore.function.Listenable;
import io.anuke.ucore.function.VisibilityProvider;
import io.anuke.ucore.graphics.Hue;
import io.anuke.ucore.graphics.Textures;
import io.anuke.ucore.modules.SceneModule;
import io.anuke.ucore.scene.Element;
import io.anuke.ucore.scene.Scene;
import io.anuke.ucore.scene.actions.Actions;
import io.anuke.ucore.scene.builders.*;
import io.anuke.ucore.scene.event.InputEvent;
import io.anuke.ucore.scene.ui.*;
import io.anuke.ucore.scene.ui.layout.*;
import io.anuke.ucore.util.Timers;

public class UI extends SceneModule{
	Table itemtable, weapontable, tools, loadingtable;
	SettingsDialog prefs;
	KeybindDialog keys;
	Dialog about, menu, restart, tutorial, levels, upgrades, load;
	Tooltip tooltip;

	VisibilityProvider play = () -> !GameState.is(State.menu);
	VisibilityProvider nplay = () -> GameState.is(State.menu);

	public UI() {
		Dialog.setShowAction(()-> sequence(Actions.moveToAligned(Gdx.graphics.getWidth()/2, Gdx.graphics.getHeight(), Align.center),
						parallel(Actions.moveToAligned(Gdx.graphics.getWidth()/2, Gdx.graphics.getHeight()/2, Align.center, 0.09f, Interpolation.fade), 
								
								Actions.fadeIn(0.09f, Interpolation.fade))));
		
		Dialog.setHideAction(()-> sequence(
					parallel(Actions.moveBy(0, -Gdx.graphics.getHeight()/2, 0.08f, Interpolation.fade), 
							Actions.fadeOut(0.08f, Interpolation.fade))));
		
		skin.font().setUseIntegerPositions(false);
		skin.font().getData().setScale(Vars.fontscale);
		TooltipManager.getInstance().animations = false;
		
		Dialog.closePadR = -1;
		Dialog.closePadT = 4;
		
		Textures.load("sprites/");
		Textures.repeatWrap("conveyor", "conveyort", "back");
	}
	
	void drawBackground(){
		
		Batch batch = scene.getBatch();
		Draw.color();
		int w = (int)screen.x;
		int h = (int)screen.y;
		
		Draw.color(Hue.lightness(0.6f));
		
		int tw = w/64+1;
		
		float scale = Unit.dp.inPixels(1f);
		
		batch.draw(Textures.get("back"), 0, 0, w, h, 0, 0, (float)w/h/scale * h/Textures.get("back").getHeight()/4f, -1f/scale * h/Textures.get("back").getHeight()/4f);
		
		for(int x = 0; x < tw; x ++){
			float offset = (Timers.time()*2*(x%2-0.5f))/32f;
			batch.draw(Textures.get("conveyort"), x*64*scale, 0, 32*scale, h*scale, 0, offset, 1, h/32 + offset);
		}
		
		Draw.color();
		
		Draw.tscl(Unit.dp.inPixels(1.5f));
		
		Draw.text("[#111111]-( Mindustry )-", w/2, h-Unit.dp.inPixels(16));
		Draw.text("[#f1de60]-( Mindustry )-", w/2, h-Unit.dp.inPixels(10));
		
		Draw.tscl(Unit.dp.inPixels(0.5f));
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
		//TODO just move these dialogs to different files
		
		load = new LoadDialog();
		
		upgrades = new UpgradeDialog();
		
		levels = new LevelDialog();
		
		prefs = new SettingsDialog();
		
		menu = new MenuDialog();
		
		prefs.sliderPref("difficulty", "Difficulty", 1, 0, 2, i -> {
			return i == 0 ? "Easy" : i == 1 ? "Normal" : "Hard";
		});
		
		prefs.screenshakePref();
		prefs.volumePrefs();
		
		prefs.checkPref("tutorial", "Show tutorial Window", true);
		prefs.checkPref("fps", "Show FPS", false);
		prefs.checkPref("noshadows", "Disable shadows", false);

		keys = new KeybindDialog();

		about = new TextDialog("About", aboutText);
		
		for(Cell<?> cell : about.content().getCells())
			cell.left();
		
		tutorial = new TutorialDialog();
		
		restart = new Dialog("The core was destroyed.", "dialog"){
			public Dialog show(Scene scene){
				super.show(scene);
				restart.content().clearChildren();
				if(control.isHighScore()){
					restart.content().add("[YELLOW]New highscore!").pad(6);
					restart.content().row();
				}
				restart.content().add("You lasted until wave [GREEN]" + control.getWave() + "[].").pad(6);
				restart.pack();
				return this;
			}
		};
		
		restart.getButtonTable().addButton("Back to menu", ()->{
			restart.hide();
			GameState.set(State.menu);
			control.reset();
		});
		
		weapontable = fill();
		weapontable.bottom();
		weapontable.setVisible(play);
		
		if(android){
			weapontable.remove();
		}
		
		build.begin(scene);

		new table(){{
			abottom();
			aright();

			new table(){{

				get().background("button");
				
				int rows = 3;
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
					
					ImageButton button = new ImageButton("icon-"+sec.name(), "toggle");
					add(button).fill().height(54).padTop(-10).units(Unit.dp);
					button.getImageCell().size(40).padBottom(4).units(Unit.dp);
					group.add(button);
					
					Table table = new Table();
					table.pad(4);
					
					int i = 0;
					
					for(Recipe r : recipes){
						ImageButton image = new ImageButton(Draw.region(r.result.name()), "select");
						
						image.clicked(()->{
							if(Inventory.hasItems(r.requirements))
								player.recipe = r;
						});
						
						table.add(image).size(size+8).pad(4).units(Unit.dp);
						image.getImageCell().size(size).units(Unit.dp);
						
						image.update(()->{
							
							boolean has = Inventory.hasItems(r.requirements);
							image.setDisabled(!has);
							image.setChecked(player.recipe == r && has);
							//image.setTouchable(has ? Touchable.enabled : Touchable.disabled);
							image.getImage().setColor(has ? Color.WHITE : Color.GRAY);
						});
						
						if(i % rows == rows-1)
							table.row();
						
						i++;
						
						Table tiptable = new Table();
						
						Listenable run = ()->{
							tiptable.clearChildren();
							
							String description = r.result.description();
							
							tiptable.background("button");
							tiptable.add("[PURPLE]" + r.result.name(), 0.75f*fontscale*2f).left().padBottom(2f).units(Unit.dp);
							
							ItemStack[] req = r.requirements;
							for(ItemStack s : req){
								tiptable.row();
								int amount = Math.min(Inventory.getAmount(s.item), s.amount);
								tiptable.add(
										(amount >= s.amount ? "[YELLOW]" : "[RED]")
								+s.item + ": " + amount + " / " +s.amount, fontscale).left();
							}
							
							tiptable.row();
							tiptable.add().size(10).units(Unit.px);
							tiptable.row();
							tiptable.add("[ORANGE]" + description).left();
							tiptable.pad(Unit.dp.inPixels(10f));
						};
						
						run.listen();
						
						Tooltip tip = new Tooltip(tiptable, run){
							public void enter (InputEvent event, float x, float y, int pointer, Element fromActor) {
								if(tooltip != this)
									hideTooltip();
								Element actor = event.getListenerActor();
								if (fromActor != null && fromActor.isDescendantOf(actor)) return;
								setContainerPosition(actor, x, y);
								manager.enter(this);
								run.listen();
								
								tooltip = this;
								
								if(android){
									
									Timer.schedule(new Task(){
										@Override
										public void run(){
											hide();
										}
									}, 1.5f);
									
								}
							}
						};
						
						tip.setInstant(true);

						image.addListener(tip);
					}
					
					//additional padding
					for(int j = 0; j < maxcol - (int)((float)recipes.size/rows+1); j ++){
						table.row();
						table.add().size(size);
					}
					
					table.setVisible(()-> button.isChecked());
					
					stack.add(table);
				}
				
				
				row();
				add(stack).colspan(3);
				get().pad(10f);
				
				get().padLeft(0f);
				get().padRight(0f);
				
				end();
			}}.right().bottom().uniformX();
			
			row();
			
			if(!android){
				new button("Upgrades", ()->{
					upgrades.show();
				}).uniformX().fillX();
			}
			get().setVisible(play);

		}}.end();

		new table(){{
			atop();
			aleft();
			itemtable = new table().top().left().get();
			itemtable.background("button");

			get().setVisible(play);
			
			Label fps = new Label("");
			fps.update(()->{
				fps.setText(Settings.getBool("fps") ? (Gdx.graphics.getFramesPerSecond() + " FPS") : "");
			});
			row();
			add(fps);
			
		}}.end();

		//wave table...
		new table(){{
			atop();
			aright();

			new table(){{
				get().background("button");

				new label(()->"[YELLOW]Wave " + control.getWave()).scale(fontscale*2f).left();

				row();

				new label(()-> control.getEnemiesRemaining() > 0 ?
						control.getEnemiesRemaining() + " Enemies remaining" : "New wave in " + (int) (control.getWaveCountdown() / 60f))
				.minWidth(150);

				get().pad(Unit.dp.inPixels(12));
			}};

			get().setVisible(play);
		}}.end();
		
		
		//if(Gdx.app.getType() != ApplicationType.Android){
		//+- table
		//TODO refactor to make this less messy
		new table(){{
			aleft();
			abottom();
			int base = baseCameraScale;
			int min = base-zoomScale*2;
			int max = base+zoomScale;
			new button("+", ()->{
				if(control.cameraScale < max){
					control.setCameraScale(control.cameraScale+zoomScale);
				}
			}).size(Unit.dp.inPixels(40));
			
			new button("-", ()->{
				if(control.cameraScale > min){
					control.setCameraScale(control.cameraScale-zoomScale);
				}
			}).size(Unit.dp.inPixels(40));
			
			get().setVisible(play);
		}}.end();
		//}
	
		//menu table
		new table(){{
			
			new table("button"){{
				defaults().size(220, 50);
				
				new button("Play", () -> {
					levels.show();
				});

				row();
				
				new button("Load Game", () -> {
					load.show();
				});

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
				
				if(Gdx.app.getType() != ApplicationType.WebGL)
				new button("Exit", () -> {
					Gdx.app.exit();
				});
				
				get().pad(Unit.dp.inPixels(20));
			}};

			get().setVisible(nplay);
		}}.end();
		
		new table(){{
			//atop();
			new table(){{
				get().background("button");
				
				new label("Respawning in"){{
					get().update(()->{
						get().setText("[yellow]Respawning in " + (int)(control.getRespawnTime()/60));
					});
					
					get().setFontScale(0.75f);
				}};
				
				visible(()->{
					return control.getRespawnTime() > 0 && !GameState.is(State.menu);
				});
			}};
		}}.end();
		
		loadingtable = new table("loadDim"){{
			new table("button"){{
				new label("[yellow]Loading..."){{
					get().setName("namelabel");
				}}.scale(1).pad(Unit.dp.inPixels(10));
			}}.end();
		}}.end().get();
		
		loadingtable.setVisible(false);
		
		tools = new Table();
		tools.addIButton("icon-cancel", Unit.dp.inPixels(42), ()->{
			player.recipe = null;
		});
		tools.addIButton("icon-rotate", Unit.dp.inPixels(42), ()->{
			player.rotation++;

			player.rotation %= 4;
		});
		tools.addIButton("icon-check", Unit.dp.inPixels(42), ()->{
			AndroidInput.place();
		});
		
		scene.add(tools);
		
		tools.setVisible(()->{
			return !GameState.is(State.menu) && android && player.recipe != null;
		});
		
		tools.update(()->{
			tools.setPosition(AndroidInput.mousex, Gdx.graphics.getHeight()-AndroidInput.mousey-15*control.cameraScale, Align.top);
		});

		updateItems();

		build.end();
	}
	
	public void updateWeapons(){
		weapontable.clearChildren();
		
		for(Weapon weapon : control.getWeapons()){
			ImageButton button = new ImageButton(Draw.region("weapon-"+weapon.name()), "static");
			button.getImageCell().size(40);
			button.setDisabled(true);
			
			if(weapon != player.weapon)
				button.setColor(Color.GRAY);
			
			weapontable.add(button).size(48, 52);
			
			Table tiptable = new Table();
			String description = weapon.description;
				
			tiptable.background("button");
			tiptable.add("[PURPLE]" + weapon.name(), 0.75f).left().padBottom(2f);
				
			tiptable.row();
			tiptable.row();
			tiptable.add("[ORANGE]" + description).left();
			tiptable.pad(10f);
			
			Tooltip tip = new Tooltip(tiptable);
			
			tip.setInstant(true);

			button.addListener(tip);
			
		}
	}
	
	public void showLoading(){
		showLoading("[yellow]Loading..");
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
	
	public void showTutorial(){
		tutorial.show();
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

		for(Item stack : Inventory.getItemTypes()){
			Image image = new Image(Draw.region("icon-" + stack.name()));
			Label label = new Label("" + Inventory.getAmount(stack));
			label.setFontScale(fontscale*2f);
			itemtable.add(image).size(32).units(Unit.dp);
			itemtable.add(label);
			itemtable.row();
		}
	}
	
}
