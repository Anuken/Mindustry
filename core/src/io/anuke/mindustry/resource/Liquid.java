package io.anuke.mindustry.resource;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.Array;
import io.anuke.ucore.util.Bundles;

public class Liquid {

	private static final Array<Liquid> liquids = new Array<>();

	public static final Liquid
		water = new Liquid("water", Color.ROYAL),
		plasma = new Liquid("plasma", Color.CORAL),
		lava = new Liquid("lava", Color.valueOf("ed5334")),
		oil = new Liquid("oil", Color.valueOf("292929")),
		cryofluid = new Liquid("cryofluid", Color.SKY);
	
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

	public static Liquid getByID(int id){
		return liquids.get(id);
	}
}
