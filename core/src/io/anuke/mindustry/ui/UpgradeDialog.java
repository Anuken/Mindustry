package io.anuke.mindustry.ui;

import static io.anuke.mindustry.Vars.*;

import com.badlogic.gdx.graphics.Color;

import io.anuke.mindustry.Inventory;
import io.anuke.mindustry.entities.Weapon;
import io.anuke.mindustry.resource.ItemStack;
import io.anuke.ucore.core.Draw;
import io.anuke.ucore.core.Effects;
import io.anuke.ucore.scene.event.Touchable;
import io.anuke.ucore.scene.ui.*;
import io.anuke.ucore.scene.ui.layout.Table;

public class UpgradeDialog extends Dialog{

	public UpgradeDialog() {
		super("Upgrades");
		setup();
	}
	
	void setup(){
		addCloseButton();
		
		hidden(()->{
			paused = false;
		});
		shown(()->{
			paused = true;
		});
		
		getButtonTable().addButton("Ok", ()->{
			hide();
		});
		
		Table weptab = new Table();
		weptab.background("button");
		weptab.pad(20);
		
		int i = 0;
		for(Weapon weapon : Weapon.values()){
			TextButton button = new TextButton(weapon.name());
			
			Image img = new Image(Draw.region("weapon-"+weapon.name()));
			button.add(img).size(8*5);
			button.getCells().reverse();
			button.row();
			button.pack();
			
			button.update(()->{
				
				if(weapons.get(weapon) == Boolean.TRUE){
					button.setDisabled(true);
					button.setColor(Color.GRAY);
					button.setTouchable(Touchable.disabled);
				}else{
					button.setColor(Color.WHITE);
					button.setDisabled(false);
				}
				
				button.setDisabled(!Inventory.hasItems(weapon.requirements));
			});
			
			if(i > 0 && (i)%2==0)
				weptab.row();
			
			i++;
			
			weptab.add(button).width(210);
			
			Table tiptable = new Table();
			
			Runnable run = ()->{
				tiptable.clearChildren();
				
				String description = weapon.description;
				
				tiptable.background("button");
				tiptable.add("[PURPLE]" + weapon.name(), 0.75f).left().padBottom(2f);
				
				if(weapons.get(weapon) != Boolean.TRUE){
					ItemStack[] req = weapon.requirements;
					for(ItemStack s : req){
						tiptable.row();
						int amount = Math.min(items.get(s.item, 0), s.amount);
						tiptable.add(
								(amount >= s.amount ? "[YELLOW]" : "[RED]")
						+s.item + ": " + amount + " / " +s.amount, 0.5f).left();
					}
				}
				
				tiptable.row();
				tiptable.add().size(10);
				tiptable.row();
				tiptable.add("[ORANGE]" + description).left();
				tiptable.row();
				if(weapons.get(weapon) == Boolean.TRUE)
					tiptable.add("[LIME]Purchased!").left();
				tiptable.pad(10f);
			};
			
			run.run();
			
			Tooltip tip = new Tooltip(tiptable, run);
			
			tip.setInstant(true);

			button.addListener(tip);
			
			button.clicked(()->{
				Inventory.removeItems(weapon.requirements);
				weapons.put(weapon, true);
				ui.updateWeapons();
				run.run();
				Effects.sound("purchase");
			});
		}
		
		content().add("Weapons");
		content().row();
		content().add(weptab);
		content().row();
	}

}
