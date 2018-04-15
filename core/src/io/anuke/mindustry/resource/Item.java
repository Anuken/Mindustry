package io.anuke.mindustry.resource;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Array;
import io.anuke.mindustry.graphics.Palette;
import io.anuke.ucore.graphics.Draw;
import io.anuke.ucore.util.Bundles;

public class Item implements Comparable<Item>{
	private static final Array<Item> items = new Array<>();

	public final int id;
	public final String name;
	public final Color color;
	public TextureRegion region;

	public float explosiveness = 0f;
	/**flammability above 0.3 makes this eleigible for item burners.*/
	public float flammability = 0f;
	/**how radioactive this item is. 0=none, 1=ridiculous*/
	public float radioactivity;
	/**how effective this item is as flux for smelting. 0 = not a flux, 0.5 = normal flux, 1 = very good*/
	public float fluxiness = 0f;
	/**whether this material goes in the core*/
	public boolean material = true;
	/**drill hardness of the item*/
	public int hardness = 0;
	/**the burning color of this item*/
	public Color flameColor = Palette.darkFlame.cpy();

	public Item(String name, Color color) {
		this.id = items.size;
		this.name = name;
		this.color = color;

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
