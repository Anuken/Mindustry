package io.anuke.mindustry.world.blocks;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.ObjectMap;

import io.anuke.mindustry.Inventory;
import io.anuke.mindustry.Renderer;
import io.anuke.mindustry.Vars;
import io.anuke.mindustry.resource.Item;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Tile;
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
	
	conveyor = new Conveyor("conveyor"){{
		update = true;
	}},
	
	steelconveyor = new Conveyor("steelconveyor"){{
		update = true;
		speed = 0.04f;
	}},
	
	router = new Block("router"){
		private ObjectMap<Tile, Byte> lastmap = new ObjectMap<>();
		int maxitems = 20;
		{
			update = true;
			solid = true;
		}
		
		@Override
		public void update(Tile tile){
			if(Timers.get(tile, 2) && tile.entity.totalItems() > 0){
				if(lastmap.get(tile, (byte)-1) != tile.rotation)
					tryDump(tile, tile.rotation, null);
				
				tile.rotation ++;
				tile.rotation %= 4;
			}
		}
		
		@Override
		public void handleItem(Tile tile, Item item, Tile source){
			super.handleItem(tile, item, source);
			lastmap.put(tile, (byte)tile.relativeTo(source.x, source.y));
		}

		@Override
		public boolean accept(Item item, Tile dest, Tile source){
			int items = dest.entity.totalItems();
			return items < maxitems;
		}
		
		@Override
		public void drawPixelOverlay(Tile tile){
			
			float fract = (float)tile.entity.totalItems()/maxitems;
			
			Renderer.drawBar(Color.GREEN, tile.worldx(), tile.worldy() + 13, fract);
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
	
	titaniumdrill = new Drill("titaniumdrill"){{
		resource = Blocks.titanium;
		result = Item.titanium;
	}},
	
	omnidrill = new Drill("omnidrill"){
		{
			time = 4;
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
