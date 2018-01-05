package io.anuke.mindustry.ui.fragments;

import static io.anuke.mindustry.Vars.*;

import io.anuke.mindustry.core.GameState;
import io.anuke.mindustry.core.GameState.State;
import io.anuke.mindustry.resource.Weapon;
import io.anuke.ucore.core.Core;
import io.anuke.ucore.core.Draw;
import io.anuke.ucore.scene.ui.ButtonGroup;
import io.anuke.ucore.scene.ui.ImageButton;
import io.anuke.ucore.scene.ui.Tooltip;
import io.anuke.ucore.scene.ui.layout.Table;

public class WeaponFragment implements Fragment{
	Table weapontable;
	
	public void build(){
		weapontable = Core.scene.table();
		weapontable.bottom().left();
		weapontable.setVisible(()-> !GameState.is(State.menu));
		
		if(android){
			weapontable.remove();
		}
	}
	
	public void updateWeapons(){
		weapontable.clearChildren();
		
		ButtonGroup<ImageButton> group = new ButtonGroup<>();
		
		weapontable.defaults().size(58, 62);
		
		for(Weapon weapon : control.getWeapons()){
			ImageButton button = new ImageButton(Draw.region(weapon.name()), "toggle");
			button.getImageCell().size(8*5);
			
			group.add(button);
			
			button.clicked(()->{
				if(weapon == player.weapon) return;
				player.weapon = weapon;
				button.setChecked(true);
			});
			
			button.setChecked(weapon == player.weapon);
			
			weapontable.add(button);
			
			Table tiptable = new Table();
			String description = weapon.description;
				
			tiptable.background("button");
			tiptable.add("$weapon."+weapon.name()+".name", 0.5f).left().padBottom(3f);
				
			tiptable.row();
			tiptable.row();
			tiptable.add("[GRAY]" + description).left();
			tiptable.margin(14f);
			
			Tooltip<Table> tip = new Tooltip<>(tiptable);
			
			tip.setInstant(true);

			button.addListener(tip);
			
		}
		
		weapontable.addImageButton("icon-menu", 8*4, ()->{
			ui.showUpgrades();
		});
	}
}
