package io.anuke.mindustry.world.blocks;

import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.blocks.types.Wall;
import io.anuke.mindustry.world.blocks.types.defense.RepairTurret;
import io.anuke.mindustry.world.blocks.types.defense.ShieldBlock;
import io.anuke.mindustry.world.blocks.types.defense.ShieldedWallBlock;

public class DefenseBlocks{
	
	public static final Block
	
	stonewall = new Wall("stonewall"){{
		health = 50;
		formalName = "stone wall";
		fullDescription = "A cheap defensive block. Useful for protecting the core and turrets in the first few waves.";
	}},
			
	ironwall = new Wall("ironwall"){{
		health = 80;
		formalName = "iron wall";
		fullDescription = "A basic defensive block. Provides protection from enemies.";
	}},
	
	steelwall = new Wall("steelwall"){{
		health = 110;
		formalName = "steel wall";
		fullDescription = "A standard defensive block. adequate protection from enemies.";
	}},
	
	titaniumwall = new Wall("titaniumwall"){{
		health = 150;
		formalName = "titanium wall";
		fullDescription = "A strong defensive block. Provides protection from enemies.";
	}},
	diriumwall = new Wall("duriumwall"){{
		health = 190;
		formalName = "dirium wall";
		fullDescription = "A very strong defensive block. Provides protection from enemies.";
	}},
	compositewall = new Wall("compositewall"){{
		health = 270;
		formalName = "composite wall";
	}},
	steelwalllarge = new Wall("steelwall-large"){{
		health = 110*4;
		formalName = "large steel wall";
		width = height = 2;
		fullDescription = "A standard defensive block. Spans multiple tiles.";
	}},
	titaniumwalllarge = new Wall("titaniumwall-large"){{
		health = 150*4;
		formalName = "large titanium wall";
		width = height = 2;
		fullDescription = "A strong defensive block. Spans multiple tiles.";
	}},
	diriumwalllarge = new Wall("duriumwall-large"){{
		health = 190*4;
		formalName = "large dirium wall";
		width = height = 2;
		fullDescription = "A very strong defensive block. Spans multiple tiles.";
	}},
	titaniumshieldwall = new ShieldedWallBlock("titaniumshieldwall"){{
		fullDescription = "A strong defensive block, with an extra built-in shield. Requires power. "
				+ "Uses energy to absorb enemy bullets. It is recommended to use power boosters to provide energy to this block.";
		health = 150;
		formalName = "shielded wall";
	}},
	
	repairturret = new RepairTurret("repairturret"){
		{
			fullDescription = "Repairs nearby damaged blocks in range at a slow rate. "
					+ "Uses small amounts of power.";
			formalName = "repair turret";
			range = 30;
			reload = 40f;
			health = 60;
		}
	},
	
	megarepairturret = new RepairTurret("megarepairturret"){
		{
			fullDescription = "Repairs nearby damaged blocks in range at a decent rate. "
					+ "Uses power.";
			formalName = "repair turret II";
			range = 44;
			reload = 20f;
			health = 90;
		}
	},
	
	shieldgenerator = new ShieldBlock("shieldgenerator"){
		{
			//TODO
			fullDescription = "An advanced defensive block. Shields all the blocks in a radius from attack. Uses power at a slow rate when idle, "
					+ "but drains energy quickly on bullet contact.";
			formalName = "shield generator";
		}
	};
}
