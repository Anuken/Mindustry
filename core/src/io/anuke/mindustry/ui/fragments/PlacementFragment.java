package io.anuke.mindustry.ui.fragments;

import static io.anuke.mindustry.Vars.*;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.utils.Align;

import io.anuke.mindustry.core.GameState;
import io.anuke.mindustry.core.GameState.State;
import io.anuke.mindustry.input.PlaceMode;
import io.anuke.ucore.core.Core;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.scene.actions.Actions;
import io.anuke.ucore.scene.builders.*;
import io.anuke.ucore.scene.event.Touchable;
import io.anuke.ucore.scene.ui.ButtonGroup;
import io.anuke.ucore.scene.ui.ImageButton;
import io.anuke.ucore.scene.ui.layout.Table;
import io.anuke.ucore.util.Mathf;

import javax.xml.soap.Text;

public class PlacementFragment implements Fragment{
	boolean shown = false;
	Table breaktable, next;
	
	public void build(){
		if(android){

			float s = 50f;

			new table(){{
				visible(() -> !GameState.is(State.menu));

				abottom();
				aleft();

				ButtonGroup<ImageButton> placeGroup = new ButtonGroup<>();
				ButtonGroup<ImageButton> breakGroup = new ButtonGroup<>();

				update(t -> {

					if(!player.placeMode.delete){
						placeGroup.setMinCheckCount(1);
						for(ImageButton button : placeGroup.getButtons()){
							if(button.getName().equals(player.placeMode.name())){
								button.setChecked(true);
								break;
							}
						}
					}else{
						placeGroup.setMinCheckCount(0);
						for(ImageButton button : placeGroup.getButtons())
							button.setChecked(false);
					}

                    if(player.placeMode.delete || player.breakMode.both){
					    PlaceMode mode = player.breakMode;
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

				new table(){{
					visible(() -> player.recipe != null);
					touchable(Touchable.enabled);

					aleft();

					new table("pane"){{
						margin(5f);
						aleft();

						defaults().size(s, s + 4).padBottom(-5.5f);

						Color color = Color.GRAY;

						new imagebutton("icon-cancel", 14*3, ()->{
							player.recipe = null;
						}).imageColor(color)
								.visible(()->player.recipe != null);

						for(PlaceMode mode : PlaceMode.values()){
							if(!mode.shown || mode.delete) continue;

							new imagebutton("icon-" + mode.name(), "toggle",  10*3, ()->{
								control.getInput().resetCursor();
								player.placeMode = mode;
							}).group(placeGroup).get().setName(mode.name());
						}

						new imagebutton("icon-arrow", 14*3, ()->{
							player.rotation = Mathf.mod(player.rotation + 1, 4);
						}).imageColor(color).visible(() -> player.recipe != null).update(image ->{
							image.getImage().setRotation(player.rotation*90);
							image.getImage().setOrigin(Align.center);
						});

					}}.padBottom(-5).left().end();
				}}.left().end();

				row();
			
                new table(){{
                    abottom();
                    aleft();

                    height(s+5+4);

                    next = new table("pane"){{
                        margin(5f);

                        defaults().padBottom(-5.5f);

                        new imagebutton("icon-arrow-right", 10 * 3, () -> {
                            toggle(!shown);
                        }).update(l -> l.getStyle().imageUp = Core.skin.getDrawable(shown ? "icon-arrow-left" : "icon-" + player.breakMode.name())).size(s, s+4);

                    }}.end().get();

                    breaktable = new table("pane"){{
                    	visible(() -> shown);
                        margin(5f);
                        marginLeft(0f);
                        touchable(Touchable.enabled);
                        aleft();

                        defaults().size(s, s+4);

                        for(PlaceMode mode : PlaceMode.values()){
                            if(!mode.shown || !mode.delete) continue;

                            defaults().padBottom(-5.5f);

                            new imagebutton("icon-" + mode.name(), "toggle",  10*3, ()->{
                                control.getInput().resetCursor();
                                player.breakMode = mode;
                                if(!mode.both) player.placeMode = mode;
                            }).group(breakGroup).get().setName(mode.name());
                        }

                    }}.end().get();

                    breaktable.getParent().swapActor(breaktable, next);

                    breaktable.getTranslation().set(-breaktable.getPrefWidth(), 0);

                }}.end().get();

            }}.end();
		}
	}

	private void toggle(boolean show){
		float dur = 0.3f;
		Interpolation in = Interpolation.pow3Out;

		if(breaktable.getActions().size != 0 || shown == show) return;

		breaktable.getParent().swapActor(breaktable, next);

		if(!show){
			breaktable.actions(Actions.translateBy(-breaktable.getWidth() - 5, 0, dur, in), Actions.call(() -> shown = false));
		}else{
			shown = true;
			breaktable.actions(Actions.translateBy(-breaktable.getTranslation().x - 5, 0, dur, in));
		}
	}
}
