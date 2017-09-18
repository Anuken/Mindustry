package io.anuke.mindustry;

import static io.anuke.mindustry.Vars.*;

import com.badlogic.gdx.utils.ObjectMap;

import io.anuke.mindustry.resource.Item;
import io.anuke.mindustry.resource.ItemStack;

public class Inventory{
	final static ObjectMap<Item, Integer> items = new ObjectMap<>();
	
	public static void clearItems(){
		items.clear();
		//TODO make this not hardcoded
		items.put(Item.stone, 40);
		
		if(debug){
			items.put(Item.stone, 2000000);
			items.put(Item.iron, 2000000);
			items.put(Item.steel, 2000000);
			items.put(Item.coal, 2000000);
			items.put(Item.titanium, 2000000);
		}
	}
	
	public static Iterable<Item> getItemTypes(){
		return items.keys();
	}
	
	public static int getAmount(Item item){
		return items.get(item, 0);
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
	
	public static void removeItems(ItemStack... reqs){
		for(ItemStack req : reqs)
		items.put(req.item, items.get(req.item, 0)-req.amount);
		ui.updateItems();
	}
	
	public static ObjectMap<Item, Integer> getItems(){
		return items;
	}
}
