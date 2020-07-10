package mindustry.content;

import arc.graphics.*;
import arc.struct.*;
import mindustry.ai.types.*;
import mindustry.annotations.Annotations.*;
import mindustry.ctype.*;
import mindustry.entities.bullet.*;
import mindustry.entities.units.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.*;

public class UnitTypes implements ContentList{
    //region definitions

    //ground
    public static @EntityDef({Unitc.class, Mechc.class}) UnitType mace, dagger, crawler, fortress, siegeArray, eradicator;

    //ground + builder
    public static @EntityDef({Unitc.class, Mechc.class, Builderc.class}) UnitType nova;

    //ground + builder + miner + commander
    public static @EntityDef({Unitc.class, Mechc.class, Builderc.class, Minerc.class, Commanderc.class}) UnitType pulsar, quasar;

    //legs
    public static @EntityDef({Unitc.class, Legsc.class}) UnitType cix, eruptor;

    //air (no special traits)
    public static @EntityDef({Unitc.class}) UnitType wraith, reaper, ghoul, revenant, lich;

    //air + building
    public static @EntityDef({Unitc.class, Builderc.class}) UnitType mono;

    //air + building + mining
    public static @EntityDef({Unitc.class, Builderc.class, Minerc.class}) UnitType poly;

    //air + building + mining + payload
    public static @EntityDef({Unitc.class, Builderc.class, Minerc.class, Payloadc.class}) UnitType mega;

    //air + building + mining
    public static @EntityDef({Unitc.class, Builderc.class, Minerc.class, Trailc.class}) UnitType alpha, beta, gamma;

    //water
    public static @EntityDef({Unitc.class, WaterMovec.class, Commanderc.class}) UnitType risse, minke, bryde;

    //special block unit type
    public static @EntityDef({Unitc.class, BlockUnitc.class}) UnitType block;

    //endregion

    @Override
    public void load(){
        //region ground attack

        dagger = new UnitType("dagger"){{
            speed = 0.5f;
            hitsize = 8f;
            health = 140;
            weapons.add(new Weapon("large-weapon"){{
                reload = 14f;
                x = 4f;
                y = 2f;
                ejectEffect = Fx.shellEjectSmall;
                bullet = Bullets.standardCopper;
            }});
        }};

        mace = new UnitType("mace"){{
            speed = 0.4f;
            hitsize = 9f;
            range = 10f;
            health = 500;
            armor = 1f;

            immunities.add(StatusEffects.burning);

            weapons.add(new Weapon("flamethrower"){{
                shootSound = Sounds.flame;
                shootY = 2f;
                reload = 14f;
                recoil = 1f;
                ejectEffect = Fx.none;
                bullet = Bullets.basicFlame;
            }});
        }};

        fortress = new UnitType("fortress"){{
            speed = 0.38f;
            hitsize = 13f;
            rotateSpeed = 3f;
            targetAir = false;
            health = 790;
            armor = 4f;

            weapons.add(new Weapon("artillery"){{
                y = 1f;
                x = 9f;
                reload = 60f;
                recoil = 4f;
                shake = 2f;
                ejectEffect = Fx.shellEjectMedium;
                shootSound = Sounds.artillery;
                bullet = new ArtilleryBulletType(2f, 8, "shell"){{
                    hitEffect = Fx.blastExplosion;
                    knockback = 0.8f;
                    lifetime = 110f;
                    width = height = 14f;
                    collides = true;
                    collidesTiles = true;
                    splashDamageRadius = 24f;
                    splashDamage = 38f;
                    backColor = Pal.bulletYellowBack;
                    frontColor = Pal.bulletYellow;
                }};
            }});
        }};

        //endregion
        //region ground support

        nova = new UnitType("nova"){{
            itemCapacity = 60;
            canBoost = true;
            boostMultiplier = 1.5f;
            speed = 0.52f;
            hitsize = 8f;
            health = 110f;
            buildSpeed = 0.8f;
            armor = 1f;

            weapons.add(new Weapon("heal-weapon"){{
                shootY = 2f;
                reload = 24f;
                x = 4.5f;
                alternate = false;
                ejectEffect = Fx.none;
                recoil = 2f;
                bullet = Bullets.healBullet;
                shootSound = Sounds.pew;
            }});
        }};

        pulsar = new UnitType("pulsar"){{
            itemCapacity = 60;
            canBoost = true;
            boostMultiplier = 1.5f;
            speed = 0.48f;
            hitsize = 10f;
            health = 300f;
            buildSpeed = 0.9f;
            armor = 3f;

            mineTier = 2;
            mineSpeed = 5f;
            commandLimit = 8;

            abilities.add(new HealFieldAbility(10f, 200f, 60f));

            weapons.add(new Weapon("heal-shotgun-weapon"){{
                x = 5f;
                shake = 2.2f;
                y = 0.5f;
                shootY = 5f;

                shootY = 2.5f;
                reload = 38f;
                shots = 3;
                inaccuracy = 35;
                shotDelay = 0.5f;
                spacing = 0f;
                ejectEffect = Fx.none;
                recoil = 2.5f;
                shootSound = Sounds.pew;

                bullet = new LightningBulletType(){{
                    lightningColor = hitColor = Pal.heal;
                    damage = 11f;
                    lightningLength = 7;
                    lightningLengthRand = 7;
                    shootEffect = Fx.shootHeal;
                }};
            }});
        }};

        quasar = new UnitType("quasar"){{
            mineTier = 1;
            hitsize = 12f;
            boostMultiplier = 2f;
            itemCapacity = 80;
            health = 640f;
            buildSpeed = 1.7f;
            canBoost = true;
            armor = 6f;
            landShake = 2f;

            speed = 0.4f;
            hitsize = 10f;

            mineTier = 2;
            mineSpeed = 7f;

            abilities.add(new HealFieldAbility(15f, 170f, 60f));

            weapons.add(new Weapon("beam-weapon"){{
                shake = 2f;
                shootY = 4f;
                x = 6.5f;
                reload = 50f;
                recoil = 4f;
                shootSound = Sounds.laser;

                bullet = new LaserBulletType(){{
                    damage = 27f;
                    recoil = 1f;
                    sideAngle = 45f;
                    sideWidth = 1f;
                    sideLength = 70f;
                    colors = new Color[]{Pal.heal.cpy().a(0.4f), Pal.heal, Color.white};
                }};
            }});
        }};

        //endregion
        //region ground legs

        crawler = new UnitType("crawler"){{
            defaultController = SuicideAI::new;

            speed = 0.8f;
            hitsize = 8f;
            health = 140;
            sway = 0.25f;
            range = 40f;

            weapons.add(new Weapon(){{
                reload = 12f;
                shootCone = 180f;
                ejectEffect = Fx.none;
                shootSound = Sounds.explosion;
                bullet = new BombBulletType(0f, 0f, "clear"){{
                    hitEffect = Fx.pulverize;
                    lifetime = 10f;
                    speed = 1f;
                    splashDamageRadius = 55f;
                    instantDisappear = true;
                    splashDamage = 30f;
                    killShooter = true;
                    hittable = false;
                }};
            }});
        }};

        eruptor = new UnitType("eruptor"){{
            speed = 0.4f;
            drag = 0.4f;
            hitsize = 10f;
            rotateSpeed = 3f;
            targetAir = false;
            health = 600;
            immunities = ObjectSet.with(StatusEffects.burning, StatusEffects.melting);
            legCount = 4;
            legLength = 9f;
            legTrns = 0.6f;
            legMoveSpace = 1.4f;

            weapons.add(new Weapon("eruption"){{
                shootY = 3f;
                reload = 10f;
                ejectEffect = Fx.none;
                recoil = 1f;
                x = 7f;
                shootSound = Sounds.flame;

                bullet = new LiquidBulletType(Liquids.slag){{
                    damage = 11;
                    speed = 2.3f;
                    drag = 0.02f;
                    shootEffect = Fx.shootSmall;
                }};
            }});
        }};

        cix = new UnitType("cix"){{
            drag = 0.1f;
            speed = 0.5f;
            hitsize = 9f;
            health = 140;
            baseElevation = 0.51f;

            legCount = 6;
            legMoveSpace = 1f;
            legPairOffset = 3;
            legLength = 34f;
            rotateShooting = false;
            legExtension = -15;
            legBaseOffset = 10f;
            landShake = 2f;
            legSpeed = 0.1f;
            legLengthScl = 1f;
            rippleScale = 2f;
            legSpeed = 0.2f;
            legSplashDamage = 32;
            legSplashRange = 30;

            weapons.add(
            new Weapon("missiles-mount"){{
                reload = 20f;
                x = 4f;
                rotate = true;
                shake = 1f;
                bullet = Bullets.missileSwarm;
            }});
        }};

        //endregion
        //region air attack

        wraith = new UnitType("wraith"){{
            speed = 3f;
            accel = 0.08f;
            drag = 0.01f;
            flying = true;
            health = 75;
            faceTarget = false;
            engineOffset = 5.5f;
            range = 140f;
            weapons.add(new Weapon(){{
                y = 0f;
                x = 2f;
                reload = 15f;
                ejectEffect = Fx.shellEjectSmall;
                bullet = Bullets.standardCopper;
                shootSound = Sounds.shoot;
            }});
        }};

        ghoul = new UnitType("ghoul"){{
            health = 220;
            speed = 2f;
            accel = 0.08f;
            drag = 0.016f;
            flying = true;
            targetAir = false;
            engineOffset = 7.8f;
            range = 140f;
            faceTarget = false;

            weapons.add(new Weapon(){{
                x = 3f;
                shootY = 0f;
                reload = 12f;
                shootCone = 180f;
                ejectEffect = Fx.none;
                inaccuracy = 15f;
                ignoreRotation = true;
                bullet = Bullets.bombExplosive;
                shootSound = Sounds.none;
            }});
        }};

        revenant = new UnitType("revenant"){{
            health = 220;
            speed = 1.9f;
            accel = 0.04f;
            drag = 0.016f;
            flying = true;
            range = 140f;
            hitsize = 18f;
            lowAltitude = true;

            engineOffset = 12f;
            engineSize = 3f;

            weapons.add(new Weapon("revenant-missiles"){{
                reload = 70f;
                x = 7f;
                rotate = true;
                shake = 1f;

                bullet = new MissileBulletType(2.7f, 12, "missile"){{
                    width = 8f;
                    height = 8f;
                    shrinkY = 0f;
                    drag = -0.003f;
                    homingRange = 60f;
                    keepVelocity = false;
                    splashDamageRadius = 25f;
                    splashDamage = 10f;
                    lifetime = 60f;
                    trailColor = Pal.unitBack;
                    backColor = Pal.unitBack;
                    frontColor = Pal.unitFront;
                    hitEffect = Fx.blastExplosion;
                    despawnEffect = Fx.blastExplosion;
                    weaveScale = 6f;
                    weaveMag = 1f;
                }};
            }});
        }};

        reaper = new UnitType("reaper"){{
            speed = 1.1f;
            accel = 0.02f;
            drag = 0.05f;
            rotateSpeed = 2.5f;
            flying = true;
            lowAltitude = true;
            health = 75000;
            engineOffset = 38;
            engineSize = 7.3f;
            hitsize = 58f;
            destructibleWreck = false;

            weapons.add(new Weapon(){{
                y = 1.5f;
                reload = 28f;
                ejectEffect = Fx.shellEjectSmall;
                bullet = Bullets.standardCopper;
                shootSound = Sounds.shoot;
            }});
        }};

        lich = new UnitType("lich"){{
            speed = 1.1f;
            accel = 0.02f;
            drag = 0.05f;
            rotateSpeed = 2.5f;
            flying = true;
            lowAltitude = true;
            health = 75000;
            engineOffset = 38;
            engineSize = 7.3f;
            hitsize = 58f;

            weapons.add(new Weapon(){{
                y = 1.5f;
                reload = 28f;
                ejectEffect = Fx.shellEjectSmall;
                bullet = Bullets.standardCopper;
                shootSound = Sounds.shoot;
            }});
        }};

        //endregion
        //region air support

        mono = new UnitType("mono"){{
            flying = true;
            drag = 0.05f;
            accel = 0.2f;
            speed = 2f;
            range = 50f;
            health = 100;
            engineSize = 1.8f;
            engineOffset = 5.7f;
            weapons.add(new Weapon(){{
                y = 1.5f;
                reload = 40f;
                x = 0.5f;
                ejectEffect = Fx.none;
                recoil = 2f;
                bullet = Bullets.healBulletBig;
                shootSound = Sounds.pew;
            }});
        }};

        poly = new UnitType("poly"){{
            defaultController = BuilderAI::new;

            flying = true;
            drag = 0.05f;
            speed = 3f;
            rotateSpeed = 15f;
            accel = 0.3f;
            range = 70f;
            itemCapacity = 70;
            health = 400;
            buildSpeed = 0.5f;
            engineOffset = 6.5f;
            hitsize = 8f;
        }};

        mega = new UnitType("mega"){{

            health = 500;
            speed = 2f;
            accel = 0.05f;
            drag = 0.016f;
            lowAltitude = true;
            flying = true;
            engineOffset = 10.5f;
            rotateShooting = false;
            hitsize = 15f;
            engineSize = 3f;

            weapons.add(
            new Weapon("heal-weapon-mount"){{
                reload = 25f;
                x = 8f;
                y = -6f;
                rotate = true;
                bullet = Bullets.healBulletBig;
            }},
            new Weapon("heal-weapon-mount"){{
                reload = 15f;
                x = 4f;
                y = 5f;
                rotate = true;
                bullet = Bullets.healBullet;
            }});
        }};

        //endregion
        //region naval attack

        risse = new UnitType("risse"){{
            speed = 1.3f;
            drag = 0.1f;
            hitsize = 8f;
            health = 130;
            immunities = ObjectSet.with(StatusEffects.wet);
            weapons.add(new Weapon("mount-weapon"){{
                reload = 10f;
                x = 1.25f;
                rotate = true;
                ejectEffect = Fx.shellEjectSmall;
                bullet = Bullets.standardCopper;
            }});
        }};

        minke = new UnitType("minke"){{
            speed = 1.3f;
            drag = 0.1f;
            hitsize = 8f;
            health = 130;
            immunities = ObjectSet.with(StatusEffects.wet);
            weapons.add(new Weapon("mount-weapon"){{
                reload = 10f;
                x = 1.25f;
                rotate = true;
                ejectEffect = Fx.shellEjectSmall;
                bullet = Bullets.standardCopper;
            }});
        }};

        bryde = new UnitType("bryde"){{
            speed = 1.3f;
            drag = 0.1f;
            hitsize = 8f;
            health = 130;
            immunities = ObjectSet.with(StatusEffects.wet);
            weapons.add(new Weapon("mount-weapon"){{
                reload = 10f;
                x = 1.25f;
                rotate = true;
                ejectEffect = Fx.shellEjectSmall;
                bullet = Bullets.standardCopper;
            }});
        }};

        //endregion
        //region core

        alpha = new UnitType("alpha"){{
            //TODO maybe these should be changed
            defaultController = BuilderAI::new;
            isCounted = false;

            flying = true;
            mineSpeed = 6f;
            mineTier = 1;
            buildSpeed = 0.5f;
            drag = 0.05f;
            speed = 2.6f;
            rotateSpeed = 15f;
            accel = 0.1f;
            itemCapacity = 30;
            health = 80f;
            engineOffset = 6f;
            hitsize = 8f;

            weapons.add(new Weapon("small-basic-weapon"){{
                reload = 20f;
                x = 2.75f;
                y = 1f;

                bullet = new BasicBulletType(2.5f, 9){{
                    width = 7f;
                    height = 9f;
                    lifetime = 60f;
                    shootEffect = Fx.shootSmall;
                    smokeEffect = Fx.shootSmallSmoke;
                    tileDamageMultiplier = 0.1f;
                }};
            }});
        }};

        beta = new UnitType("beta"){{
            //TODO maybe these should be changed
            defaultController = BuilderAI::new;
            isCounted = false;

            flying = true;
            mineSpeed = 7f;
            mineTier = 1;
            buildSpeed = 0.75f;
            drag = 0.05f;
            speed = 2.9f;
            rotateSpeed = 17f;
            accel = 0.1f;
            itemCapacity = 50;
            health = 120f;
            engineOffset = 6f;
            hitsize = 9f;
            rotateShooting = false;
            lowAltitude = true;

            weapons.add(new Weapon("small-mount-weapon"){{
                reload = 20f;
                x = 3f;
                y = 0.5f;
                rotate = true;
                shots = 2;
                shotDelay = 4f;
                spacing = 0f;

                bullet = new BasicBulletType(3f, 9){{
                    width = 7f;
                    height = 9f;
                    lifetime = 60f;
                    shootEffect = Fx.shootSmall;
                    smokeEffect = Fx.shootSmallSmoke;
                    tileDamageMultiplier = 0.14f;
                }};
            }});
        }};

        gamma = new UnitType("gamma"){{
            //TODO maybe these should be changed
            defaultController = BuilderAI::new;
            isCounted = false;

            flying = true;
            mineSpeed = 8f;
            mineTier = 2;
            buildSpeed = 1f;
            drag = 0.05f;
            speed = 3.4f;
            rotateSpeed = 19f;
            accel = 0.11f;
            itemCapacity = 70;
            health = 160f;
            engineOffset = 6f;
            hitsize = 10f;

            weapons.add(new Weapon("small-mount-weapon"){{
                reload = 15f;
                x = 1f;
                y = 2f;
                shots = 2;
                spacing = 2f;
                inaccuracy = 3f;
                shotDelay = 3f;

                bullet = new BasicBulletType(3.5f, 9){{
                    width = 6.5f;
                    height = 11f;
                    lifetime = 70f;
                    shootEffect = Fx.shootSmall;
                    smokeEffect = Fx.shootSmallSmoke;
                    tileDamageMultiplier = 0.15f;
                    homingPower = 0.04f;
                }};
            }});
        }};

        //endregion
        //region internal

        block = new UnitType("block"){
            {
                speed = 0f;
                hitsize = 0f;
                health = 1;
                rotateSpeed = 360f;
            }

            @Override
            public boolean isHidden(){
                return true;
            }
        };

        //endregion
    }
}
