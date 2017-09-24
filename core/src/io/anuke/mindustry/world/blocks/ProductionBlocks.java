package io.anuke.mindustry.world.blocks;

import io.anuke.mindustry.Inventory;
import io.anuke.mindustry.Vars;
import io.anuke.mindustry.resource.Item;
import io.anuke.mindustry.resource.Liquid;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.types.*;
import io.anuke.ucore.core.Effects;
import io.anuke.ucore.core.Timers;

public class ProductionBlocks{
	public static final Block
	
	core = new Block("core"){
		{
			health = Vars.debug ? 999999999 : 300;
			solid = true;
			update = true;
		}
		
		@Override
		public void handleItem(Tile tile, Item item, Tile source){
			Inventory.addItem(item, 1);
		}
		
		@Override
		public boolean accept(Item item, Tile dest, Tile source){
			return true;
		}
	},
	
	conduit = new Conduit("conduit"){{
		
	}},
	
	pump = new Pump("pump"){{
		
	}},
	
	liquidrouter = new LiquidRouter("liquidrouter"){{
				
	}},
	
	conveyor = new Conveyor("conveyor"){{
		
	}},
	
	steelconveyor = new Conveyor("steelconveyor"){{
		health = 55;
		speed = 0.04f;
		formalName = "steel conveyor";
	}},
	
	router = new Router("router"){
	},
	
	junction = new Block("junction"){
		{
			update = true;
			solid = true;
		}
		
		@Override
		public void handleItem(Tile tile, Item item, Tile source){
			int dir = source.relativeTo(tile.x, tile.y);
			dir = (dir+4)%4;
			Tile to = tile.getNearby()[dir];
			Timers.run(10, ()->{
				to.block().handleItem(to, item, tile);
			});
			
		}

		@Override
		public boolean accept(Item item, Tile dest, Tile source){
			int dir = source.relativeTo(dest.x, dest.y);
			dir = (dir+4)%4;
			Tile to = dest.getNearby()[dir];
			return to != null && to.block() != junction && to.block().accept(item, to, dest);
		}
		
		@Override
		public String description(){
			return "Serves as a conveyor junction.";
		}
	},
	
	smelter = new Crafter("smelter"){
		{
			health = 70;
			requirements = new Item[]{Item.coal, Item.iron};
			result = Item.steel;
		}
		
		@Override
		public String description(){
			return "Takes in coal + iron, outputs steel.";
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
			return "Takes in steel + titanium, outputs dirium.";
		}
	},
	
	coalpurifier = new Purifier("coalpurifier"){
		{
			formalName = "coal extractor";
			input = Item.stone;
			inputLiquid = Liquid.water;
			output = Item.coal;
			health = 50;
		}
		
		@Override
		public String description(){
			return "Takes in stone + water, outputs coal.";
		}
	},
	
	titaniumpurifier = new Purifier("titaniumpurifier"){
		{
			formalName = "titanium\nextractor";
			input = Item.iron;
			inputAmount = 11;
			inputLiquid = Liquid.water;
			liquidAmount = 40f;
			liquidCapacity = 41f;
			purifyTime = 90;
			output = Item.titanium;
			health = 70;
		}
		
		@Override
		public String description(){
			return "Takes in iron + water, outputs coal.";
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

			if(tile.floor().drops != null && Timers.get(tile, 60 * time)){
				offloadNear(tile, tile.floor().drops.item);
				Effects.effect("sparkbig", tile.worldx(), tile.worldy());
			}

			if(Timers.get(tile.hashCode() + "dump", 30)){
				tryDump(tile);
			}
		}
		
		@Override
		public String description(){
			return "Mines 1 of any resource every "+time+" seconds.";
		}
	}
	
	;
}
