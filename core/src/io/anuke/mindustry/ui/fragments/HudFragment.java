package io.anuke.mindustry.ui.fragments;

import static io.anuke.mindustry.Vars.*;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;

import io.anuke.mindustry.Mindustry;
import io.anuke.mindustry.Vars;
import io.anuke.mindustry.core.GameState;
import io.anuke.mindustry.core.GameState.State;
import io.anuke.mindustry.resource.Item;
import io.anuke.mindustry.world.GameMode;
import io.anuke.ucore.core.Core;
import io.anuke.ucore.core.Draw;
import io.anuke.ucore.core.Settings;
import io.anuke.ucore.scene.actions.Actions;
import io.anuke.ucore.scene.builders.imagebutton;
import io.anuke.ucore.scene.builders.label;
import io.anuke.ucore.scene.builders.table;
import io.anuke.ucore.scene.event.Touchable;
import io.anuke.ucore.scene.ui.Image;
import io.anuke.ucore.scene.ui.Label;
import io.anuke.ucore.scene.ui.layout.Cell;
import io.anuke.ucore.scene.ui.layout.Table;
import io.anuke.ucore.util.Bundles;

public class HudFragment implements Fragment{
	private Table itemtable, respawntable;
	private Cell<Table> itemcell;
	
	public void build(){
		//menu at top left
		new table(){{
			atop();
			aleft();
			
			new table(){{
				left();
				defaults().size(68).left();
				float isize = 40;
				
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
			
			new table(){{
				get().setTouchable(Touchable.enabled);
				addWaveTable();
			}}.fillX().end();
			
			row();
			
			itemtable = new table("button").end().top().left().fillX().size(-1).get();
			itemtable.setTouchable(Touchable.enabled);
			itemtable.setVisible(()-> !control.getMode().infiniteResources);
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
				new label("[orange]< "+ Bundles.get("text.paused") + " >").scale(0.75f).pad(6);
			}}.end();
		}}.end();

		//wave table...
		new table(){{
			
			if(!Vars.android){
				atop();
				aright();
			}else{
				abottom();
				aleft();
			}
			
			//addWaveTable();

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
				
				new label(()->"[orange]"+Bundles.get("text.respawn")+" " + (int)(control.getRespawnTime()/60)).scale(0.75f).pad(10);
				
				visible(()->control.getRespawnTime() > 0 && !GameState.is(State.menu));
				
			}}.end();
		}}.end();
		
		//profiling table
		if(debug){
			new table(){{
				abottom();
				aleft();
				new label("[green]density: " + Gdx.graphics.getDensity()).left();
				row();
				new label(() -> "[blue]requests: " + renderer.getBlocks().getRequests()).left();
				row();
				new label(() -> "[purple]tiles: " + Vars.control.tileGroup.amount()).left();
				row();
				new label(() -> "[purple]enemies: " + Vars.control.enemyGroup.amount()).left();
				row();
				new label(() -> "[orange]noclip: " + Vars.noclip).left();
				row();
				new label("[red]DEBUG MODE").scale(0.5f).left();
			}}.end();
		}
	}

	private String getEnemiesRemaining() {
		if(control.getEnemiesRemaining() == 1) {
			return Bundles.format("text.enemies.single", control.getEnemiesRemaining());
		} else return Bundles.format("text.enemies", control.getEnemiesRemaining());
	}
	
	private void addWaveTable(){
		float uheight = 66f;
		
		new table("button"){{
			aleft();
			new table(){{
				aleft();

				new label(() -> Bundles.format("text.wave", control.getWave())).scale(fontscale*1.5f).left();

				row();
			
				new label(()-> control.getEnemiesRemaining() > 0 ?
					getEnemiesRemaining() :
						(control.getTutorial().active() || Vars.control.getMode().toggleWaves) ? "$text.waiting"
								: Bundles.format("text.wave.waiting", (int) (control.getWaveCountdown() / 60f)))
				.minWidth(140).left();

				margin(12f);
				get().marginLeft(6);
			}}.left().end();
			
			playButton(uheight);
		}}.height(uheight).fillX().expandX().end();
		
	}
	
	private void playButton(float uheight){
		new imagebutton("icon-play", 30f, ()->{
			Vars.control.runWave();
		}).height(uheight).fillX().right().padTop(-8f).padBottom(-12f).padRight(-36)
				.padLeft(-10f).width(40f).update(l->{
			boolean vis = Vars.control.getMode().toggleWaves && Vars.control.getEnemiesRemaining() <= 0;
			boolean paused = GameState.is(State.paused) || !vis;
			
			l.setVisible(vis);
			l.getStyle().imageUp = Core.skin.getDrawable(vis ? "icon-play" : "clear");
			l.setTouchable(!paused ? Touchable.enabled : Touchable.disabled);
		});
	}
	
	public void updateItems(){
		
		itemtable.clear();
		itemtable.left();
		
		if(control.getMode().infiniteResources){
			return;
		}
		
		Item[] items = Item.values();

		for(int i = 0; i < control.getItems().length; i ++){
			int amount = control.getItems()[i];
			if(amount == 0) continue;
			String formatted = Mindustry.platforms.format(amount);
			if(amount > 99999999){
				formatted = "inf";
			}
			Image image = new Image(Draw.region("icon-" + items[i].name()));
			Label label = new Label(formatted);
			label.setFontScale(fontscale*1.5f);
			itemtable.add(image).size(8*3);
			itemtable.add(label).left();
			itemtable.row();
		}
	}
	
	public void fadeRespawn(boolean in){
		
		respawntable.addAction(Actions.color(in ? new Color(0, 0, 0, 0.3f) : Color.CLEAR, 0.3f));
	}
}
