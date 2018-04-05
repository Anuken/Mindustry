package io.anuke.mindustry.content.blocks;

import com.badlogic.gdx.graphics.Color;
import io.anuke.mindustry.content.AmmoTypes;
import io.anuke.mindustry.content.fx.BulletFx;
import io.anuke.mindustry.resource.AmmoType;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.blocks.types.defense.Turret;
import io.anuke.mindustry.world.blocks.types.defense.turrets.*;
import io.anuke.ucore.graphics.Draw;
import io.anuke.ucore.util.Angles;
import io.anuke.ucore.util.Mathf;
import io.anuke.ucore.util.Strings;

public class WeaponBlocks{
	public static Block
	
	doubleturret = new DoubleTurret("doubleturret"){{
		ammoTypes = new AmmoType[]{AmmoTypes.basicIron};
		reload = 25f;
		restitution = 0.03f;
		shootEffect = BulletFx.shootSmall;
		smokeEffect = BulletFx.shootSmallSmoke;
		ammoUseEffect = BulletFx.shellEjectSmall;
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

		drawer = (tile, entity) -> {
			Draw.rect(entity.target != null ? name + "-shoot" : name, tile.drawx() + tr2.x, tile.drawy() + tr2.y, entity.rotation - 90);
		};
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
		ammoUseEffect = BulletFx.shellEjectMedium;
	}},
	
	flakturret = new ItemTurret("flakturret"){{
		size = 2;
		range = 100f;
		ammoTypes = new AmmoType[]{AmmoTypes.basicLeadFrag};
		reload = 70f;
		restitution = 0.03f;
		ammoEjectBack = 3f;
		cooldown = 0.03f;
		recoil = 3f;
		shootShake = 2f;
		shootEffect = BulletFx.shootBig2;
		smokeEffect = BulletFx.shootBigSmoke2;
		ammoUseEffect = BulletFx.shellEjectBig;

		drawer = (tile, entity) -> {
			Draw.rect(name, tile.drawx() + tr2.x, tile.drawy() + tr2.y, entity.rotation - 90);
			float offsetx = (int)(Mathf.abscurve(Mathf.curve(entity.reload/reload, 0.3f, 0.2f)) * 3f);
			float offsety = -(int)(Mathf.abscurve(Mathf.curve(entity.reload/reload, 0.3f, 0.2f)) * 2f);

			for(int i : Mathf.signs){
				float rot = entity.rotation + 90*i;
				Draw.rect(name + "-panel-" + Strings.dir(i),
						tile.drawx() + tr2.x + Angles.trnsx(rot, offsetx, offsety),
						tile.drawy() + tr2.y + Angles.trnsy(rot, -offsetx, offsety), entity.rotation - 90);
			}
		};
	}},
	
	laserturret = new LaserTurret("laserturret"){{
		range = 70f;
		chargeTime = 70f;
		chargeMaxDelay = 30f;
		chargeEffects = 7;
		shootType = AmmoTypes.lancerLaser;
		recoil = 2f;
		reload = 130f;
		cooldown = 0.03f;
		shootEffect = BulletFx.lancerLaserShoot;
		smokeEffect = BulletFx.lancerLaserShootSmoke;
		chargeEffect = BulletFx.lancerLaserCharge;
		chargeBeginEffect = BulletFx.lancerLaserChargeBegin;
		heatColor = Color.RED;
	}},
	
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
