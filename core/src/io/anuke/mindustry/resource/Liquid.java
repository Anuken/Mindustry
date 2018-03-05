package io.anuke.mindustry.resource;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.Array;
import io.anuke.ucore.util.Bundles;

public class Liquid {

	private static final Array<Liquid> liquids = new Array<>();

	public static final Liquid

	water = new Liquid("water", Color.ROYAL),
	plasma = new Liquid("plasma", Color.CORAL){
		{
			flammability = 0.4f;
			viscosity = 0.1f;
			heatCapacity = 0.2f;
		}
	},
	lava = new Liquid("lava", Color.valueOf("ed5334")){
		{
			temperature = 0.7f;
			viscosity = 0.8f;
		}
	},
	oil = new Liquid("oil", Color.valueOf("292929")){
		{
			viscosity = 0.7f;
			flammability = 0.5f;
			explosiveness = 0.6f;
		}
	},
	cryofluid = new Liquid("cryofluid", Color.SKY){
		{
			heatCapacity = 0.8f;
			temperature = 0.1f;
		}
	},
	sulfuricAcid = new Liquid("sulfuricAcid", Color.YELLOW){
		{
			flammability = 0.4f;
			explosiveness = 0.4f;
			heatCapacity = 0.4f;
		}
	};
	
	public final Color color;
	public final String name;
	public final int id;

	public float flammability;
	public float temperature = 0.5f;
	public float heatCapacity = 0.5f;
	public float viscosity = 0.5f;
	public float explosiveness;
	
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
