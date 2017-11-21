package io.anuke.mindustry.ui.fragments;

import static io.anuke.mindustry.Vars.*;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;

import io.anuke.mindustry.GameState;
import io.anuke.mindustry.GameState.State;
import io.anuke.ucore.core.Core;
import io.anuke.ucore.core.Settings;
import io.anuke.ucore.scene.builders.imagebutton;
import io.anuke.ucore.scene.builders.label;
import io.anuke.ucore.scene.builders.table;
import io.anuke.ucore.scene.ui.Label;
import io.anuke.ucore.scene.ui.layout.Table;
import io.anuke.ucore.scene.ui.layout.Unit;

public class HudFragment implements Fragment{
	private Table itemtable, respawntable;
	
	public void build(){
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

			get().setVisible(()->!GameState.is(State.menu));
			
			Label fps = new Label(()->(Settings.getBool("fps") ? (Gdx.graphics.getFramesPerSecond() + " FPS") : ""));
			row();
			add(fps).size(-1);
			
		}}.end();
		
		//ui table
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

			visible(()->!GameState.is(State.menu));
		}}.end();
		
		//settings icon
		new table(){{
			atop().aright();
			new imagebutton("icon-info", Unit.dp.inPixels(30f), ()->{
				ui.showAbout();
			}).get().pad(14);
		}}.end().visible(()->GameState.is(State.menu));
		
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
	}
}
