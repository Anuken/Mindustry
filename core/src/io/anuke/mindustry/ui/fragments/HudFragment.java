package io.anuke.mindustry.ui.fragments;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Interpolation;
import io.anuke.mindustry.core.GameState.State;
import io.anuke.mindustry.net.Net;
import io.anuke.ucore.core.Core;
import io.anuke.ucore.core.Inputs;
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
	public final BlocksFragment blockfrag = new BlocksFragment();

	private ImageButton menu, flip;
	private Table respawntable;
	private Table wavetable;
	private Label infolabel;
	private boolean shown = true;
	private float dsize = 58;
	private float isize = 40;

	public void build(){

		//menu at top left
		new table(){{
			atop();
			aleft();

			new table(){{

				new table() {{
					left();
					defaults().size(dsize).left();

					menu = new imagebutton("icon-menu", isize, ui.paused::show).get();
					flip = new imagebutton("icon-arrow-up", isize, () -> toggleMenus()).get();

					update(t -> {
						if(Inputs.keyTap("toggle_menus") && !ui.chatfrag.chatOpen()){
							toggleMenus();
						}
					});

					new imagebutton("icon-pause", isize, () -> {
						if (Net.active()) {
							ui.listfrag.visible = !ui.listfrag.visible;
						} else {
							state.set(state.is(State.paused) ? State.playing : State.paused);
						}
					}).update(i -> {
						if (Net.active()) {
							i.getStyle().imageUp = Core.skin.getDrawable("icon-players");
						} else {
							i.setDisabled(Net.active());
							i.getStyle().imageUp = Core.skin.getDrawable(state.is(State.paused) ? "icon-play" : "icon-pause");
						}
					}).get();

					new imagebutton("icon-settings", isize, () -> {
						if (Net.active() && mobile) {
							if (ui.chatfrag.chatOpen()) {
								ui.chatfrag.hide();
							} else {
								ui.chatfrag.toggle();
							}
						} else {
							ui.settings.show();
						}
					}).update(i -> {
						if (Net.active() && mobile) {
							i.getStyle().imageUp = Core.skin.getDrawable("icon-chat");
						} else {
							i.getStyle().imageUp = Core.skin.getDrawable("icon-settings");
						}
					}).get();

				}}.end();

				row();

				new table() {{
					touchable(Touchable.enabled);
					visible(() -> shown);
					addWaveTable();
				}}.fillX().end();

				row();

				visible(() -> !state.is(State.menu));

				infolabel = new Label(() -> (Settings.getBool("fps") ? (Gdx.graphics.getFramesPerSecond() + " FPS") +
						(threads.isEnabled() ?  " / " + threads.getFPS() + " TPS" : "") + (Net.client() && !gwt ? "\nPing: " + Net.getPing() : "") : ""));
				row();
				add(infolabel).size(-1);

			}}.end();



		}}.end();

		//tutorial ui table
		new table(){{
			control.tutorial().buildUI(this);

			visible(() -> control.tutorial().active());
		}}.end();

		//paused table
		new table(){{
			visible(() -> state.is(State.paused) && !Net.active());
			atop();

			new table("pane"){{
				new label("[orange]< "+ Bundles.get("text.paused") + " >").scale(0.75f).pad(6);
			}}.end();
		}}.end();

		//respawn background table
		new table("white"){{
			respawntable = get();
			respawntable.setColor(Color.CLEAR);
			update(t -> {
				if(state.is(State.menu)){
					respawntable.setColor(Color.CLEAR);
				}
			});
		}}.end();

		//respawn table
		new table(){{
			new table("pane"){{

				new label(()->"[orange]"+Bundles.get("text.respawn")+" " + (int)(control.getRespawnTime()/60)).scale(0.75f).pad(10);

				visible(()->control.getRespawnTime() > 0 && !state.is(State.menu));

			}}.end();
		}}.end();

		new table(){{
			abottom();
			visible(() -> !state.is(State.menu) && control.getSaves().isSaving());

			new label("$text.saveload");

		}}.end();

		blockfrag.build();
	}

	private void toggleMenus(){
		if (wavetable.getActions().size != 0) return;

		float dur = 0.3f;
		Interpolation in = Interpolation.pow3Out;

		flip.getStyle().imageUp = Core.skin.getDrawable(shown ? "icon-arrow-down" : "icon-arrow-up");

		if (shown) {
			blockfrag.toggle(false, dur, in);
			wavetable.actions(Actions.translateBy(0, wavetable.getHeight() + dsize, dur, in), Actions.call(() -> shown = false));
			infolabel.actions(Actions.translateBy(0, wavetable.getHeight(), dur, in), Actions.call(() -> shown = false));
		} else {
			shown = true;
			blockfrag.toggle(true, dur, in);
			wavetable.actions(Actions.translateBy(0, -wavetable.getTranslation().y, dur, in));
			infolabel.actions(Actions.translateBy(0, -infolabel.getTranslation().y, dur, in));
		}
	}

	private String getEnemiesRemaining() {
		if(state.enemies == 1) {
			return Bundles.format("text.enemies.single", state.enemies);
		} else return Bundles.format("text.enemies", state.enemies);
	}

	private void addWaveTable(){
		float uheight = 66f;

		wavetable = new table("button"){{
			aleft();
			new table(){{
				aleft();

				new label(() -> Bundles.format("text.wave", state.wave)).scale(fontscale*1.5f).left().padLeft(-6);

				row();

				new label(()-> state.enemies > 0 ?
					getEnemiesRemaining() :
						(control.tutorial().active() || state.mode.disableWaveTimer) ? "$text.waiting"
								: Bundles.format("text.wave.waiting", (int) (state.wavetime / 60f)))
				.minWidth(126).padLeft(-6).left();

				margin(10f);
				get().marginLeft(6);
			}}.left().end();

			add().growX();

			playButton(uheight);
		}}.height(uheight).fillX().expandX().end().get();
		wavetable.getParent().getParent().swapActor(wavetable.getParent(), menu.getParent());
	}

	private void playButton(float uheight){
		new imagebutton("icon-play", 30f, () -> {
			state.wavetime = 0f;
		}).height(uheight).fillX().right().padTop(-8f).padBottom(-12f).padLeft(-15).padRight(-10).width(40f).update(l->{
			boolean vis = state.enemies <= 0 && (Net.server() || !Net.active());
			boolean paused = state.is(State.paused) || !vis;
			
			l.setVisible(vis);
			l.getStyle().imageUp = Core.skin.getDrawable(vis ? "icon-play" : "clear");
			l.setTouchable(!paused ? Touchable.enabled : Touchable.disabled);
		});
	}

	public void updateItems(){
		blockfrag.updateItems();
	}

	public void updateWeapons(){
		blockfrag.updateWeapons();
	}
	
	public void fadeRespawn(boolean in){
		respawntable.addAction(Actions.color(in ? new Color(0, 0, 0, 0.3f) : Color.CLEAR, 0.3f));
	}
}
