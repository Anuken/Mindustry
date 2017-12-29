package io.anuke.mindustry.ui;

import static io.anuke.mindustry.Vars.*;

import com.badlogic.gdx.graphics.Color;

import io.anuke.mindustry.Vars;
import io.anuke.mindustry.core.GameState;
import io.anuke.mindustry.core.GameState.State;
import io.anuke.mindustry.resource.ItemStack;
import io.anuke.mindustry.resource.Weapon;
import io.anuke.ucore.core.Draw;
import io.anuke.ucore.core.Effects;
import io.anuke.ucore.function.Listenable;
import io.anuke.ucore.scene.ui.Image;
import io.anuke.ucore.scene.ui.TextButton;
import io.anuke.ucore.scene.ui.Tooltip;
import io.anuke.ucore.scene.ui.layout.Table;

public class UpgradeDialog extends FloatingDialog{
	boolean wasPaused = false;

	public UpgradeDialog() {
		super("$text.upgrades");
		setup();
	}
	
	void setup(){
		addCloseButton();
		
		hidden(()->{
			if(!wasPaused)
				GameState.set(State.playing);
		});
		shown(()->{
			wasPaused = GameState.is(State.paused);
			GameState.set(State.paused);
		});
		
		Table weptab = new Table();
		weptab.margin(20);
		
		int i = 0;
		for(Weapon weapon : Weapon.values()){
			TextButton button = new TextButton("$weapon."+weapon.name()+".name");
			
			Image img = new Image(Draw.region(weapon.name()));
			button.add(img).size(8*5);
			button.getCells().reverse();
			button.row();
			button.margin(14);
			button.getLabelCell().left();
			button.pack();
			
			
			button.update(()->{
				
				if(control.hasWeapon(weapon)){
					button.setDisabled(true);
					button.setColor(Color.GRAY);
				}else if(!Vars.control.hasItems(weapon.requirements)){
					button.setDisabled(true);
				}else{
					button.setDisabled(false);
					button.setColor(Color.WHITE);
				}
			});
			
			if(i > 0 && (i)%2==0)
				weptab.row();
			
			i++;
			
			weptab.add(button).width(220);
			
			Table tiptable = new Table();
			
			Listenable run = ()->{
				tiptable.clearChildren();
				
				String description = weapon.description;
				
				tiptable.background("pane");
				tiptable.add("[orange]" + weapon.localized(), 0.5f).left().padBottom(4f);
				
				Table reqtable = new Table();
				
				tiptable.row();
				tiptable.add(reqtable).left();
				
				if(!control.hasWeapon(weapon)){
					ItemStack[] req = weapon.requirements;
					for(ItemStack s : req){
						
						int amount = Math.min(Vars.control.getAmount(s.item), s.amount);
						reqtable.addImage(Draw.region("icon-" + s.item.name)).padRight(3).size(8*2);
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
					tiptable.add("$text.purchased").padTop(6).left();
				}
				tiptable.margin(14f);
			};
			
			run.listen();
			
			Tooltip tip = new Tooltip(tiptable, run);
			
			tip.setInstant(true);

			button.addListener(tip);
			
			button.clicked(()->{
				if(button.isDisabled()) return;
				
				Vars.control.removeItems(weapon.requirements);
				control.addWeapon(weapon);
				ui.updateWeapons();
				run.listen();
				Effects.sound("purchase");
			});
		}
		
		content().add("$text.weapons");
		content().row();
		content().add(weptab);
		content().row();
	}

}
