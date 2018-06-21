package io.anuke.mindustry.ui.fragments;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Scaling;
import io.anuke.mindustry.core.GameState.State;
import io.anuke.mindustry.net.Net;
import io.anuke.mindustry.type.Recipe;
import io.anuke.ucore.core.Core;
import io.anuke.ucore.core.Graphics;
import io.anuke.ucore.core.Inputs;
import io.anuke.ucore.core.Settings;
import io.anuke.ucore.graphics.Draw;
import io.anuke.ucore.scene.Element;
import io.anuke.ucore.scene.Group;
import io.anuke.ucore.scene.actions.Actions;
import io.anuke.ucore.scene.builders.imagebutton;
import io.anuke.ucore.scene.builders.label;
import io.anuke.ucore.scene.builders.table;
import io.anuke.ucore.scene.event.InputEvent;
import io.anuke.ucore.scene.event.InputListener;
import io.anuke.ucore.scene.event.Touchable;
import io.anuke.ucore.scene.style.TextureRegionDrawable;
import io.anuke.ucore.scene.ui.Image;
import io.anuke.ucore.scene.ui.ImageButton;
import io.anuke.ucore.scene.ui.Label;
import io.anuke.ucore.scene.ui.layout.Stack;
import io.anuke.ucore.scene.ui.layout.Table;
import io.anuke.ucore.util.Bundles;

import static io.anuke.mindustry.Vars.*;

public class HudFragment implements Fragment{
	public final BlocksFragment blockfrag = new BlocksFragment();

	private ImageButton menu, flip;
	private Table respawntable;
	private Table wavetable;
	private Label infolabel;
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

		new table(){{
			visible(() -> !state.is(State.menu));
			atop();
			aright();

			new table("button"){{
				Table table = get();
				margin(5);
				marginBottom(10);
				TextureRegionDrawable draw = new TextureRegionDrawable(new TextureRegion());
				Image image = new Image(){
					@Override
					public void draw(Batch batch, float parentAlpha) {
						super.draw(batch, parentAlpha);
						if(renderer.minimap().getTexture() != null){
							renderer.minimap().drawEntities(x, y, width, height);
						}
					}
				};
				image.setDrawable(draw);
				table.addListener(new InputListener(){
					public boolean scrolled (InputEvent event, float x, float y, int amount) {
						renderer.minimap().zoomBy(amount);
						return true;
					}
				});
				image.update(() -> {

					Element e = Core.scene.hit(Graphics.mouse().x, Graphics.mouse().y, true);
					if(e != null && e.isDescendantOf(table)){
						Core.scene.setScrollFocus(table);
					}else if(Core.scene.getScrollFocus() == table){
						Core.scene.setScrollFocus(null);
					}

					if (renderer.minimap().getTexture() == null) {
						draw.getRegion().setRegion(Draw.region("white"));
					} else {
						draw.getRegion().setRegion(renderer.minimap().getRegion());
					}
				});
				add(image).size(140f, 140f);
			}}.end();
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

				new label(() -> Bundles.format("text.wave", state.wave)).scale(fontScale *1.5f).left().padLeft(-6);

				row();

				new label(()-> state.enemies > 0 ?
					getEnemiesRemaining() :
						(state.mode.disableWaveTimer) ? "$text.waiting"
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
}
