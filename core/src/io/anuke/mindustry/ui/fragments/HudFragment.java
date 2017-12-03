package io.anuke.mindustry.ui.fragments;

import static io.anuke.mindustry.Vars.*;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.Array;

import io.anuke.mindustry.Mindustry;
import io.anuke.mindustry.Vars;
import io.anuke.mindustry.core.GameState;
import io.anuke.mindustry.core.GameState.State;
import io.anuke.mindustry.entities.enemies.Enemy;
import io.anuke.mindustry.resource.Item;
import io.anuke.mindustry.world.GameMode;
import io.anuke.ucore.core.Core;
import io.anuke.ucore.core.Draw;
import io.anuke.ucore.core.Settings;
import io.anuke.ucore.entities.Entities;
import io.anuke.ucore.function.StringSupplier;
import io.anuke.ucore.scene.actions.Actions;
import io.anuke.ucore.scene.builders.imagebutton;
import io.anuke.ucore.scene.builders.label;
import io.anuke.ucore.scene.builders.table;
import io.anuke.ucore.scene.ui.Image;
import io.anuke.ucore.scene.ui.Label;
import io.anuke.ucore.scene.ui.layout.Cell;
import io.anuke.ucore.scene.ui.layout.Table;
import io.anuke.ucore.scene.ui.layout.Unit;
import io.anuke.ucore.util.Profiler;

public class HudFragment implements Fragment{
	private Table itemtable, respawntable;
	private Cell<Table> itemcell;
	private Array<Item> tempItems = new Array<>();
	
	public void build(){
		//menu at top left
		new table(){{
			atop();
			aleft();
			
			new table(){{
				left();
				defaults().size(66).units(Unit.dp).left();
				float isize = Unit.dp.inPixels(40);
				
				new imagebutton("icon-menu", isize, ()->{
					ui.showMenu();
				});
				
				new imagebutton("icon-settings", isize, ()->{
					ui.showPrefs();
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
			itemtable.setVisible(()-> control.getMode() != GameMode.sandbox);
			itemcell = get().getCell(itemtable);

			get().setVisible(()->!GameState.is(State.menu));
			
			Label fps = new Label(()->(Settings.getBool("fps") ? (Gdx.graphics.getFramesPerSecond() + " FPS") : ""));
			row();
			add(fps).size(-1);
			
		}}.end();
		
		//ui table
		new table(){{
			control.getTutorial().buildUI(this);
			
			visible(()->control.getTutorial().active());
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
			
			float uheight = 72f;
			
			new imagebutton("icon-play", Unit.dp.inPixels(30f), ()->{
				Vars.control.runWave();
			}).size(uheight).uniformY().units(Unit.dp)
				.visible(()-> Vars.control.getMode() == GameMode.sandbox && Vars.control.getEnemiesRemaining() <= 0);

			new table("button"){{

				new label(()->"[orange]Wave " + control.getWave()).scale(fontscale*2f).left();

				row();

				new label(()-> control.getEnemiesRemaining() > 0 ?
						control.getEnemiesRemaining() + " Enemies remaining" : 
							(control.getTutorial().active() || Vars.control.getMode() == GameMode.sandbox) ? "waiting..." : "New wave in " + (int) (control.getWaveCountdown() / 60f))
				.minWidth(150);

				get().pad(Unit.dp.inPixels(12));
			}}.height(uheight).units(Unit.dp);

			visible(()->!GameState.is(State.menu));
		}}.end();
		
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
		
		//profiling table
		if(debug){
			new table(){{
				abottom();
				aleft();
				new label((StringSupplier)()->"[purple]enemies: " + Entities.getGroup(Enemy.class).amount()).left();
				row();
				new label((StringSupplier)()->"[orange]noclip: " + Vars.noclip).left();
				row();
				new label("[red]DEBUG MODE").scale(0.5f).left();
			}}.end();
			
			if(profile){
				new table(){{
					atop();
					new table("button"){{
						defaults().left().growX();
						atop();
						aleft();
						new label((StringSupplier)()->Profiler.formatDisplayTimes());
					}}.width(400f).units(Unit.dp).end();
				}}.end();
			}
		}
	}
	
	public void updateItems(){
		
		itemtable.clear();
		itemtable.left();
		
		if(control.getMode() == GameMode.sandbox){
			return;
		}
		
		tempItems.clear();
		for(Item item : control.getItems().keys()){
			tempItems.add(item);
		}
		tempItems.sort();

		for(Item stack : tempItems){
			int amount = control.getAmount(stack);
			String formatted = Mindustry.formatter.format(amount);
			if(amount > 99999999){
				formatted = "inf";
			}
			Image image = new Image(Draw.region("icon-" + stack.name()));
			Label label = new Label(formatted);
			label.setFontScale(fontscale*1.5f);
			itemtable.add(image).size(8*3).units(Unit.dp);
			itemtable.add(label).left();
			itemtable.row();
		}
	}
	
	public void fadeRespawn(boolean in){
		
		respawntable.addAction(Actions.color(in ? new Color(0, 0, 0, 0.3f) : Color.CLEAR, 0.3f));
	}
}
