package io.anuke.mindustry;

import static io.anuke.mindustry.Vars.*;

import io.anuke.mindustry.resource.Item;
import io.anuke.mindustry.resource.ItemStack;

public class Inventory{
	
	public static void clearItems(){
		items.clear();
		//TODO make this not hardcoded
		items.put(Item.stone, 40);
		
		if(debug){
			items.put(Item.stone, 2000);
			items.put(Item.iron, 2000);
			items.put(Item.steel, 2000);
		}
	}
	
	public static void addItem(Item item, int amount){
		items.put(item, items.get(item, 0)+amount);
		ui.updateItems();
	}
	
	public static boolean hasItems(ItemStack[] items){
		for(ItemStack stack : items)
			if(!hasItem(stack))
				return false;
		return true;
	}
	
	public static boolean hasItem(ItemStack req){
		return items.get(req.item, 0) >= req.amount; 
	}
	
	public static void removeItem(ItemStack req){
		items.put(req.item, items.get(req.item, 0)-req.amount);
		ui.updateItems();
	}
}
