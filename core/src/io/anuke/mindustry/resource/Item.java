package io.anuke.mindustry.resource;

import com.badlogic.gdx.utils.Array;
import io.anuke.ucore.util.Bundles;

public class Item{

	public static final Array<Item> items = new Array<>();

	public static final Item stone = new Item("stone");
	public static final Item iron = new Item("iron");
	public static final Item coal = new Item("coal");
	public static final Item steel = new Item("steel");
	public static final Item titanium = new Item("titanium");
	public static final Item dirium = new Item("dirium");
	public static final Item uranium = new Item("uranium");

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
}
