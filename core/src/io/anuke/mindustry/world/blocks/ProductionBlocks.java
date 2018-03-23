package io.anuke.mindustry.world.blocks;

import io.anuke.mindustry.graphics.Fx;
import io.anuke.mindustry.resource.Item;
import io.anuke.mindustry.resource.Liquid;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.blocks.types.generation.ItemPowerGenerator;
import io.anuke.mindustry.world.blocks.types.generation.LiquidPowerGenerator;
import io.anuke.mindustry.world.blocks.types.generation.NuclearReactor;
import io.anuke.mindustry.world.blocks.types.generation.SolarGenerator;
import io.anuke.mindustry.world.blocks.types.production.*;
import io.anuke.mindustry.world.blocks.types.storage.CoreBlock;

public class ProductionBlocks{
	public static final Block
	
	core = new CoreBlock("core"){},
	
	pump = new Pump("pump"){
		{
			pumpAmount = 0.8f;
		}
	},
	
	fluxpump = new Pump("fluxpump"){
		{
			pumpAmount = 1.2f;
		}
	},
	
	smelter = new Smelter("smelter"){
		{
			health = 70;
			inputs = new Item[]{Item.iron};
			fuel = Item.coal;
			result = Item.steel;
			craftTime = 25f;
		}
	},
	
	alloysmelter = new Smelter("alloysmelter"){
		{
			health = 90;
			inputs = new Item[]{Item.titanium, Item.steel};
			fuel = Item.coal;
			result = Item.densealloy;
			burnDuration = 45f;
			craftTime = 25f;
		}
	},

	powersmelter = new PowerSmelter("powersmelter"){
		{
			/*
			health = 90;
			inputs = new Item[]{Item.titanium, Item.steel};
			fuel = Item.coal;
			results = Item.dirium;
			burnDuration = 45f;
			craftTime = 25f;
			size = 2;*/
		}
	},

	cryofluidmixer = new LiquidMixer("cryofluidmixer"){
		{
			health = 200;
			inputLiquid = Liquid.water;
			outputLiquid = Liquid.cryofluid;
			inputItem = Item.titanium;
			liquidPerItem = 50f;
			itemCapacity = 50;
			powerUse = 0.1f;
			size = 2;
		}
	},
	
	coalextractor = new LiquidCrafter("coalextractor"){
		{
			input = Item.stone;
			inputAmount = 6;
			inputLiquid = Liquid.water;
			liquidAmount = 19f;
			output = Item.coal;
			health = 50;
			purifyTime = 50;
			health = 60;
		}
	},
	
	titaniumextractor = new LiquidCrafter("titaniumextractor"){
		{
			input = Item.stone;
			inputAmount = 8;
			inputLiquid = Liquid.water;
			liquidAmount = 40f;
			liquidCapacity = 41f;
			purifyTime = 60;
			output = Item.titanium;
			health = 70;
		}
	},
	
	oilrefinery = new LiquidCrafter("oilrefinery"){
		{
			inputLiquid = Liquid.oil;
			liquidAmount = 55f;
			liquidCapacity = 56f;
			purifyTime = 65;
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

	siliconextractor = new LiquidCrafter("siliconextractor"){
		{
			input = Item.stone;
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
			drillTime = 240;
		}
	},
	
	irondrill = new Drill("irondrill"){
		{
			resource = Blocks.iron;
			result = Item.iron;
			drillTime = 360;
		}
	},

	copperdrill = new Drill("copperdrill"){
		{
			resource = Blocks.copper;
			result = Item.copper;
			drillTime = 400;
		}
	},
	
	coaldrill = new Drill("coaldrill"){
		{
			resource = Blocks.coal;
			result = Item.coal;
			drillTime = 420;
		}
	},
	
	uraniumdrill = new Drill("uraniumdrill"){
		{
			resource = Blocks.uranium;
			result = Item.uranium;
			drillTime = 600;
		}
	},
	
	titaniumdrill = new Drill("titaniumdrill"){
		{
			resource = Blocks.titanium;
			result = Item.titanium;
			drillTime = 540;
		}
	},
	
	laserdrill = new GenericDrill("laserdrill"){
		{
			drillTime = 200;
			size = 2;
			powerUse = 0.2f;
			hasPower = true;
		}
	},

	nucleardrill = new GenericDrill("nucleardrill"){
		{
			drillTime = 240;
			size = 3;
			powerUse = 0.32f;
			hasPower = true;
		}
	},

	plasmadrill = new GenericDrill("plasmadrill"){
		{
			inputLiquid = Liquid.plasma;
			drillTime = 240;
			size = 4;
			powerUse = 0.16f;
			hasLiquids = true;
			hasPower = true;
		}
	},

	quartzextractor = new GenericDrill("quartzextractor"){
		{
			powerUse = 0.1f;
			resource = Blocks.sand;
			result = Item.silicon;
			drillTime = 320;
			size = 2;
		}
	},

	waterextractor = new SolidPump("waterextractor"){
		{
			result = Liquid.water;
			powerUse = 0.1f;
			pumpAmount = 0.4f;
			size = 2;
			liquidCapacity = 30f;
		}
	},

	oilextractor = new SolidPump("oilextractor"){
		{
			result = Liquid.oil;
			powerUse = 0.5f;
			pumpAmount = 0.4f;
			size = 3;
			liquidCapacity = 80f;
		}
	},

	cultivator = new GenericDrill("cultivator"){
		{
			resource = Blocks.grass;
			result = Item.biomatter;
			inputLiquid = Liquid.water;
			liquidUse = 0.1f;
			drillTime = 300;
			size = 2;
			hasLiquids = true;
			hasPower = true;
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
		{
			generation = 0.003f;
		}
	},
	largesolarpanel = new SolarGenerator("largesolarpanel"){
		{
			size = 3;
			generation = 0.012f;
		}
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
