package io.anuke.mindustry.world.blocks;

import io.anuke.mindustry.Inventory;
import io.anuke.mindustry.Vars;
import io.anuke.mindustry.resource.Item;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Tile;
import io.anuke.ucore.util.Timers;

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
			if(Timers.get(tile, 2) && tile.entity.totalItems() > 0){
				tryDump(tile, tile.rotation++, null);
				tile.rotation %= 4;
			}
		}

		@Override
		public boolean accept(Item item, Tile dest, Tile source){
			return true;
		}
		
		@Override
		public String description(){
			return "Split input materials into 3 directions.";
		}
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
			Timers.run(15, ()->{
				to.block().handleItem(to, item, tile);
			});
			
		}

		@Override
		public boolean accept(Item item, Tile dest, Tile source){
			int dir = source.relativeTo(dest.x, dest.y);
			dir = (dir+4)%4;
			Tile to = dest.getNearby()[dir];
			return to != null && to.block() != junction && to.block().accept(item, dest, to);
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
