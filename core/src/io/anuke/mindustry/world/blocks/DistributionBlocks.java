package io.anuke.mindustry.world.blocks;

import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.blocks.types.LiquidBlock;
import io.anuke.mindustry.world.blocks.types.distribution.*;

public class DistributionBlocks{
	
	public static final Block
	
	conduit = new LiquidBlock("conduit"){{
		health = 45;
	}},
	
	pulseconduit = new LiquidBlock("pulseconduit"){{
		liquidCapacity = 16f;
		flowfactor = 4.9f;
		health = 65;
	}},
	
	liquidrouter = new LiquidRouter("liquidrouter"){{
		formalName = "liquid router";
	}},
	
	conveyor = new Conveyor("conveyor"){{
		
	}},
	
	steelconveyor = new Conveyor("steelconveyor"){{
		health = 55;
		speed = 0.04f;
		formalName = "steel conveyor";
	}},
	
	//TODO
	pulseconveyor = new Conveyor("poweredconveyor"){{
		health = 90;
		speed = 0.09f;
		formalName = "pulse conveyor";
	}},
	
	router = new Router("router"){
	},
	
	junction = new Junction("junction"){
		
	},
	liquidjunction = new LiquidJunction("liquidjunction"){
		
	},
	liquiditemjunction = new LiquidItemJunction("liquiditemjunction"){
		{
			formalName = "liquid-item junction";
		}
	},
	powerbooster = new PowerBooster("powerbooster"){
		{
			formalName = "power booster";
		}
	},
	powerlaser = new PowerLaser("powerlaser"){
		{
			formalName = "power laser";
		}
	},
	powerlaserrouter = new PowerLaserRouter("powerlaserrouter"){
		{
			formalName = "laser router";
		}
	},
	teleporter = new Teleporter("teleporter"){
		{
			
		}
	},
	sorter = new Sorter("sorter"){
		{
			
		}
	};
}
