package io.anuke.mindustry.ui;

import static io.anuke.mindustry.Vars.*;

import com.badlogic.gdx.graphics.Color;

import io.anuke.mindustry.GameState;
import io.anuke.mindustry.GameState.State;
import io.anuke.mindustry.Inventory;
import io.anuke.mindustry.resource.ItemStack;
import io.anuke.mindustry.resource.Weapon;
import io.anuke.ucore.core.Draw;
import io.anuke.ucore.core.Effects;
import io.anuke.ucore.function.Listenable;
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
		}).size(96, 50).pad(5);
		
		Table weptab = new Table();
		//weptab.background("button");
		weptab.pad(20);
		
		int i = 0;
		for(Weapon weapon : Weapon.values()){
			TextButton button = new TextButton(weapon.name());
			
			Image img = new Image(Draw.region(weapon.name()));
			button.add(img).size(8*5);
			button.getCells().reverse();
			button.row();
			button.pad(14);
			button.getLabelCell().left();
			button.pack();
			
			
			button.update(()->{
				
				if(control.hasWeapon(weapon)){
					button.setDisabled(true);
					button.setColor(Color.GRAY);
				}else if(!Inventory.hasItems(weapon.requirements)){
					button.setDisabled(true);
				}
			});
			
			if(i > 0 && (i)%2==0)
				weptab.row();
			
			i++;
			
			weptab.add(button).width(250);
			
			Table tiptable = new Table();
			
			Listenable run = ()->{
				tiptable.clearChildren();
				
				String description = weapon.description;
				
				tiptable.background("pane");
				tiptable.add("[orange]" + weapon.name(), 0.5f).left().padBottom(4f);
				
				Table reqtable = new Table();
				
				tiptable.row();
				tiptable.add(reqtable).left();
				
				if(!control.hasWeapon(weapon)){
					ItemStack[] req = weapon.requirements;
					for(ItemStack s : req){
						
						int amount = Math.min(Inventory.getAmount(s.item), s.amount);
						reqtable.addImage(Draw.region("icon-" + s.item.name())).padRight(3).size(8*2);
						reqtable.add(
								(amount >= s.amount ? "" : "[RED]")
						+ amount + " / " +s.amount, 0.5f).left();
						reqtable.row();
					}
				}
				
				tiptable.row();
				tiptable.add().size(10);
				tiptable.row();
				tiptable.add("[gray]" + description).left();
				tiptable.row();
				if(control.hasWeapon(weapon)){
					tiptable.add("[LIME]Purchased!").padTop(6).left();
				}
				tiptable.pad(14f);
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
