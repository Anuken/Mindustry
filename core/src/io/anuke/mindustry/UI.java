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

import io.anuke.mindustry.entities.Weapon;
import io.anuke.mindustry.resource.*;
import io.anuke.mindustry.ui.*;
import io.anuke.ucore.core.Draw;
import io.anuke.ucore.core.Settings;
import io.anuke.ucore.function.VisibilityProvider;
import io.anuke.ucore.graphics.Hue;
import io.anuke.ucore.graphics.Textures;
import io.anuke.ucore.modules.SceneModule;
import io.anuke.ucore.scene.Scene;
import io.anuke.ucore.scene.actions.Actions;
import io.anuke.ucore.scene.builders.*;
import io.anuke.ucore.scene.ui.*;
import io.anuke.ucore.scene.ui.layout.Cell;
import io.anuke.ucore.scene.ui.layout.Stack;
import io.anuke.ucore.scene.ui.layout.Table;
import io.anuke.ucore.util.Timers;

public class UI extends SceneModule{
	Table itemtable, weapontable;
	SettingsDialog prefs;
	KeybindDialog keys;
	Dialog about, menu, restart, tutorial, levels, upgrades;

	VisibilityProvider play = () -> {
		return playing;
	};

	VisibilityProvider nplay = () -> {
		return !playing;
	};

	public UI() {
		Dialog.setShowAction(()->{
			return sequence(Actions.moveToAligned(Gdx.graphics.getWidth()/2, Gdx.graphics.getHeight(), Align.center), 
						parallel(Actions.moveToAligned(Gdx.graphics.getWidth()/2, Gdx.graphics.getHeight()/2, Align.center, 0.09f, Interpolation.fade), 
								
								Actions.fadeIn(0.09f, Interpolation.fade)));
		});
		
		Dialog.setHideAction(()->{
			return sequence(
					parallel(Actions.moveBy(0, -Gdx.graphics.getHeight()/2, 0.08f, Interpolation.fade), 
							Actions.fadeOut(0.08f, Interpolation.fade)));
		});
		
		skin.font().setUseIntegerPositions(false);
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
		
		batch.draw(Textures.get("back"), 0, 0, 0, 0, w, h);
		
		for(int x = 0; x < tw; x ++){
			batch.draw(Textures.get("conveyort"), x*64, 0, 0, (int)(Timers.time()*2*(x%2-0.5f)), 32, h);
		}
		
		Draw.color();
		
		Draw.tscl(1.5f);
		
		Draw.text("[#111111]-( Mindustry )-", w/2, h-16);
		Draw.text("[#f1de60]-( Mindustry )-", w/2, h-10);
		
		Draw.tscl(0.5f);
	}

	@Override
	public void update(){

		if(!playing){
			scene.getBatch().getProjectionMatrix().setToOrtho2D(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
			scene.getBatch().begin();
			
			drawBackground();
			
			scene.getBatch().end();
		}
		
		super.update();
	}

	@Override
	public void init(){
		//TODO oh my god just move these dialogs to different files
		
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

		keys = new KeybindDialog();

		about = new TextDialog("About", aboutText);
		
		for(Cell<?> cell : about.content().getCells())
			cell.left();
		
		tutorial = new TutorialDialog();
		
		restart = new Dialog("The core was destroyed.", "dialog"){
			public Dialog show(Scene scene){
				super.show(scene);
				restart.content().clearChildren();
				if(hiscore){
					restart.content().add("[YELLOW]New highscore!").pad(6);
					restart.content().row();
				}
				restart.content().add("You lasted until wave [GREEN]" + wave + "[].").pad(6);
				restart.pack();
				return this;
			}
		};
		
		restart.getButtonTable().addButton("Back to menu", ()->{
			restart.hide();
			playing = false;
			GameState.reset();
		});
		
		weapontable = fill();
		weapontable.bottom();
		weapontable.setVisible(play);
		
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
					add(button).fill().height(54).padTop(-10);
					button.getImageCell().size(40).padBottom(4);
					group.add(button);
					
					Table table = new Table();
					table.pad(4);
					
					int i = 0;
					
					for(Recipe r : recipes){
						ImageButton image = new ImageButton(Draw.region(r.result.name()), "select");
						
						image.clicked(()->{
							if(Inventory.hasItems(r.requirements))
							recipe = r;
						});
						
						table.add(image).size(size+8).pad(4);
						image.getImageCell().size(size);
						
						image.update(()->{
							
							boolean has = Inventory.hasItems(r.requirements);
							image.setDisabled(!has);
							image.setChecked(recipe == r && has);
							//image.setTouchable(has ? Touchable.enabled : Touchable.disabled);
							image.getImage().setColor(has ? Color.WHITE : Color.GRAY);
						});
						
						if(i % rows == rows-1)
							table.row();
						
						i++;
						
						Table tiptable = new Table();
						
						Runnable run = ()->{
							tiptable.clearChildren();
							
							String description = r.result.description();
							
							tiptable.background("button");
							tiptable.add("[PURPLE]" + r.result.name(), 0.75f).left().padBottom(2f);
							
							ItemStack[] req = r.requirements;
							for(ItemStack s : req){
								tiptable.row();
								int amount = Math.min(items.get(s.item, 0), s.amount);
								tiptable.add(
										(amount >= s.amount ? "[YELLOW]" : "[RED]")
								+s.item + ": " + amount + " / " +s.amount, 0.5f).left();
							}
							
							tiptable.row();
							tiptable.add().size(10);
							tiptable.row();
							tiptable.add("[ORANGE]" + description).left();
							tiptable.pad(10f);
						};
						
						run.run();
						
						Tooltip tip = new Tooltip(tiptable, run);
						
						tip.setInstant(true);

						image.addListener(tip);
					}
					
					//additional padding
					for(int j = 0; j < maxcol - (int)((float)recipes.size/rows+1); j ++){
						table.row();
						table.add().size(size);
					}
					
					table.setVisible(()->{
						return button.isChecked();
					});
					
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
			
			new button("Upgrades", ()->{
				upgrades.show();
			}).uniformX().fillX();

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

					new label("Wave 1"){{
							get().setFontScale(1f);
							get().update(() -> {
								get().setText("[YELLOW]Wave " + wave);
							});
						}}.left();

					row();

					new label("Time"){{
							get().update(() -> {
								get().setText(enemies > 0 ? 
										enemies + " Enemies remaining" : "New wave in " + (int) (wavetime / 60f));
							});
						}}.minWidth(150);

					get().pad(12);
				}};

			get().setVisible(play);
		}}.end();
		
		
		//+- table
		new table(){{
			aleft();
			abottom();
			new button("+", ()->{
				if(control.cameraScale < 4){
					control.cameraScale = 4;
					control.resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
					Draw.getSurface("pixel").setScale(control.cameraScale);
					Draw.getSurface("shadow").setScale(control.cameraScale);
				}
			}).size(40);
			
			new button("-", ()->{
				if(control.cameraScale > 3){
					control.cameraScale = 3;
					control.resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
					Draw.getSurface("pixel").setScale(control.cameraScale);
					Draw.getSurface("shadow").setScale(control.cameraScale);
				}
			}).size(40);
			
			get().setVisible(play);
		}}.end();
	
		//menu table
		new table(){{
			float w = 200;
			
			new table("button"){{
				new button("Play", () -> {
					levels.show();
				}).width(w);

				row();

				new button("Settings", () -> {
					prefs.show(scene);
				}).width(w);

				row();

				new button("Controls", () -> {
					keys.show(scene);
				}).width(w);

				row();

				new button("About", () -> {
					about.show(scene);
				}).width(w);
				
				row();
				
				if(Gdx.app.getType() != ApplicationType.WebGL)
				new button("Exit", () -> {
					Gdx.app.exit();
				}).width(w);
				
				get().pad(20);
			}};

			get().setVisible(nplay);
		}}.end();
		
		new table(){{
			//atop();
			new table(){{
				get().background("button");
				
				new label("Respawning in"){{
					get().update(()->{
						get().setText("[yellow]Respawning in " + (int)(respawntime/60));
					});
					
					get().setFontScale(0.75f);
				}};
				
				visible(()->{
					return respawntime > 0 && playing;
				});
			}};
		}}.end();

		updateItems();

		build.end();
	}
	
	public void updateWeapons(){
		weapontable.clearChildren();
		
		for(Weapon weapon : Weapon.values()){
			if(weapons.get(weapon) == Boolean.TRUE){
				ImageButton button = new ImageButton(Draw.region("weapon-"+weapon.name()), "static");
				button.getImageCell().size(40);
				button.setDisabled(true);
				if(weapon != currentWeapon)
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
	}
	
	public void showTutorial(){
		tutorial.show();
	}
	
	public void showRestart(){
		restart.show();
	}

	public void updateItems(){
		itemtable.clear();

		for(Item stack : items.keys()){
			Image image = new Image(Draw.region("icon-" + stack.name()));
			Label label = new Label("" + items.get(stack));
			label.setFontScale(1f);
			itemtable.add(image).size(32);
			itemtable.add(label);
			itemtable.row();
		}
	}
	
}
