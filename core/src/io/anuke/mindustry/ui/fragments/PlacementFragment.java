package io.anuke.mindustry.ui.fragments;

import static io.anuke.mindustry.Vars.*;

import com.badlogic.gdx.utils.Align;

import io.anuke.mindustry.GameState;
import io.anuke.mindustry.GameState.State;
import io.anuke.mindustry.input.AndroidInput;
import io.anuke.mindustry.input.PlaceMode;
import io.anuke.ucore.scene.builders.imagebutton;
import io.anuke.ucore.scene.builders.label;
import io.anuke.ucore.scene.builders.table;
import io.anuke.ucore.scene.ui.ButtonGroup;
import io.anuke.ucore.scene.ui.ImageButton;
import io.anuke.ucore.scene.ui.layout.Unit;

public class PlacementFragment implements Fragment{
	
	public void build(){
		if(android){
			//placement table
			new table(){{
				visible(()->player.recipe != null && !GameState.is(State.menu));
				abottom();
				aleft();
				
				
				new table("pane"){{
					new label(()->"Placement Mode: [orange]" + AndroidInput.mode.name()).pad(4).units(Unit.dp);
					row();
					
					aleft();
					
					new table(){{
						aleft();
						ButtonGroup<ImageButton> group = new ButtonGroup<>();
						
						defaults().size(58, 62).pad(6).units(Unit.dp);
						
						for(PlaceMode mode : PlaceMode.values()){
							new imagebutton("icon-" + mode.name(), "toggle",  Unit.dp.inPixels(10*3), ()->{
								AndroidInput.mode = mode;
							}){{
								group.add(get());
							}};
						}
						
						new imagebutton("icon-cancel", Unit.dp.inPixels(14*3), ()->{
							player.recipe = null;
						}).visible(()->player.recipe != null && AndroidInput.mode == PlaceMode.touch);
						
						new imagebutton("icon-rotate-arrow", Unit.dp.inPixels(14*3), ()->{
							player.rotation ++;
							player.rotation %= 4;
						}).update(i->{
							i.getImage().setOrigin(Align.center);
							i.getImage().setRotation(player.rotation*90);
						}).visible(()->player.recipe != null && AndroidInput.mode == PlaceMode.touch 
								&& player.recipe.result.rotate);
						
					}}.left().end();
				}}.end();
			}}.end();
		
		}
	}
}
