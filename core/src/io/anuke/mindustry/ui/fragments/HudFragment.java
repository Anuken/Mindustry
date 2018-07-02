package io.anuke.mindustry.ui.fragments;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Scaling;
import io.anuke.mindustry.core.GameState.State;
import io.anuke.mindustry.net.Net;
import io.anuke.mindustry.type.Recipe;
import io.anuke.mindustry.ui.IntFormat;
import io.anuke.mindustry.ui.Minimap;
import io.anuke.ucore.core.Core;
import io.anuke.ucore.core.Inputs;
import io.anuke.ucore.core.Settings;
import io.anuke.ucore.scene.Element;
import io.anuke.ucore.scene.Group;
import io.anuke.ucore.scene.actions.Actions;
import io.anuke.ucore.scene.builders.imagebutton;
import io.anuke.ucore.scene.builders.label;
import io.anuke.ucore.scene.builders.table;
import io.anuke.ucore.scene.event.Touchable;
import io.anuke.ucore.scene.ui.Image;
import io.anuke.ucore.scene.ui.ImageButton;
import io.anuke.ucore.scene.ui.Label;
import io.anuke.ucore.scene.ui.layout.Stack;
import io.anuke.ucore.scene.ui.layout.Table;
import io.anuke.ucore.util.Bundles;

import static io.anuke.mindustry.Vars.*;

public class HudFragment extends Fragment{
	public final BlocksFragment blockfrag = new BlocksFragment();

	private ImageButton menu, flip;
	private Table respawntable;
	private Table wavetable;
	private Table infolabel;
	private Table lastUnlockTable;
	private Table lastUnlockLayout;
	private boolean shown = true;
	private float dsize = 58;
	private float isize = 40;

	public void build(Group parent){

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
							ui.listfrag.toggle();
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
					addWaveTable();
				}}.fillX().end();

				row();

				visible(() -> !state.is(State.menu));
				row();
				new table(){{
					IntFormat fps = new IntFormat("text.fps");
					IntFormat tps = new IntFormat("text.tps");
					IntFormat ping = new IntFormat("text.ping");
					new label(() -> fps.get(Gdx.graphics.getFramesPerSecond())).padRight(10);
					new label(() -> tps.get(threads.getTPS())).visible(() -> threads.isEnabled());
					row();
					new label(() -> ping.get(Net.getPing())).visible(() -> Net.client() && !gwt).colspan(2);

					infolabel = get();
				}}.size(-1).end().visible(() -> Settings.getBool("fps"));

			}}.end();
		}}.end();

		new table(){{
			visible(() -> !state.is(State.menu));
			atop();
			aright();

			Minimap minimap = new Minimap();

			add(minimap).visible(() -> Settings.getBool("minimap"));
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

		new table(){{
			abottom();
			visible(() -> !state.is(State.menu) && control.getSaves().isSaving());

			new label("$text.saveload");

		}}.end();

		blockfrag.build(Core.scene.getRoot());
	}

	/**Show unlock notification for a new recipe.*/
	public void showUnlock(Recipe recipe){
		blockfrag.rebuild();

		//if there's currently no unlock notification...
		if(lastUnlockTable == null) {
			Table table = new Table("button");
			table.update(() -> {
				if(state.is(State.menu)){
					table.remove();
					lastUnlockLayout = null;
					lastUnlockTable = null;
				}
			});
			table.margin(12);

			Table in = new Table();

			//create texture stack for displaying
			Stack stack = new Stack();
			for (TextureRegion region : recipe.result.getCompactIcon()) {
				Image image = new Image(region);
				image.setScaling(Scaling.fit);
				stack.add(image);
			}

			in.add(stack).size(48f).pad(2);

			//add to table
			table.add(in).padRight(8);
			table.add("$text.unlocked");
			table.pack();

			//create container table which will align and move
			Table container = Core.scene.table();
			container.top().add(table);
			container.setTranslation(0, table.getPrefHeight());
			container.actions(Actions.translateBy(0, -table.getPrefHeight(), 1f, Interpolation.fade), Actions.delay(4f),
					//nesting actions() calls is necessary so the right prefHeight() is used
					Actions.run(() -> container.actions(Actions.translateBy(0, table.getPrefHeight(), 1f, Interpolation.fade), Actions.run(() ->{
						lastUnlockTable = null;
						lastUnlockLayout = null;
					}), Actions.removeActor())));

			lastUnlockTable = container;
			lastUnlockLayout = in;
		}else{
			//max column size
			int col = 3;
			//max amount of elements minus extra 'plus'
			int cap = col*col-1;

			//get old elements
			Array<Element> elements = new Array<>(lastUnlockLayout.getChildren());
			int esize = elements.size;

			//...if it's already reached the cap, ignore everything
			if(esize > cap) return;

			//get size of each element
			float size = 48f / Math.min(elements.size + 1, col);

			//correct plurals if needed
			if(esize == 1){
				((Label)lastUnlockLayout.getParent().find(e -> e instanceof Label)).setText("$text.unlocked.plural");
			}

			lastUnlockLayout.clearChildren();
			lastUnlockLayout.defaults().size(size).pad(2);

			for(int i = 0; i < esize && i <= cap; i ++){
				lastUnlockLayout.add(elements.get(i));

				if(i % col == col - 1){
					lastUnlockLayout.row();
				}
			}

			//if there's space, add it
			if(esize < cap) {

				Stack stack = new Stack();
				for (TextureRegion region : recipe.result.getCompactIcon()) {
					Image image = new Image(region);
					image.setScaling(Scaling.fit);
					stack.add(image);
				}

				lastUnlockLayout.add(stack);
			}else{ //else, add a specific icon to denote no more space
				lastUnlockLayout.addImage("icon-add");
			}

			lastUnlockLayout.pack();
		}
	}

	private void toggleMenus(){
		wavetable.clearActions();
		infolabel.clearActions();

		float dur = 0.3f;
		Interpolation in = Interpolation.pow3Out;

		flip.getStyle().imageUp = Core.skin.getDrawable(shown ? "icon-arrow-down" : "icon-arrow-up");

		if (shown) {
			shown = false;
			blockfrag.toggle(false, dur, in);
			wavetable.actions(Actions.translateBy(0, (wavetable.getHeight() + dsize) - wavetable.getTranslation().y, dur, in));
			infolabel.actions(Actions.translateBy(0, (wavetable.getHeight()) - wavetable.getTranslation().y, dur, in));
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
		} else {
			return Bundles.format("text.enemies", state.enemies);
		}
	}

	private void addWaveTable(){
		float uheight = 66f;

		IntFormat wavef = new IntFormat("text.wave");
		IntFormat timef = new IntFormat("text.wave.waiting");

		wavetable = new table("button"){{
			aleft();
			new table(){{
				aleft();

				new label(() -> wavef.get(state.wave)).scale(fontScale *1.5f).left().padLeft(-6);

				row();

				new label(() -> state.enemies > 0 ?
					getEnemiesRemaining() :
						(state.mode.disableWaveTimer) ? "$text.waiting"
								: timef.get((int) (state.wavetime / 60f)))
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
			boolean vis = state.mode.disableWaveTimer && (Net.server() || !Net.active());
			boolean paused = state.is(State.paused) || !vis;
			
			l.setVisible(vis);
			l.getStyle().imageUp = Core.skin.getDrawable(vis ? "icon-play" : "clear");
			l.setTouchable(!paused ? Touchable.enabled : Touchable.disabled);
		});
	}
}
