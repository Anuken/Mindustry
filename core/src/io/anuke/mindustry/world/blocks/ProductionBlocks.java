package io.anuke.mindustry.world.blocks;

import io.anuke.mindustry.Inventory;
import io.anuke.mindustry.resource.Item;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Tile;
import io.anuke.ucore.util.Timers;

public class ProductionBlocks{
	public static final Block
	
	core = new Block("core"){
		{
			health = 300;
			solid = true;
			update = true;
		}
		
		@Override
		protected void handleItem(Tile tile, Item item, Tile source){
			Inventory.addItem(item, 1);
		}
		
		@Override
		public boolean accept(Item item){
			return true;
		}
	},
	
	conveyor = new Conveyor("conveyor"){{
		update = true;
	}},
	
	steelconveyor = new Conveyor("steelconveyor"){{
		update = true;
		speed = 0.04f;
	}},
	
	router = new Block("router"){
		{
			update = true;
			solid = true;
		}
		
		@Override
		public void update(Tile tile){
			if(Timers.get(tile, 10) && tile.entity.totalItems() > 0){
				tryDump(tile, tile.rotation++, null);
				tile.rotation %= 4;
			}
		}

		@Override
		public boolean accept(Item item){
			return true;
		}
		
		@Override
		public String description(){
			return "Split input materials into 3 directions.";
		}
	},
	
	smelter = new Crafter("smelter"){{
		health = 70;
		requirements = new Item[]{Item.coal, Item.iron};
		result = Item.steel;
	}},
	
	stonedrill = new Drill("stonedrill"){{
		resource = Blocks.stone;
		result = Item.stone;
	}},
	
	irondrill = new Drill("irondrill"){{
		resource = Blocks.iron;
		result = Item.iron;
	}},
	
	coaldrill = new Drill("coaldrill"){{
		resource = Blocks.coal;
		result = Item.coal;
	}},
	
	end = null;
}
