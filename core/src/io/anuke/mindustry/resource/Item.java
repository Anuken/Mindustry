package io.anuke.mindustry.resource;

import com.badlogic.gdx.utils.Array;
import io.anuke.ucore.util.Bundles;

public class Item{
	private static final Array<Item> items = new Array<>();

	public static final Item
		stone = new Item("stone"),
		iron = new Item("iron"),
		coal = new Item("coal"),
		steel = new Item("steel"),
		titanium = new Item("titanium"),
		dirium = new Item("dirium"),
		uranium = new Item("uranium"),
		sand = new Item("sand"),
		glass = new Item("glass"),
		silicon = new Item("silicon");

	public final int id;
	public final String name;

	public Item(String name) {
		this.id = items.size;
		this.name = name;

		Item.items.add(this);
	}

	public String localizedName(){
		return Bundles.get("item." + this.name + ".name");
	}

	@Override
	public String toString() {
		return localizedName();
	}

	public static Array<Item> getAllItems() {
		return Item.items;
	}

	public static Item getByID(int id){
		return items.get(id);
	}
}
