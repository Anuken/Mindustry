package io.anuke.mindustry.ui.fragments;

import static io.anuke.mindustry.Vars.*;

import com.badlogic.gdx.Gdx;

import io.anuke.mindustry.core.GameState;
import io.anuke.mindustry.core.GameState.State;
import io.anuke.mindustry.ui.MenuButton;
import io.anuke.mindustry.world.Map;
import io.anuke.ucore.scene.builders.imagebutton;
import io.anuke.ucore.scene.builders.table;
import io.anuke.ucore.scene.ui.layout.Unit;

public class MenuFragment implements Fragment{
	
	public void build(){
		if(!android){
			//menu table
			new table(){{
				
				new table(){{
					float scale = 4f;
					defaults().size(100*scale, 21*scale).pad(-10f).units(Unit.dp);
					
					add(new MenuButton("text-play", ()-> ui.showLevels()));
					row();
					
					add(new MenuButton("text-tutorial", ()-> control.playMap(Map.tutorial)));
					row();
					
					if(!gwt){
						add(new MenuButton("text-load", ()-> ui.showLoadGame()));
						row();
					}
					
					add(new MenuButton("text-settings", ()-> ui.showPrefs()));
					row();
					
					if(!gwt){
						add(new MenuButton("text-exit", ()-> Gdx.app.exit()));
					}
					get().pad(Unit.dp.inPixels(16));
				}};
	
				visible(()->GameState.is(State.menu));
			}}.end();
		}else{
			new table(){{
				defaults().size(120f).pad(5).units(Unit.dp);
				float isize = Unit.dp.inPixels(14f*4);
				
				new imagebutton("icon-play-2", isize, () -> ui.showLevels()).text("Play").padTop(4f);
				
				new imagebutton("icon-tutorial", isize, ()-> control.playMap(Map.tutorial)).text("Tutorial").padTop(4f);
				
				new imagebutton("icon-load", isize, () -> ui.showLoadGame()).text("Load").padTop(4f);

				new imagebutton("icon-tools", isize, () -> ui.showPrefs()).text("Settings").padTop(4f);
				
				visible(()->GameState.is(State.menu));
			}}.end();
		}		
		
		//settings icon
		new table(){{
			atop().aright();
			new imagebutton("icon-info", Unit.dp.inPixels(30f), ()->{
				ui.showAbout();
			}).get().pad(Unit.dp.inPixels(14));
		}}.end().visible(()->GameState.is(State.menu));
	}
}
