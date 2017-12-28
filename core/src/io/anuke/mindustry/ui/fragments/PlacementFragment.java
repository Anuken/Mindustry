package io.anuke.mindustry.ui.fragments;

import static io.anuke.mindustry.Vars.*;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.utils.Align;

import io.anuke.mindustry.core.GameState;
import io.anuke.mindustry.core.GameState.State;
import io.anuke.mindustry.input.PlaceMode;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.scene.actions.Actions;
import io.anuke.ucore.scene.builders.*;
import io.anuke.ucore.scene.event.Touchable;
import io.anuke.ucore.scene.ui.ButtonGroup;
import io.anuke.ucore.scene.ui.ImageButton;
import io.anuke.ucore.scene.ui.layout.Table;
import io.anuke.ucore.util.Mathf;

public class PlacementFragment implements Fragment{
	boolean shown = false;
	Table breaktable, next;
	
	public void build(){
		if(android){
			//placement table

			float s = 50f;

			new table(){{
				visible(() -> !GameState.is(State.menu));

				abottom();
				aleft();

				ButtonGroup<ImageButton> group = new ButtonGroup<>();

				new table(){{
					visible(() -> player.recipe != null);
					touchable(Touchable.enabled);

					aleft();
					new label("$text.placemode");
					row();

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
							}).group(group);
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
                            float dur = 0.3f;
                            Interpolation in = Interpolation.pow3Out;
                            if(breaktable.getActions().size != 0) return;

                            breaktable.getParent().swapActor(breaktable, next);

                            if(shown){
                                breaktable.actions(Actions.translateBy(-breaktable.getWidth() - 5, 0, dur, in), Actions.call(() -> shown = false));
                            }else{
                                shown = true;
                                breaktable.actions(Actions.translateBy(-breaktable.getTranslation().x - 5, 0, dur, in));
                            }
                        }).size(s, s+4);

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
                                player.placeMode = mode;
                            }).group(group);
                        }

                    }}.end().get();

                    breaktable.getParent().swapActor(breaktable, next);

                    breaktable.getTranslation().set(-breaktable.getPrefWidth(), 0);

                }}.end().get();

                //one.getParent().swapActor(one, two);

            }}.end();
		}
	}
}
