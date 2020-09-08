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
    public static @EntityDef({Unitc.class, Mechc.class}) UnitType mace, dagger, crawler, fortress, scepter, reign;

    //ground + builder
    public static @EntityDef({Unitc.class, Mechc.class, Builderc.class}) UnitType nova;

    //ground + builder + miner + commander
    public static @EntityDef({Unitc.class, Mechc.class, Builderc.class, Minerc.class, Commanderc.class}) UnitType pulsar, quasar;

    //legs
    public static @EntityDef({Unitc.class, Legsc.class}) UnitType atrax;

    //legs + building
    public static @EntityDef({Unitc.class, Legsc.class, Builderc.class}) UnitType spiroct, arkyid, toxopid;

    //air (no special traits)
    public static @EntityDef({Unitc.class}) UnitType flare, eclipse, horizon, zenith, antumbra;

    //air + mining
    public static @EntityDef({Unitc.class, Minerc.class}) UnitType mono;

    //air + building + mining
    public static @EntityDef({Unitc.class, Builderc.class, Minerc.class}) UnitType poly;

    //air + building + mining + payload
    public static @EntityDef({Unitc.class, Builderc.class, Minerc.class, Payloadc.class}) UnitType mega;

    //air + building + mining
    public static @EntityDef({Unitc.class, Builderc.class, Minerc.class}) UnitType alpha, beta, gamma;

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
            health = 500;
            armor = 4f;

            immunities.add(StatusEffects.burning);

            weapons.add(new Weapon("flamethrower"){{
                shootSound = Sounds.flame;
                shootY = 2f;
                reload = 14f;
                recoil = 1f;
                ejectEffect = Fx.none;
                bullet = new BulletType(3.9f, 30f){{
                    ammoMultiplier = 3f;
                    hitSize = 7f;
                    lifetime = 12f;
                    pierce = true;
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

        scepter = new UnitType("scepter"){{
            speed = 0.35f;
            hitsize = 20f;
            rotateSpeed = 2.1f;
            targetAir = false;
            health = 9000;
            armor = 11f;
            mechLegMoveScl = 1.3f;
            canDrown = false;

            weapons.add(
            new Weapon("scepter-weapon"){{
                y = 1f;
                x = 16f;
                shootY = 8f;
                reload = 50f;
                recoil = 5f;
                shake = 2f;
                ejectEffect = Fx.shellEjectBig;
                shootSound = Sounds.artillery;
                shots = 3;
                inaccuracy = 3f;
                shotDelay = 4f;

                bullet = new BasicBulletType(7f, 40){{
                    width = 11f;
                    height = 20f;
                    lifetime = 25f;
                    shootEffect = Fx.shootBig;
                    lightning = 2;
                    lightningLength = 6;
                    lightningColor = Pal.surge;
                    //standard bullet damage is far too much for lightning
                    lightningDamage = 17;
                }};
            }},

            new Weapon("mount-weapon"){{
                reload = 13f;
                x = 8.5f;
                y = 6f;
                rotate = true;
                ejectEffect = Fx.shellEjectSmall;
                bullet = Bullets.standardCopper;
            }},
            new Weapon("mount-weapon"){{
                reload = 16f;
                x = 8.5f;
                y = -7f;
                rotate = true;
                ejectEffect = Fx.shellEjectSmall;
                bullet = Bullets.standardCopper;
            }}

            );
        }};

        reign = new UnitType("reign"){{
            speed = 0.35f;
            hitsize = 26f;
            rotateSpeed = 1.65f;
            targetAir = false;
            health = 24000;
            armor = 14f;
            mechLegMoveScl = 1.75f;
            canDrown = false;

            weapons.add(
            new Weapon("reign-weapon"){{
                y = 1f;
                x = 21.5f;
                shootY = 11f;
                reload = 9f;
                recoil = 5f;
                shake = 4f;
                ejectEffect = Fx.shellEjectBig;
                shootSound = Sounds.artillery;

                bullet = new BasicBulletType(13f, 55){{
                    pierce = true;
                    width = 14f;
                    height = 32f;
                    lifetime = 15f;
                    shootEffect = Fx.shootBig;
                    fragVelocityMin = 0.4f;

                    hitEffect = Fx.blastExplosion;
                    splashDamage = 18f;
                    splashDamageRadius = 30f;

                    fragBullets = 2;
                    fragLifeMin = 0f;
                    fragCone = 30f;

                    fragBullet = new BasicBulletType(9f, 15){{
                        width = 10f;
                        height = 10f;
                        pierce = true;

                        lifetime = 20f;
                        hitEffect = Fx.flakExplosion;
                        splashDamage = 15f;
                        splashDamageRadius = 15f;
                    }};
                }};
            }}

            );
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
            health = 320f;
            buildSpeed = 0.9f;
            armor = 4f;

            mineTier = 2;
            mineSpeed = 5f;
            commandLimit = 8;

            abilities.add(new ShieldFieldAbility(20f, 40f, 60f * 5, 60f));

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
                    damage = 15f;
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
            health = 650f;
            buildSpeed = 1.7f;
            canBoost = true;
            armor = 9f;
            landShake = 2f;

            speed = 0.4f;
            hitsize = 10f;

            mineTier = 2;
            mineSpeed = 7f;
            drawShields = false;

            abilities.add(new ForceFieldAbility(60f, 0.3f, 400f, 60f * 6));

            weapons.add(new Weapon("beam-weapon"){{
                shake = 2f;
                shootY = 4f;
                x = 6.5f;
                reload = 50f;
                recoil = 4f;
                shootSound = Sounds.laser;

                bullet = new LaserBulletType(){{
                    damage = 40f;
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
                    splashDamage = 45f;
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
            health = 760;
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
                    knockback = -1.24f;
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
                    damage = 13;
                    shootEffect = Fx.shootSmall;
                    hitColor = color = Color.valueOf("bf92f9");
                    despawnEffect = Fx.none;
                    width = 0.4f;
                    lifetime = 25f;
                    knockback = -0.65f;
                }};
            }});
        }};

        arkyid = new UnitType("arkyid"){{
            drag = 0.1f;
            speed = 0.5f;
            hitsize = 21f;
            health = 8000;
            armor = 6f;

            rotateSpeed = 2.7f;

            legCount = 6;
            legMoveSpace = 1f;
            legPairOffset = 3;
            legLength = 30f;
            legExtension = -15;
            legBaseOffset = 10f;
            landShake = 1f;
            legSpeed = 0.1f;
            legLengthScl = 0.96f;
            rippleScale = 2f;
            legSpeed = 0.2f;

            legSplashDamage = 32;
            legSplashRange = 30;

            hovering = true;
            allowLegStep = true;
            visualElevation = 0.65f;
            groundLayer = Layer.legUnit;

            BulletType sapper = new SapBulletType(){{
                sapStrength = 0.83f;
                length = 55f;
                damage = 34;
                shootEffect = Fx.shootSmall;
                hitColor = color = Color.valueOf("bf92f9");
                despawnEffect = Fx.none;
                width = 0.55f;
                lifetime = 30f;
                knockback = -1f;
            }};

            weapons.add(
            new Weapon("spiroct-weapon"){{
                reload = 9f;
                x = 4f;
                y = 8f;
                rotate = true;
                bullet = sapper;
            }},
            new Weapon("spiroct-weapon"){{
                reload = 15f;
                x = 9f;
                y = 6f;
                rotate = true;
                bullet = sapper;
            }},
            new Weapon("spiroct-weapon"){{
                reload = 23f;
                x = 14f;
                y = 0f;
                rotate = true;
                bullet = sapper;
            }},
            new Weapon("large-purple-mount"){{
                y = -7f;
                x = 9f;
                shootY = 7f;
                reload = 45;
                shake = 3f;
                rotateSpeed = 2f;
                ejectEffect = Fx.shellEjectSmall;
                shootSound = Sounds.shootBig;
                rotate = true;
                occlusion = 8f;
                recoil = 3f;

                bullet = new ArtilleryBulletType(2f, 12){{
                    hitEffect = Fx.sapExplosion;
                    knockback = 0.8f;
                    lifetime = 70f;
                    width = height = 19f;
                    collidesTiles = true;
                    ammoMultiplier = 4f;
                    splashDamageRadius = 95f;
                    splashDamage = 65f;
                    backColor = Pal.sapBulletBack;
                    frontColor = lightningColor = Pal.sapBullet;
                    lightning = 3;
                    lightningLength = 10;
                    smokeEffect = Fx.shootBigSmoke2;
                    shake = 5f;

                    status = StatusEffects.sapped;
                    statusDuration = 60f * 10;
                }};
            }});
        }};

        toxopid = new UnitType("toxopid"){{
            drag = 0.1f;
            speed = 0.5f;
            hitsize = 21f;
            health = 23000;
            armor = 14f;

            rotateSpeed = 1.9f;

            legCount = 8;
            legMoveSpace = 0.8f;
            legPairOffset = 3;
            legLength = 75f;
            legExtension = -20;
            legBaseOffset = 8f;
            landShake = 1f;
            legSpeed = 0.1f;
            legLengthScl = 0.93f;
            rippleScale = 3f;
            legSpeed = 0.19f;

            legSplashDamage = 80;
            legSplashRange = 60;

            hovering = true;
            allowLegStep = true;
            visualElevation = 0.95f;
            groundLayer = Layer.legUnit;

            weapons.add(
            new Weapon("large-purple-mount"){{
                y = -5f;
                x = 11f;
                shootY = 7f;
                reload = 30;
                shake = 4f;
                rotateSpeed = 2f;
                ejectEffect = Fx.shellEjectSmall;
                shootSound = Sounds.shootBig;
                rotate = true;
                occlusion = 12f;
                recoil = 3f;
                shots = 2;
                spacing = 17f;

                bullet = new ShrapnelBulletType(){{
                    length = 90f;
                    damage = 110f;
                    width = 25f;
                    serrationLenScl = 7f;
                    serrationSpaceOffset = 60f;
                    serrationFadeOffset = 0f;
                    serrations = 10;
                    serrationWidth = 6f;
                    fromColor = Pal.sapBullet;
                    toColor = Pal.sapBulletBack;
                    shootEffect = smokeEffect = Fx.sparkShoot;
                }};
            }});

            weapons.add(new Weapon("toxopid-cannon"){{
                y = -14f;
                x = 0f;
                shootY = 22f;
                mirror = false;
                reload = 180;
                shake = 10f;
                recoil = 10f;
                rotateSpeed = 1f;
                ejectEffect = Fx.shellEjectBig;
                shootSound = Sounds.shootBig;
                rotate = true;
                occlusion = 30f;

                bullet = new ArtilleryBulletType(3f, 70){{
                    hitEffect = Fx.sapExplosion;
                    knockback = 0.8f;
                    lifetime = 80f;
                    width = height = 25f;
                    collidesTiles = collides = true;
                    ammoMultiplier = 4f;
                    splashDamageRadius = 95f;
                    splashDamage = 90f;
                    backColor = Pal.sapBulletBack;
                    frontColor = lightningColor = Pal.sapBullet;
                    lightning = 5;
                    lightningLength = 20;
                    smokeEffect = Fx.shootBigSmoke2;
                    hitShake = 10f;

                    status = StatusEffects.sapped;
                    statusDuration = 60f * 10;

                    fragLifeMin = 0.3f;
                    fragBullets = 9;

                    fragBullet = new ArtilleryBulletType(2.3f, 30){{
                        hitEffect = Fx.sapExplosion;
                        knockback = 0.8f;
                        lifetime = 90f;
                        width = height = 20f;
                        collidesTiles = false;
                        splashDamageRadius = 90f;
                        splashDamage = 45f;
                        backColor = Pal.sapBulletBack;
                        frontColor = lightningColor = Pal.sapBullet;
                        lightning = 2;
                        lightningLength = 5;
                        smokeEffect = Fx.shootBigSmoke2;
                        hitShake = 5f;

                        status = StatusEffects.sapped;
                        statusDuration = 60f * 10;
                    }};
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
            crashDamageMultiplier = 4f;

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
            health = 350;
            speed = 2f;
            accel = 0.08f;
            drag = 0.016f;
            flying = true;
            targetAir = false;
            engineOffset = 7.8f;
            range = 140f;
            faceTarget = false;
            armor = 4f;

            weapons.add(new Weapon(){{
                minShootVelocity = 0.75f;
                x = 3f;
                shootY = 0f;
                reload = 11f;
                shootCone = 180f;
                ejectEffect = Fx.none;
                inaccuracy = 15f;
                ignoreRotation = true;
                shootSound = Sounds.none;
                bullet = new BombBulletType(28f, 25f){{
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
            health = 700;
            speed = 1.7f;
            accel = 0.04f;
            drag = 0.016f;
            flying = true;
            range = 140f;
            hitsize = 18f;
            lowAltitude = true;
            armor = 5f;

            engineOffset = 12f;
            engineSize = 3f;

            weapons.add(new Weapon("zenith-missiles"){{
                reload = 40f;
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
            speed = 1.13f;
            accel = 0.035f;
            drag = 0.05f;
            rotateSpeed = 1.9f;
            flying = true;
            lowAltitude = true;
            health = 7000;
            armor = 9f;
            engineOffset = 21;
            engineSize = 5.3f;
            hitsize = 56f;

            BulletType missiles = new MissileBulletType(2.7f, 10){{
                width = 8f;
                height = 8f;
                shrinkY = 0f;
                drag = -0.01f;
                splashDamageRadius = 20f;
                splashDamage = 30f;
                ammoMultiplier = 4f;
                lifetime = 50f;
                hitEffect = Fx.blastExplosion;
                despawnEffect = Fx.blastExplosion;

                status = StatusEffects.blasted;
                statusDuration = 60f;
            }};

            weapons.add(
            new Weapon("missiles-mount"){{
                y = 8f;
                x = 17f;
                reload = 20f;
                ejectEffect = Fx.shellEjectSmall;
                rotateSpeed = 8f;
                bullet = missiles;
                shootSound = Sounds.shoot;
                rotate = true;
                occlusion = 6f;
            }},
            new Weapon("missiles-mount"){{
                y = -8f;
                x = 17f;
                reload = 35;
                rotateSpeed = 8f;
                ejectEffect = Fx.shellEjectSmall;
                bullet = missiles;
                shootSound = Sounds.shoot;
                rotate = true;
                occlusion = 6f;
            }},
            new Weapon("large-bullet-mount"){{
                y = 2f;
                x = 10f;
                shootY = 10f;
                reload = 12;
                shake = 1f;
                rotateSpeed = 2f;
                ejectEffect = Fx.shellEjectSmall;
                shootSound = Sounds.shootBig;
                rotate = true;
                occlusion = 8f;
                bullet = new BasicBulletType(7f, 50){{
                    width = 12f;
                    height = 18f;
                    lifetime = 25f;
                    shootEffect = Fx.shootBig;
                }};
            }}
            );
        }};

        eclipse = new UnitType("eclipse"){{
            speed = 1.09f;
            accel = 0.02f;
            drag = 0.05f;
            rotateSpeed = 1f;
            flying = true;
            lowAltitude = true;
            health = 20000;
            engineOffset = 38;
            engineSize = 7.3f;
            hitsize = 58f;
            destructibleWreck = false;
            armor = 13f;

            BulletType fragBullet = new FlakBulletType(4f, 5){{
                shootEffect = Fx.shootBig;
                ammoMultiplier = 4f;
                splashDamage = 42f;
                splashDamageRadius = 25f;
                collidesGround = true;
                lifetime = 38f;

                status = StatusEffects.blasted;
                statusDuration = 60f;
            }};

            weapons.add(
            new Weapon("large-laser-mount"){{
                shake = 4f;
                shootY = 9f;
                x = 18f;
                y = 5f;
                rotateSpeed = 2f;
                reload = 45f;
                recoil = 4f;
                shootSound = Sounds.laser;
                occlusion = 20f;
                rotate = true;

                bullet = new LaserBulletType(){{
                    damage = 90f;
                    sideAngle = 20f;
                    sideWidth = 1.5f;
                    sideLength = 80f;
                    width = 25f;
                    length = 200f;
                    shootEffect = Fx.shockwave;
                    colors = new Color[]{Color.valueOf("ec7458aa"), Color.valueOf("ff9c5a"), Color.white};
                }};
            }},
            new Weapon("large-artillery"){{
                x = 11f;
                y = 27f;
                rotateSpeed = 2f;
                reload = 9f;
                shootSound = Sounds.flame;
                occlusion = 7f;
                rotate = true;
                recoil = 0.5f;

                bullet = fragBullet;
            }},
            new Weapon("large-artillery"){{
                y = -13f;
                x = 20f;
                reload = 12f;
                ejectEffect = Fx.shellEjectSmall;
                rotateSpeed = 7f;
                shake = 1f;
                shootSound = Sounds.shoot;
                rotate = true;
                occlusion = 12f;
                bullet = fragBullet;
            }});
        }};

        //endregion
        //region air support

        mono = new UnitType("mono"){{
            defaultController = MinerAI::new;

            flying = true;
            drag = 0.06f;
            accel = 0.12f;
            speed = 1.1f;
            health = 100;
            engineSize = 1.8f;
            engineOffset = 5.7f;
            itemCapacity = 30;
            range = 50f;
            isCounted = false;

            mineTier = 1;
            mineSpeed = 2.5f;
        }};

        poly = new UnitType("poly"){{
            defaultController = BuilderAI::new;

            flying = true;
            drag = 0.05f;
            speed = 1.9f;
            rotateSpeed = 15f;
            accel = 0.1f;
            range = 70f;
            itemCapacity = 70;
            health = 400;
            buildSpeed = 0.5f;
            engineOffset = 6.5f;
            hitsize = 8f;
            lowAltitude = true;
            isCounted = false;

            mineTier = 2;
            mineSpeed = 3.5f;

            abilities.add(new HealFieldAbility(5f, 60f * 5, 50f));

            weapons.add(new Weapon("heal-weapon-mount"){{
                y = -2.5f;
                x = 3.5f;
                reload = 30f;
                ejectEffect = Fx.none;
                recoil = 2f;
                shootSound = Sounds.pew;
                shots = 1;
                velocityRnd = 0.5f;
                inaccuracy = 15f;
                alternate = true;

                bullet = new MissileBulletType(4f, 12){{
                    homingPower = 0.08f;
                    weaveMag = 4;
                    weaveScale = 4;
                    lifetime = 56f;
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
            payloadCapacity = 4 * (8 * 8);

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
                    tileDamageMultiplier = 0.09f;
                }};
            }});
        }};

        beta = new UnitType("beta"){{
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
                itemCapacity = 0;
            }

            @Override
            public boolean isHidden(){
                return true;
            }
        };

        //endregion
    }
}
