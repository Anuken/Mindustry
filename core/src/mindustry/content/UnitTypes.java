package mindustry.content;

import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.struct.*;
import mindustry.ai.types.*;
import mindustry.annotations.Annotations.*;
import mindustry.ctype.*;
import mindustry.entities.*;
import mindustry.entities.abilities.*;
import mindustry.entities.bullet.*;
import mindustry.entities.effect.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.*;
import mindustry.type.ammo.*;
import mindustry.type.weapons.*;
import mindustry.world.meta.*;

import static arc.graphics.g2d.Draw.*;
import static arc.graphics.g2d.Lines.*;
import static arc.math.Angles.*;
import static mindustry.Vars.*;

public class UnitTypes implements ContentList{
    //region definitions

    //mech
    public static @EntityDef({Unitc.class, Mechc.class}) UnitType mace, dagger, crawler, fortress, scepter, reign, vela;

    //mech, legacy
    public static @EntityDef(value = {Unitc.class, Mechc.class}, legacy = true) UnitType nova, pulsar, quasar;

    //legs
    public static @EntityDef({Unitc.class, Legsc.class}) UnitType corvus, atrax;

    //legs, legacy
    public static @EntityDef(value = {Unitc.class, Legsc.class}, legacy = true) UnitType spiroct, arkyid, toxopid;

    //air
    public static @EntityDef({Unitc.class}) UnitType flare, eclipse, horizon, zenith, antumbra;

    //air, legacy
    public static @EntityDef(value = {Unitc.class}, legacy = true) UnitType mono;

    //air, legacy
    public static @EntityDef(value = {Unitc.class}, legacy = true) UnitType poly;

    //air + payload
    public static @EntityDef({Unitc.class, Payloadc.class}) UnitType mega;

    //air + payload, legacy
    public static @EntityDef(value = {Unitc.class, Payloadc.class}, legacy = true) UnitType quad;

    //air + payload + legacy (different branch)
    public static @EntityDef(value = {Unitc.class, Payloadc.class}, legacy = true) UnitType oct;

    //air, legacy
    public static @EntityDef(value = {Unitc.class}, legacy = true) UnitType alpha, beta, gamma;

    //naval
    public static @EntityDef({Unitc.class, WaterMovec.class}) UnitType risso, minke, bryde, sei, omura, retusa, oxynoe, cyerce, aegires, navanax;

    //special block unit type
    public static @EntityDef({Unitc.class, BlockUnitc.class}) UnitType block;

    //endregion

    @Override
    public void load(){
        //region ground attack

        dagger = new UnitType("dagger"){{
            speed = 0.5f;
            hitSize = 8f;
            health = 150;
            weapons.add(new Weapon("large-weapon"){{
                reload = 13f;
                x = 4f;
                y = 2f;
                top = false;
                ejectEffect = Fx.casing1;
                bullet = Bullets.standardCopper;
            }});
        }};

        mace = new UnitType("mace"){{
            speed = 0.45f;
            hitSize = 10f;
            health = 540;
            armor = 4f;
            ammoType = new ItemAmmoType(Items.coal);

            immunities.add(StatusEffects.burning);

            weapons.add(new Weapon("flamethrower"){{
                top = false;
                shootSound = Sounds.flame;
                shootY = 2f;
                reload = 11f;
                recoil = 1f;
                ejectEffect = Fx.none;
                bullet = new BulletType(4.1f, 35f){{
                    ammoMultiplier = 3f;
                    hitSize = 7f;
                    lifetime = 13f;
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
            speed = 0.43f;
            hitSize = 13f;
            rotateSpeed = 3f;
            targetAir = false;
            health = 900;
            armor = 9f;
            mechFrontSway = 0.55f;
            ammoType = new ItemAmmoType(Items.graphite);

            weapons.add(new Weapon("artillery"){{
                top = false;
                y = 1f;
                x = 9f;
                reload = 60f;
                recoil = 4f;
                shake = 2f;
                ejectEffect = Fx.casing2;
                shootSound = Sounds.artillery;
                bullet = new ArtilleryBulletType(2f, 20, "shell"){{
                    hitEffect = Fx.blastExplosion;
                    knockback = 0.8f;
                    lifetime = 120f;
                    width = height = 14f;
                    collides = true;
                    collidesTiles = true;
                    splashDamageRadius = 35f;
                    splashDamage = 80f;
                    backColor = Pal.bulletYellowBack;
                    frontColor = Pal.bulletYellow;
                }};
            }});
        }};

        scepter = new UnitType("scepter"){{
            speed = 0.35f;
            hitSize = 22f;
            rotateSpeed = 2.1f;
            health = 9000;
            armor = 10f;
            canDrown = false;
            mechFrontSway = 1f;
            ammoType = new ItemAmmoType(Items.thorium);

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

                bullet = new BasicBulletType(7f, 45){{
                    width = 11f;
                    height = 20f;
                    lifetime = 25f;
                    shootEffect = Fx.shootBig;
                    lightning = 2;
                    lightningLength = 6;
                    lightningColor = Pal.surge;
                    //standard bullet damage is far too much for lightning
                    lightningDamage = 19;
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
            ammoType = new ItemAmmoType(Items.thorium);

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

                bullet = new BasicBulletType(13f, 65){{
                    pierce = true;
                    pierceCap = 10;
                    width = 14f;
                    height = 33f;
                    lifetime = 15f;
                    shootEffect = Fx.shootBig;
                    fragVelocityMin = 0.4f;

                    hitEffect = Fx.blastExplosion;
                    splashDamage = 16f;
                    splashDamageRadius = 13f;

                    fragBullets = 3;
                    fragLifeMin = 0f;
                    fragCone = 30f;

                    fragBullet = new BasicBulletType(9f, 18){{
                        width = 10f;
                        height = 10f;
                        pierce = true;
                        pierceBuilding = true;
                        pierceCap = 3;

                        lifetime = 20f;
                        hitEffect = Fx.flakExplosion;
                        splashDamage = 15f;
                        splashDamageRadius = 10f;
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
            ammoType = new PowerAmmoType(1000);

            weapons.add(new Weapon("heal-weapon"){{
                top = false;
                shootY = 2f;
                reload = 24f;
                x = 4.5f;
                alternate = false;
                ejectEffect = Fx.none;
                recoil = 2f;
                shootSound = Sounds.lasershoot;

                bullet = new LaserBoltBulletType(5.2f, 13){{
                    lifetime = 30f;
                    healPercent = 5f;
                    collidesTeam = true;
                    backColor = Pal.heal;
                    frontColor = Color.white;
                }};
            }});
        }};

        pulsar = new UnitType("pulsar"){{
            canBoost = true;
            boostMultiplier = 1.6f;
            speed = 0.7f;
            hitSize = 11f;
            health = 320f;
            buildSpeed = 0.9f;
            armor = 4f;
            riseSpeed = 0.07f;

            mineTier = 2;
            mineSpeed = 5f;
            commandLimit = 9;

            abilities.add(new ShieldRegenFieldAbility(20f, 40f, 60f * 5, 60f));
            ammoType = new PowerAmmoType(1300);

            weapons.add(new Weapon("heal-shotgun-weapon"){{
                top = false;
                x = 5f;
                shake = 2.2f;
                y = 0.5f;
                shootY = 2.5f;

                reload = 36f;
                shots = 3;
                inaccuracy = 35;
                shotDelay = 0.5f;
                spacing = 0f;
                ejectEffect = Fx.none;
                recoil = 2.5f;
                shootSound = Sounds.spark;

                bullet = new LightningBulletType(){{
                    lightningColor = hitColor = Pal.heal;
                    damage = 14f;
                    lightningLength = 7;
                    lightningLengthRand = 7;
                    shootEffect = Fx.shootHeal;
                    //Does not actually do anything; Just here to make stats work
                    healPercent = 2f;

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
            boostMultiplier = 2f;
            health = 640f;
            buildSpeed = 1.7f;
            canBoost = true;
            armor = 9f;
            landShake = 2f;
            riseSpeed = 0.05f;

            commandLimit = 10;
            mechFrontSway = 0.55f;
            ammoType = new PowerAmmoType(1500);

            speed = 0.4f;
            hitSize = 13f;

            mineSpeed = 6f;
            drawShields = false;

            abilities.add(new ForceFieldAbility(60f, 0.3f, 400f, 60f * 6));

            weapons.add(new Weapon("beam-weapon"){{
                top = false;
                shake = 2f;
                shootY = 4f;
                x = 6.5f;
                reload = 55f;
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
                    length = 135f;
                    colors = new Color[]{Pal.heal.cpy().a(0.4f), Pal.heal, Color.white};
                }};
            }});
        }};

        vela = new UnitType("vela"){{
            hitSize = 24f;

            rotateSpeed = 1.7f;
            canDrown = false;
            mechFrontSway = 1f;
            buildSpeed = 3f;

            mechStepParticles = true;
            mechStepShake = 0.15f;
            ammoType = new PowerAmmoType(2500);

            speed = 0.44f;
            boostMultiplier = 2.2f;
            engineOffset = 12f;
            engineSize = 6f;
            lowAltitude = true;
            riseSpeed = 0.02f;

            health = 8200f;
            armor = 9f;
            canBoost = true;
            landShake = 4f;
            immunities = ObjectSet.with(StatusEffects.burning);

            commandLimit = 8;

            weapons.add(new Weapon("vela-weapon"){{
                mirror = false;
                top = false;
                shake = 4f;
                shootY = 13f;
                x = y = 0f;

                firstShotDelay = Fx.greenLaserChargeSmall.lifetime - 1f;

                reload = 155f;
                recoil = 0f;
                chargeSound = Sounds.lasercharge2;
                shootSound = Sounds.beam;
                continuous = true;
                cooldownTime = 200f;

                bullet = new ContinuousLaserBulletType(){{
                    damage = 35f;
                    length = 180f;
                    hitEffect = Fx.hitMeltHeal;
                    drawSize = 420f;
                    lifetime = 160f;
                    shake = 1f;
                    despawnEffect = Fx.smokeCloud;
                    smokeEffect = Fx.none;

                    shootEffect = Fx.greenLaserChargeSmall;

                    incendChance = 0.1f;
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
            hitSize = 29f;
            health = 18000f;
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
            ammoType = new PowerAmmoType(4000);
            groundLayer = Layer.legUnit;

            speed = 0.3f;

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
            health = 200;
            mechSideSway = 0.25f;
            range = 40f;
            ammoType = new ItemAmmoType(Items.coal);

            weapons.add(new Weapon(){{
                reload = 24f;
                shootCone = 180f;
                ejectEffect = Fx.none;
                shootSound = Sounds.explosion;
                x = shootY = 0f;
                mirror = false;
                bullet = new BombBulletType(0f, 0f, "clear"){{
                    hitEffect = Fx.pulverize;
                    lifetime = 10f;
                    speed = 1f;
                    splashDamageRadius = 60f;
                    instantDisappear = true;
                    splashDamage = 90f;
                    killShooter = true;
                    hittable = false;
                    collidesAir = true;
                }};
            }});
        }};

        atrax = new UnitType("atrax"){{
            speed = 0.54f;
            drag = 0.4f;
            hitSize = 13f;
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
            ammoType = new ItemAmmoType(Items.coal);

            allowLegStep = true;
            visualElevation = 0.2f;
            groundLayer = Layer.legUnit - 1f;

            weapons.add(new Weapon("eruption"){{
                top = false;
                shootY = 3f;
                reload = 9f;
                ejectEffect = Fx.none;
                recoil = 1f;
                x = 7f;
                shootSound = Sounds.flame;

                bullet = new LiquidBulletType(Liquids.slag){{
                    damage = 11;
                    speed = 2.4f;
                    drag = 0.009f;
                    shootEffect = Fx.shootSmall;
                    lifetime = 57f;
                    collidesAir = false;
                }};
            }});
        }};

        spiroct = new UnitType("spiroct"){{
            speed = 0.48f;
            drag = 0.4f;
            hitSize = 15f;
            rotateSpeed = 3f;
            health = 910;
            immunities = ObjectSet.with(StatusEffects.burning, StatusEffects.melting);
            legCount = 6;
            legLength = 13f;
            legTrns = 0.8f;
            legMoveSpace = 1.4f;
            legBaseOffset = 2f;
            hovering = true;
            armor = 5f;
            ammoType = new PowerAmmoType(1000);

            buildSpeed = 0.75f;

            allowLegStep = true;
            visualElevation = 0.3f;
            groundLayer = Layer.legUnit;

            weapons.add(new Weapon("spiroct-weapon"){{
                shootY = 4f;
                reload = 14f;
                ejectEffect = Fx.none;
                recoil = 2f;
                rotate = true;
                shootSound = Sounds.sap;

                x = 8.5f;
                y = -1.5f;

                bullet = new SapBulletType(){{
                    sapStrength = 0.5f;
                    length = 75f;
                    damage = 20;
                    shootEffect = Fx.shootSmall;
                    hitColor = color = Color.valueOf("bf92f9");
                    despawnEffect = Fx.none;
                    width = 0.54f;
                    lifetime = 35f;
                    knockback = -1.24f;
                }};
            }});

            weapons.add(new Weapon("mount-purple-weapon"){{
                reload = 18f;
                rotate = true;
                x = 4f;
                y = 3f;
                shootSound = Sounds.sap;

                bullet = new SapBulletType(){{
                    sapStrength = 0.8f;
                    length = 40f;
                    damage = 16;
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
            speed = 0.6f;
            hitSize = 23f;
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
            legLengthScl = 0.96f;
            rippleScale = 2f;
            legSpeed = 0.2f;
            ammoType = new PowerAmmoType(2000);
            buildSpeed = 1f;

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
                reload = 14f;
                x = 9f;
                y = 6f;
                rotate = true;
                bullet = sapper;
                shootSound = Sounds.sap;
            }},
            new Weapon("spiroct-weapon"){{
                reload = 22f;
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
                shadow = 8f;
                recoil = 3f;

                bullet = new ArtilleryBulletType(2f, 12){{
                    hitEffect = Fx.sapExplosion;
                    knockback = 0.8f;
                    lifetime = 70f;
                    width = height = 19f;
                    collidesTiles = true;
                    ammoMultiplier = 4f;
                    splashDamageRadius = 70f;
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
            hitSize = 26f;
            health = 22000;
            armor = 13f;
            lightRadius = 140f;

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
            ammoType = new ItemAmmoType(Items.graphite, 8);
            buildSpeed = 1f;

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
                shadow = 12f;
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
                shadow = 30f;

                bullet = new ArtilleryBulletType(3f, 50){{
                    hitEffect = Fx.sapExplosion;
                    knockback = 0.8f;
                    lifetime = 80f;
                    width = height = 25f;
                    collidesTiles = collides = true;
                    ammoMultiplier = 4f;
                    splashDamageRadius = 80f;
                    splashDamage = 75f;
                    backColor = Pal.sapBulletBack;
                    frontColor = lightningColor = Pal.sapBullet;
                    lightning = 5;
                    lightningLength = 20;
                    smokeEffect = Fx.shootBigSmoke2;
                    hitShake = 10f;
                    lightRadius = 40f;
                    lightColor = Pal.sap;
                    lightOpacity = 0.6f;

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
                        splashDamageRadius = 70f;
                        splashDamage = 40f;
                        backColor = Pal.sapBulletBack;
                        frontColor = lightningColor = Pal.sapBullet;
                        lightning = 2;
                        lightningLength = 5;
                        smokeEffect = Fx.shootBigSmoke2;
                        hitShake = 5f;
                        lightRadius = 30f;
                        lightColor = Pal.sap;
                        lightOpacity = 0.5f;

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
            //as default AI, flares are not very useful in core rushes, they attack nothing in the way
            playerTargetFlags = new BlockFlag[]{null};
            targetFlags = new BlockFlag[]{BlockFlag.generator, null};
            commandLimit = 4;
            circleTarget = true;
            hitSize = 7;

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
            speed = 1.65f;
            accel = 0.08f;
            drag = 0.016f;
            flying = true;
            hitSize = 9f;
            targetAir = false;
            engineOffset = 7.8f;
            range = 140f;
            faceTarget = false;
            armor = 3f;
            //do not rush core, attack closest
            playerTargetFlags = new BlockFlag[]{null};
            targetFlags = new BlockFlag[]{BlockFlag.factory, null};
            commandLimit = 5;
            circleTarget = true;
            ammoType = new ItemAmmoType(Items.graphite);

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
            forceMultiTarget = true;
            armor = 5f;

            targetFlags = new BlockFlag[]{BlockFlag.launchPad, BlockFlag.storage, BlockFlag.battery, null};
            engineOffset = 12f;
            engineSize = 3f;
            ammoType = new ItemAmmoType(Items.graphite);

            weapons.add(new Weapon("zenith-missiles"){{
                reload = 40f;
                x = 7f;
                rotate = true;
                shake = 1f;
                shots = 2;
                inaccuracy = 5f;
                velocityRnd = 0.2f;
                shootSound = Sounds.missile;

                bullet = new MissileBulletType(3f, 14){{
                    width = 8f;
                    height = 8f;
                    shrinkY = 0f;
                    drag = -0.003f;
                    homingRange = 60f;
                    keepVelocity = false;
                    splashDamageRadius = 25f;
                    splashDamage = 15f;
                    lifetime = 50f;
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
            hitSize = 46f;
            targetFlags = new BlockFlag[]{BlockFlag.generator, BlockFlag.core, null};
            ammoType = new ItemAmmoType(Items.thorium);

            BulletType missiles = new MissileBulletType(2.7f, 14){{
                width = 8f;
                height = 8f;
                shrinkY = 0f;
                drag = -0.01f;
                splashDamageRadius = 20f;
                splashDamage = 34f;
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
                shootSound = Sounds.missile;
                rotate = true;
                shadow = 6f;
            }},
            new Weapon("missiles-mount"){{
                y = -8f;
                x = 17f;
                reload = 35;
                rotateSpeed = 8f;
                ejectEffect = Fx.casing1;
                bullet = missiles;
                shootSound = Sounds.missile;
                rotate = true;
                shadow = 6f;
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
                shadow = 8f;
                bullet = new BasicBulletType(7f, 55){{
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
            health = 21000;
            engineOffset = 38;
            engineSize = 7.3f;
            hitSize = 58f;
            destructibleWreck = false;
            armor = 13f;
            targetFlags = new BlockFlag[]{BlockFlag.reactor, BlockFlag.battery, BlockFlag.core, null};
            ammoType = new ItemAmmoType(Items.thorium);

            BulletType fragBullet = new FlakBulletType(4f, 5){{
                shootEffect = Fx.shootBig;
                ammoMultiplier = 4f;
                splashDamage = 60f;
                splashDamageRadius = 25f;
                collidesGround = true;
                lifetime = 47f;

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
                shadow = 20f;
                rotate = true;

                bullet = new LaserBulletType(){{
                    damage = 110f;
                    sideAngle = 20f;
                    sideWidth = 1.5f;
                    sideLength = 80f;
                    width = 25f;
                    length = 230f;
                    shootEffect = Fx.shockwave;
                    colors = new Color[]{Color.valueOf("ec7458aa"), Color.valueOf("ff9c5a"), Color.white};
                }};
            }},
            new Weapon("large-artillery"){{
                x = 11f;
                y = 27f;
                rotateSpeed = 2f;
                reload = 9f;
                shootSound = Sounds.shoot;
                shadow = 7f;
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
                shadow = 12f;
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

            ammoType = new PowerAmmoType(500);

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
            hitSize = 9f;
            lowAltitude = true;

            ammoType = new PowerAmmoType(900);

            mineTier = 2;
            mineSpeed = 3.5f;

            abilities.add(new RepairFieldAbility(5f, 60f * 8, 50f));

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
                    lifetime = 50f;
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
            mineSpeed = 4f;
            health = 460;
            armor = 3f;
            speed = 2.5f;
            accel = 0.06f;
            drag = 0.017f;
            lowAltitude = true;
            flying = true;
            engineOffset = 10.5f;
            rotateShooting = false;
            hitSize = 16.05f;
            engineSize = 3f;
            payloadCapacity = (2 * 2) * tilePayload;
            buildSpeed = 2.6f;
            isCounted = false;

            ammoType = new PowerAmmoType(1100);

            weapons.add(
            new Weapon("heal-weapon-mount"){{
                shootSound = Sounds.lasershoot;
                reload = 24f;
                x = 8f;
                y = -6f;
                rotate = true;
                bullet = new LaserBoltBulletType(5.2f, 10){{
                    lifetime = 35f;
                    healPercent = 5.5f;
                    collidesTeam = true;
                    backColor = Pal.heal;
                    frontColor = Color.white;
                }};
            }},
            new Weapon("heal-weapon-mount"){{
                shootSound = Sounds.lasershoot;
                reload = 15f;
                x = 4f;
                y = 5f;
                rotate = true;
                bullet = new LaserBoltBulletType(5.2f, 8){{
                    lifetime = 35f;
                    healPercent = 3f;
                    collidesTeam = true;
                    backColor = Pal.heal;
                    frontColor = Color.white;
                }};
            }});
        }};

        quad = new UnitType("quad"){{
            armor = 8f;
            health = 6000;
            speed = 1.3f;
            rotateSpeed = 2f;
            accel = 0.05f;
            drag = 0.017f;
            lowAltitude = false;
            flying = true;
            circleTarget = true;
            engineOffset = 12f;
            engineSize = 6f;
            rotateShooting = false;
            hitSize = 36f;
            payloadCapacity = (3 * 3) * tilePayload;
            buildSpeed = 2.5f;
            buildBeamOffset = 23;
            range = 140f;
            targetAir = false;
            targetFlags = new BlockFlag[]{BlockFlag.battery, BlockFlag.factory, null};

            ammoType = new PowerAmmoType(3000);

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

                    maxRange = 30f;
                    ignoreRotation = true;

                    backColor = Pal.heal;
                    frontColor = Color.white;
                    mixColorTo = Color.white;

                    hitSound = Sounds.plasmaboom;

                    shootCone = 180f;
                    ejectEffect = Fx.none;
                    hitShake = 4f;

                    collidesAir = false;

                    lifetime = 70f;

                    despawnEffect = Fx.greenBomb;
                    hitEffect = Fx.massiveExplosion;
                    keepVelocity = false;
                    spin = 2f;

                    shrinkX = shrinkY = 0.7f;

                    speed = 0f;
                    collides = false;

                    healPercent = 15f;
                    splashDamage = 220f;
                    splashDamageRadius = 80f;
                }};
            }});
        }};

        oct = new UnitType("oct"){{
            defaultController = DefenderAI::new;

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
            hitSize = 66f;
            payloadCapacity = (5.3f * 5.3f) * tilePayload;
            buildSpeed = 4f;
            drawShields = false;
            commandLimit = 6;
            lowAltitude = true;
            buildBeamOffset = 43;
            ammoCapacity = 1;

            abilities.add(new ForceFieldAbility(140f, 4f, 7000f, 60f * 8), new RepairFieldAbility(130f, 60f * 2, 140f));
        }};

        //endregion
        //region naval attack

        risso = new UnitType("risso"){{
            speed = 1.1f;
            drag = 0.13f;
            hitSize = 10f;
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
                reload = 23f;
                x = 0f;
                y = -5f;
                rotate = true;
                ejectEffect = Fx.casing1;
                shootSound = Sounds.missile;
                bullet = new MissileBulletType(2.7f, 12, "missile"){{
                    keepVelocity = true;
                    width = 8f;
                    height = 8f;
                    shrinkY = 0f;
                    drag = -0.003f;
                    homingRange = 60f;
                    splashDamageRadius = 25f;
                    splashDamage = 10f;
                    lifetime = 65f;
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
            hitSize = 13f;
            armor = 4f;
            accel = 0.3f;
            rotateSpeed = 2.6f;
            rotateShooting = false;
            ammoType = new ItemAmmoType(Items.graphite);

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
                shootSound = Sounds.shoot;
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
                shootSound = Sounds.bang;
                bullet = Bullets.artilleryDense;
            }});
        }};

        bryde = new UnitType("bryde"){{
            health = 910;
            speed = 0.85f;
            accel = 0.2f;
            rotateSpeed = 1.8f;
            drag = 0.17f;
            hitSize = 20f;
            armor = 7f;
            rotateShooting = false;
            ammoType = new ItemAmmoType(Items.graphite);

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
                shadow = 12f;

                shots = 1;
                inaccuracy = 3f;
                ejectEffect = Fx.casing3;
                shootSound = Sounds.artillery;

                bullet = new ArtilleryBulletType(3.2f, 12){{
                    trailMult = 0.8f;
                    hitEffect = Fx.massiveExplosion;
                    knockback = 1.5f;
                    lifetime = 80f;
                    height = 15.5f;
                    width = 15f;
                    collidesTiles = false;
                    ammoMultiplier = 4f;
                    splashDamageRadius = 40f;
                    splashDamage = 70f;
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

                shadow = 6f;

                rotateSpeed = 4f;
                rotate = true;
                shots = 2;
                shotDelay = 3f;
                inaccuracy = 5f;
                velocityRnd = 0.1f;
                shootSound = Sounds.missile;
                ammoType = new ItemAmmoType(Items.thorium);

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
                    lifetime = 70f;
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
            health = 11000;
            armor = 12f;

            speed = 0.73f;
            drag = 0.17f;
            hitSize = 39f;
            accel = 0.2f;
            rotateSpeed = 1.3f;
            rotateShooting = false;
            ammoType = new ItemAmmoType(Items.thorium);

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

                shadow = 20f;

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

                bullet = new MissileBulletType(4.2f, 42){{
                    homingPower = 0.12f;
                    width = 8f;
                    height = 8f;
                    shrinkX = shrinkY = 0f;
                    drag = -0.003f;
                    homingRange = 80f;
                    keepVelocity = false;
                    splashDamageRadius = 35f;
                    splashDamage = 45f;
                    lifetime = 62f;
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
                shadow = 12f;
                ejectEffect = Fx.casing3;
                shootSound = Sounds.shootBig;

                shots = 3;
                shotDelay = 4f;
                inaccuracy = 1f;
                bullet = new BasicBulletType(7f, 57){{
                    width = 13f;
                    height = 19f;
                    shootEffect = Fx.shootBig;
                    lifetime = 35f;
                }};
            }});
        }};

        omura = new UnitType("omura"){{
            health = 22000;
            speed = 0.62f;
            drag = 0.18f;
            hitSize = 58f;
            armor = 16f;
            accel = 0.19f;
            rotateSpeed = 0.9f;
            rotateShooting = false;
            ammoType = new PowerAmmoType(4000);

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
                shadow = 50f;
                shootSound = Sounds.railgun;

                shots = 1;
                ejectEffect = Fx.none;

                bullet = new RailBulletType(){{
                    shootEffect = Fx.railShoot;
                    length = 500;
                    updateEffectSeg = 60f;
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
        //region naval support
        retusa = new UnitType("retusa"){{
            defaultController = HugAI::new;
            speed = 0.9f;
            targetAir = false;
            drag = 0.14f;
            hitSize = 11f;
            health = 270;
            accel = 0.4f;
            rotateSpeed = 5f;
            trailLength = 20;
            trailX = 5f;
            trailScl = 1.3f;
            rotateShooting = false;
            range = 100f;
            ammoType = new PowerAmmoType(900);

            armor = 3f;

            buildSpeed = 1.5f;

            weapons.add(new RepairBeamWeapon("repair-beam-weapon-center"){{
                x = 0f;
                y = -5.5f;
                shootY = 6f;
                beamWidth = 0.8f;
                mirror = false;
                repairSpeed = 0.75f;

                bullet = new BulletType(){{
                    maxRange = 120f;
                }};
            }});

            weapons.add(new Weapon(){{
                mirror = false;
                reload = 80f;
                shots = 3;
                shotDelay = 7f;
                x = y = shootX = shootY = 0f;
                shootSound = Sounds.mineDeploy;

                bullet = new BasicBulletType(){{
                    sprite = "mine-bullet";
                    width = height = 11f;
                    layer = Layer.scorch;
                    shootEffect = smokeEffect = Fx.none;

                    maxRange = 50f;
                    ignoreRotation = true;
                    healPercent = 4f;

                    backColor = Pal.heal;
                    frontColor = Color.white;
                    mixColorTo = Color.white;

                    hitSound = Sounds.plasmaboom;

                    shootCone = 360f;
                    ejectEffect = Fx.none;
                    hitSize = 22f;

                    collidesAir = false;

                    lifetime = 500f;

                    hitEffect = new MultiEffect(Fx.blastExplosion, Fx.greenCloud);
                    keepVelocity = false;

                    shrinkX = shrinkY = 0f;

                    speed = 0f;

                    splashDamage = 55f;
                    splashDamageRadius = 45f;
                }};
            }});
        }};

        oxynoe = new UnitType("oxynoe"){{
            health = 560;
            speed = 0.83f;
            drag = 0.14f;
            hitSize = 14f;
            armor = 4f;
            accel = 0.4f;
            rotateSpeed = 4f;
            rotateShooting = false;

            trailLength = 22;
            trailX = 5.5f;
            trailY = -4f;
            trailScl = 1.9f;
            ammoType = new ItemAmmoType(Items.coal);

            buildSpeed = 2f;

            weapons.add(new Weapon("plasma-mount-weapon"){{

                reload = 5f;
                x = 4.5f;
                y = 6.5f;
                rotate = true;
                rotateSpeed = 5f;
                inaccuracy = 10f;
                ejectEffect = Fx.casing1;
                shootSound = Sounds.flame;
                shootCone = 30f;

                bullet = new BulletType(3.4f, 23f){{
                    healPercent = 1.5f;
                    collidesTeam = true;
                    ammoMultiplier = 3f;
                    hitSize = 7f;
                    lifetime = 18f;
                    pierce = true;
                    collidesAir = false;
                    statusDuration = 60f * 4;
                    hitEffect = Fx.hitFlamePlasma;
                    ejectEffect = Fx.none;
                    despawnEffect = Fx.none;
                    status = StatusEffects.burning;
                    keepVelocity = false;
                    hittable = false;
                    shootEffect = new Effect(32f, 80f, e -> {
                        color(Color.white, Pal.heal, Color.gray, e.fin());

                        randLenVectors(e.id, 8, e.finpow() * 60f, e.rotation, 10f, (x, y) -> {
                            Fill.circle(e.x + x, e.y + y, 0.65f + e.fout() * 1.5f);
                            Drawf.light(e.x + x, e.y + y, 16f * e.fout(), Pal.heal, 0.6f);
                        });
                    });
                }};
            }});

            weapons.add(new PointDefenseWeapon("point-defense-mount"){{
                mirror = false;
                x = 0f;
                y = 1f;
                reload = 9f;
                targetInterval = 10f;
                targetSwitchInterval = 15f;

                bullet = new BulletType(){{
                    shootEffect = Fx.sparkShoot;
                    hitEffect = Fx.pointHit;
                    maxRange = 100f;
                    damage = 15f;
                }};
            }});

        }};

        cyerce = new UnitType("cyerce"){{
            health = 870;
            speed = 0.86f;
            accel = 0.22f;
            rotateSpeed = 2.6f;
            drag = 0.16f;
            hitSize = 20f;
            armor = 6f;
            rotateShooting = false;
            ammoType = new ItemAmmoType(Items.graphite);

            trailLength = 23;
            trailX = 9f;
            trailY = -9f;
            trailScl = 2f;

            buildSpeed = 2f;

            weapons.add(new RepairBeamWeapon("repair-beam-weapon-center"){{
                x = 11f;
                y = -10f;
                shootY = 6f;
                beamWidth = 0.8f;
                repairSpeed = 0.7f;

                bullet = new BulletType(){{
                    maxRange = 130f;
                }};
            }});

            weapons.add(new Weapon("plasma-missile-mount"){{
                reload = 60f;
                x = 9f;
                y = 3f;

                shadow = 5f;

                rotateSpeed = 4f;
                rotate = true;
                inaccuracy = 1f;
                velocityRnd = 0.1f;
                shootSound = Sounds.missile;

                ejectEffect = Fx.none;
                bullet = new FlakBulletType(2.5f, 25){{
                    sprite = "missile-large";
                    //for targeting
                    collidesGround = collidesAir = true;
                    explodeRange = 40f;
                    width = height = 12f;
                    shrinkY = 0f;
                    drag = -0.003f;
                    homingRange = 60f;
                    keepVelocity = false;
                    lightRadius = 60f;
                    lightOpacity = 0.7f;
                    lightColor = Pal.heal;

                    splashDamageRadius = 30f;
                    splashDamage = 25f;

                    lifetime = 80f;
                    backColor = Pal.heal;
                    frontColor = Color.white;

                    hitEffect = new ExplosionEffect(){{
                        lifetime = 28f;
                        waveStroke = 6f;
                        waveLife = 10f;
                        waveRadBase = 7f;
                        waveColor = Pal.heal;
                        waveRad = 30f;
                        smokes = 6;
                        smokeColor = Color.white;
                        sparkColor = Pal.heal;
                        sparks = 6;
                        sparkRad = 35f;
                        sparkStroke = 1.5f;
                        sparkLen = 4f;
                    }};

                    weaveScale = 8f;
                    weaveMag = 1f;

                    trailColor = Pal.heal;
                    trailWidth = 4.5f;
                    trailLength = 29;

                    fragBullets = 7;
                    fragVelocityMin = 0.3f;

                    fragBullet = new MissileBulletType(3.9f, 11){{
                        homingPower = 0.2f;
                        weaveMag = 4;
                        weaveScale = 4;
                        lifetime = 60f;
                        shootEffect = Fx.shootHeal;
                        smokeEffect = Fx.hitLaser;
                        splashDamage = 13f;
                        splashDamageRadius = 20f;
                        frontColor = Color.white;
                        hitSound = Sounds.none;

                        lightColor = Pal.heal;
                        lightRadius = 40f;
                        lightOpacity = 0.7f;

                        trailColor = Pal.heal;
                        trailWidth = 2.5f;
                        trailLength = 20;
                        trailChance = -1f;

                        healPercent = 2.8f;
                        collidesTeam = true;
                        backColor = Pal.heal;

                        despawnEffect = Fx.none;
                        hitEffect = new ExplosionEffect(){{
                            lifetime = 20f;
                            waveStroke = 2f;
                            waveColor = Pal.heal;
                            waveRad = 12f;
                            smokeSize = 0f;
                            smokeSizeBase = 0f;
                            sparkColor = Pal.heal;
                            sparks = 9;
                            sparkRad = 35f;
                            sparkLen = 4f;
                            sparkStroke = 1.5f;
                        }};
                    }};
                }};
            }});
        }};

        aegires = new UnitType("aegires"){{
            health = 12000;
            armor = 12f;

            speed = 0.7f;
            drag = 0.17f;
            hitSize = 44f;
            accel = 0.2f;
            rotateSpeed = 1.4f;
            rotateShooting = false;
            ammoType = new PowerAmmoType(3500);
            ammoCapacity = 40;

            //clip size is massive due to energy field
            clipSize = 250f;

            trailLength = 50;
            trailX = 18f;
            trailY = -17f;
            trailScl = 3.2f;

            buildSpeed = 3f;

            abilities.add(new EnergyFieldAbility(35f, 65f, 180f){{
                statusDuration = 60f * 6f;
                maxTargets = 25;
            }});

            for(float mountY : new float[]{-18f, 14}){
                weapons.add(new PointDefenseWeapon("point-defense-mount"){{
                    x = 12.5f;
                    y = mountY;
                    reload = 6f;
                    targetInterval = 8f;
                    targetSwitchInterval = 8f;

                    bullet = new BulletType(){{
                        shootEffect = Fx.sparkShoot;
                        hitEffect = Fx.pointHit;
                        maxRange = 180f;
                        damage = 24f;
                    }};
                }});
            }
        }};

        navanax = new UnitType("navanax"){{
            health = 20000;
            speed = 0.65f;
            drag = 0.17f;
            hitSize = 58f;
            armor = 16f;
            accel = 0.2f;
            rotateSpeed = 1.1f;
            rotateShooting = false;
            ammoType = new PowerAmmoType(4500);

            trailLength = 70;
            trailX = 23f;
            trailY = -32f;
            trailScl = 3.5f;

            buildSpeed = 3.5f;

            for(float mountY : new float[]{-117/4f, 50/4f}){
                for(float sign : Mathf.signs){
                    weapons.add(new Weapon("plasma-laser-mount"){{
                        shadow = 20f;
                        controllable = false;
                        autoTarget = true;
                        mirror = false;
                        shake = 3f;
                        shootY = 7f;
                        rotate = true;
                        x = 84f/4f * sign;
                        y = mountY;

                        targetInterval = 20f;
                        targetSwitchInterval = 35f;

                        rotateSpeed = 3.5f;
                        reload = 170f;
                        recoil = 1f;
                        shootSound = Sounds.beam;
                        continuous = true;
                        cooldownTime = reload;
                        immunities.add(StatusEffects.burning);

                        bullet = new ContinuousLaserBulletType(){{
                            maxRange = 90f;
                            damage = 27f;
                            length = 95f;
                            hitEffect = Fx.hitMeltHeal;
                            drawSize = 200f;
                            lifetime = 155f;
                            shake = 1f;

                            shootEffect = Fx.shootHeal;
                            smokeEffect = Fx.none;
                            width = 4f;
                            largeHit = false;

                            incendChance = 0.03f;
                            incendSpread = 5f;
                            incendAmount = 1;

                            healPercent = 0.4f;
                            collidesTeam = true;

                            colors = new Color[]{Pal.heal.cpy().a(.2f), Pal.heal.cpy().a(.5f), Pal.heal.cpy().mul(1.2f), Color.white};
                        }};
                    }});
                }
            }

            weapons.add(new Weapon("emp-cannon-mount"){{
                rotate = true;

                x = 70f/4f;
                y = -26f/4f;

                reload = 65f;
                shake = 3f;
                rotateSpeed = 2f;
                shadow = 30f;
                shootY = 7f;
                recoil = 4f;
                cooldownTime = reload - 10f;
                //TODO better sound
                shootSound = Sounds.laser;

                bullet = new EmpBulletType(){{
                    float rad = 100f;

                    scaleVelocity = true;
                    lightOpacity = 0.7f;
                    unitDamageScl = 0.8f;
                    healPercent = 20f;
                    timeIncrease = 3f;
                    timeDuration = 60f * 20f;
                    powerDamageScl = 3f;
                    damage = 60;
                    hitColor = lightColor = Pal.heal;
                    lightRadius = 70f;
                    clipSize = 250f;
                    shootEffect = Fx.hitEmpSpark;
                    smokeEffect = Fx.shootBigSmoke2;
                    lifetime = 60f;
                    sprite = "circle-bullet";
                    backColor = Pal.heal;
                    frontColor = Color.white;
                    width = height = 12f;
                    speed = 5f;
                    trailLength = 20;
                    trailWidth = 6f;
                    trailColor = Pal.heal;
                    trailInterval = 3f;
                    splashDamage = 70f;
                    splashDamageRadius = rad;
                    hitShake = 4f;
                    trailRotation = true;
                    status = StatusEffects.electrified;
                    hitSound = Sounds.plasmaboom;

                    trailEffect = new Effect(16f, e -> {
                        color(Pal.heal);
                        for(int s : Mathf.signs){
                            Drawf.tri(e.x, e.y, 4f, 30f * e.fslope(), e.rotation + 90f*s);
                        }
                    });

                    hitEffect = new Effect(50f, 100f, e -> {
                        e.scaled(7f, b -> {
                            color(Pal.heal, b.fout());
                            Fill.circle(e.x, e.y, rad);
                        });

                        color(Pal.heal);
                        stroke(e.fout() * 3f);
                        Lines.circle(e.x, e.y, rad);

                        int points = 10;
                        float offset = Mathf.randomSeed(e.id, 360f);
                        for(int i = 0; i < points; i++){
                            float angle = i* 360f / points + offset;
                            //for(int s : Mathf.zeroOne){
                                Drawf.tri(e.x + Angles.trnsx(angle, rad), e.y + Angles.trnsy(angle, rad), 6f, 50f * e.fout(), angle/* + s*180f*/);
                            //}
                        }

                        Fill.circle(e.x, e.y, 12f * e.fout());
                        color();
                        Fill.circle(e.x, e.y, 6f * e.fout());
                        Drawf.light(e.x, e.y, rad * 1.6f, Pal.heal, e.fout());
                    });
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
                ejectEffect = Fx.casing1;

                bullet = new BasicBulletType(2.5f, 11){{
                    width = 7f;
                    height = 9f;
                    lifetime = 60f;
                    shootEffect = Fx.shootSmall;
                    smokeEffect = Fx.shootSmallSmoke;
                    buildingDamageMultiplier = 0.01f;
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
                ejectEffect = Fx.casing1;

                bullet = new BasicBulletType(3f, 11){{
                    width = 7f;
                    height = 9f;
                    lifetime = 60f;
                    shootEffect = Fx.shootSmall;
                    smokeEffect = Fx.shootSmallSmoke;
                    buildingDamageMultiplier = 0.01f;
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
                ejectEffect = Fx.casing1;

                bullet = new BasicBulletType(3.5f, 11){{
                    width = 6.5f;
                    height = 11f;
                    lifetime = 70f;
                    shootEffect = Fx.shootSmall;
                    smokeEffect = Fx.shootSmallSmoke;
                    buildingDamageMultiplier = 0.01f;
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
