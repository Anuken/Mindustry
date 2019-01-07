package io.anuke.mindustry.content.blocks;

import io.anuke.arc.graphics.Color;
import io.anuke.arc.graphics.g2d.Draw;
import io.anuke.mindustry.content.Items;
import io.anuke.mindustry.content.Liquids;
import io.anuke.mindustry.content.bullets.*;
import io.anuke.mindustry.content.fx.ShootFx;
import io.anuke.mindustry.game.ContentList;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.blocks.defense.turrets.*;

public class TurretBlocks extends BlockList implements ContentList{
    public static Block duo, /*scatter,*/
            scorch, hail, wave, lancer, arc, swarmer, salvo, fuse, ripple, cyclone, spectre, meltdown;

    @Override
    public void load(){
        duo = new DoubleTurret("duo"){{
            ammo(
                Items.copper, StandardBullets.copper,
                Items.densealloy, StandardBullets.dense,
                Items.pyratite, StandardBullets.tracer,
                Items.silicon, StandardBullets.homing
            );
            reload = 25f;
            restitution = 0.03f;
            range = 90f;
            shootCone = 15f;
            ammoUseEffect = ShootFx.shellEjectSmall;
            health = 80;
            inaccuracy = 2f;
            rotatespeed = 10f;
        }};

        hail = new ArtilleryTurret("hail"){{
            ammo(
                Items.densealloy, ArtilleryBullets.dense,
                Items.silicon, ArtilleryBullets.homing,
                Items.pyratite, ArtilleryBullets.incendiary
            );
            reload = 60f;
            recoil = 2f;
            range = 230f;
            inaccuracy = 1f;
            shootCone = 10f;
            health = 120;
        }};

        scorch = new LiquidTurret("scorch"){{
            ammo(Liquids.oil, TurretBullets.basicFlame);
            recoil = 0f;
            reload = 4f;
            shootCone = 50f;
            ammoUseEffect = ShootFx.shellEjectSmall;
            health = 160;
        }};

        wave = new LiquidTurret("wave"){{
            ammo(
                Liquids.water, TurretBullets.waterShot,
                Liquids.lava, TurretBullets.lavaShot,
                Liquids.cryofluid, TurretBullets.cryoShot,
                Liquids.oil, TurretBullets.oilShot
            );
            size = 2;
            recoil = 0f;
            reload = 4f;
            inaccuracy = 5f;
            shootCone = 50f;
            shootEffect = ShootFx.shootLiquid;
            range = 90f;
            health = 360;

            drawer = (tile, entity) -> {
                Draw.rect(region, tile.drawx() + tr2.x, tile.drawy() + tr2.y, entity.rotation - 90);

                Draw.color(entity.liquids.current().color);
                Draw.alpha(entity.liquids.total() / liquidCapacity);
                Draw.rect(name + "-liquid", tile.drawx() + tr2.x, tile.drawy() + tr2.y, entity.rotation - 90);
                Draw.color();
            };
        }};

        lancer = new ChargeTurret("lancer"){{
            range = 90f;
            chargeTime = 60f;
            chargeMaxDelay = 30f;
            chargeEffects = 7;
            shootType = TurretBullets.lancerLaser;
            recoil = 2f;
            reload = 100f;
            cooldown = 0.03f;
            powerUsed = 1 / 3f;
            consumes.powerBuffered(60f);
            shootShake = 2f;
            shootEffect = ShootFx.lancerLaserShoot;
            smokeEffect = ShootFx.lancerLaserShootSmoke;
            chargeEffect = ShootFx.lancerLaserCharge;
            chargeBeginEffect = ShootFx.lancerLaserChargeBegin;
            heatColor = Color.RED;
            size = 2;
            health = 320;
            targetAir = false;
        }};

        arc = new PowerTurret("arc"){{
            shootType = TurretBullets.arc;
            reload = 85f;
            shootShake = 1f;
            shootCone = 40f;
            rotatespeed = 8f;
            powerUsed = 1f / 3f;
            consumes.powerBuffered(30f);
            range = 150f;
            shootEffect = ShootFx.lightningShoot;
            heatColor = Color.RED;
            recoil = 1f;
            size = 1;
        }};

        swarmer = new BurstTurret("swarmer"){{
            ammo(
                Items.blastCompound, MissileBullets.explosive,
                Items.pyratite, MissileBullets.incindiary,
                Items.surgealloy, MissileBullets.surge
            );
            reload = 50f;
            shots = 4;
            burstSpacing = 5;
            inaccuracy = 10f;
            range = 140f;
            xRand = 6f;
            size = 2;
            health = 380;
        }};

        salvo = new BurstTurret("salvo"){{
            ammo(
                Items.copper, StandardBullets.copper,
                Items.densealloy, StandardBullets.dense,
                Items.pyratite, StandardBullets.tracer,
                Items.silicon, StandardBullets.homing,
                Items.thorium, StandardBullets.thorium
            );

            size = 2;
            range = 120f;
            reload = 35f;
            restitution = 0.03f;
            ammoEjectBack = 3f;
            cooldown = 0.03f;
            recoil = 3f;
            shootShake = 2f;
            burstSpacing = 4;
            shots = 3;
            ammoUseEffect = ShootFx.shellEjectBig;
            health = 360;
        }};

        ripple = new ArtilleryTurret("ripple"){{
            ammo(
                Items.densealloy, ArtilleryBullets.dense,
                Items.silicon, ArtilleryBullets.homing,
                Items.pyratite, ArtilleryBullets.incendiary,
                Items.blastCompound, ArtilleryBullets.explosive,
                Items.plastanium, ArtilleryBullets.plastic
            );
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
            range = 320f;

            health = 550;
        }};

        cyclone = new ItemTurret("cyclone"){{
            ammo(
                Items.blastCompound, FlakBullets.explosive,
                Items.plastanium, FlakBullets.plastic,
                Items.surgealloy, FlakBullets.surge
            );
            xRand = 4f;
            reload = 8f;
            range = 145f;
            size = 3;
            recoil = 3f;
            rotatespeed = 10f;
            inaccuracy = 13f;
            shootCone = 30f;

            health = 145 * size * size;
        }};

        fuse = new ItemTurret("fuse"){{
            ammo(Items.densealloy, TurretBullets.fuseShot);
            reload = 50f;
            shootShake = 4f;
            range = 80f;
            recoil = 5f;
            restitution = 0.1f;
            size = 3;

            health = 155 * size * size;
        }};

        spectre = new DoubleTurret("spectre"){{
            ammo(
                Items.densealloy, StandardBullets.denseBig,
                Items.pyratite, StandardBullets.tracerBig,
                Items.thorium, StandardBullets.thoriumBig
            );
            reload = 6f;
            coolantMultiplier = 0.5f;
            maxCoolantUsed = 1.5f;
            restitution = 0.1f;
            ammoUseEffect = ShootFx.shellEjectBig;
            range = 200f;
            inaccuracy = 3f;
            recoil = 3f;
            xRand = 3f;
            shotWidth = 4f;
            shootShake = 2f;
            shots = 2;
            size = 4;
            shootCone = 24f;

            health = 155 * size * size;
        }};

        meltdown = new LaserTurret("meltdown"){{
            shootType = TurretBullets.meltdownLaser;
            shootEffect = ShootFx.shootBigSmoke2;
            shootCone = 40f;
            recoil = 4f;
            size = 4;
            shootShake = 2f;
            powerUsed = 0.5f;
            consumes.powerBuffered(120f);
            range = 160f;
            reload = 200f;
            firingMoveFract = 0.1f;
            shootDuration = 220f;

            health = 165 * size * size;
        }};
    }
}
