package io.anuke.moment;

import static io.anuke.moment.world.TileType.tilesize;

import java.util.function.BooleanSupplier;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureWrap;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.utils.Array;

import io.anuke.moment.entities.Enemy;
import io.anuke.moment.resource.*;
import io.anuke.moment.world.Tile;
import io.anuke.moment.world.TileType;
import io.anuke.ucore.core.*;
import io.anuke.ucore.modules.SceneModule;
import io.anuke.ucore.scene.Scene;
import io.anuke.ucore.scene.builders.*;
import io.anuke.ucore.scene.style.Styles;
import io.anuke.ucore.scene.ui.*;
import io.anuke.ucore.scene.ui.layout.Stack;
import io.anuke.ucore.scene.ui.layout.Table;
import io.anuke.ucore.util.Mathf;
import io.anuke.ucore.util.Timers;

public class UI extends SceneModule<Moment>{
	Table itemtable;
	PrefsDialog prefs;
	KeybindDialog keys;
	Dialog about, menu, restart, tutorial;
	Texture conveyor = new Texture("sprites/conveyor.png"), conveyort = new Texture("sprites/conveyort.png");

	BooleanSupplier play = () -> {
		return main.playing;
	};

	BooleanSupplier nplay = () -> {
		return !main.playing;
	};

	public UI() {
		Styles.styles.font().setUseIntegerPositions(false);
		TooltipManager.getInstance().animations = false;
		
		Dialog.closePadR = -1;
		Dialog.closePadT = 4;
		conveyor.setWrap(TextureWrap.Repeat, TextureWrap.Repeat);
		conveyort.setWrap(TextureWrap.Repeat, TextureWrap.Repeat);
	}
	
	void drawBackground(){
		
		Batch batch = scene.getBatch();
		Draw.color();
		int w = gwidth();
		int h = gheight();
		
		batch.draw(conveyor, 0, 0, (int)Timers.time(), 0, w, h);
		
		int tw = w/64+1;//, th = h/64+1;
		
		for(int x = 0; x < tw; x ++){
			batch.draw(conveyort, x*64, 0, 0, (int)Timers.time(), 32, h);
		}
		
		Draw.tscl(1.5f);
		
		
		Draw.text("[DARK_GRAY]-( Mindustry )-", w/2, h-16);
		Draw.text("[#f1de60]-( Mindustry )-", w/2, h-10);
		
		Draw.tscl(0.5f);
		

		
		
		//Draw.rect("conveyor", w/2, h/2, 1000, 1000);
	}

	@Override
	public void update(){

		if(main.playing){
			scene.getBatch().setProjectionMatrix(get(Control.class).camera.combined);
			scene.getBatch().begin();
			Tile tile = main.tiles[tilex()][tiley()];
			if(tile.block() != TileType.air){
				String error = tile.block().error(tile);
				if(error != null){
					Draw.tcolor(Color.SCARLET);
					Draw.tscl(1 / 8f);
					Draw.text(error, tile.worldx(), tile.worldy() + tilesize);

				}else if(tile.block().ammo != null){
					Draw.tscl(1 / 8f);
					Draw.tcolor(Color.GREEN);
					Draw.text("Ammo: " + tile.entity.shots, tile.worldx(), tile.worldy() - tilesize);
				}

				Draw.tscl(0.5f);
				Draw.clear();
			}
			scene.getBatch().end();
			
			if(UInput.keyUp("menu")){
				if(menu.getScene() != null){
					menu.hide();
					main.paused = false;
				}else{
					main.paused = true;
					menu.show(scene);
				}
			}
			
		}else{
			scene.getBatch().getProjectionMatrix().setToOrtho2D(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
			scene.getBatch().begin();
			
			drawBackground();
			
			scene.getBatch().end();
		}
		
		super.update();
	}

	@Override
	public void init(){

		prefs = new PrefsDialog("Settings");

		prefs.sliderPref("screenshake", "Screen Shake", 4, 0, 12, i -> {
			return (i / 4f) + "x";
		});
		
		prefs.sliderPref("difficulty", "Difficulty", 1, 0, 2, i -> {
			return i == 0 ? "Easy" : i == 1 ? "Normal" : "Hard";
		});
		
		
		prefs.checkPref("fps", "Show FPS", false);

		keys = new KeybindDialog();

		about = new Dialog("About");
		about.getContentTable().add("Made by [ROYAL]Anuken[] for the" + "\nGDL Metal Monstrosity jam.\n" 
		+ "\nSources used:"
		+ "\n- [YELLOW]bfxr.com[] for sound effects"
		+ "\n- [RED]freemusicarchive.org[] for music"
		+ "\n- Music made by [GREEN]RoccoW[]"
				);
		about.addCloseButton();
		
		tutorial = new Dialog("Tutorial", "dialog"){
			@Override
			public void hide(){
				super.hide();
				main.playing = true;
				main.paused = false;
			}
		};
		tutorial.addCloseButton();
		tutorial.getButtonTable().addButton("OK", ()->{
			tutorial.hide();
		});
		
		tutorial.content().add(
				  "[GREEN]Default Controls:[WHITE]\n[YELLOW][[WASD][] to move, [YELLOW][[R][] to rotate blocks." 
				+ "\nHold [YELLOW][[R-MOUSE][] to destroy blocks, click [YELLOW][[L-MOUSE][] to place them."
				+ "\n[YELLOW][[L-MOUSE][] to shoot."
				+ "\n\n[GOLD]Every 20 seconds, a new wave will appear."
				+ "\nBuild turrets to defend the core."
				+ "\nIf the core is destroyed, you lose the game."
				+ "\n[LIME]To collect building resources, \nmove them into the core with conveyors."
				+ "\n[LIME]Place [ORANGE]drills[] on the right material,\nthey will automatically mine material\nand dump it to nearby conveyors or turrets."
				+ "\n\n[SCARLET]To produce steel, feed coal and iron into a smelter."
				);
		
		tutorial.content().pad(8);
		
		restart = new Dialog("The core was destroyed.", "dialog"){
			public Dialog show(Scene scene){
				super.show(scene);
				restart.content().clearChildren();
				restart.content().add("You lasted until wave [GREEN]" + main.wave + "[].").pad(6);
				restart.pack();
				return this;
			}
		};
		
		restart.getButtonTable().addButton("Back to menu", ()->{
			restart.hide();
			main.playing = false;
			main.restart();
		});
		
		menu = new Dialog("Paused", "dialog");
		menu.content().addButton("Back", ()->{
			menu.hide();
			main.paused = false;
		}).width(200);
		
		menu.content().row();
		menu.content().addButton("Back to menu", ()->{
			new Dialog("Confirm", "dialog"){
				{
					text("Are you sure you want to quit?");
					button("Ok", true);
					button("Cancel", false);
				}
				
				protected void result(Object object){
					if(object == Boolean.TRUE){
						menu.hide();
						main.paused = false;
						main.playing = false;
					}
				}
			}.show(scene);
			
		}).width(200);
		
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
					add(button).size(size).height(size+8);
					button.getImageCell().size(40).padBottom(4);
					group.add(button);
					
					Table table = new Table();
					
					int i = 0;
					
					for(Recipe r : recipes){
						ImageButton image = new ImageButton(Draw.region(r.result.name()), "select");
						
						image.clicked(()->{
							if(main.hasItems(r.requirements))
							main.recipe = r;
						});
						
						table.add(image).size(size+8).pad(4);
						image.getImageCell().size(size);
						
						image.update(()->{
							
							boolean has = main.hasItems(r.requirements);
							image.setDisabled(!has);
							image.setChecked(main.recipe == r && has);
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
							if(r.result.ammo != null){
								description += "\n[SALMON]Ammo: " + r.result.ammo.name();
							}
	
							
							tiptable.background("button");
							tiptable.add("[PURPLE]" + r.result.name(), 0.75f).left().padBottom(2f);
							
							ItemStack[] req = r.requirements;
							for(ItemStack s : req){
								tiptable.row();
								int amount = Math.min(main.items.get(s.item, 0), s.amount);
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
					
					
					if(sec == Section.distribution){
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
				
			}}.right().bottom();

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
								get().setText("[YELLOW]Wave " + Moment.i.wave);
							});
						}}.left();

					row();

					new label("Time"){{
							get().update(() -> {
								get().setText(Enemy.amount > 0 ? Enemy.amount + " Enemies remaining" : "New wave in " + (int) (main.wavetime / 60f));
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
			Control c = main.get(Control.class);
			new button("+", ()->{
				if(c.cameraScale < 4f){
					c.cameraScale = 4f;
					c.resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
				}
			}).size(40);
			
			new button("-", ()->{
				if(c.cameraScale > 3f){
					c.cameraScale = 3f;
					c.resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
				}
			}).size(40);
			
			get().setVisible(play);
		}}.end();
	
		//menu table
		new table(){{
			float w = 200;

			new button("Play", () -> {
				main.play();
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

			get().setVisible(nplay);
		}};

		updateItems();

		build.end();
	}
	
	public void showRestart(){
		restart.show(scene);
	}

	public void updateItems(){
		itemtable.clear();

		for(Item stack : main.items.keys()){
			Image image = new Image(Draw.region("icon-" + stack.name()));
			Label label = new Label("" + main.items.get(stack));
			label.setFontScale(1f);
			itemtable.add(image).size(32);
			itemtable.add(label);
			itemtable.row();
		}
	}

	float roundx(){
		return Mathf.round2(UGraphics.mouseWorldPos().x, TileType.tilesize);
	}

	float roundy(){
		return Mathf.round2(UGraphics.mouseWorldPos().y, TileType.tilesize);
	}

	int tilex(){
		return Mathf.scl2(UGraphics.mouseWorldPos().x, TileType.tilesize);
	}

	int tiley(){
		return Mathf.scl2(UGraphics.mouseWorldPos().y, TileType.tilesize);
	}
}
