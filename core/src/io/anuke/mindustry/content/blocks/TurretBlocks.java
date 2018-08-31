package io.anuke.mindustry.content.blocks;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import io.anuke.mindustry.content.AmmoTypes;
import io.anuke.mindustry.content.fx.ShootFx;
import io.anuke.mindustry.type.AmmoType;
import io.anuke.mindustry.type.ContentList;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.blocks.defense.turrets.*;
import io.anuke.ucore.graphics.Draw;
import io.anuke.ucore.util.Angles;
import io.anuke.ucore.util.Mathf;

public class TurretBlocks extends BlockList implements ContentList{
    public static Block duo, /*scatter,*/
            scorch, hail, wave, lancer, arc, swarmer, salvo, fuse, ripple, cyclone, spectre, meltdown;

    @Override
    public void load(){
        duo = new DoubleTurret("duo"){{
            ammoTypes = new AmmoType[]{AmmoTypes.bulletCopper, AmmoTypes.bulletDense, AmmoTypes.bulletPyratite, AmmoTypes.bulletSilicon};
            reload = 25f;
            restitution = 0.03f;
            range = 90f;
            shootCone = 15f;
            ammoUseEffect = ShootFx.shellEjectSmall;
            health = 80;
            inaccuracy = 2f;
            rotatespeed = 10f;
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

        hail = new ArtilleryTurret("hail"){{
            ammoTypes = new AmmoType[]{AmmoTypes.artilleryDense, AmmoTypes.artilleryHoming, AmmoTypes.artilleryIncindiary};
            reload = 100f;
            recoil = 2f;
            range = 200f;
            inaccuracy = 5f;
            health = 120;
        }};

        scorch = new LiquidTurret("scorch"){
            protected TextureRegion shootRegion;

            @Override
            public void load(){
                super.load();
                shootRegion = Draw.region(name + "-shoot");
            }

            {
                ammoTypes = new AmmoType[]{AmmoTypes.basicFlame};
                recoil = 0f;
                reload = 4f;
                shootCone = 50f;
                ammoUseEffect = ShootFx.shellEjectSmall;
                health = 160;

                drawer = (tile, entity) -> Draw.rect(entity.target != null ? shootRegion : region, tile.drawx() + tr2.x, tile.drawy() + tr2.y, entity.rotation - 90);
                }
        };

        wave = new LiquidTurret("wave"){{
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
                Draw.rect(region, tile.drawx() + tr2.x, tile.drawy() + tr2.y, entity.rotation - 90);

                Draw.color(entity.liquids.current().color);
                Draw.alpha(entity.liquids.total() / liquidCapacity);
                Draw.rect(name + "-liquid", tile.drawx() + tr2.x, tile.drawy() + tr2.y, entity.rotation - 90);
                Draw.color();
            };
        }};

        lancer = new LaserTurret("lancer"){{
            range = 90f;
            chargeTime = 60f;
            chargeMaxDelay = 30f;
            chargeEffects = 7;
            shootType = AmmoTypes.lancerLaser;
            recoil = 2f;
            reload = 100f;
            cooldown = 0.03f;
            powerUsed = 20f;
            powerCapacity = 60f;
            shootShake = 2f;
            shootEffect = ShootFx.lancerLaserShoot;
            smokeEffect = ShootFx.lancerLaserShootSmoke;
            chargeEffect = ShootFx.lancerLaserCharge;
            chargeBeginEffect = ShootFx.lancerLaserChargeBegin;
            heatColor = Color.RED;
            size = 2;
            health = 320;
        }};

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
        }};

        swarmer = new BurstTurret("swarmer"){{
            ammoTypes = new AmmoType[]{AmmoTypes.missileExplosive, AmmoTypes.missileIncindiary, AmmoTypes.missileSurge};
            reload = 60f;
            shots = 4;
            burstSpacing = 5;
            inaccuracy = 10f;
            range = 140f;
            xRand = 6f;
            size = 2;
            health = 380;
        }};

        salvo = new BurstTurret("salvo"){
            TextureRegion[] panels = new TextureRegion[2];

            @Override
            public void load() {
                super.load();
                panels[0] = Draw.region(name + "-panel-left");
                panels[1] = Draw.region(name + "-panel-right");
            }

            {
                size = 2;
                range = 120f;
                ammoTypes = new AmmoType[]{AmmoTypes.bulletCopper, AmmoTypes.bulletDense, AmmoTypes.bulletPyratite, AmmoTypes.bulletThorium, AmmoTypes.bulletSilicon};
                reload = 40f;
                restitution = 0.03f;
                ammoEjectBack = 3f;
                cooldown = 0.03f;
                recoil = 3f;
                shootShake = 2f;
                burstSpacing = 4;
                shots = 3;
                ammoUseEffect = ShootFx.shellEjectBig;

                drawer = (tile, entity) -> {
                    Draw.rect(region, tile.drawx() + tr2.x, tile.drawy() + tr2.y, entity.rotation - 90);
                    float offsetx = (int) (Mathf.abscurve(Mathf.curve(entity.reload / reload, 0.3f, 0.2f)) * 3f);
                    float offsety = -(int) (Mathf.abscurve(Mathf.curve(entity.reload / reload, 0.3f, 0.2f)) * 2f);

                    for(int i : Mathf.signs){
                        float rot = entity.rotation + 90 * i;
                        Draw.rect(panels[i == -1 ? 0 : 1],
                                tile.drawx() + tr2.x + Angles.trnsx(rot, offsetx, offsety),
                                tile.drawy() + tr2.y + Angles.trnsy(rot, -offsetx, offsety), entity.rotation - 90);
                    }
                };

                health = 360;
            }
        };

        ripple = new ArtilleryTurret("ripple"){{
            ammoTypes = new AmmoType[]{AmmoTypes.artilleryDense, AmmoTypes.artilleryHoming, AmmoTypes.artilleryIncindiary, AmmoTypes.artilleryExplosive, AmmoTypes.artilleryPlastic};
            size = 3;
            shots = 4;
            inaccuracy = 12f;
            reload = 60f;
            ammoEjectBack = 5f;
            ammoUseEffect = ShootFx.shellEjectBig;
            cooldown = 0.03f;
            velocityInaccuracy = 0.2f;
            restitution = 0.02f;
            recoil = 6f;
            shootShake = 2f;
            range = 300f;

            health = 550;
        }};

        cyclone = new ItemTurret("cyclone"){{
            ammoTypes = new AmmoType[]{AmmoTypes.flakExplosive, AmmoTypes.flakPlastic, AmmoTypes.flakSurge};
            xRand = 4f;
            reload = 10f;
            range = 140f;
            size = 3;
            recoil = 3f;
            rotatespeed = 10f;
            inaccuracy = 13f;
            shootCone = 30f;
        }};

        fuse = new ItemTurret("fuse"){{
            //TODO make it use power
            ammoTypes = new AmmoType[]{AmmoTypes.fuseShotgun};
            reload = 50f;
            shootShake = 4f;
            range = 80f;
            recoil = 5f;
            restitution = 0.1f;
            size = 3;
        }};

        spectre = new ItemTurret("spectre"){{
            ammoTypes = new AmmoType[]{AmmoTypes.bulletCopper, AmmoTypes.bulletDense, AmmoTypes.bulletPyratite, AmmoTypes.bulletThorium, AmmoTypes.bulletSilicon};
            reload = 25f;
            restitution = 0.03f;
            ammoUseEffect = ShootFx.shellEjectSmall;
            size = 4;
        }};

        meltdown = new PowerTurret("meltdown"){{
            shootType = AmmoTypes.meltdownLaser;
            size = 4;
        }};
    }
}
