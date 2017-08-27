package io.anuke.mindustry.world.blocks;

import io.anuke.mindustry.resource.Item;
import io.anuke.mindustry.resource.ItemStack;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Tile;

public class Blocks{
	public static final Block
	
	air = new Block("air"){
		//no drawing here
		public void draw(Tile tile){}
	},
	
	deepwater = new Block("deepwater"){{
		vary = false;
		solid = true;
	}},
	
	water = new Block("water"){{
		vary = false;
		solid = true;
	}},
	
	stone = new Block("stone"){{
		drops = new ItemStack(Item.stone, 1);
	}},
	
	iron = new Block("iron"){{
		drops = new ItemStack(Item.iron, 1);
	}},
	
	coal = new Block("coal"){{
		drops = new ItemStack(Item.coal, 1);
	}},
	
	titanium = new Block("titanium"){{
		drops = new ItemStack(Item.titanium, 1);
	}},
	
	dirt = new Block("dirt"),
	
	grass = new Block("grass"),
	
	stoneblock = new Block("stoneblock"){{
		solid = true;
	}},
	
	stoneblock2 = new Block("stoneblock2"){{
		solid = true;
	}},
			
	stoneblock3 = new Block("stoneblock3"){{
		solid = true;
	}},
	
	grassblock = new Block("grassblock"){{
		solid = true;
	}},
	
	grassblock2 = new Block("grassblock2"){{
		solid = true;
	}},
					
	mossblock = new Block("mossblock"){{
		solid = true;
	}},
	
	shrub = new Block("shrub"){{
		shadow = "shrubshadow";
		breakable = true;
		breaktime = 10;
	}},
	
	rock = new Block("rock"){{
		shadow = "rockshadow";
		breakable = true;
		breaktime = 15;
		drops = new ItemStack(Item.stone, 3);
	}},
			
	rock2 = new Block("rock2"){{
		shadow = "rock2shadow";
		breakable = true;
		breaktime = 15;
		drops = new ItemStack(Item.stone, 3);
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
