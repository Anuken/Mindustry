package io.anuke.moment;

import static io.anuke.moment.world.TileType.tilesize;

import java.util.function.BooleanSupplier;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.Array;

import io.anuke.moment.entities.Enemy;
import io.anuke.moment.resource.*;
import io.anuke.moment.world.Tile;
import io.anuke.moment.world.TileType;
import io.anuke.ucore.core.Draw;
import io.anuke.ucore.core.UGraphics;
import io.anuke.ucore.core.UInput;
import io.anuke.ucore.modules.SceneModule;
import io.anuke.ucore.scene.builders.*;
import io.anuke.ucore.scene.style.Styles;
import io.anuke.ucore.scene.ui.*;
import io.anuke.ucore.scene.ui.layout.Stack;
import io.anuke.ucore.scene.ui.layout.Table;
import io.anuke.ucore.util.Mathf;

public class UI extends SceneModule<Moment>{
	Table itemtable;
	PrefsDialog prefs;
	KeybindDialog keys;
	Dialog about, menu;

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

				}else if(tile.block().name().contains("turret")){
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
			
		}
		
		super.update();
	}

	@Override
	public void init(){

		prefs = new PrefsDialog("Settings");

		prefs.sliderPref("screenshake", "Screen Shake", 4, 0, 12, i -> {
			return (i / 4f) + "x";
		});

		/*
		 * prefs.sliderPref("difficulty", "Difficulty", 1, 0, 3, i->{ return
		 * (i/3f) + "x"; });
		 */

		keys = new KeybindDialog();

		about = new Dialog("About");
		about.getContentTable().add("Made by Anuken for the" + "\nGDL Metal Monstrosity jam." + "\nTools used:");
		about.addCloseButton();
		
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
							main.recipe = r;
						});
						
						table.add(image).size(size+8).pad(4);
						image.getImageCell().size(size);
						
						image.update(()->{
							image.setChecked(main.recipe == r);
							image.setDisabled(!main.hasItems(r.requirements));
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
					
					//if((int)((float)recipes.size/rows+1) == 2){
					//	table.row();
					//}
					
					table.setVisible(()->{
						return button.isChecked();
					});
					
					stack.add(table);
				}
				
				
				row();
				add(stack).colspan(3);
				
				/*
				for(Recipe r : Recipe.values()){
					Image image = new Image(Draw.region(r.result.name()));
					
					get().add(image).size(40);
					
					if(i % rows == rows-1)
						row();
					
					i++;
					/*
					new button(r.result.name(), () -> {
						main.recipe = r;
					}){{
						get().clearChildren();
						get().pad(10f);
						get().add(image).size(42).padRight(4f);
						Table table = new Table();
						table.add(get().getLabel()).left();
						get().add(table);
						get().left();
						
						ItemStack[] req = r.requirements;
						for(ItemStack stack : req){
							table.row();
							table.add("[YELLOW]" + stack.amount + "x " + stack.item.name()).left();
						}
						get().getLabel().setAlignment(Align.left);

						String description = r.result.description();
						if(r.result.ammo != null){
							description += "\n[SALMON]Ammo: " + r.result.ammo.name();
						}

						Table tiptable = new Table();
						tiptable.background("button");
						tiptable.add("[PURPLE]" + r.result.name(), 0.5f).left().padBottom(2f);
						tiptable.row();
						tiptable.add("[ORANGE]" + description).left();
						tiptable.pad(10f);

						Tooltip tip = new Tooltip(tiptable);
						tip.setInstant(true);

						get().addListener(tip);

						Recipe current = r;
						get().update(() -> {
							get().setDisabled(!main.hasItems(current.requirements));
						});

					}}.width(234f);
					
					//row();
				}
				*/

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
