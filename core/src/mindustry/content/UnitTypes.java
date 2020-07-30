package mindustry.content;

import arc.graphics.*;
import arc.struct.*;
import mindustry.ai.types.*;
import mindustry.annotations.Annotations.*;
import mindustry.ctype.*;
import mindustry.entities.abilities.*;
import mindustry.entities.bullet.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.*;

public class UnitTypes implements ContentList{
    //region definitions

    //ground
    public static @EntityDef({Unitc.class, Mechc.class}) UnitType mace, dagger, crawler, fortress, vestige, cataclyst;

    //ground + builder
    public static @EntityDef({Unitc.class, Mechc.class, Builderc.class}) UnitType nova;

    //ground + builder + miner + commander
    public static @EntityDef({Unitc.class, Mechc.class, Builderc.class, Minerc.class, Commanderc.class}) UnitType pulsar, quasar;

    //legs
    public static @EntityDef({Unitc.class, Legsc.class}) UnitType atrax;

    //legs + building
    public static @EntityDef({Unitc.class, Legsc.class, Builderc.class}) UnitType spiroct, arkyid;

    //air (no special traits)
    public static @EntityDef({Unitc.class}) UnitType flare, eclipse, horizon, zenith, antumbra;

    //air + mining
    public static @EntityDef({Unitc.class, Minerc.class}) UnitType mono;

    //air + building + mining
    public static @EntityDef({Unitc.class, Builderc.class, Minerc.class}) UnitType poly;

    //air + building + mining + payload
    public static @EntityDef({Unitc.class, Builderc.class, Minerc.class, Payloadc.class}) UnitType mega;

    //air + building + mining
    public static @EntityDef({Unitc.class, Builderc.class, Minerc.class, Trailc.class}) UnitType alpha, beta, gamma;

    //water
    public static @EntityDef({Unitc.class, WaterMovec.class, Commanderc.class}) UnitType risso, minke, bryde;

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
            armor = 4f;

            immunities.add(StatusEffects.burning);

            weapons.add(new Weapon("flamethrower"){{
                shootSound = Sounds.flame;
                shootY = 2f;
                reload = 14f;
                recoil = 1f;
                ejectEffect = Fx.none;
                bullet = new BulletType(3f, 30f){{
                    ammoMultiplier = 3f;
                    hitSize = 7f;
                    lifetime = 42f;
                    pierce = true;
                    drag = 0.05f;
                    statusDuration = 60f * 4;
                    shootEffect = Fx.shootSmallFlame;
                    hitEffect = Fx.hitFlameSmall;
                    despawnEffect = Fx.none;
                    status = StatusEffects.burning;
                    keepVelocity = false;
                    hittable = false;
                }};
            }});
        }};

        fortress = new UnitType("fortress"){{
            speed = 0.38f;
            hitsize = 13f;
            rotateSpeed = 3f;
            targetAir = false;
            health = 790;
            armor = 9f;

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

            abilities.add(new HealFieldAbility(10f, 60f * 4, 60f));

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
            armor = 4f;

            mineTier = 2;
            mineSpeed = 5f;
            commandLimit = 8;

            abilities.add(new ShieldFieldAbility(15f, 30f, 60f * 5, 60f));

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
            armor = 9f;
            landShake = 2f;

            speed = 0.4f;
            hitsize = 10f;

            mineTier = 2;
            mineSpeed = 7f;
            drawShields = false;

            abilities.add(new ForceFieldAbility(60f, 0.12f, 200f, 60f * 8));

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

            speed = 0.85f;
            hitsize = 8f;
            health = 170;
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
                    splashDamage = 40f;
                    killShooter = true;
                    hittable = false;
                    collidesAir = true;
                }};
            }});
        }};

        atrax = new UnitType("atrax"){{
            itemCapacity = 80;
            speed = 0.5f;
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
            hovering = true;
            armor = 3f;

            allowLegStep = true;
            visualElevation = 0.2f;
            groundLayer = Layer.legUnit - 1f;

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
                    drag = 0.01f;
                    shootEffect = Fx.shootSmall;
                    lifetime = 56f;
                    collidesAir = false;
                }};
            }});
        }};

        spiroct = new UnitType("spiroct"){{
            itemCapacity = 200;
            speed = 0.4f;
            drag = 0.4f;
            hitsize = 12f;
            rotateSpeed = 3f;
            health = 600;
            immunities = ObjectSet.with(StatusEffects.burning, StatusEffects.melting);
            legCount = 6;
            legLength = 13f;
            legTrns = 0.8f;
            legMoveSpace = 1.4f;
            legBaseOffset = 2f;
            hovering = true;
            armor = 5f;

            buildSpeed = 0.75f;

            allowLegStep = true;
            visualElevation = 0.3f;
            groundLayer = Layer.legUnit;

            weapons.add(new Weapon("spiroct-weapon"){{
                shootY = 4f;
                reload = 15f;
                ejectEffect = Fx.none;
                recoil = 2f;
                rotate = true;
                shootSound = Sounds.flame;

                x = 8.5f;
                y = -1.5f;

                bullet = new SapBulletType(){{
                    sapStrength = 0.3f;
                    length = 75f;
                    damage = 15;
                    shootEffect = Fx.shootSmall;
                    hitColor = color = Color.valueOf("bf92f9");
                    despawnEffect = Fx.none;
                    width = 0.54f;
                    lifetime = 35f;
                    knockback = -1.2f;
                }};
            }});

            weapons.add(new Weapon("mount-purple-weapon"){{
                reload = 20f;
                rotate = true;
                x = 4f;
                y = 3f;

                bullet = new SapBulletType(){{
                    sapStrength = 0.65f;
                    length = 40f;
                    damage = 12;
                    shootEffect = Fx.shootSmall;
                    hitColor = color = Color.valueOf("bf92f9");
                    despawnEffect = Fx.none;
                    width = 0.4f;
                    lifetime = 25f;
                    knockback = -0.6f;
                }};
            }});
        }};

        //TODO implement
        arkyid = new UnitType("arkyid"){{
            drag = 0.1f;
            speed = 0.5f;
            hitsize = 9f;
            health = 140;

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
                bullet = new MissileBulletType(2.7f, 12, "missile"){{
                    width = 8f;
                    height = 8f;
                    shrinkY = 0f;
                    drag = -0.003f;
                    homingRange = 60f;
                    keepVelocity = false;
                    splashDamageRadius = 25f;
                    splashDamage = 10f;
                    lifetime = 120f;
                    trailColor = Color.gray;
                    backColor = Pal.bulletYellowBack;
                    frontColor = Pal.bulletYellow;
                    hitEffect = Fx.blastExplosion;
                    despawnEffect = Fx.blastExplosion;
                    weaveScale = 8f;
                    weaveMag = 2f;
                }};
            }});
        }};

        //endregion
        //region air attack

        flare = new UnitType("flare"){{
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

        horizon = new UnitType("horizon"){{
            health = 300;
            speed = 2f;
            accel = 0.08f;
            drag = 0.016f;
            flying = true;
            targetAir = false;
            engineOffset = 7.8f;
            range = 140f;
            faceTarget = false;
            armor = 2f;

            weapons.add(new Weapon(){{
                minShootVelocity = 0.75f;
                x = 3f;
                shootY = 0f;
                reload = 12f;
                shootCone = 180f;
                ejectEffect = Fx.none;
                inaccuracy = 15f;
                ignoreRotation = true;
                shootSound = Sounds.none;
                bullet = new BombBulletType(23f, 25f){{
                    width = 10f;
                    height = 14f;
                    hitEffect = Fx.flakExplosion;
                    shootEffect = Fx.none;
                    smokeEffect = Fx.none;

                    status = StatusEffects.blasted;
                    statusDuration = 60f;
                }};
            }});
        }};

        zenith = new UnitType("zenith"){{
            health = 1000;
            speed = 1.9f;
            accel = 0.04f;
            drag = 0.016f;
            flying = true;
            range = 140f;
            hitsize = 18f;
            lowAltitude = true;
            armor = 6f;

            engineOffset = 12f;
            engineSize = 3f;

            weapons.add(new Weapon("zenith-missiles"){{
                reload = 32f;
                x = 7f;
                rotate = true;
                shake = 1f;
                shots = 2;
                inaccuracy = 5f;
                velocityRnd = 0.2f;

                bullet = new MissileBulletType(3f, 12){{
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

        antumbra = new UnitType("antumbra"){{
            speed = 1.25f;
            accel = 0.035f;
            drag = 0.05f;
            rotateSpeed = 3.5f;
            flying = true;
            lowAltitude = true;
            health = 9000;
            armor = 9f;
            engineOffset = 21;
            engineSize = 5.3f;
            hitsize = 58f;

            weapons.add(new Weapon(){{
                y = 1.5f;
                reload = 28f;
                ejectEffect = Fx.shellEjectSmall;
                bullet = Bullets.standardCopper;
                shootSound = Sounds.shoot;
            }});
        }};

        eclipse = new UnitType("eclipse"){{
            speed = 1.1f;
            accel = 0.02f;
            drag = 0.05f;
            rotateSpeed = 2.5f;
            flying = true;
            lowAltitude = true;
            health = 18000;
            engineOffset = 38;
            engineSize = 7.3f;
            hitsize = 58f;
            destructibleWreck = false;
            armor = 13f;

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
            accel = 0.15f;
            speed = 2f;
            health = 100;
            engineSize = 1.8f;
            engineOffset = 5.7f;

            mineTier = 1;
            mineSpeed = 2.5f;
        }};

        poly = new UnitType("poly"){{
            defaultController = BuilderAI::new;

            flying = true;
            drag = 0.05f;
            speed = 2f;
            rotateSpeed = 15f;
            accel = 0.1f;
            range = 70f;
            itemCapacity = 70;
            health = 400;
            buildSpeed = 0.5f;
            engineOffset = 6.5f;
            hitsize = 8f;
            lowAltitude = true;

            mineTier = 2;
            mineSpeed = 3.5f;

            abilities.add(new HealFieldAbility(5f, 60f * 5, 50f));

            weapons.add(new Weapon("heal-weapon-mount"){{
                y = -2.5f;
                x = 3.5f;
                reload = 34f;
                ejectEffect = Fx.none;
                recoil = 2f;
                shootSound = Sounds.pew;
                shots = 1;
                velocityRnd = 0.5f;
                inaccuracy = 15f;
                alternate = true;

                bullet = new MissileBulletType(4f, 10){{
                    homingPower = 0.08f;
                    weaveMag = 4;
                    weaveScale = 4;
                    lifetime = 50f;
                    keepVelocity = false;
                    shootEffect = Fx.shootHeal;
                    smokeEffect = Fx.hitLaser;
                    frontColor = Color.white;

                    backColor = Pal.heal;
                    trailColor = Pal.heal;
                }};
            }});
        }};

        mega = new UnitType("mega"){{

            health = 500;
            speed = 1.8f;
            accel = 0.06f;
            drag = 0.017f;
            lowAltitude = true;
            flying = true;
            engineOffset = 10.5f;
            rotateShooting = false;
            hitsize = 15f;
            engineSize = 3f;
            payloadCapacity = 4;

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

        risso = new UnitType("risso"){{
            speed = 1.1f;
            drag = 0.13f;
            hitsize = 9f;
            health = 280;
            accel = 0.4f;
            rotateSpeed = 3.3f;
            immunities = ObjectSet.with(StatusEffects.wet);
            trailLength = 20;

            armor = 2f;

            weapons.add(new Weapon("mount-weapon"){{
                reload = 12f;
                x = 4f;
                shootY = 4f;
                y = 1.5f;
                rotate = true;
                ejectEffect = Fx.shellEjectSmall;
                bullet = Bullets.standardCopper;
            }});

            weapons.add(new Weapon("missiles-mount"){{
                mirror = false;
                reload = 20f;
                x = 0f;
                y = -5f;
                rotate = true;
                ejectEffect = Fx.shellEjectSmall;
                bullet = new MissileBulletType(2.7f, 12, "missile"){{
                    width = 8f;
                    height = 8f;
                    shrinkY = 0f;
                    drag = -0.003f;
                    homingRange = 60f;
                    keepVelocity = false;
                    splashDamageRadius = 25f;
                    splashDamage = 10f;
                    lifetime = 80f;
                    trailColor = Color.gray;
                    backColor = Pal.bulletYellowBack;
                    frontColor = Pal.bulletYellow;
                    hitEffect = Fx.blastExplosion;
                    despawnEffect = Fx.blastExplosion;
                    weaveScale = 8f;
                    weaveMag = 2f;
                }};
            }});
        }};

        minke = new UnitType("minke"){{
            health = 600;
            speed = 0.9f;
            drag = 0.15f;
            hitsize = 11f;
            armor = 4f;
            accel = 0.3f;
            rotateSpeed = 2.6f;
            immunities = ObjectSet.with(StatusEffects.wet);

            trailLength = 20;
            trailX = 5.5f;
            trailY = -4f;
            trailScl = 1.9f;

            abilities.add(new StatusFieldAbility(StatusEffects.overclock, 60f * 6, 60f * 6f, 60f));

            weapons.add(new Weapon("mount-weapon"){{
                reload = 15f;
                x = 5f;
                y = 3.5f;
                rotate = true;
                rotateSpeed = 5f;
                inaccuracy = 10f;
                ejectEffect = Fx.shellEjectSmall;
                bullet = Bullets.flakLead;
            }});

            weapons.add(new Weapon("artillery-mount"){{
                reload = 30f;
                x = 5f;
                y = -5f;
                rotate = true;
                inaccuracy = 2f;
                rotateSpeed = 2f;
                shake = 1.5f;
                ejectEffect = Fx.shellEjectMedium;
                bullet = Bullets.artilleryIncendiary;
            }});
        }};

        bryde = new UnitType("bryde"){{
            health = 900;
            speed = 0.85f;
            accel = 0.2f;
            rotateSpeed = 1.8f;
            drag = 0.17f;
            hitsize = 14f;
            armor = 6f;
            immunities = ObjectSet.with(StatusEffects.wet);

            trailLength = 22;
            trailX = 7f;
            trailY = -9f;
            trailScl = 1.5f;

            abilities.add(new HealFieldAbility(22f, 60f * 4, 70f), new ShieldFieldAbility(20f, 40f, 60f * 4, 60f));

            weapons.add(new Weapon("large-artillery"){{
                reload = 65f;
                mirror = false;
                x = 0f;
                y = -3.5f;
                rotateSpeed = 1.7f;
                rotate = true;
                shootY = 7f;
                shake = 5f;
                recoil = 4f;
                occlusion = 17f;

                shots = 1;
                inaccuracy = 3f;

                ejectEffect = Fx.shellEjectBig;

                bullet = new ArtilleryBulletType(3.2f, 12){{
                    trailMult = 0.8f;
                    hitEffect = Fx.massiveExplosion;
                    knockback = 1.5f;
                    lifetime = 140f;
                    height = 15.5f;
                    width = 15f;
                    collidesTiles = false;
                    ammoMultiplier = 4f;
                    splashDamageRadius = 60f;
                    splashDamage = 85f;
                    backColor = Pal.missileYellowBack;
                    frontColor = Pal.missileYellow;
                    trailEffect = Fx.artilleryTrail;
                    trailSize = 6f;
                    hitShake = 4f;

                    shootEffect = Fx.shootBig2;

                    status = StatusEffects.blasted;
                    statusDuration = 60f;
                }};
            }});

            weapons.add(new Weapon("missiles-mount"){{
                reload = 20f;
                x = 8.5f;
                y = -9f;

                occlusion = 6f;

                rotateSpeed = 4f;
                rotate = true;
                shots = 2;
                shotDelay = 3f;
                inaccuracy = 5f;
                velocityRnd = 0.1f;

                ejectEffect = Fx.none;
                bullet = new MissileBulletType(2.7f, 12){{
                    width = 8f;
                    height = 8f;
                    shrinkY = 0f;
                    drag = -0.003f;
                    homingRange = 60f;
                    keepVelocity = false;
                    splashDamageRadius = 25f;
                    splashDamage = 10f;
                    lifetime = 80f;
                    trailColor = Color.gray;
                    backColor = Pal.bulletYellowBack;
                    frontColor = Pal.bulletYellow;
                    hitEffect = Fx.blastExplosion;
                    despawnEffect = Fx.blastExplosion;
                    weaveScale = 8f;
                    weaveMag = 1f;
                }};
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
            health = 120f;
            engineOffset = 6f;
            hitsize = 8f;

            weapons.add(new Weapon("small-basic-weapon"){{
                reload = 17f;
                x = 2.75f;
                y = 1f;

                bullet = new BasicBulletType(2.5f, 9){{
                    width = 7f;
                    height = 9f;
                    lifetime = 60f;
                    shootEffect = Fx.shootSmall;
                    smokeEffect = Fx.shootSmallSmoke;
                    tileDamageMultiplier = 0.95f;
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
            health = 150f;
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
                    tileDamageMultiplier = 0.1f;
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
            health = 190f;
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
                    tileDamageMultiplier = 0.1f;
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
