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
	titaniumwalllarge = new Wall("titaniumwall-large"){{
		health = 150*4;
		formalName = "large titanium wall";
		width = height = 2;
	}},
	diriumwalllarge = new Wall("duriumwall-large"){{
		health = 190*4;
		formalName = "large dirium wall";
		width = height = 2;
	}},
	titaniumshieldwall = new ShieldedWallBlock("titaniumshieldwall"){{
		health = 150;
		formalName = "shielded wall";
	}},
	
	repairturret = new RepairTurret("repairturret"){
		{
			formalName = "heal turret";
			range = 30;
			reload = 40f;
			health = 60;
		}
	},
	
	megarepairturret = new RepairTurret("megarepairturret"){
		{
			formalName = "heal turret II";
			range = 44;
			reload = 20f;
			health = 90;
		}
	},
	
	shieldgenerator = new ShieldBlock("shieldgenerator"){
		{
			//TODO
			formalName = "shield generator";
		}
	};
}
