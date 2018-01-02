package io.anuke.mindustry.ui.fragments;

import com.badlogic.gdx.Gdx;
import io.anuke.mindustry.Mindustry;
import io.anuke.mindustry.core.GameState;
import io.anuke.mindustry.core.GameState.State;
import io.anuke.mindustry.ui.MenuButton;
import io.anuke.mindustry.ui.PressGroup;
import io.anuke.ucore.scene.builders.imagebutton;
import io.anuke.ucore.scene.builders.table;

import static io.anuke.mindustry.Vars.*;

public class MenuFragment implements Fragment{
	
	public void build(){
		if(!android){
			//menu table
			new table(){{
				
				new table(){{
					PressGroup group = new PressGroup();
					
					float scale = 4f;
					defaults().size(100*scale, 21*scale).pad(-10f);
					
					add(new MenuButton("$text.play", group, ui::showLevels));
					row();

					if(!gwt){
						add(new MenuButton("$text.joingame", group, ui::showJoinGame));
						row();
					}
					
					add(new MenuButton("$text.tutorial", group, ()-> control.playMap(world.maps().getMap("tutorial"))));
					row();
					
					if(!gwt){
						add(new MenuButton("$text.loadgame", group, ui::showLoadGame));
						row();
						
						add(new MenuButton("$text.editor", group, ui::showEditor));
						row();
					}
					
					add(new MenuButton("$text.settings", group, ui::showPrefs));
					row();
					
					if(!gwt){
						add(new MenuButton("$text.quit", group, Gdx.app::exit));
					}
					get().margin(16);
				}}.end();
	
				visible(()->GameState.is(State.menu));
			}}.end();
		}else{
			new table(){{
				new table(){{
					defaults().size(120f).pad(5);
					float isize = 14f*4;
					
					new imagebutton("icon-play-2", isize, () -> ui.showLevels()).text("$text.play").padTop(4f);
					
					new imagebutton("icon-tutorial", isize, () -> control.playMap(world.maps().getMap("tutorial"))).text("$text.tutorial").padTop(4f);
					
					new imagebutton("icon-load", isize, () -> ui.showLoadGame()).text("$text.load").padTop(4f);
					
					row();
					
					new imagebutton("icon-editor", isize, () -> ui.showEditor()).text("$text.editor").padTop(4f);
	
					new imagebutton("icon-tools", isize, () -> ui.showPrefs()).text("$text.settings").padTop(4f);

					new imagebutton("icon-tools", isize, () -> ui.showJoinGame()).text("$text.joingame").padTop(4f);
					
					if(Mindustry.donationsCallable != null){
						new imagebutton("icon-donate", isize, () -> {
							Mindustry.donationsCallable.run();
						}).text("$text.donate").padTop(4f);
					}
					
					visible(()->GameState.is(State.menu));
				}}.end();
			}}.end();
		}		
		
		//settings icon
		new table(){{
			atop().aright();
			if(Mindustry.hasDiscord){
				new imagebutton("icon-discord", 30f, ()->{
					ui.showDiscord();
				}).margin(14);
			}
			new imagebutton("icon-info", 30f, ()->{
				ui.showAbout();
			}).margin(14);
		}}.end().visible(()->GameState.is(State.menu));
	}
}
