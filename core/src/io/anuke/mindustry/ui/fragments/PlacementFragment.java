package io.anuke.mindustry.ui.fragments;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.utils.Align;
import io.anuke.mindustry.core.GameState.State;
import io.anuke.mindustry.input.AndroidInput;
import io.anuke.mindustry.input.InputHandler;
import io.anuke.mindustry.input.PlaceMode;
import io.anuke.ucore.core.Core;
import io.anuke.ucore.scene.actions.Actions;
import io.anuke.ucore.scene.builders.imagebutton;
import io.anuke.ucore.scene.builders.label;
import io.anuke.ucore.scene.builders.table;
import io.anuke.ucore.scene.event.Touchable;
import io.anuke.ucore.scene.ui.ButtonGroup;
import io.anuke.ucore.scene.ui.ImageButton;
import io.anuke.ucore.scene.ui.Label;
import io.anuke.ucore.scene.ui.layout.Table;
import io.anuke.ucore.scene.ui.layout.Unit;
import io.anuke.ucore.util.Bundles;
import io.anuke.ucore.util.Mathf;

import static io.anuke.mindustry.Vars.*;

public class PlacementFragment implements Fragment{
	boolean shown = false, placing = false;
	Table breaktable, next, container;
	Label modelabel;
	
	public void build(){
		if(!mobile) return;

		InputHandler input = control.input();

		float s = 50f;
		float translation = Unit.dp.scl(58f);

		new table(){{
			visible(() -> !state.is(State.menu));

			abottom();
			aleft();

			ButtonGroup<ImageButton> placeGroup = new ButtonGroup<>();
			ButtonGroup<ImageButton> breakGroup = new ButtonGroup<>();

			update(t -> {
				if((input.recipe == null) == placing){
					float i = 0.1f;
					Interpolation n = Interpolation.pow3Out;
					if(input.recipe == null){
						placing = false;
						container.clearActions();
						container.actions(Actions.translateBy(0, -(container.getTranslation().y + translation), i, n));
						if (!input.lastBreakMode.both) input.placeMode = input.lastBreakMode;
					}else{
						placing = true;
						container.clearActions();
						container.actions(Actions.translateBy(0, -(container.getTranslation().y), i, n));
						input.placeMode = input.lastPlaceMode;
					}
				}

				if(!input.placeMode.delete){
					placeGroup.setMinCheckCount(1);
					for(ImageButton button : placeGroup.getButtons()){
						if(button.getName().equals(input.placeMode.name())){
							button.setChecked(true);
							break;
						}
					}
				}else{
					placeGroup.setMinCheckCount(0);
					for(ImageButton button : placeGroup.getButtons())
						button.setChecked(false);
				}

				if(input.placeMode.delete || input.breakMode.both){
					PlaceMode mode = input.breakMode;
					breakGroup.setMinCheckCount(1);
					for(ImageButton button : breakGroup.getButtons()){
						if(button.getName().equals(mode.name())){
							button.setChecked(true);
							break;
						}
					}
				}else{
					breakGroup.setMinCheckCount(0);
					for(ImageButton button : breakGroup.getButtons())
						button.setChecked(false);
				}
			});

			container = new table(){{
				modelabel = new label("").get();

				row();

				//break menu
				new table() {{
					abottom();
					aleft();

					height(s + 5 + 4);

					next = new table("pane") {{
						margin(5f);

						defaults().padBottom(-5.5f);

						new imagebutton("icon-arrow-right", 10 * 3, () -> {
							toggle(!shown);
						}).update(l -> l.getStyle().imageUp = Core.skin.getDrawable(shown ? "icon-arrow-left" : "icon-" + input.breakMode.name())).size(s, s + 4);

					}}.end().get();

					breaktable = new table("pane") {{
						visible(() -> shown);
						margin(5f);
						marginLeft(-(Unit.dp.scl(1f) - 1f) * 2.5f);
						touchable(Touchable.enabled);
						aleft();

						defaults().size(s, s + 4);

						for (PlaceMode mode : PlaceMode.values()) {
							if (!mode.shown || !mode.delete) continue;

							defaults().padBottom(-5.5f);

							ImageButton button = new imagebutton("icon-" + mode.name(), "toggle", 10 * 3, () -> {
								control.input().resetCursor();
								input.breakMode = mode;
								input.lastBreakMode = mode;
								if (!mode.both){
									input.placeMode = mode;
								}else{
									input.placeMode = input.lastPlaceMode;
								}
								modeText(Bundles.format("text.mode.break", mode.toString()));
							}).group(breakGroup).get();

							button.setName(mode.name());
							button.released(() -> {
								//TODO hack
								if(mode == PlaceMode.areaDelete){
									((AndroidInput)input).placing = false;
								}
							});
						}

					}}.end().get();

					breaktable.getParent().swapActor(breaktable, next);

					breaktable.getTranslation().set(-breaktable.getPrefWidth(), 0);

				}}.end().get();

				row();

				//place menu
				new table() {{
					touchable(Touchable.enabled);

					aleft();

					new table("pane") {{
						margin(5f);
						aleft();

						defaults().size(s, s + 4).padBottom(-5.5f);

						Color color = Color.GRAY;

						new imagebutton("icon-cancel", 14 * 3, () -> {
							input.recipe = null;
						}).imageColor(color)
								.visible(() -> input.recipe != null);

						for (PlaceMode mode : PlaceMode.values()) {
							if (!mode.shown || mode.delete) continue;

							new imagebutton("icon-" + mode.name(), "toggle", 10 * 3, () -> {
								control.input().resetCursor();
								input.placeMode = mode;
								input.lastPlaceMode = mode;
								modeText(Bundles.format("text.mode.place", mode.toString()));
							}).group(placeGroup).get().setName(mode.name());
						}

						new imagebutton("icon-arrow", 14 * 3, () -> {
							input.rotation = Mathf.mod(input.rotation + 1, 4);
						}).imageColor(color).visible(() -> input.recipe != null).update(image -> {
							image.getImage().setRotation(input.rotation * 90);
							image.getImage().setOrigin(Align.center);
						});

					}}.left().end();
				}}.left().end();

			}}.end().get();

			container.setTranslation(0, -translation);

		}}.end();
	}

	private void modeText(String text){
		modelabel.setText(text);
		modelabel.clearActions();
		modelabel.setColor(Color.WHITE);
		modelabel.actions(Actions.fadeOut(5f, Interpolation.fade));
	}

	private void toggle(boolean show){
		float dur = 0.3f;
		Interpolation in = Interpolation.pow3Out;

		if(breaktable.getActions().size != 0 || shown == show) return;

		breaktable.getParent().swapActor(breaktable, next);

		if(!show){
			control.input().breakMode = PlaceMode.none;
			if(control.input().placeMode.delete) control.input().placeMode = PlaceMode.none;
			breaktable.actions(Actions.translateBy(-breaktable.getWidth() - 5, 0, dur, in), Actions.call(() -> shown = false));
		}else{
			shown = true;
			breaktable.actions(Actions.translateBy(-breaktable.getTranslation().x - 5, 0, dur, in));
		}
	}
}
