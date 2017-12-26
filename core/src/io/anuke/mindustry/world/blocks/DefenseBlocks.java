package io.anuke.mindustry.world.blocks;

import io.anuke.mindustry.graphics.Fx;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.blocks.types.Wall;
import io.anuke.mindustry.world.blocks.types.defense.*;

public class DefenseBlocks{
	
	public static final Block
	
	stonewall = new Wall("stonewall"){{
		health = 50;
	}},
			
	ironwall = new Wall("ironwall"){{
		health = 80;
	}},
	
	steelwall = new Wall("steelwall"){{
		health = 110;
	}},
	
	titaniumwall = new Wall("titaniumwall"){{
		health = 150;
	}},
	diriumwall = new Wall("duriumwall"){{
		health = 190;
	}},
	compositewall = new Wall("compositewall"){{
		health = 270;
	}},
	steelwalllarge = new Wall("steelwall-large"){{
		health = 110*4;
		width = height = 2;
	}},
	titaniumwalllarge = new Wall("titaniumwall-large"){{
		health = 150*4;
		width = height = 2;
	}},
	diriumwalllarge = new Wall("duriumwall-large"){{
		health = 190*4;
		width = height = 2;
	}},
	titaniumshieldwall = new ShieldedWallBlock("titaniumshieldwall"){{
		health = 150;
	}},
	
	repairturret = new RepairTurret("repairturret"){
		{
			range = 30;
			reload = 60f;
			health = 60;
		}
	},
	
	megarepairturret = new RepairTurret("megarepairturret"){
		{
			range = 44;
			reload = 30f;
			powerUsed = 0.15f;
			health = 90;
		}
	},
	
	shieldgenerator = new ShieldBlock("shieldgenerator"){
		{

		}
	},
	door = new Door("door"){{
		health = 90;
	}},
	largedoor = new Door("door-large"){{
		openfx = Fx.dooropenlarge;
		closefx = Fx.doorcloselarge;
		health = 90*4;
		width = height = 2;
	}};
}
