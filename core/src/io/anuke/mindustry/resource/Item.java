package io.anuke.mindustry.resource;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Array;
import io.anuke.ucore.graphics.Draw;
import io.anuke.ucore.util.Bundles;

public class Item implements Comparable<Item>{
	private static final Array<Item> items = new Array<>();

	public static final Item
	stone = new Item("stone"){
		{
			material = false;
		}
	},
	iron = new Item("iron"),
	lead = new Item("lead"),
	coal = new Item("coal"){
		{
			explosiveness = 0.2f;
			flammability = 0.5f;
			fluxiness = 0.5f;
			material = false;
		}
	},
	steel = new Item("steel"),
	titanium = new Item("titanium"),
	uranium = new Item("uranium"){
		{
			explosiveness = 0.1f;
			material = false;
		}
	},
	silicon = new Item("silicon"),
	plastic = new Item("plastic"),
	densealloy = new Item("densealloy"),
	biomatter = new Item("biomatter"){
		{
			material = false;
			flammability = 0.4f;
		}
	};

	public final int id;
	public final String name;
	public TextureRegion region;

	public float explosiveness = 0f;
	public float flammability = 0f;
	/**how effective this item is as flux for smelting. 0 = not a flux, 0.5 = normal flux, 1 = very good*/
	public float fluxiness = 0f;
	public boolean material = true;

	public Item(String name) {
		this.id = items.size;
		this.name = name;

		items.add(this);
	}

	public void init(){
		this.region = Draw.region("item-" + name);
	}

	public String localizedName(){
		return Bundles.get("item." + this.name + ".name");
	}

	@Override
	public String toString() {
		return localizedName();
	}

	@Override
	public int compareTo(Item item) {
		return Integer.compare(id, item.id);
	}

	public static Array<Item> getAllItems() {
		return Item.items;
	}

	public static Item getByID(int id){
		return items.get(id);
	}
}
