package io.anuke.mindustry.world.blocks;

import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Tile;

public class Blocks{
	public static final Block
	
	air = new Block("air"){
		//no drawing here
		public void draw(Tile tile){}
	},
	
	stone = new Block("stone"),
	
	dirt = new Block("dirt"),
	
	iron = new Block("iron"),
	
	coal = new Block("coal"),
	
	grass = new Block("grass"),
	
	stoneblock = new Block("stoneblock"){{
		solid = true;
	}},
	
	dirtblock = new Block("dirtblock"){{
		solid = true;
	}},
	
	stonewall = new Block("stonewall"){{
		solid = true;
		update = true;
		health = 50;
	}},
			
	ironwall = new Block("ironwall"){{
		solid = true;
		update = true;
		health = 80;
	}},
	
	steelwall = new Block("steelwall"){{
		solid = true;
		update = true;
		health = 100;
	}};
}
