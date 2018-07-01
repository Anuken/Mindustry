package io.anuke.mindustry.content.blocks;

import com.badlogic.gdx.graphics.Color;
import io.anuke.mindustry.content.AmmoTypes;
import io.anuke.mindustry.content.fx.ShootFx;
import io.anuke.mindustry.type.AmmoType;
import io.anuke.mindustry.type.ContentList;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.blocks.defense.turrets.*;
import io.anuke.ucore.graphics.Draw;
import io.anuke.ucore.util.Angles;
import io.anuke.ucore.util.Mathf;
import io.anuke.ucore.util.Strings;

public class TurretBlocks extends BlockList implements ContentList {
	public static Block duo, /*scatter,*/ scorch, hail, wave, lancer, arc, swarmer, salvo, fuse, ripple, cyclone, spectre, meltdown;

	@Override
	public void load() {
		duo = new DoubleTurret("duo") {{
			ammoTypes = new AmmoType[]{AmmoTypes.bulletTungsten, AmmoTypes.bulletLead, AmmoTypes.bulletCarbide, AmmoTypes.bulletThermite, AmmoTypes.bulletSilicon};
			reload = 25f;
			restitution = 0.03f;
			range = 80f;
			shootCone = 15f;
			ammoUseEffect = ShootFx.shellEjectSmall;
			health = 80;
		}};
/*
		scatter = new BurstTurret("scatter") {{
			ammoTypes = new AmmoType[]{AmmoTypes.flakLead, AmmoTypes.flakExplosive, AmmoTypes.flakPlastic};
			ammoPerShot = 1;
			shots = 3;
			reload = 60f;
			restitution = 0.03f;
			recoil = 1.5f;
			burstSpacing = 1f;
			inaccuracy = 7f;
			ammoUseEffect = ShootFx.shellEjectSmall;
		}};*/

		hail = new ArtilleryTurret("hail") {{
			ammoTypes = new AmmoType[]{AmmoTypes.artilleryCarbide, AmmoTypes.artilleryHoming, AmmoTypes.artilleryIncindiary};
			reload = 100f;
			recoil = 2f;
			range = 200f;
			inaccuracy = 5f;
			health = 120;
		}};

        scorch = new LiquidTurret("scorch") {{
            ammoTypes = new AmmoType[]{AmmoTypes.basicFlame};
            recoil = 0f;
            reload = 5f;
            shootCone = 50f;
            ammoUseEffect = ShootFx.shellEjectSmall;
			health = 140;

            drawer = (tile, entity) -> Draw.rect(entity.target != null ? name + "-shoot" : name, tile.drawx() + tr2.x, tile.drawy() + tr2.y, entity.rotation - 90);
        }};

		wave = new LiquidTurret("wave") {{
			ammoTypes = new AmmoType[]{AmmoTypes.water, AmmoTypes.lava, AmmoTypes.cryofluid, AmmoTypes.oil};
			size = 2;
			recoil = 0f;
			reload = 4f;
			inaccuracy = 5f;
			shootCone = 50f;
			shootEffect = ShootFx.shootLiquid;
			range = 70f;
			health = 360;

			drawer = (tile, entity) -> {
				Draw.rect(name, tile.drawx() + tr2.x, tile.drawy() + tr2.y, entity.rotation - 90);

				Draw.color(entity.liquids.liquid.color);
				Draw.alpha(entity.liquids.amount / liquidCapacity);
				Draw.rect(name + "-liquid", tile.drawx() + tr2.x, tile.drawy() + tr2.y, entity.rotation - 90);
				Draw.color();
			};
		}};

		lancer = new LaserTurret("lancer") {{
			range = 70f;
			chargeTime = 70f;
			chargeMaxDelay = 30f;
			chargeEffects = 7;
			shootType = AmmoTypes.lancerLaser;
			recoil = 2f;
			reload = 130f;
			cooldown = 0.03f;
			powerUsed = 20f;
			powerCapacity = 60f;
			shootEffect = ShootFx.lancerLaserShoot;
			smokeEffect = ShootFx.lancerLaserShootSmoke;
			chargeEffect = ShootFx.lancerLaserCharge;
			chargeBeginEffect = ShootFx.lancerLaserChargeBegin;
			heatColor = Color.RED;
			size = 2;
			health = 320;
		}};

		arc = new LaserTurret("arc") {{
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
		}};

		swarmer = new ItemTurret("swarmer") {{
			ammoTypes = new AmmoType[]{AmmoTypes.missileExplosive, AmmoTypes.missileIncindiary, AmmoTypes.missileSurge};
			size = 2;
			health = 380;
		}};

		salvo = new ItemTurret("salvo") {{
			size = 2;
			range = 100f;
			ammoTypes = new AmmoType[]{AmmoTypes.bulletTungsten, AmmoTypes.bulletLead, AmmoTypes.bulletCarbide, AmmoTypes.bulletThermite, AmmoTypes.bulletThorium, AmmoTypes.bulletSilicon};
			reload = 70f;
			restitution = 0.03f;
			ammoEjectBack = 3f;
			cooldown = 0.03f;
			recoil = 3f;
			shootShake = 2f;
			ammoUseEffect = ShootFx.shellEjectBig;

			drawer = (tile, entity) -> {
				Draw.rect(name, tile.drawx() + tr2.x, tile.drawy() + tr2.y, entity.rotation - 90);
				float offsetx = (int) (Mathf.abscurve(Mathf.curve(entity.reload / reload, 0.3f, 0.2f)) * 3f);
				float offsety = -(int) (Mathf.abscurve(Mathf.curve(entity.reload / reload, 0.3f, 0.2f)) * 2f);

				for (int i : Mathf.signs) {
					float rot = entity.rotation + 90 * i;
					Draw.rect(name + "-panel-" + Strings.dir(i),
							tile.drawx() + tr2.x + Angles.trnsx(rot, offsetx, offsety),
							tile.drawy() + tr2.y + Angles.trnsy(rot, -offsetx, offsety), entity.rotation - 90);
				}
			};

			health = 430;
		}};

		ripple = new ArtilleryTurret("ripple") {{
			ammoTypes = new AmmoType[]{AmmoTypes.artilleryCarbide, AmmoTypes.artilleryHoming, AmmoTypes.artilleryIncindiary, AmmoTypes.artilleryPlastic, AmmoTypes.artilleryThorium};
			size = 3;

			health = 550;
		}};

		cyclone = new ItemTurret("cyclone") {{
			ammoTypes = new AmmoType[]{AmmoTypes.flakLead, AmmoTypes.flakExplosive, AmmoTypes.flakPlastic, AmmoTypes.flakSurge};
			size = 3;
		}};

		fuse = new ItemTurret("fuse") {{
			//TODO make it use power
			ammoTypes = new AmmoType[]{AmmoTypes.fuseShotgun};
			size = 3;
		}};

		spectre = new ItemTurret("spectre") {{
			ammoTypes = new AmmoType[]{AmmoTypes.bulletTungsten, AmmoTypes.bulletLead, AmmoTypes.bulletCarbide, AmmoTypes.bulletThermite, AmmoTypes.bulletThorium, AmmoTypes.bulletSilicon};
			reload = 25f;
			restitution = 0.03f;
			ammoUseEffect = ShootFx.shellEjectSmall;
			size = 4;
		}};

		meltdown = new PowerTurret("meltdown") {{
			shootType = AmmoTypes.meltdownLaser;
			size = 4;
		}};
	}
}
