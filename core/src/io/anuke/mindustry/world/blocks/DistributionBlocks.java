package io.anuke.mindustry.world.blocks;

import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.blocks.types.distribution.*;

public class DistributionBlocks{
	
	public static final Block
	
	conduit = new Conduit("conduit"){{
		health = 45;
	}},
	
	pulseconduit = new Conduit("pulseconduit"){{
		liquidCapacity = 16f;
		flowfactor = 4.9f;
		health = 65;
	}},
	
	liquidrouter = new LiquidRouter("liquidrouter"){{

	}},
	
	conveyor = new Conveyor("conveyor"){{
	}},
	
	steelconveyor = new Conveyor("steelconveyor"){{
		health = 55;
		speed = 0.04f;
	}},
	
	pulseconveyor = new Conveyor("poweredconveyor"){{
		health = 75;
		speed = 0.09f;
	}},
	
	router = new Router("router"){{

	}},
	
	junction = new Junction("junction"){{
		
	}},
	tunnel = new TunnelConveyor("conveyortunnel"){{
	}},
	liquidjunction = new LiquidJunction("liquidjunction"){{

	}},
	powerbooster = new PowerBooster("powerbooster"){{
		powerRange = 4;
	}},
	powerlaser = new PowerLaser("powerlaser"){{
	}},
	powerlaserrouter = new PowerLaserRouter("powerlaserrouter"){{
	}},
	powerlasercorner = new PowerLaserRouter("powerlasercorner"){{
		laserDirections = 2;
	}},
	teleporter = new Teleporter("teleporter"){{
	}},
	sorter = new Sorter("sorter"){{
	}};
}
