package io.anuke.mindustry.world.blocks;

import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.blocks.types.distribution.*;
import io.anuke.mindustry.world.blocks.types.storage.SortedUnloader;
import io.anuke.mindustry.world.blocks.types.storage.Unloader;
import io.anuke.mindustry.world.blocks.types.storage.Vault;

public class DistributionBlocks{
	
	public static final Block
	
	conduit = new Conduit("conduit"){{
		health = 45;
	}},
	
	pulseconduit = new Conduit("pulseconduit"){{
		liquidCapacity = 16f;
		liquidFlowFactor = 4.9f;
		health = 65;
	}},
	
	liquidrouter = new LiquidRouter("liquidrouter"){{
		liquidCapacity = 40f;
	}},

	liquidtank = new LiquidRouter("liquidtank"){{
		size = 3;
		liquidCapacity = 1500f;
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

	multiplexer = new Router("multiplexer"){{
		size = 2;
		itemCapacity = 80;
	}},

	vault = new Vault("vault"){{
		size = 3;
	}},

	unloader = new Unloader("unloader"){{

	}},

	sortedunloader = new SortedUnloader("sortedunloader"){{

	}},
	
	junction = new Junction("junction"){{
		
	}},
	tunnel = new TunnelConveyor("conveyortunnel"){{
	}},
	conduittunnel = new TunnelConduit("conduittunnel"){{

	}},
	liquidjunction = new LiquidJunction("liquidjunction"){{

	}},
	powerlaser = new PowerLaser("powerlaser"){{
	}},
	powerlaserrouter = new PowerLaser("powerlaserrouter"){{
		laserDirections = 3;
	}},
	powerlasercorner = new PowerLaser("powerlasercorner"){{
		laserDirections = 2;
	}},
	battery = new PowerLaser("battery"){{
		laserDirections = 1;
		powerCapacity = 320f;
	}},
	batteryLarge = new PowerLaser("batterylarge"){{
		laserDirections = 1;
		size = 3;
		powerCapacity = 2000f;
		base = "batterylarge-base";
	}},
	teleporter = new Teleporter("teleporter"){{
	}},
	sorter = new Sorter("sorter"){{
	}},
	splitter = new Splitter("splitter"){{
	}};
}
