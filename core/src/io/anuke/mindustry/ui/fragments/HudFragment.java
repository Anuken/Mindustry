package io.anuke.mindustry.ui.fragments;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Interpolation;
import io.anuke.mindustry.Vars;
import io.anuke.mindustry.core.GameState;
import io.anuke.mindustry.core.GameState.State;
import io.anuke.mindustry.net.Net;
import io.anuke.ucore.core.Core;
import io.anuke.ucore.core.Settings;
import io.anuke.ucore.scene.actions.Actions;
import io.anuke.ucore.scene.builders.imagebutton;
import io.anuke.ucore.scene.builders.label;
import io.anuke.ucore.scene.builders.table;
import io.anuke.ucore.scene.event.Touchable;
import io.anuke.ucore.scene.ui.ImageButton;
import io.anuke.ucore.scene.ui.Label;
import io.anuke.ucore.scene.ui.layout.Table;
import io.anuke.ucore.util.Bundles;

import static io.anuke.mindustry.Vars.*;

public class HudFragment implements Fragment{
	private ImageButton menu, flip, pause;
	private Table respawntable;
	private Table wavetable;
	private boolean shown = true;
	private BlocksFragment blockfrag = new BlocksFragment();
	
	public void build(){

		//menu at top left
		new table(){{
			atop();
			aleft();
			
			new table(){{
				left();
				float dsize = 58;
				defaults().size(dsize).left();
				float isize = 40;

				menu = new imagebutton("icon-menu", isize, ()->{
					ui.showMenu();
				}).get();

				flip = new imagebutton("icon-arrow-up", isize, ()->{
					if(wavetable.getActions().size != 0) return;

					float dur = 0.3f;
					Interpolation in = Interpolation.pow3Out;

					flip.getStyle().imageUp = Core.skin.getDrawable(shown ? "icon-arrow-down" : "icon-arrow-up");

					if(shown){
						blockfrag.toggle(false, dur, in);
						wavetable.actions(Actions.translateBy(0, wavetable.getHeight() + dsize, dur, in), Actions.call(() -> shown = false));
					}else{
						shown = true;
						blockfrag.toggle(true, dur, in);
						wavetable.actions(Actions.translateBy(0, -wavetable.getTranslation().y, dur, in));
					}

				}).get();

				pause = new imagebutton("icon-pause", isize, ()->{
					GameState.set(GameState.is(State.paused) ? State.playing : State.paused);
				}).update(i -> i.getStyle().imageUp = Core.skin.getDrawable(GameState.is(State.paused) ? "icon-play" : "icon-pause")).cell
						.disabled(b -> Net.active()).get();

			}}.end();

			row();
			
			new table(){{
				touchable(Touchable.enabled);
				visible(() -> shown);
				addWaveTable();
			}}.fillX().end();
			
			row();

			visible(()->!GameState.is(State.menu));
			
			Label fps = new Label(()->(Settings.getBool("fps") ? (Gdx.graphics.getFramesPerSecond() + " FPS") : ""));
			row();
			add(fps).size(-1);
			
		}}.end();
		
		//tutorial ui table
		new table(){{
			control.getTutorial().buildUI(this);
			
			visible(()->control.getTutorial().active());
		}}.end();
		
		//paused table
		new table(){{
			visible(()->GameState.is(State.paused) && !Net.active());
			atop();
			
			new table("pane"){{
				new label("[orange]< "+ Bundles.get("text.paused") + " >").scale(0.75f).pad(6);
			}}.end();
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

		new table(){{
			abottom();
			visible(() -> !GameState.is(State.menu) && Vars.control.getSaves().isSaving());

			new label("$text.saveload");

		}}.end();

		blockfrag.build();
	}

	private String getEnemiesRemaining() {
		if(control.getEnemiesRemaining() == 1) {
			return Bundles.format("text.enemies.single", control.getEnemiesRemaining());
		} else return Bundles.format("text.enemies", control.getEnemiesRemaining());
	}
	
	private void addWaveTable(){
		float uheight = 66f;
		
		wavetable = new table("button"){{
			aleft();
			new table(){{
				aleft();

				new label(() -> Bundles.format("text.wave", control.getWave())).scale(fontscale*1.5f).left().padLeft(-6);

				row();
			
				new label(()-> control.getEnemiesRemaining() > 0 ?
					getEnemiesRemaining() :
						(control.getTutorial().active() || Vars.control.getMode().toggleWaves) ? "$text.waiting"
								: Bundles.format("text.wave.waiting", (int) (control.getWaveCountdown() / 60f)))
				.minWidth(140).padLeft(-6).padRight(-12).left();

				margin(10f);
				get().marginLeft(6);
			}}.left().end();
			
			playButton(uheight);
		}}.height(uheight).fillX().expandX().end().get();
		wavetable.getParent().getParent().swapActor(wavetable.getParent(), menu.getParent());
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
		blockfrag.updateItems();
	}
	
	public void fadeRespawn(boolean in){
		respawntable.addAction(Actions.color(in ? new Color(0, 0, 0, 0.3f) : Color.CLEAR, 0.3f));
	}
}
