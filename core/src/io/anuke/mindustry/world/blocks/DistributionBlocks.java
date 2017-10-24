package io.anuke.mindustry.world.blocks;

import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.blocks.types.LiquidBlock;
import io.anuke.mindustry.world.blocks.types.distribution.*;

public class DistributionBlocks{
	
	public static final Block
	
	conduit = new LiquidBlock("conduit"){{
		
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
	poweredconveyor = new Conveyor("poweredconveyor"){{
		health = 90;
		speed = 0.09f;
		formalName = "pulse conveyor";
	}},
	
	router = new Router("router"){
	},
	
	junction = new Junction("junction"){
		
	},
	liquidjunction = new LiquidJunction("liquidjunction"){
		
	};
}
