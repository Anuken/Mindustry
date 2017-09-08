package io.anuke.mindustry.ui;

import static io.anuke.mindustry.Vars.*;

import com.badlogic.gdx.graphics.Color;

import io.anuke.mindustry.GameState;
import io.anuke.mindustry.GameState.State;
import io.anuke.mindustry.Inventory;
import io.anuke.mindustry.entities.Weapon;
import io.anuke.mindustry.resource.ItemStack;
import io.anuke.ucore.core.Draw;
import io.anuke.ucore.core.Effects;
import io.anuke.ucore.function.Listenable;
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
			GameState.set(State.playing);
		});
		shown(()->{
			GameState.set(State.paused);
		});
		
		getButtonTable().addButton("Ok", ()->{
			hide();
		}).size(84, 48).pad(4);
		
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
				
				if(control.hasWeapon(weapon)){
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
			
			Listenable run = ()->{
				tiptable.clearChildren();
				
				String description = weapon.description;
				
				tiptable.background("button");
				tiptable.add("[PURPLE]" + weapon.name(), 0.75f).left().padBottom(2f);
				
				if(!control.hasWeapon(weapon)){
					ItemStack[] req = weapon.requirements;
					for(ItemStack s : req){
						tiptable.row();
						int amount = Math.min(Inventory.getAmount(s.item), s.amount);
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
				if(control.hasWeapon(weapon))
					tiptable.add("[LIME]Purchased!").left();
				tiptable.pad(10f);
			};
			
			run.listen();
			
			Tooltip tip = new Tooltip(tiptable, run);
			
			tip.setInstant(true);

			button.addListener(tip);
			
			button.clicked(()->{
				if(button.isDisabled()) return;
				
				Inventory.removeItems(weapon.requirements);
				control.addWeapon(weapon);
				ui.updateWeapons();
				run.listen();
				Effects.sound("purchase");
			});
		}
		
		content().add("Weapons");
		content().row();
		content().add(weptab);
		content().row();
	}

}
