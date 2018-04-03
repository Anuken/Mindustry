package io.anuke.mindustry.content.blocks;

import io.anuke.mindustry.content.AmmoTypes;
import io.anuke.mindustry.graphics.fx.BulletFx;
import io.anuke.mindustry.resource.AmmoType;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.blocks.types.defense.LaserTurret;
import io.anuke.mindustry.world.blocks.types.defense.Turret;
import io.anuke.mindustry.world.blocks.types.defense.turrets.DoubleTurret;
import io.anuke.mindustry.world.blocks.types.defense.turrets.LiquidTurret;
import io.anuke.mindustry.world.blocks.types.defense.turrets.PowerTurret;

public class WeaponBlocks{
	public static Block
	
	doubleturret = new DoubleTurret("doubleturret"){{
		ammoTypes = new AmmoType[]{AmmoTypes.basicIron};
		reload = 40f;
		shootEffect = BulletFx.shootSmall;
	}},
	
	gatlingturret = new Turret("gatlingturret"){

	},
	
	flameturret = new Turret("flameturret"){

	},
	
	railgunturret = new Turret("railgunturret"){

	},
	
	flakturret = new Turret("flakturret"){

	},
	
	laserturret = new LaserTurret("laserturret"){

	},
	
	teslaturret = new PowerTurret("teslaturret"){

	},

	magmaturret = new LiquidTurret("magmaturret") {

	},
		
	plasmaturret = new Turret("plasmaturret"){

	},
	
	chainturret = new Turret("chainturret"){

	},
	
	titanturret = new Turret("titancannon"){

	},

	fornaxcannon = new PowerTurret("fornaxcannon") {

	},
	missileturret = new PowerTurret("missileturret") {

	};
}
