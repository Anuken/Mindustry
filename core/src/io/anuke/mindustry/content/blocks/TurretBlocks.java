package io.anuke.mindustry.content.blocks;

import io.anuke.arc.graphics.Color;
import io.anuke.arc.graphics.g2d.Draw;
import io.anuke.mindustry.content.Bullets;
import io.anuke.mindustry.content.Items;
import io.anuke.mindustry.content.Liquids;
import io.anuke.mindustry.content.Fx;
import io.anuke.mindustry.game.ContentList;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.blocks.defense.turrets.*;

public class TurretBlocks extends BlockList implements ContentList{
    public static Block duo, scorch, hail, wave, lancer, arc, swarmer, salvo,
            fuse, ripple, cyclone, spectre, meltdown;

    @Override
    public void load(){
        duo = new DoubleTurret("duo"){{
            ammo(
                Items.copper, Bullets.standardCopper,
                Items.densealloy, Bullets.standardDense,
                Items.pyratite, Bullets.standardIncendiary,
                Items.silicon, Bullets.standardHoming
            );
            reload = 25f;
            restitution = 0.03f;
            range = 90f;
            shootCone = 15f;
            ammoUseEffect = Fx.shellEjectSmall;
            health = 80;
            inaccuracy = 2f;
            rotatespeed = 10f;
        }};

        hail = new ArtilleryTurret("hail"){{
            ammo(
                Items.densealloy, Bullets.artilleryDense,
                Items.silicon, Bullets.artilleryHoming,
                Items.pyratite, Bullets.artlleryIncendiary
            );
            reload = 60f;
            recoil = 2f;
            range = 230f;
            inaccuracy = 1f;
            shootCone = 10f;
            health = 120;
        }};

        scorch = new LiquidTurret("scorch"){{
            ammo(Liquids.oil, Bullets.basicFlame);
            recoil = 0f;
            reload = 4f;
            shootCone = 50f;
            ammoUseEffect = Fx.shellEjectSmall;
            health = 160;
        }};

        wave = new LiquidTurret("wave"){{
            ammo(
                Liquids.water, Bullets.waterShot,
                Liquids.lava, Bullets.lavaShot,
                Liquids.cryofluid, Bullets.cryoShot,
                Liquids.oil, Bullets.oilShot
            );
            size = 2;
            recoil = 0f;
            reload = 4f;
            inaccuracy = 5f;
            shootCone = 50f;
            shootEffect = Fx.shootLiquid;
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
            shootType = Bullets.lancerLaser;
            recoil = 2f;
            reload = 100f;
            cooldown = 0.03f;
            powerUsed = 1 / 3f;
            consumes.powerBuffered(60f);
            shootShake = 2f;
            shootEffect = Fx.lancerLaserShoot;
            smokeEffect = Fx.lancerLaserShootSmoke;
            chargeEffect = Fx.lancerLaserCharge;
            chargeBeginEffect = Fx.lancerLaserChargeBegin;
            heatColor = Color.RED;
            size = 2;
            health = 320;
            targetAir = false;
        }};

        arc = new PowerTurret("arc"){{
            shootType = Bullets.arc;
            reload = 85f;
            shootShake = 1f;
            shootCone = 40f;
            rotatespeed = 8f;
            powerUsed = 1f / 3f;
            consumes.powerBuffered(30f);
            range = 150f;
            shootEffect = Fx.lightningShoot;
            heatColor = Color.RED;
            recoil = 1f;
            size = 1;
        }};

        swarmer = new BurstTurret("swarmer"){{
            ammo(
                Items.blastCompound, Bullets.missileExplosive,
                Items.pyratite, Bullets.missileIncendiary,
                Items.surgealloy, Bullets.missileSurge
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
                Items.copper, Bullets.standardCopper,
                Items.densealloy, Bullets.standardDense,
                Items.pyratite, Bullets.standardIncendiary,
                Items.silicon, Bullets.standardHoming,
                Items.thorium, Bullets.standardThorium
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
            ammoUseEffect = Fx.shellEjectBig;
            health = 360;
        }};

        ripple = new ArtilleryTurret("ripple"){{
            ammo(
                Items.densealloy, Bullets.artilleryDense,
                Items.silicon, Bullets.artilleryHoming,
                Items.pyratite, Bullets.artlleryIncendiary,
                Items.blastCompound, Bullets.artilleryExplosive,
                Items.plastanium, Bullets.arilleryPlastic
            );
            size = 3;
            shots = 4;
            inaccuracy = 12f;
            reload = 60f;
            ammoEjectBack = 5f;
            ammoUseEffect = Fx.shellEjectBig;
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
                Items.blastCompound, Bullets.flakExplosive,
                Items.plastanium, Bullets.flakPlastic,
                Items.surgealloy, Bullets.flakSurge
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
            ammo(Items.densealloy, Bullets.fuseShot);
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
                Items.densealloy, Bullets.standardDenseBig,
                Items.pyratite, Bullets.standardIncendiaryBig,
                Items.thorium, Bullets.standardThoriumBig
            );
            reload = 6f;
            coolantMultiplier = 0.5f;
            maxCoolantUsed = 1.5f;
            restitution = 0.1f;
            ammoUseEffect = Fx.shellEjectBig;
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
            shootType = Bullets.meltdownLaser;
            shootEffect = Fx.shootBigSmoke2;
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
