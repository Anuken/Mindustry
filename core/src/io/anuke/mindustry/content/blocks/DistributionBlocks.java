package io.anuke.mindustry.content.blocks;

import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.blocks.types.distribution.*;

public class DistributionBlocks{
	
	public static final Block
	
	conveyor = new Conveyor("conveyor"){{
		health = 40;
		speed = 0.02f;
	}},
	
	steelconveyor = new Conveyor("steelconveyor"){{
		health = 55;
		speed = 0.04f;
	}},
	
	pulseconveyor = new Conveyor("poweredconveyor"){{
		health = 75;
		speed = 0.09f;
	}},
	
	router = new Router("router"),

	multiplexer = new Router("multiplexer"){{
		size = 2;
		itemCapacity = 80;
	}},
	
	junction = new Junction("junction"){{
		speed = 26;
		capacity = 32;
	}},

	tunnel = new TunnelConveyor("conveyortunnel"){{
		speed = 53;
	}},

	itembridge = new ItemBridge("itembridge"){{
		range = 7;
	}},

	sorter = new Sorter("sorter"),

	splitter = new Splitter("splitter");
}
