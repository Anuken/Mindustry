package io.anuke.mindustry.world.blocks;

import io.anuke.mindustry.core.GameState;
import io.anuke.mindustry.core.GameState.State;
import io.anuke.mindustry.entities.effect.Fx;
import io.anuke.mindustry.resource.Item;
import io.anuke.mindustry.resource.ItemStack;
import io.anuke.mindustry.resource.Liquid;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.types.BlockPart;
import io.anuke.mindustry.world.blocks.types.Floor;
import io.anuke.ucore.core.Effects;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.util.Mathf;

public class Blocks{
	public static final Block
	
	air = new Block("air"){
		//no drawing here
		public void drawCache(Tile tile){}
		
		//update floor blocks for effects, if needed
		public void draw(Tile tile){
			if(!GameState.is(State.paused))
				tile.floor().update(tile);
		}
	},
	
	blockpart = new BlockPart(),
	
	deepwater = new Floor("deepwater"){{
		variants = 0;
		solid = true;
		liquidDrop = Liquid.water;
	}},
	
	water = new Floor("water"){{
		variants = 0;
		solid = true;
		liquidDrop = Liquid.water;
	}},
	
	lava = new Floor("lava"){
		{
			variants = 0;
			solid = true;
			liquidDrop = Liquid.lava;
		}
		
		@Override
		public void update(Tile tile){
			if(Mathf.chance(0.001 * Timers.delta())){
				Effects.effect(Fx.lava, tile.worldx() + Mathf.range(5f), tile.worldy() + Mathf.range(5f));
			}
			
			if(Mathf.chance(0.003 * Timers.delta())){
				Effects.effect(Fx.lavabubble, tile.worldx() + Mathf.range(3f), tile.worldy() + Mathf.range(3f));
			}
		}
	},
	
	oil = new Floor("oil"){
		{
			variants = 0;
			solid = true;
			liquidDrop = Liquid.oil;
		}
		
		@Override
		public void update(Tile tile){
			if(Mathf.chance(0.0025 * Timers.delta())){
				Effects.effect(Fx.oilbubble, tile.worldx() + Mathf.range(2f), tile.worldy() + Mathf.range(2f));
			}
		}
	},
	
	stone = new Floor("stone"){{
		drops = new ItemStack(Item.stone, 1);
	}},
	
	blackstone = new Floor("blackstone"){{
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
	
	uranium = new Floor("uranium"){{
		drops = new ItemStack(Item.uranium, 1);
	}},
	
	dirt = new Floor("dirt"),
	
	grass = new Floor("grass"),
	
	stoneblock = new Block("stoneblock"){
		{
			solid = true;
			variants = 3;
		}
		
		@Override
		public void draw(Tile tile){
		//	Draw.colorl(0.9f + Mathf.randomSeed(tile.id()*9999, 1, 10)/10f / 10f);
			super.draw(tile);
		//	Draw.color();
		}
	},
	
	blackstoneblock = new Block("blackstoneblock"){
		{
			solid = true;
			variants = 3;
		}
		
		@Override
		public void draw(Tile tile){
		//	float f = 0.15f;
		//	Draw.colorl(1f - f + Mathf.randomSeed(tile.id()*9999, 1, 10)/10f / (1f / f));
			super.draw(tile);
		//	Draw.color();
		}
	},
	
	grassblock = new Block("grassblock"){{
		solid = true;
		variants = 2;
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
		variants = 2;
		varyShadow = true;
		drops = new ItemStack(Item.stone, 3);
	}},
	
	blackrock = new Block("blackrock"){{
		shadow = "blackrockshadow";
		breakable = true;
		breaktime = 15;
		variants = 1;
		varyShadow = true;
		drops = new ItemStack(Item.stone, 3);
	}},
	
	dirtblock = new Block("dirtblock"){{
		solid = true;
	}};
}
