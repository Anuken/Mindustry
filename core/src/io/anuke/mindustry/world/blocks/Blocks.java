package io.anuke.mindustry.world.blocks;

import io.anuke.mindustry.resource.Item;
import io.anuke.mindustry.resource.ItemStack;
import io.anuke.mindustry.resource.Liquid;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.types.Floor;
import io.anuke.mindustry.world.blocks.types.Wall;
import io.anuke.mindustry.world.blocks.types.defense.ShieldedWallBlock;
import io.anuke.ucore.core.Effects;
import io.anuke.ucore.util.Mathf;

public class Blocks{
	public static final Block
	
	air = new Block("air"){
		//no drawing here
		public void drawCache(Tile tile){}
		
		//update floor blocks for effects, if needed
		public void draw(Tile tile){
			tile.floor().update(tile);
		}
	},
	
	deepwater = new Floor("deepwater"){{
		vary = false;
		solid = true;
		liquidDrop = Liquid.water;
	}},
	
	water = new Floor("water"){{
		vary = false;
		solid = true;
		liquidDrop = Liquid.water;
	}},
	
	lava = new Floor("lava"){
		{
			vary = false;
			solid = true;
			liquidDrop = Liquid.lava;
		}
		
		@Override
		public void update(Tile tile){
			if(Mathf.chance(0.001)){
				Effects.effect("lava", tile.worldx() + Mathf.range(5f), tile.worldy() + Mathf.range(5f));
			}
			
			if(Mathf.chance(0.004)){
				Effects.effect("lavabubble", tile.worldx() + Mathf.range(3f), tile.worldy() + Mathf.range(3f));
			}
		}
	},
	
	stone = new Floor("stone"){{
		drops = new ItemStack(Item.stone, 1);
	}},
	
	iron = new Floor("iron"){{
		drops = new ItemStack(Item.iron, 1);
	}},
	
	coal = new Floor("coal"){{
		drops = new ItemStack(Item.coal, 1);
	}},
	
	titanium = new Floor("titanium"){{
		drops = new ItemStack(Item.titanium, 1);
	}},
	
	dirt = new Floor("dirt"),
	
	grass = new Floor("grass"),
	
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
	
	stonewall = new Wall("stonewall"){{
		health = 50;
		formalName = "stone wall";
	}},
			
	ironwall = new Wall("ironwall"){{
		health = 80;
		formalName = "iron wall";
	}},
	
	steelwall = new Wall("steelwall"){{
		health = 110;
		formalName = "steel wall";
	}},
	
	titaniumwall = new Wall("titaniumwall"){{
		health = 150;
		formalName = "titanium wall";
	}},
	diriumwall = new Wall("duriumwall"){{
		health = 190;
		formalName = "dirium wall";
	}},
	compositewall = new Wall("compositewall"){{
		health = 270;
		formalName = "composite wall";
	}},
	titaniumshieldwall = new ShieldedWallBlock("titaniumshieldwall"){{
		health = 150;
		formalName = "shielded wall";
	}};
}
