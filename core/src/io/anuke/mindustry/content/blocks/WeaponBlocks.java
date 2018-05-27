package io.anuke.mindustry.content.blocks;

import com.badlogic.gdx.graphics.Color;
import io.anuke.mindustry.content.AmmoTypes;
import io.anuke.mindustry.content.fx.ShootFx;
import io.anuke.mindustry.type.AmmoType;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.blocks.types.defense.turrets.*;
import io.anuke.ucore.graphics.Draw;
import io.anuke.ucore.util.Angles;
import io.anuke.ucore.util.Mathf;
import io.anuke.ucore.util.Strings;

public class WeaponBlocks{
	public static Block
	
	duo = new DoubleTurret("duo"){{
		ammoTypes = new AmmoType[]{AmmoTypes.basicIron, AmmoTypes.basicLead, AmmoTypes.standardLead, AmmoTypes.standardTracer};
		reload = 25f;
		restitution = 0.03f;
		ammoUseEffect = ShootFx.shellEjectSmall;
	}},
	
	scatter = new BurstTurret("scatter") {{
		ammoTypes = new AmmoType[]{AmmoTypes.basicLeadFlak, AmmoTypes.standardLeadFlak, AmmoTypes.compositeFlak};
		ammoPerShot = 1;
		shots = 3;
		reload = 60f;
		restitution = 0.03f;
		recoil = 1.5f;
		burstSpacing = 6f;
		ammoUseEffect = ShootFx.shellEjectSmall;
	}},
	
	scorch = new LiquidTurret("scorch"){{
		ammoTypes = new AmmoType[]{AmmoTypes.basicFlame};
		recoil = 0f;
		reload = 5f;
		shootCone = 50f;
		ammoUseEffect = ShootFx.shellEjectSmall;

		drawer = (tile, entity) -> {
			Draw.rect(entity.target != null ? name + "-shoot" : name, tile.drawx() + tr2.x, tile.drawy() + tr2.y, entity.rotation - 90);
		};
	}},

	wave = new LiquidTurret("wave") {{
		ammoTypes = new AmmoType[]{AmmoTypes.water, AmmoTypes.lava, AmmoTypes.cryofluid, AmmoTypes.oil};
		size = 2;
		recoil = 0f;
		reload = 4f;
		inaccuracy = 5f;
		shootCone = 50f;
		shootEffect = ShootFx.shootLiquid;
		range = 70f;

		drawer = (tile, entity) -> {
			Draw.rect(name, tile.drawx() + tr2.x, tile.drawy() + tr2.y, entity.rotation - 90);

			Draw.color(entity.liquids.liquid.color);
			Draw.alpha(entity.liquids.amount/liquidCapacity);
			Draw.rect(name + "-liquid", tile.drawx() + tr2.x, tile.drawy() + tr2.y, entity.rotation - 90);
			Draw.color();
		};
	}},
	
	crux = new ItemTurret("crux"){{
		size = 2;
		range = 100f;
		ammoTypes = new AmmoType[]{AmmoTypes.basicLeadShell, AmmoTypes.explosiveFragShell, AmmoTypes.fragShell, AmmoTypes.standardThoriumShell};
		reload = 70f;
		restitution = 0.03f;
		ammoEjectBack = 3f;
		cooldown = 0.03f;
		recoil = 3f;
		shootShake = 2f;
		ammoUseEffect = ShootFx.shellEjectBig;

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
	
	lancer = new LaserTurret("lancer"){{
		range = 70f;
		chargeTime = 70f;
		chargeMaxDelay = 30f;
		chargeEffects = 7;
		shootType = AmmoTypes.lancerLaser;
		recoil = 2f;
		reload = 130f;
		cooldown = 0.03f;
		shootEffect = ShootFx.lancerLaserShoot;
		smokeEffect = ShootFx.lancerLaserShootSmoke;
		chargeEffect = ShootFx.lancerLaserCharge;
		chargeBeginEffect = ShootFx.lancerLaserChargeBegin;
		heatColor = Color.RED;
		size = 2;
	}},
	
	arc = new LaserTurret("arc"){{
		shootType = AmmoTypes.lightning;
		reload = 100f;
		chargeTime = 70f;
		shootShake = 1f;
		chargeMaxDelay = 30f;
		chargeEffects = 7;
		shootEffect = ShootFx.lightningShoot;
		chargeEffect = ShootFx.lightningCharge;
		chargeBeginEffect = ShootFx.lancerLaserChargeBegin;
		heatColor = Color.RED;
		recoil = 3f;
		size = 2;
	}},

	swarmer = new ItemTurret("missileturret") {{
		ammoTypes = new AmmoType[]{AmmoTypes.basicLeadShell, AmmoTypes.explosiveFragShell, AmmoTypes.fragShell, AmmoTypes.standardThoriumShell};
		size = 2;
	}},

	fuse = new ItemTurret("fuse") {{
		ammoTypes = new AmmoType[]{AmmoTypes.basicLeadShell, AmmoTypes.explosiveFragShell, AmmoTypes.fragShell, AmmoTypes.standardThoriumShell};
		size = 3;
	}},

	spectre = new PowerTurret("spectre") {{
		size = 3;
	}},

	meltdown = new PowerTurret("meltdown") {{
		size = 3;
	}};
}
