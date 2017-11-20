package io.anuke.mindustry.world.blocks;

import io.anuke.mindustry.Fx;
import io.anuke.mindustry.Vars;
import io.anuke.mindustry.resource.Item;
import io.anuke.mindustry.resource.Liquid;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.types.production.*;
import io.anuke.ucore.core.Effects;
import io.anuke.ucore.core.Timers;

public class ProductionBlocks{
	public static final Block
	
	core = new Block("core"){
		{
			health = 600;
			solid = true;
			destructible = true;
			width = 3;
			height = 3;
		}
		
		@Override
		public int handleDamage(Tile tile, int amount){
			return Vars.debug ? 0 : amount;
		}
		
		@Override
		public void handleItem(Item item, Tile tile, Tile source){
			Vars.control.addItem(item, 1);
		}
		
		@Override
		public boolean acceptItem(Item item, Tile dest, Tile source){
			return true;
		}
	},
	
	pump = new Pump("pump"){{
		
	}},
	
	fluxpump = new Pump("fluxpump"){{
		pumpspeed = 3f;
	}},
	
	smelter = new Crafter("smelter"){
		{
			health = 70;
			requirements = new Item[]{Item.coal, Item.iron};
			result = Item.steel;
		}
		
		@Override
		public String description(){
			return "Converts coal + iron to steel.";
		}
	},
	
	crucible = new Crafter("crucible"){
		{
			health = 90;
			requirements = new Item[]{Item.titanium, Item.steel};
			result = Item.dirium;
		}
		
		@Override
		public String description(){
			return "Converts steel + titanium to dirium.";
		}
	},
	
	coalpurifier = new LiquidCrafter("coalpurifier"){
		{
			formalName = "coal extractor";
			input = Item.stone;
			inputAmount = 5;
			inputLiquid = Liquid.water;
			liquidAmount = 18.99f;
			output = Item.coal;
			health = 50;
			purifyTime = 60;
		}
		
		@Override
		public String description(){
			return "Converts stone + water to coal.";
		}
	},
	
	titaniumpurifier = new LiquidCrafter("titaniumpurifier"){
		{
			formalName = "titanium extractor";
			input = Item.iron;
			inputAmount = 6;
			inputLiquid = Liquid.water;
			liquidAmount = 40f;
			liquidCapacity = 41f;
			purifyTime = 80;
			output = Item.titanium;
			health = 70;
		}
		
		@Override
		public String description(){
			return "Converts iron + water to titanium.";
		}
	},
	
	oilrefinery = new LiquidCrafter("oilrefinery"){
		{
			formalName = "oil refinery";
			inputLiquid = Liquid.oil;
			liquidAmount = 45f;
			liquidCapacity = 46f;
			purifyTime = 70;
			output = Item.coal;
			health = 80;
			craftEffect = Fx.purifyoil;
		}
		
		@Override
		public String description(){
			return "Converts oil to coal.";
		}
	},
	
	stoneformer = new LiquidCrafter("stoneformer"){
		{
			formalName = "stone former";
			input = Item.iron;
			inputAmount = 2;
			inputLiquid = Liquid.lava;
			liquidAmount = 20f;
			liquidCapacity = 21f;
			purifyTime = 10;
			output = Item.stone;
			health = 80;
			craftEffect = Fx.purifystone;
		}
		
		@Override
		public String description(){
			return "Converts lava to stone.";
		}
	},
	
	lavasmelter = new LiquidCrafter("lavasmelter"){
		{
			formalName = "lava smelter";
			inputLiquid = Liquid.lava;
			liquidAmount = 40f;
			liquidCapacity = 41f;
			purifyTime = 25;
			output = Item.stone;
			health = 80;
			craftEffect = Fx.purifystone;
		}
		
		@Override
		public String description(){
			return "Converts iron + lava to steel.";
		}
	},
	
	stonedrill = new Drill("stonedrill"){{
		resource = Blocks.stone;
		result = Item.stone;
		formalName = "stone drill";
	}},
	
	irondrill = new Drill("irondrill"){{
		resource = Blocks.iron;
		result = Item.iron;
		formalName = "iron drill";
	}},
	
	coaldrill = new Drill("coaldrill"){{
		resource = Blocks.coal;
		result = Item.coal;
		formalName = "coal drill";
	}},
	
	uraniumdrill = new Drill("uraniumdrill"){{
		resource = Blocks.uranium;
		result = Item.uranium;
		formalName = "uranium drill";
		time = 7;
	}},
	
	titaniumdrill = new Drill("titaniumdrill"){{
		resource = Blocks.titanium;
		result = Item.titanium;
		formalName = "titanium drill";
	}},
	
	omnidrill = new Drill("omnidrill"){
		{
			time = 3;
			formalName = "omnidrill";
		}
		
		@Override
		public void update(Tile tile){

			if(tile.floor().drops != null && Timers.get(tile, "drill", 60 * time)){
				offloadNear(tile, tile.floor().drops.item);
				Effects.effect(Fx.sparkbig, tile.worldx(), tile.worldy());
			}

			if(Timers.get(tile, "dump", 30)){
				tryDump(tile);
			}
		}
		
		@Override
		public String description(){
			return "Mines 1 of any resource every "+time+" seconds.";
		}
	},
	coalgenerator = new ItemPowerGenerator("coalgenerator"){
		{
			//TODO
			formalName = "coal generator";
			generateItem = Item.coal;
			generateAmount = 4f;
			powerCapacity = 40f;
		}
		
		@Override
		public String description(){
			return "Generates power from coal.";
		}
	},
	thermalgenerator = new LiquidPowerGenerator("thermalgenerator"){
		{
			formalName = "thermal generator";
			//TODO
			generateLiquid = Liquid.lava;
			inputLiquid = 20f;
			generatePower = 1f;
			powerCapacity = 40f;
		}
		
		@Override
		public String description(){
			return "Generates power from lava.";
		}
	},
	combustiongenerator = new LiquidPowerGenerator("combustiongenerator"){
		{
			formalName = "combustion generator";
			//TODO
			generateLiquid = Liquid.oil;
			inputLiquid = 14f;
			generatePower = 1f;
			powerCapacity = 40f;
		}
		
		@Override
		public String description(){
			return "Generates power from oil.";
		}
	},
	rtgenerator = new ItemPowerGenerator("rtgenerator"){
		{
			//TODO make this generate slowly
			formalName = "radioisotope generator";
			generateItem = Item.uranium;
			generateAmount = 10f;
			powerCapacity = 40f;
			generateTime = 50f;
		}
		
		@Override
		public String description(){
			return "Generates small amounts of power from uranium.";
		}
	},
	nuclearReactor = new LiquidItemPowerGenerator("nuclearreactor"){
		{
			//TODO
			formalName = "nuclear reactor";
			width = 3;
			height = 3;
			generateLiquid = Liquid.water;
			generateItem = Item.uranium;
			itemCapacity = 60;
			itemInput = 6;
			inputLiquid = 2f;
			health = 500;
			breaktime *= 2.2f;
			powerCapacity = 100f;
		}
		
		@Override
		public String description(){
			return "Generates power from uranium + water.";
		}
	};
}
