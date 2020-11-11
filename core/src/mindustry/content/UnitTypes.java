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
import mindustry.world.meta.*;

import static mindustry.Vars.*;

public class UnitTypes implements ContentList{
    //region definitions

    //mech
    public static @EntityDef({Unitc.class, Mechc.class}) UnitType mace, dagger, crawler, fortress, scepter, reign;

    //mech + builder + miner
    public static @EntityDef({Unitc.class, Mechc.class, Builderc.class}) UnitType nova, pulsar, quasar;

    //mech
    public static @EntityDef({Unitc.class, Mechc.class}) UnitType vela;

    //legs
    public static @EntityDef({Unitc.class, Legsc.class}) UnitType corvus, atrax;

    //legs + building
    public static @EntityDef({Unitc.class, Legsc.class, Builderc.class}) UnitType spiroct, arkyid, toxopid;

    //air (no special traits)
    public static @EntityDef({Unitc.class}) UnitType flare, eclipse, horizon, zenith, antumbra;

    //air, legacy mining
    public static @EntityDef(value = {Unitc.class}, legacy = true) UnitType mono;

    //air + building + mining
    public static @EntityDef({Unitc.class, Builderc.class}) UnitType poly;

    //air + building + mining + payload
    public static @EntityDef({Unitc.class, Builderc.class, Payloadc.class}) UnitType mega;

    //air + building + payload
    public static @EntityDef(value = {Unitc.class, Builderc.class, Payloadc.class}, legacy = true) UnitType quad;

    //air + building + payload
    public static @EntityDef({Unitc.class, Builderc.class, Payloadc.class, AmmoDistributec.class}) UnitType oct;

    //air + building + mining
    public static @EntityDef({Unitc.class, Builderc.class}) UnitType alpha, beta, gamma;

    //water
    public static @EntityDef({Unitc.class, WaterMovec.class}) UnitType risso, minke, bryde, sei, omura;

    //special block unit type
    public static @EntityDef({Unitc.class, BlockUnitc.class}) UnitType block;

    //endregion

    @Override
    public void load(){
        //region ground attack

        dagger = new UnitType("dagger"){{
            speed = 0.5f;
            hitSize = 8f;
            health = 140;
            weapons.add(new Weapon("large-weapon"){{
                reload = 14f;
                x = 4f;
                y = 2f;
                top = false;
                ejectEffect = Fx.casing1;
                bullet = Bullets.standardCopper;
            }});
        }};

        mace = new UnitType("mace"){{
            speed = 0.4f;
            hitSize = 9f;
            health = 500;
            armor = 4f;

            immunities.add(StatusEffects.burning);

            weapons.add(new Weapon("flamethrower"){{
                top = false;
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
            hitSize = 13f;
            rotateSpeed = 3f;
            targetAir = false;
            health = 790;
            armor = 9f;
            mechFrontSway = 0.55f;

            weapons.add(new Weapon("artillery"){{
                top = false;
                y = 1f;
                x = 9f;
                reload = 60f;
                recoil = 4f;
                shake = 2f;
                ejectEffect = Fx.casing2;
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
            hitSize = 20f;
            rotateSpeed = 2.1f;
            health = 9000;
            armor = 11f;
            canDrown = false;
            mechFrontSway = 1f;

            mechStepParticles = true;
            mechStepShake = 0.15f;
            singleTarget = true;

            weapons.add(
            new Weapon("scepter-weapon"){{
                top = false;
                y = 1f;
                x = 16f;
                shootY = 8f;
                reload = 45f;
                recoil = 5f;
                shake = 2f;
                ejectEffect = Fx.casing3;
                shootSound = Sounds.bang;
                shots = 3;
                inaccuracy = 3f;
                shotDelay = 4f;

                bullet = new BasicBulletType(7f, 50){{
                    width = 11f;
                    height = 20f;
                    lifetime = 25f;
                    shootEffect = Fx.shootBig;
                    lightning = 2;
                    lightningLength = 6;
                    lightningColor = Pal.surge;
                    //standard bullet damage is far too much for lightning
                    lightningDamage = 30;
                }};
            }},

            new Weapon("mount-weapon"){{
                reload = 13f;
                x = 8.5f;
                y = 6f;
                rotate = true;
                ejectEffect = Fx.casing1;
                bullet = Bullets.standardCopper;
            }},
            new Weapon("mount-weapon"){{
                reload = 16f;
                x = 8.5f;
                y = -7f;
                rotate = true;
                ejectEffect = Fx.casing1;
                bullet = Bullets.standardCopper;
            }}

            );
        }};

        reign = new UnitType("reign"){{
            speed = 0.35f;
            hitSize = 26f;
            rotateSpeed = 1.65f;
            health = 24000;
            armor = 14f;
            mechStepParticles = true;
            mechStepShake = 0.75f;
            canDrown = false;
            mechFrontSway = 1.9f;
            mechSideSway = 0.6f;

            weapons.add(
            new Weapon("reign-weapon"){{
                top = false;
                y = 1f;
                x = 21.5f;
                shootY = 11f;
                reload = 9f;
                recoil = 5f;
                shake = 2f;
                ejectEffect = Fx.casing4;
                shootSound = Sounds.bang;

                bullet = new BasicBulletType(13f, 60){{
                    pierce = true;
                    pierceCap = 10;
                    width = 14f;
                    height = 33f;
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
                        pierceBuilding = true;
                        pierceCap = 3;

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
            canBoost = true;
            boostMultiplier = 1.5f;
            speed = 0.55f;
            hitSize = 8f;
            health = 120f;
            buildSpeed = 0.8f;
            armor = 1f;
            commandLimit = 8;

            abilities.add(new RepairFieldAbility(10f, 60f * 4, 60f));
            ammoType = AmmoTypes.power;

            weapons.add(new Weapon("heal-weapon"){{
                top = false;
                shootY = 2f;
                reload = 24f;
                x = 4.5f;
                alternate = false;
                ejectEffect = Fx.none;
                recoil = 2f;
                shootSound = Sounds.lasershoot;

                bullet = new LaserBoltBulletType(5.2f, 14){{
                    healPercent = 5f;
                    collidesTeam = true;
                    backColor = Pal.heal;
                    frontColor = Color.white;
                }};
                shootSound = Sounds.pew;
            }});
        }};

        pulsar = new UnitType("pulsar"){{
            canBoost = true;
            boostMultiplier = 1.6f;
            speed = 0.7f;
            hitSize = 10f;
            health = 320f;
            buildSpeed = 0.9f;
            armor = 4f;

            mineTier = 2;
            mineSpeed = 5f;
            commandLimit = 8;

            abilities.add(new ShieldRegenFieldAbility(20f, 40f, 60f * 5, 60f));
            ammoType = AmmoTypes.power;

            weapons.add(new Weapon("heal-shotgun-weapon"){{
                top = false;
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
                shootSound = Sounds.spark;

                bullet = new LightningBulletType(){{
                    lightningColor = hitColor = Pal.heal;
                    damage = 15f;
                    lightningLength = 7;
                    lightningLengthRand = 7;
                    shootEffect = Fx.shootHeal;

                    lightningType = new BulletType(0.0001f, 0f){{
                        lifetime = Fx.lightning.lifetime;
                        hitEffect = Fx.hitLancer;
                        despawnEffect = Fx.none;
                        status = StatusEffects.shocked;
                        statusDuration = 10f;
                        hittable = false;
                        healPercent = 2f;
                        collidesTeam = true;
                    }};
                }};
            }});
        }};

        quasar = new UnitType("quasar"){{
            mineTier = 3;
            hitSize = 12f;
            boostMultiplier = 2f;
            health = 650f;
            buildSpeed = 1.7f;
            canBoost = true;
            armor = 9f;
            landShake = 2f;

            commandLimit = 10;
            mechFrontSway = 0.55f;
            ammoType = AmmoTypes.power;

            speed = 0.4f;
            hitSize = 10f;

            mineSpeed = 6f;
            drawShields = false;

            abilities.add(new ForceFieldAbility(60f, 0.3f, 400f, 60f * 6));

            weapons.add(new Weapon("beam-weapon"){{
                top = false;
                shake = 2f;
                shootY = 4f;
                x = 6.5f;
                reload = 50f;
                recoil = 4f;
                shootSound = Sounds.laser;

                bullet = new LaserBulletType(){{
                    damage = 45f;
                    recoil = 1f;
                    sideAngle = 45f;
                    sideWidth = 1f;
                    sideLength = 70f;
                    healPercent = 10f;
                    collidesTeam = true;
                    colors = new Color[]{Pal.heal.cpy().a(0.4f), Pal.heal, Color.white};
                }};
            }});
        }};

        vela = new UnitType("vela"){{
            hitSize = 23f;

            rotateSpeed = 1.6f;
            canDrown = false;
            mechFrontSway = 1f;

            mechStepParticles = true;
            mechStepShake = 0.15f;
            ammoType = AmmoTypes.powerHigh;

            speed = 0.35f;
            boostMultiplier = 2.1f;
            engineOffset = 12f;
            engineSize = 6f;
            lowAltitude = true;

            health = 7000f;
            armor = 7f;
            canBoost = true;
            landShake = 4f;

            commandLimit = 8;

            weapons.add(new Weapon("vela-weapon"){{
                mirror = false;
                top = false;
                shake = 4f;
                shootY = 13f;
                x = y = 0f;

                firstShotDelay = Fx.greenLaserChargeSmall.lifetime - 1f;

                reload = 320f;
                recoil = 0f;
                chargeSound = Sounds.lasercharge2;
                shootSound = Sounds.beam;
                continuous = true;
                cooldownTime = 200f;

                bullet = new ContinuousLaserBulletType(){{
                    damage = 23f;
                    length = 160f;
                    hitEffect = Fx.hitMeltHeal;
                    drawSize = 420f;
                    lifetime = 160f;
                    shake = 1f;
                    despawnEffect = Fx.smokeCloud;
                    smokeEffect = Fx.none;

                    shootEffect = Fx.greenLaserChargeSmall;

                    incendChance = 0.05f;
                    incendSpread = 5f;
                    incendAmount = 1;

                    //constant healing
                    healPercent = 1f;
                    collidesTeam = true;

                    colors = new Color[]{Pal.heal.cpy().a(.2f), Pal.heal.cpy().a(.5f), Pal.heal.cpy().mul(1.2f), Color.white};
                }};

                shootStatus = StatusEffects.slow;
                shootStatusDuration = bullet.lifetime + firstShotDelay;
            }});
        }};

        corvus = new UnitType("corvus"){{
            mineTier = 1;
            hitSize = 29f;
            health = 18000f;
            buildSpeed = 1.7f;
            armor = 9f;
            landShake = 1.5f;
            rotateSpeed = 1.5f;

            commandLimit = 8;

            legCount = 4;
            legLength = 14f;
            legBaseOffset = 11f;
            legMoveSpace = 1.5f;
            legTrns = 0.58f;
            hovering = true;
            visualElevation = 0.2f;
            allowLegStep = true;
            ammoType = AmmoTypes.powerHigh;
            groundLayer = Layer.legUnit;

            speed = 0.3f;

            mineTier = 2;
            mineSpeed = 7f;
            drawShields = false;

            weapons.add(new Weapon("corvus-weapon"){{
                shootSound = Sounds.laserblast;
                chargeSound = Sounds.lasercharge;
                soundPitchMin = 1f;
                top = false;
                mirror = false;
                shake = 14f;
                shootY = 5f;
                x = y = 0;
                reload = 350f;
                recoil = 0f;

                cooldownTime = 350f;

                shootStatusDuration = 60f * 2f;
                shootStatus = StatusEffects.unmoving;
                firstShotDelay = Fx.greenLaserCharge.lifetime;

                bullet = new LaserBulletType(){{
                    length = 460f;
                    damage = 560f;
                    width = 75f;

                    lifetime = 65f;

                    lightningSpacing = 35f;
                    lightningLength = 5;
                    lightningDelay = 1.1f;
                    lightningLengthRand = 15;
                    lightningDamage = 50;
                    lightningAngleRand = 40f;
                    largeHit = true;
                    lightColor = lightningColor = Pal.heal;

                    shootEffect = Fx.greenLaserCharge;

                    healPercent = 25f;
                    collidesTeam = true;

                    sideAngle = 15f;
                    sideWidth = 0f;
                    sideLength = 0f;
                    colors = new Color[]{Pal.heal.cpy().a(0.4f), Pal.heal, Color.white};
                }};
            }});
        }};

        //endregion
        //region ground legs

        crawler = new UnitType("crawler"){{
            defaultController = SuicideAI::new;

            speed = 1f;
            hitSize = 8f;
            health = 180;
            mechSideSway = 0.25f;
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
                    splashDamageRadius = 70f;
                    instantDisappear = true;
                    splashDamage = 80f;
                    killShooter = true;
                    hittable = false;
                    collidesAir = true;
                }};
            }});
        }};

        atrax = new UnitType("atrax"){{
            speed = 0.5f;
            drag = 0.4f;
            hitSize = 10f;
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
                top = false;
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
            speed = 0.4f;
            drag = 0.4f;
            hitSize = 12f;
            rotateSpeed = 3f;
            health = 800;
            immunities = ObjectSet.with(StatusEffects.burning, StatusEffects.melting);
            legCount = 6;
            legLength = 13f;
            legTrns = 0.8f;
            legMoveSpace = 1.4f;
            legBaseOffset = 2f;
            hovering = true;
            armor = 5f;
            ammoType = AmmoTypes.power;

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
                shootSound = Sounds.sap;

                x = 8.5f;
                y = -1.5f;

                bullet = new SapBulletType(){{
                    sapStrength = 0.4f;
                    length = 75f;
                    damage = 18;
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
                shootSound = Sounds.sap;

                bullet = new SapBulletType(){{
                    sapStrength = 0.8f;
                    length = 40f;
                    damage = 15;
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
            hitSize = 21f;
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
            ammoType = AmmoTypes.power;

            legSplashDamage = 32;
            legSplashRange = 30;

            hovering = true;
            allowLegStep = true;
            visualElevation = 0.65f;
            groundLayer = Layer.legUnit;

            BulletType sapper = new SapBulletType(){{
                sapStrength = 0.85f;
                length = 55f;
                damage = 37;
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
                shootSound = Sounds.sap;
            }},
            new Weapon("spiroct-weapon"){{
                reload = 15f;
                x = 9f;
                y = 6f;
                rotate = true;
                bullet = sapper;
                shootSound = Sounds.sap;
            }},
            new Weapon("spiroct-weapon"){{
                reload = 23f;
                x = 14f;
                y = 0f;
                rotate = true;
                bullet = sapper;
                shootSound = Sounds.sap;
            }},
            new Weapon("large-purple-mount"){{
                y = -7f;
                x = 9f;
                shootY = 7f;
                reload = 45;
                shake = 3f;
                rotateSpeed = 2f;
                ejectEffect = Fx.casing1;
                shootSound = Sounds.artillery;
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
            hitSize = 21f;
            health = 22000;
            armor = 13f;

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
            ammoType = AmmoTypes.powerHigh;

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
                ejectEffect = Fx.casing1;
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
                reload = 210;
                shake = 10f;
                recoil = 10f;
                rotateSpeed = 1f;
                ejectEffect = Fx.casing3;
                shootSound = Sounds.artillery;
                rotate = true;
                occlusion = 30f;

                bullet = new ArtilleryBulletType(3f, 50){{
                    hitEffect = Fx.sapExplosion;
                    knockback = 0.8f;
                    lifetime = 80f;
                    width = height = 25f;
                    collidesTiles = collides = true;
                    ammoMultiplier = 4f;
                    splashDamageRadius = 90f;
                    splashDamage = 75f;
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
                        splashDamageRadius = 80f;
                        splashDamage = 40f;
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
            engineOffset = 5.5f;
            range = 140f;
            targetAir = false;
            commandLimit = 4;

            weapons.add(new Weapon(){{
                y = 0f;
                x = 2f;
                reload = 13f;
                ejectEffect = Fx.casing1;
                bullet = new BasicBulletType(2.5f, 9){{
                    width = 7f;
                    height = 9f;
                    lifetime = 45f;
                    shootEffect = Fx.shootSmall;
                    smokeEffect = Fx.shootSmallSmoke;
                    ammoMultiplier = 2;
                }};
                shootSound = Sounds.pew;
            }});
        }};

        horizon = new UnitType("horizon"){{
            health = 340;
            speed = 1.7f;
            accel = 0.08f;
            drag = 0.016f;
            flying = true;
            hitSize = 9f;
            targetAir = false;
            engineOffset = 7.8f;
            range = 140f;
            faceTarget = false;
            armor = 4f;
            targetFlag = BlockFlag.factory;
            commandLimit = 5;

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
                bullet = new BombBulletType(27f, 25f){{
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
            hitSize = 20f;
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
            speed = 0.8f;
            accel = 0.04f;
            drag = 0.04f;
            rotateSpeed = 1.9f;
            flying = true;
            lowAltitude = true;
            health = 7000;
            armor = 9f;
            engineOffset = 21;
            engineSize = 5.3f;
            hitSize = 56f;
            targetFlag = BlockFlag.battery;

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
                ejectEffect = Fx.casing1;
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
                ejectEffect = Fx.casing1;
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
                ejectEffect = Fx.casing1;
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
            speed = 0.52f;
            accel = 0.04f;
            drag = 0.04f;
            rotateSpeed = 1f;
            flying = true;
            lowAltitude = true;
            health = 20000;
            engineOffset = 38;
            engineSize = 7.3f;
            hitSize = 58f;
            destructibleWreck = false;
            armor = 13f;
            targetFlag = BlockFlag.reactor;

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
                ejectEffect = Fx.casing1;
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
            speed = 1.5f;
            health = 100;
            engineSize = 1.8f;
            engineOffset = 5.7f;
            range = 50f;
            isCounted = false;

            ammoType = AmmoTypes.powerLow;

            mineTier = 1;
            mineSpeed = 2.5f;
        }};

        poly = new UnitType("poly"){{
            defaultController = BuilderAI::new;

            flying = true;
            drag = 0.05f;
            speed = 2.6f;
            rotateSpeed = 15f;
            accel = 0.1f;
            range = 130f;
            health = 400;
            buildSpeed = 0.5f;
            engineOffset = 6.5f;
            hitSize = 8f;
            lowAltitude = true;

            ammoType = AmmoTypes.power;

            mineTier = 2;
            mineSpeed = 3.5f;

            abilities.add(new RepairFieldAbility(5f, 60f * 5, 50f));

            weapons.add(new Weapon("heal-weapon-mount"){{
                top = false;
                y = -2.5f;
                x = 3.5f;
                reload = 30f;
                ejectEffect = Fx.none;
                recoil = 2f;
                shootSound = Sounds.missile;
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
                    hitEffect = despawnEffect = Fx.hitLaser;
                    frontColor = Color.white;
                    hitSound = Sounds.none;

                    healPercent = 5.5f;
                    collidesTeam = true;
                    backColor = Pal.heal;
                    trailColor = Pal.heal;
                }};
            }});
        }};

        mega = new UnitType("mega"){{
            defaultController = RepairAI::new;

            mineTier = 3;
            health = 500;
            armor = 5f;
            speed = 2.4f;
            accel = 0.06f;
            drag = 0.017f;
            lowAltitude = true;
            flying = true;
            engineOffset = 10.5f;
            rotateShooting = false;
            hitSize = 15f;
            engineSize = 3f;
            payloadCapacity = (2 * 2) * tilePayload;
            buildSpeed = 2.5f;

            ammoType = AmmoTypes.power;

            weapons.add(
            new Weapon("heal-weapon-mount"){{
                shootSound = Sounds.lasershoot;
                reload = 25f;
                x = 8f;
                y = -6f;
                rotate = true;
                bullet = Bullets.healBulletBig;
            }},
            new Weapon("heal-weapon-mount"){{
                shootSound = Sounds.lasershoot;
                reload = 15f;
                x = 4f;
                y = 5f;
                rotate = true;
                bullet = Bullets.healBullet;
            }});
        }};

        quad = new UnitType("quad"){{
            armor = 8f;
            health = 6000;
            speed = 1.4f;
            rotateSpeed = 2f;
            accel = 0.05f;
            drag = 0.017f;
            lowAltitude = false;
            flying = true;
            engineOffset = 12f;
            engineSize = 6f;
            rotateShooting = false;
            hitSize = 32f;
            payloadCapacity = (3 * 3) * tilePayload;
            buildSpeed = 2.5f;
            range = 140f;
            targetAir = false;
            targetFlag = BlockFlag.battery;

            ammoType = AmmoTypes.powerHigh;

            weapons.add(
            new Weapon(){{
                x = y = 0f;
                mirror = false;
                reload = 55f;
                minShootVelocity = 0.01f;

                soundPitchMin = 1f;
                shootSound = Sounds.plasmadrop;

                bullet = new BasicBulletType(){{
                    sprite = "large-bomb";
                    width = height = 120/4f;

                    range = 30f;
                    ignoreRotation = true;

                    backColor = Pal.heal;
                    frontColor = Color.white;
                    mixColorTo = Color.white;

                    hitSound = Sounds.plasmaboom;

                    shootCone = 180f;
                    ejectEffect = Fx.none;
                    despawnShake = 4f;

                    collidesAir = false;

                    lifetime = 70f;

                    despawnEffect = Fx.greenBomb;
                    hitEffect = Fx.massiveExplosion;
                    keepVelocity = false;
                    spin = 2f;

                    shrinkX = shrinkY = 0.7f;

                    speed = 0.001f;
                    collides = false;

                    healPercent = 15f;
                    splashDamage = 240f;
                    splashDamageRadius = 120f;
                }};
            }});
        }};

        oct = new UnitType("oct"){{
            armor = 16f;
            health = 24000;
            speed = 0.8f;
            rotateSpeed = 1f;
            accel = 0.04f;
            drag = 0.018f;
            flying = true;
            engineOffset = 46f;
            engineSize = 7.8f;
            rotateShooting = false;
            hitSize = 60f;
            payloadCapacity = (5.3f * 5.3f) * tilePayload;
            buildSpeed = 4f;
            drawShields = false;
            commandLimit = 6;
            lowAltitude = true;

            ammoCapacity = 1300;
            ammoResupplyAmount = 20;

            abilities.add(new ForceFieldAbility(140f, 4f, 7000f, 60f * 8), new RepairFieldAbility(130f, 60f * 2, 140f));
        }};

        //endregion
        //region naval attack

        risso = new UnitType("risso"){{
            speed = 1.1f;
            drag = 0.13f;
            hitSize = 9f;
            health = 280;
            accel = 0.4f;
            rotateSpeed = 3.3f;
            trailLength = 20;
            rotateShooting = false;

            armor = 2f;

            weapons.add(new Weapon("mount-weapon"){{
                reload = 12f;
                x = 4f;
                shootY = 4f;
                y = 1.5f;
                rotate = true;
                ejectEffect = Fx.casing1;
                bullet = Bullets.standardCopper;
            }});

            weapons.add(new Weapon("missiles-mount"){{
                mirror = false;
                reload = 20f;
                x = 0f;
                y = -5f;
                rotate = true;
                ejectEffect = Fx.casing1;
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
            hitSize = 11f;
            armor = 4f;
            accel = 0.3f;
            rotateSpeed = 2.6f;
            rotateShooting = false;

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
                ejectEffect = Fx.casing1;
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
                ejectEffect = Fx.casing2;
                bullet = Bullets.artilleryIncendiary;
            }});
        }};

        bryde = new UnitType("bryde"){{
            health = 900;
            speed = 0.85f;
            accel = 0.2f;
            rotateSpeed = 1.8f;
            drag = 0.17f;
            hitSize = 16f;
            armor = 7f;
            rotateShooting = false;

            trailLength = 22;
            trailX = 7f;
            trailY = -9f;
            trailScl = 1.5f;

            abilities.add(new ShieldRegenFieldAbility(20f, 40f, 60f * 4, 60f));

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
                occlusion = 12f;

                shots = 1;
                inaccuracy = 3f;
                ejectEffect = Fx.casing3;

                bullet = new ArtilleryBulletType(3.2f, 12){{
                    trailMult = 0.8f;
                    hitEffect = Fx.massiveExplosion;
                    knockback = 1.5f;
                    lifetime = 100f;
                    height = 15.5f;
                    width = 15f;
                    collidesTiles = false;
                    ammoMultiplier = 4f;
                    splashDamageRadius = 60f;
                    splashDamage = 80f;
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

        sei = new UnitType("sei"){{
            health = 10000;
            armor = 12f;

            speed = 0.73f;
            drag = 0.17f;
            hitSize = 39f;
            accel = 0.2f;
            rotateSpeed = 1.3f;
            rotateShooting = false;

            trailLength = 50;
            trailX = 18f;
            trailY = -21f;
            trailScl = 3f;

            weapons.add(new Weapon("sei-launcher"){{

                x = 0f;
                y = 0f;
                rotate = true;
                rotateSpeed = 4f;
                mirror = false;

                occlusion = 20f;

                shootY = 2f;
                recoil = 4f;
                reload = 45f;
                shots = 6;
                spacing = 10f;
                velocityRnd = 0.4f;
                inaccuracy = 7f;
                ejectEffect = Fx.none;
                shake = 3f;
                shootSound = Sounds.missile;
                xRand = 8f;
                shotDelay = 1f;

                bullet = new MissileBulletType(4.2f, 40){{
                    homingPower = 0.12f;
                    width = 8f;
                    height = 8f;
                    shrinkX = shrinkY = 0f;
                    drag = -0.003f;
                    homingRange = 80f;
                    keepVelocity = false;
                    splashDamageRadius = 35f;
                    splashDamage = 45f;
                    lifetime = 56f;
                    trailColor = Pal.bulletYellowBack;
                    backColor = Pal.bulletYellowBack;
                    frontColor = Pal.bulletYellow;
                    hitEffect = Fx.blastExplosion;
                    despawnEffect = Fx.blastExplosion;
                    weaveScale = 8f;
                    weaveMag = 2f;
                }};
            }});

            weapons.add(new Weapon("large-bullet-mount"){{
                reload = 60f;
                cooldownTime = 90f;
                x = 70f/4f;
                y = -66f/4f;
                rotateSpeed = 4f;
                rotate = true;
                shootY = 7f;
                shake = 2f;
                recoil = 3f;
                occlusion = 12f;
                ejectEffect = Fx.casing3;

                shots = 3;
                shotDelay = 4f;
                inaccuracy = 1f;
                bullet = new BasicBulletType(7f, 55){{
                    width = 13f;
                    height = 19f;
                    shootEffect = Fx.shootBig;
                    lifetime = 30f;
                }};
            }});
        }};

        omura = new UnitType("omura"){{
            health = 22000;
            speed = 0.62f;
            drag = 0.18f;
            hitSize = 50f;
            armor = 16f;
            accel = 0.19f;
            rotateSpeed = 0.9f;
            rotateShooting = false;

            float spawnTime = 60f * 15f;

            abilities.add(new UnitSpawnAbility(flare, spawnTime, 19.25f, -31.75f), new UnitSpawnAbility(flare, spawnTime, -19.25f, -31.75f));

            trailLength = 70;
            trailX = 23f;
            trailY = -32f;
            trailScl = 3.5f;

            weapons.add(new Weapon("omura-cannon"){{
                reload = 110f;
                cooldownTime = 90f;
                mirror = false;
                x = 0f;
                y = -3.5f;
                rotateSpeed = 1.4f;
                rotate = true;
                shootY = 23f;
                shake = 6f;
                recoil = 10.5f;
                occlusion = 50f;
                shootSound = Sounds.railgun;

                shots = 1;
                ejectEffect = Fx.none;

                bullet = new RailBulletType(){{
                    shootEffect = Fx.railShoot;
                    speed = 67f;
                    lifetime = 8f;
                    pierceEffect = Fx.railHit;
                    updateEffect = Fx.railTrail;
                    hitEffect = Fx.massiveExplosion;
                    smokeEffect = Fx.shootBig2;
                    damage = 1250;
                    pierceDamageFactor = 0.5f;
                }};
            }});
        }};

        //endregion
        //region core

        alpha = new UnitType("alpha"){{
            defaultController = BuilderAI::new;
            isCounted = false;

            flying = true;
            mineSpeed = 6.5f;
            mineTier = 1;
            buildSpeed = 0.5f;
            drag = 0.05f;
            speed = 3f;
            rotateSpeed = 15f;
            accel = 0.1f;
            itemCapacity = 30;
            health = 150f;
            engineOffset = 6f;
            hitSize = 8f;
            commandLimit = 3;
            alwaysUnlocked = true;

            weapons.add(new Weapon("small-basic-weapon"){{
                reload = 17f;
                x = 2.75f;
                y = 1f;
                top = false;

                bullet = new BasicBulletType(2.5f, 10){{
                    width = 7f;
                    height = 9f;
                    lifetime = 60f;
                    shootEffect = Fx.shootSmall;
                    smokeEffect = Fx.shootSmallSmoke;
                    tileDamageMultiplier = 0.03f;
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
            speed = 3.3f;
            rotateSpeed = 17f;
            accel = 0.1f;
            itemCapacity = 50;
            health = 170f;
            engineOffset = 6f;
            hitSize = 9f;
            rotateShooting = false;
            lowAltitude = true;
            commandLimit = 4;

            weapons.add(new Weapon("small-mount-weapon"){{
                top = false;
                reload = 20f;
                x = 3f;
                y = 0.5f;
                rotate = true;
                shots = 2;
                shotDelay = 4f;
                spacing = 0f;

                bullet = new BasicBulletType(3f, 10){{
                    width = 7f;
                    height = 9f;
                    lifetime = 60f;
                    shootEffect = Fx.shootSmall;
                    smokeEffect = Fx.shootSmallSmoke;
                    tileDamageMultiplier = 0.03f;
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
            speed = 3.55f;
            rotateSpeed = 19f;
            accel = 0.11f;
            itemCapacity = 70;
            health = 220f;
            engineOffset = 6f;
            hitSize = 11f;
            commandLimit = 5;

            weapons.add(new Weapon("small-mount-weapon"){{
                top = false;
                reload = 15f;
                x = 1f;
                y = 2f;
                shots = 2;
                spacing = 2f;
                inaccuracy = 3f;
                shotDelay = 3f;

                bullet = new BasicBulletType(3.5f, 10){{
                    width = 6.5f;
                    height = 11f;
                    lifetime = 70f;
                    shootEffect = Fx.shootSmall;
                    smokeEffect = Fx.shootSmallSmoke;
                    tileDamageMultiplier = 0.03f;
                    homingPower = 0.04f;
                }};
            }});
        }};

        //endregion
        //region internal

        block = new UnitType("block"){
            {
                speed = 0f;
                hitSize = 0f;
                health = 1;
                rotateSpeed = 360f;
                itemCapacity = 0;
                commandLimit = 0;
            }

            @Override
            public boolean isHidden(){
                return true;
            }
        };

        //endregion
    }
}
