package io.anuke.mindustry.world.blocks;

import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.blocks.types.LiquidBlock;
import io.anuke.mindustry.world.blocks.types.distribution.*;

public class DistributionBlocks{
	
	public static final Block
	
	conduit = new LiquidBlock("conduit"){{
		fullDescription = "Basic liquid transport block. Works like a conveyor, but with liquids. "
				+ "Best used with pumps or other conduits. Can be used as a bridge over liquids for enemies and players.";
		health = 45;
	}},
	
	pulseconduit = new LiquidBlock("pulseconduit"){{
		formalName = "pulse conduit";
		fullDescription = "Advanced liquid transport block. Transports liquids faster and stores more than standard conduits.";
		liquidCapacity = 16f;
		flowfactor = 4.9f;
		health = 65;
	}},
	
	liquidrouter = new LiquidRouter("liquidrouter"){{
		description = "Splits input liquid into 3 directions.";
		fullDescription = "Works similarly to a router. Accepts liquid input from one side and outputs it to the other sides. "
				+ "Useful for splitting liquid from a single conduit into multiple other conduits.";
		formalName = "liquid router";
	}},
	
	conveyor = new Conveyor("conveyor"){{
		fullDescription = "Basic item transport block. Moves items forward and automatically deposits them into turrets or crafters. "
				+ "Rotatable. Can be used as a bridge over liquids for enemies and players.";
	}},
	
	steelconveyor = new Conveyor("steelconveyor"){{
		health = 55;
		speed = 0.04f;
		formalName = "steel conveyor";
		fullDescription = "Advanced item transport block. Moves items faster than standard conveyors.";
	}},
	
	pulseconveyor = new Conveyor("poweredconveyor"){{
		health = 75;
		speed = 0.09f;
		formalName = "pulse conveyor";
		fullDescription = "The ultimate item transport block. Moves items faster than steel conveyors.";
	}},
	
	router = new Router("router"){{
		description = "Split input materials into 3 directions.";
		fullDescription = "Accepts items from one direction and outputs them to 3 other directions. Can also store a certain amount of items."
				+ "Useful for splitting the materials from one drill into multiple turrets.";
	}},
	
	junction = new Junction("junction"){{
		description = "Serves as a conveyor junction.";
		fullDescription = "Acts as a bridge for two crossing conveyor belts. Useful in situations with "
				+ "two different conveyors carrying different materials to different locations.";
		
	}},
	tunnel = new TunnelConveyor("conveyortunnel"){{
		formalName = "conveyor tunnel";
		description = "Transports items under blocks.";
		fullDescription = "Transports item under blocks. "
				+ "To use, place one tunnel leading into the block to be tunneled under, and one on the other side.";	
	}},
	liquidjunction = new LiquidJunction("liquidjunction"){{
		formalName = "liquid junction";
		fullDescription = "Acts as a bridge for two crossing conduits. Useful in situations with "
				+ "two different conduits carrying different liquids to different locations.";
	}},
	liquiditemjunction = new LiquidItemJunction("liquiditemjunction"){{
		formalName = "liquid-item junction";
		description = "Serves as a junction for items and liquids.";
		fullDescription = "Acts as a bridge for crossing conduits and conveyors.";
	}},
	powerbooster = new PowerBooster("powerbooster"){{
		formalName = "power booster";
		powerRange = 4;
		description = "Distributes power within a radius.";
		fullDescription = "Distributes power to all blocks within its radius. ";
	}},
	powerlaser = new PowerLaser("powerlaser"){{
		formalName = "power laser";
		description = "Transmits power.";
		fullDescription = "Creates a laser that transmits power to the block in front of it. Does not generate any power itself. "
				+ "Best used with generators or other lasers.";
	}},
	powerlaserrouter = new PowerLaserRouter("powerlaserrouter"){{
		formalName = "laser router";
		description = "Splits input power into 3 lasers.";
		fullDescription = "Laser that distributes power to three directions at once. "
				+ "Useful in situations where it is required to power multiple blocks from one generator.";
	}},
	powerlasercorner = new PowerLaserRouter("powerlasercorner"){{
		laserDirections = 2;
		formalName = "laser corner";
		description = "Splits input power into 2 lasers.";
		fullDescription = "Laser that distributes power to two directions at once. "
				+ "Useful in situations where it is required to power multiple blocks from one generator, and a router is imprecise.";
	}},
	teleporter = new Teleporter("teleporter"){{
		description = "[interact]Tap block to config[]";
		fullDescription = "Advanced item transport block. Teleporters input items to other teleporters of the same color."
				+ " Does nothing if no teleporters of the same color exist. If multiple teleporters exist of the same color, a random one is selected."
				+ " Tap and click the arrows to change color.";
	}},
	sorter = new Sorter("sorter"){{
		description = "[interact]Tap block to config[]";
		fullDescription = "Sorts item by material type. Material to accept is indicated by the color in the block. "
				+ "All items that match the sort material are outputted forward, everything else is outputted to the left and right.";
	}};
}
