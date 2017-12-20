package io.anuke.mindustry.ui.fragments;

import static io.anuke.mindustry.Vars.*;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.Align;

import io.anuke.mindustry.core.GameState;
import io.anuke.mindustry.core.GameState.State;
import io.anuke.mindustry.input.PlaceMode;
import io.anuke.ucore.scene.builders.*;
import io.anuke.ucore.scene.event.Touchable;
import io.anuke.ucore.scene.ui.ButtonGroup;
import io.anuke.ucore.scene.ui.ImageButton;
import io.anuke.ucore.scene.ui.layout.Unit;
import io.anuke.ucore.util.Mathf;

public class PlacementFragment implements Fragment{
	
	public void build(){
		if(android){
			//placement table
			new table(){{
				visible(()->player.recipe != null && !GameState.is(State.menu));
				abottom();
				aleft();
				/*
				Image image = new Image("icon-arrow");
				image.update(() -> { 
					image.setRotation(player.rotation*90);
					image.setOrigin(Align.center);
				});
				
				new table("pane"){{
					visible(() -> player.recipe != null && player.recipe.result.rotate);
					add(image).size(40f);
				}}.size(54f).end();
				
				row();*/
				
				new table(){{
					touchable(Touchable.enabled);
					
					aleft();
					new label("place mode");
					row();
					
					new table("pane"){{
						margin(5f);
						aleft();
						ButtonGroup<ImageButton> group = new ButtonGroup<>();
						
						defaults().size(54, 58).pad(0);
						
						for(PlaceMode mode : PlaceMode.values()){
							if(!mode.shown || mode.delete) continue;
							
							defaults().padBottom(-5.5f);
							
							new imagebutton("icon-" + mode.name(), "toggle",  Unit.dp.scl(10*3), ()->{
								control.getInput().resetCursor();
								player.placeMode = mode;
							}).group(group);	
						}
						
						row();
						
						Color color = Color.GRAY;//Colors.get("accent"); //Color.valueOf("4d4d4d")
						
						new imagebutton("icon-cancel", Unit.dp.scl(14*3), ()->{
							player.recipe = null;
						}).imageColor(color)
						.visible(()->player.recipe != null);
						
						new button("", ()->{}).get().setTouchable(Touchable.disabled);;
						
						new imagebutton("icon-arrow", Unit.dp.scl(14*3), ()->{
							player.rotation = Mathf.mod(player.rotation + 1, 4);
						}).imageColor(color).visible(() -> player.recipe != null).update(image ->{
							image.getImage().setRotation(player.rotation*90);
							image.getImage().setOrigin(Align.center);
						});
						
					}}.left().end();
				}}.end();
			}}.end();
			
			new table(){{
				visible(()->player.recipe == null && !GameState.is(State.menu));
				abottom();
				aleft();
				
				new label("break mode");
				row();
				
				new table("pane"){{
					margin(5f);
					touchable(Touchable.enabled);
					aleft();
					ButtonGroup<ImageButton> group = new ButtonGroup<>();
					
					defaults().size(54, 58).pad(0);
					
					int d = 0;
					
					for(PlaceMode mode : PlaceMode.values()){
						if(!mode.shown || !mode.delete) continue;
						
						defaults().padBottom(d < 2 ? -5.5f : 0);
						
						new imagebutton("icon-" + mode.name(), "toggle",  Unit.dp.scl(10*3), ()->{
							control.getInput().resetCursor();
							player.breakMode = mode;
						}){{
							group.add(get());
						}};
					}
					
				}}.end();
			}}.end();
		}
	}
}
