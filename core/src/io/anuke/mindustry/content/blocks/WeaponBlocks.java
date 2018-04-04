package io.anuke.mindustry.content.blocks;

import io.anuke.mindustry.content.AmmoTypes;
import io.anuke.mindustry.graphics.fx.BulletFx;
import io.anuke.mindustry.resource.AmmoType;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.blocks.types.defense.LaserTurret;
import io.anuke.mindustry.world.blocks.types.defense.Turret;
import io.anuke.mindustry.world.blocks.types.defense.turrets.*;

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
	
	flameturret = new LiquidTurret("flameturret"){{
		ammoTypes = new AmmoType[]{AmmoTypes.basicFlame};
		recoil = 0f;
		reload = 5f;
		shootCone = 50f;
		shootEffect = BulletFx.shootSmallFlame;
		ammoUseEffect = BulletFx.shellEjectSmall;
	}},
	
	railgunturret = new ItemTurret("railgunturret"){{
		range = 100f;
		ammoTypes = new AmmoType[]{AmmoTypes.basicSteel};
		reload = 100f;
		restitution = 0.03f;
		ammoEjectBack = 2f;
		recoil = 3f;
		shootShake = 2f;
		shootEffect = BulletFx.shootBig;
		smokeEffect = BulletFx.shootBigSmoke;
		ammoUseEffect = BulletFx.shellEjectBig;
	}},
	
	flakturret = new ItemTurret("flakturret"){{
		range = 100f;
		ammoTypes = new AmmoType[]{AmmoTypes.basicSteel};
		reload = 100f;
		restitution = 0.03f;
		ammoEjectBack = 2f;
		recoil = 3f;
		shootShake = 2f;
		shootEffect = BulletFx.shootBig;
		smokeEffect = BulletFx.shootBigSmoke;
		ammoUseEffect = BulletFx.shellEjectBig;
	}},
	
	laserturret = new LaserTurret("laserturret"){

	},
	
	teslaturret = new PowerTurret("teslaturret"){

	},

	magmaturret = new LiquidTurret("magmaturret") {{
		ammoTypes = new AmmoType[]{AmmoTypes.basicFlame};
	}},
		
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
