package io.anuke.mindustry.resource;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.Array;
import io.anuke.ucore.util.Bundles;

public class Liquid {

	public static final Array<Liquid> liquids = new Array<>();

	public static final Liquid water = new Liquid("water", Color.ROYAL);
	public static final Liquid plasma = new Liquid("plasma", Color.CORAL);
	public static final Liquid lava = new Liquid("lava", Color.valueOf("ed5334"));
	public static final Liquid oil = new Liquid("oil", Color.valueOf("292929"));
	
	public final Color color;
	public final String name;
	public final int id;
	
	public Liquid(String name, Color color) {
		this.name = name;
		this.color = new Color(color);

		this.id = liquids.size;

		Liquid.liquids.add(this);
	}

	public String localizedName(){
		return Bundles.get("liquid."+ this.name + ".name");
	}

	@Override
	public String toString(){
		return localizedName();
	}

	public static Array<Liquid> getAllLiquids() {
		return Liquid.liquids;
	}
}
