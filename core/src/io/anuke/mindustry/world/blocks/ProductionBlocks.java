package io.anuke.mindustry.world.blocks;

import io.anuke.mindustry.graphics.Fx;
import io.anuke.mindustry.resource.Item;
import io.anuke.mindustry.resource.ItemStack;
import io.anuke.mindustry.resource.Liquid;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.blocks.types.generation.*;
import io.anuke.mindustry.world.blocks.types.generation.SolarGenerator;
import io.anuke.mindustry.world.blocks.types.storage.CoreBlock;
import io.anuke.mindustry.world.blocks.types.production.*;

public class ProductionBlocks{
	public static final Block
	
	core = new CoreBlock("core"){},
	
	pump = new Pump("pump"){},
	
	fluxpump = new Pump("fluxpump"){
		{
			pumpAmount = 3f;
		}
	},
	
	smelter = new Smelter("smelter"){
		{
			health = 70;
			inputs = new Item[]{Item.iron};
			fuel = Item.coal;
			result = Item.steel;
		}
	},
	
	alloysmelter = new Smelter("alloysmelter"){
		{
			health = 90;
			inputs = new Item[]{Item.titanium, Item.steel};
			fuel = Item.coal;
			result = Item.dirium;
			burnDuration = 40f;
			craftTime = 20f;
		}
	},
	
	coalextractor = new LiquidCrafter("coalextractor"){
		{
			input = Item.stone;
			inputAmount = 5;
			inputLiquid = Liquid.water;
			liquidAmount = 18.99f;
			output = Item.coal;
			health = 50;
			purifyTime = 50;
		}
	},
	
	titaniumextractor = new LiquidCrafter("titaniumextractor"){
		{
			input = Item.iron;
			inputAmount = 6;
			inputLiquid = Liquid.water;
			liquidAmount = 40f;
			liquidCapacity = 41f;
			purifyTime = 60;
			output = Item.titanium;
			health = 70;
		}
	},

	uraniumextractor = new LiquidCrafter("uraniumextractor"){
		{
			input = Item.iron;
			inputAmount = 6;
			inputLiquid = Liquid.water;
			liquidAmount = 40f;
			liquidCapacity = 41f;
			purifyTime = 60;
			output = Item.uranium;
			health = 70;
		}
	},
	
	oilrefinery = new LiquidCrafter("oilrefinery"){
		{
			inputLiquid = Liquid.oil;
			liquidAmount = 45f;
			liquidCapacity = 46f;
			purifyTime = 60;
			output = Item.coal;
			health = 80;
			craftEffect = Fx.purifyoil;
		}
	},
	
	stoneformer = new LiquidCrafter("stoneformer"){
		{
			input = null;
			inputLiquid = Liquid.lava;
			liquidAmount = 16f;
			liquidCapacity = 21f;
			purifyTime = 12;
			output = Item.stone;
			health = 80;
			craftEffect = Fx.purifystone;
		}
	},
	
	lavasmelter = new LiquidCrafter("lavasmelter"){
		{
			input = Item.iron;
			inputAmount = 1;
			inputLiquid = Liquid.lava;
			liquidAmount = 40f;
			liquidCapacity = 41f;
			purifyTime = 30;
			output = Item.steel;
			health = 80;
			craftEffect = Fx.purifystone;
		}
	},

	pulverizer = new PowerSmelter("pulverizer"){
		{
			inputs = new ItemStack[]{new ItemStack(Item.stone, 5)};
			result = Item.quartz;
			health = 50;
			craftTime = 60f;
			powerDrain = 0.02f;
			craftEffect = Fx.pulverize;
		}
	},

	siliconextractor = new LiquidCrafter("siliconextractor"){
		{
			input = Item.quartz;
			inputAmount = 5;
			inputLiquid = Liquid.water;
			liquidAmount = 18.99f;
			output = Item.silicon;
			health = 50;
			purifyTime = 50;
		}
	},
	
	stonedrill = new Drill("stonedrill"){
		{
			resource = Blocks.stone;
			result = Item.stone;
			time = 4;
		}
	},
	
	irondrill = new Drill("irondrill"){
		{
			resource = Blocks.iron;
			result = Item.iron;
			time = 6;
		}
	},
	
	coaldrill = new Drill("coaldrill"){
		{
			resource = Blocks.coal;
			result = Item.coal;
			time = 6;
		}
	},
	
	uraniumdrill = new Drill("uraniumdrill"){
		{
			resource = Blocks.uranium;
			result = Item.uranium;
			time = 7;
		}
	},
	
	titaniumdrill = new Drill("titaniumdrill"){
		{
			resource = Blocks.titanium;
			result = Item.titanium;
			time = 7;
		}
	},
	
	laserdrill = new PowerDrill("laserdrill"){
		{
			time = 4;
			size = 2;
			powerUse = 0.08f;
		}
	},

	plasmadrill = new PowerDrill("plasmadrill"){
		{
			time = 4;
			size = 3;
			powerUse = 0.16f;
		}
	},

	nucleardrill = new PowerDrill("nucleardrill"){
		{
			time = 4;
			size = 4;
			powerUse = 0.32f;
		}
	},

	quartzextractor = new Drill("quartzextractor"){
		{
			resource = Blocks.sand;
			result = Item.quartz;
			time = 5;
			size = 2;
		}
	},

	cultivator = new LiquidDrill("cultivator"){
		{
			resource = Blocks.grass;
			result = Item.biomatter;
			inputLiquid = Liquid.water;
			inputLiquidAmount = 0.1f;
			time = 5;
			size = 2;
		}
	},

	coalgenerator = new ItemPowerGenerator("coalgenerator"){
		{
			generateItem = Item.coal;
			powerOutput = 0.04f;
			powerCapacity = 40f;
		}
	},
	thermalgenerator = new LiquidPowerGenerator("thermalgenerator"){
		{
			generateLiquid = Liquid.lava;
			maxLiquidGenerate = 0.5f;
			powerPerLiquid = 0.08f;
			powerCapacity = 40f;
			generateEffect = Fx.redgeneratespark;
		}
	},
	combustiongenerator = new LiquidPowerGenerator("combustiongenerator"){
		{
			generateLiquid = Liquid.oil;
			maxLiquidGenerate = 0.4f;
			powerPerLiquid = 0.12f;
			powerCapacity = 40f;
		}
	},
	rtgenerator = new ItemPowerGenerator("rtgenerator"){
		{
			generateItem = Item.uranium;
			powerCapacity = 40f;
			powerOutput = 0.03f;
			itemDuration = 240f;
		}
	},
	solarpanel = new SolarGenerator("solarpanel"){

	},
	nuclearReactor = new NuclearReactor("nuclearreactor"){
		{
			size = 3;
			health = 600;
			breaktime *= 2.3f;
		}
	},
	weaponFactory = new WeaponFactory("weaponfactory"){
		{
			size = 2;
			health = 250;
		}
	};
}
