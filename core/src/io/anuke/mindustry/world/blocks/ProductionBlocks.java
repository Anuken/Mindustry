package io.anuke.mindustry.world.blocks;

import io.anuke.mindustry.entities.TileEntity;
import io.anuke.mindustry.graphics.Fx;
import io.anuke.mindustry.resource.Item;
import io.anuke.mindustry.resource.Liquid;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.types.defense.CoreBlock;
import io.anuke.mindustry.world.blocks.types.production.*;
import io.anuke.ucore.core.Effects;

public class ProductionBlocks{
	public static final Block
	
	core = new CoreBlock("core"){},
	
	pump = new Pump("pump"){},
	
	fluxpump = new Pump("fluxpump"){
		{
			pumpAmount = 3f;
		}
	},
	
	smelter = new Crafter("smelter"){
		{
			health = 70;
			requirements = new Item[]{Item.coal, Item.iron};
			result = Item.steel;
		}
	},
	
	crucible = new Crafter("crucible"){
		{
			health = 90;
			requirements = new Item[]{Item.titanium, Item.steel};
			result = Item.dirium;
		}
	},
	
	coalpurifier = new LiquidCrafter("coalpurifier"){
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
	
	titaniumpurifier = new LiquidCrafter("titaniumpurifier"){
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
		}
	},
	
	coaldrill = new Drill("coaldrill"){
		{
			resource = Blocks.coal;
			result = Item.coal;
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
		}
	},
	
	omnidrill = new Drill("omnidrill"){
		{
			drillEffect = Fx.sparkbig;
			resource = null;
			result = null;
			time = 3;
		}
		
		@Override
		public void update(Tile tile){
			TileEntity entity = tile.entity;

			if(tile.floor().drops != null && entity.timer.get(timerDrill, 60 * time)){
				offloadNear(tile, tile.floor().drops.item);
				Effects.effect(drillEffect, tile.worldx(), tile.worldy());
			}

			if(entity.timer.get(timerDump, 30)){
				tryDump(tile);
			}
		}
		
                @Override
                public boolean isLayer(Tile tile){
                    return tile.floor().drops == null;
                }
	},
	coalgenerator = new ItemPowerGenerator("coalgenerator"){
		{
			generateItem = Item.coal;
			powerOutput = 0.05f;
			powerCapacity = 40f;
		}
	},
	thermalgenerator = new LiquidPowerGenerator("thermalgenerator"){
		{
			generateLiquid = Liquid.lava;
			maxLiquidGenerate = 0.5f;
			powerPerLiquid = 0.09f;
			powerCapacity = 40f;
			generateEffect = Fx.redgeneratespark;
		}
	},
	combustiongenerator = new LiquidPowerGenerator("combustiongenerator"){
		{
			generateLiquid = Liquid.oil;
			maxLiquidGenerate = 0.4f;
			powerPerLiquid = 0.13f;
			powerCapacity = 40f;
		}
	},
	rtgenerator = new ItemPowerGenerator("rtgenerator"){
		{
			generateItem = Item.uranium;
			powerCapacity = 40f;
			powerOutput = 0.04f;
			itemDuration = 240f;
		}
	},
	nuclearReactor = new NuclearReactor("nuclearreactor"){
		{
			width = 3;
			height = 3;
			health = 600;
			breaktime *= 2.3f;
		}
	};
	/*
	siliconextractor = new LiquidCrafter("siliconextractor"){
		{
			input = Item.sand;
			inputAmount = 5;
			inputLiquid = Liquid.water;
			liquidAmount = 18.99f;
			output = Item.sand;
			health = 50;
			purifyTime = 50;
		}
	}*/;
}
