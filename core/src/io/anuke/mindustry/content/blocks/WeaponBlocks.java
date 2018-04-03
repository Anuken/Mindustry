package io.anuke.mindustry.content.blocks;

import io.anuke.mindustry.content.AmmoTypes;
import io.anuke.mindustry.graphics.fx.BulletFx;
import io.anuke.mindustry.resource.AmmoType;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.blocks.types.defense.LaserTurret;
import io.anuke.mindustry.world.blocks.types.defense.Turret;
import io.anuke.mindustry.world.blocks.types.defense.turrets.BurstTurret;
import io.anuke.mindustry.world.blocks.types.defense.turrets.DoubleTurret;
import io.anuke.mindustry.world.blocks.types.defense.turrets.LiquidTurret;
import io.anuke.mindustry.world.blocks.types.defense.turrets.PowerTurret;

public class WeaponBlocks{
	public static Block
	
	doubleturret = new DoubleTurret("doubleturret"){{
		ammoTypes = new AmmoType[]{AmmoTypes.basicIron};
		reload = 25f;
		restitution = 0.03f;
		shootEffect = BulletFx.shootSmall;
		smokeEffect = BulletFx.shootSmallSmoke;
	}},
	
	gatlingturret = new BurstTurret("gatlingturret") {{
		ammoTypes = new AmmoType[]{AmmoTypes.basicIron};
		ammoPerShot = 1;
		shots = 3;
		reload = 60f;
		restitution = 0.03f;
		recoil = 1.5f;
		burstSpacing = 6f;
		shootEffect = BulletFx.shootSmall;
		smokeEffect = BulletFx.shootSmallSmoke;
		ammoUseEffect = BulletFx.shellEjectSmall;
	}},
	
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
