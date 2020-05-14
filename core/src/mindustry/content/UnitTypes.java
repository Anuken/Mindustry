package mindustry.content;

import arc.struct.*;
import mindustry.annotations.Annotations.*;
import mindustry.ctype.*;
import mindustry.gen.*;
import mindustry.type.*;

public class UnitTypes implements ContentList{

    //ground
    public static @EntityDef({Unitc.class, Legsc.class}) UnitType titan, dagger, crawler, fortress, eruptor, chaosArray, eradicator;

    //air
    public static @EntityDef({Unitc.class}) UnitType wraith, reaper, ghoul, revenant, lich;

    //mining
    public static @EntityDef({Unitc.class, Minerc.class}) UnitType draug;

    //building
    public static @EntityDef({Unitc.class, Builderc.class}) UnitType phantom, spirit;

    //water
    public static @EntityDef({Unitc.class, WaterMovec.class, Commanderc.class}) UnitType vanguard;

    @Override
    public void load(){

        dagger = new UnitType("dagger"){{
            speed = 0.5f;
            drag = 0.3f;
            hitsize = 8f;
            mass = 1.75f;
            health = 130;
            weapons.add(new Weapon("chain-blaster"){{
                reload = 14f;
                x = 4f;
                alternate = true;
                ejectEffect = Fx.shellEjectSmall;
                bullet = Bullets.standardCopper;
            }});
        }};

        titan = new UnitType("titan"){{
            speed = 0.4f;
            drag = 0.3f;
            mass = 3.5f;
            hitsize = 9f;
            range = 10f;
            health = 460;

            immunities.add(StatusEffects.burning);

            weapons.add(new Weapon("flamethrower"){{
                shootSound = Sounds.flame;
                shootY = 2f;
                reload = 14f;
                alternate = true;
                recoil = 1f;
                ejectEffect = Fx.none;
                bullet = Bullets.basicFlame;
            }});

        }};

        wraith = new UnitType("wraith"){{
            speed = 3f;
            accel = 0.08f;
            drag = 0.01f;
            mass = 1.5f;
            flying = true;
            health = 75;
            engineOffset = 5.5f;
            range = 140f;
            weapons.add(new Weapon(){{
                y = 1.5f;
                reload = 28f;
                alternate = true;
                ejectEffect = Fx.shellEjectSmall;
                bullet = Bullets.standardCopper;
                shootSound = Sounds.shoot;
            }});
        }};

        reaper = new UnitType("reaper"){{
            speed = 1.1f;
            accel = 0.08f;
            drag = 0.05f;
            mass = 30f;
            flying = true;
            health = 75000;
            engineOffset = 40;
            engineSize = 7.3f;
            hitsize = 58f;

            weapons.add(new Weapon(){{
                y = 1.5f;
                reload = 28f;
                alternate = true;
                ejectEffect = Fx.shellEjectSmall;
                bullet = Bullets.standardCopper;
                shootSound = Sounds.shoot;
            }});
        }};


        vanguard = new UnitType("vanguard"){{
            speed = 1.3f;
            drag = 0.1f;
            hitsize = 8f;
            mass = 1.75f;
            health = 130;
            immunities = ObjectSet.with(StatusEffects.wet);
            weapons.add(new Weapon("chain-blaster"){{
                reload = 10f;
                x = 1.25f;
                alternate = true;
                rotate = true;
                ejectEffect = Fx.shellEjectSmall;
                bullet = Bullets.standardCopper;
            }});
        }};

        draug = new UnitType("draug"){{
            flying = true;
            drag = 0.05f;
            speed = 2f;
            range = 50f;
            accel = 0.2f;
            health = 80;
            mineSpeed = 0.9f;
            engineSize = 1.8f;
            engineOffset = 5.7f;
            drillTier = 1;
        }};

        spirit = new UnitType("spirit"){{
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
                alternate = true;
                ejectEffect = Fx.none;
                recoil = 2f;
                bullet = Bullets.healBulletBig;
                shootSound = Sounds.pew;
            }});
        }};

        phantom = new UnitType("phantom"){{
            flying = true;
            drag = 0.05f;
            mass = 2f;
            speed = 3f;
            rotateSpeed = 15f;
            accel = 0.3f;
            range = 70f;
            itemCapacity = 70;
            health = 400;
            buildSpeed = 0.4f;
            engineOffset = 6.5f;
            hitsize = 8f;
        }};
        
        /*
        crawler = new UnitType("crawler", GroundUnit::new){{
            maxVelocity = 1.27f;
            speed = 0.285f;
            drag = 0.4f;
            hitsize = 8f;
            mass = 1.75f;
            health = 120;
            weapons.add(new Weapon(){{
                reload = 12f;
                ejectEffect = Fx.none;
                shootSound = Sounds.explosion;
                bullet = new BombBulletType(2f, 3f, "clear"){{
                    hitEffect = Fx.pulverize;
                    lifetime = 30f;
                    speed = 1.1f;
                    splashDamageRadius = 55f;
                    instantDisappear = true;
                    splashDamage = 30f;
                    killShooter = true;
                }};
            }});
        }};

        titan = new UnitType("titan", GroundUnit::new){{
            maxVelocity = 0.8f;
            speed = 0.22f;
            drag = 0.4f;
            mass = 3.5f;
            hitsize = 9f;
            range = 10f;
            rotatespeed = 0.1f;
            health = 460;
            immunities.add(StatusEffects.burning);
            weapons.add(new Weapon("flamethrower"){{
                shootSound = Sounds.flame;
                length = 1f;
                reload = 14f;
                alternate = true;
                recoil = 1f;
                ejectEffect = Fx.none;
                bullet = Bullets.basicFlame;
            }});
        }};

        fortress = new UnitType("fortress", GroundUnit::new){{
            maxVelocity = 0.78f;
            speed = 0.15f;
            drag = 0.4f;
            mass = 5f;
            hitsize = 10f;
            rotatespeed = 0.06f;
            targetAir = false;
            health = 750;
            weapons.add(new Weapon("artillery"){{
                length = 1f;
                reload = 60f;
                width = 10f;
                alternate = true;
                recoil = 4f;
                shake = 2f;
                ejectEffect = Fx.shellEjectMedium;
                bullet = Bullets.artilleryUnit;
                shootSound = Sounds.artillery;
            }});
        }};

        eruptor = new UnitType("eruptor", GroundUnit::new){{
            maxVelocity = 0.81f;
            speed = 0.16f;
            drag = 0.4f;
            mass = 5f;
            hitsize = 9f;
            rotatespeed = 0.05f;
            targetAir = false;
            health = 600;
            immunities = ObjectSet.with(StatusEffects.burning, StatusEffects.melting);
            weapons.add(new Weapon("eruption"){{
                length = 3f;
                reload = 10f;
                alternate = true;
                ejectEffect = Fx.none;
                bullet = Bullets.eruptorShot;
                recoil = 1f;
                width = 7f;
                shootSound = Sounds.flame;
            }});
        }};

        chaosArray = new UnitType("chaos-array", GroundUnit::new){{
            maxVelocity = 0.68f;
            speed = 0.12f;
            drag = 0.4f;
            mass = 5f;
            hitsize = 20f;
            rotatespeed = 0.06f;
            health = 3000;
            weapons.add(new Weapon("chaos"){{
                length = 8f;
                reload = 50f;
                width = 17f;
                alternate = true;
                recoil = 3f;
                shake = 2f;
                shots = 4;
                spacing = 4f;
                shotDelay = 5;
                ejectEffect = Fx.shellEjectMedium;
                bullet = Bullets.flakSurge;
                shootSound = Sounds.shootBig;
            }});
        }};

        eradicator = new UnitType("eradicator", GroundUnit::new){{
            maxVelocity = 0.68f;
            speed = 0.12f;
            drag = 0.4f;
            mass = 5f;
            hitsize = 20f;
            rotatespeed = 0.06f;
            health = 9000;
            weapons.add(new Weapon("eradication"){{
                length = 13f;
                reload = 30f;
                width = 22f;
                alternate = true;
                recoil = 3f;
                shake = 2f;
                inaccuracy = 3f;
                shots = 4;
                spacing = 0f;
                shotDelay = 3;
                ejectEffect = Fx.shellEjectMedium;
                bullet = Bullets.standardThoriumBig;
                shootSound = Sounds.shootBig;
            }});
        }};

        wraith = new UnitType("wraith", FlyingUnit::new){{
            speed = 0.3f;
            maxVelocity = 1.9f;
            drag = 0.01f;
            mass = 1.5f;
            flying = true;
            health = 75;
            engineOffset = 5.5f;
            range = 140f;
            weapons.add(new Weapon(){{
                length = 1.5f;
                reload = 28f;
                alternate = true;
                ejectEffect = Fx.shellEjectSmall;
                bullet = Bullets.standardCopper;
                shootSound = Sounds.shoot;
            }});
        }};

        ghoul = new UnitType("ghoul", FlyingUnit::new){{
            health = 220;
            speed = 0.2f;
            maxVelocity = 1.4f;
            mass = 3f;
            drag = 0.01f;
            flying = true;
            targetAir = false;
            engineOffset = 7.8f;
            range = 140f;
            weapons.add(new Weapon(){{
                length = 0f;
                width = 2f;
                reload = 12f;
                alternate = true;
                ejectEffect = Fx.none;
                velocityRnd = 1f;
                inaccuracy = 40f;
                ignoreRotation = true;
                bullet = Bullets.bombExplosive;
                shootSound = Sounds.none;
            }});
        }};

        revenant = new UnitType("revenant", HoverUnit::new){{
            health = 1000;
            mass = 5f;
            hitsize = 20f;
            speed = 0.1f;
            maxVelocity = 1f;
            drag = 0.01f;
            range = 80f;
            shootCone = 40f;
            flying = true;
            //rotateWeapons = true;
            engineOffset = 12f;
            engineSize = 3f;
            rotatespeed = 0.01f;
            attackLength = 90f;
            baseRotateSpeed = 0.06f;
            weapons.add(new Weapon("revenant-missiles"){{
                length = 3f;
                reload = 70f;
                width = 10f;
                shots = 2;
                inaccuracy = 2f;
                alternate = true;
                ejectEffect = Fx.none;
                velocityRnd = 0.2f;
                spacing = 1f;
                shootSound = Sounds.missile;
                bullet = Bullets.missileRevenant;
            }});
        }};

        lich = new UnitType("lich", HoverUnit::new){{
            health = 6000;
            mass = 20f;
            hitsize = 40f;
            speed = 0.01f;
            maxVelocity = 0.6f;
            drag = 0.02f;
            range = 80f;
            shootCone = 20f;
            flying = true;
            //rotateWeapons = true;
            engineOffset = 21;
            engineSize = 5.3f;
            rotatespeed = 0.01f;
            attackLength = 90f;
            baseRotateSpeed = 0.04f;
            weapons.add(new Weapon("lich-missiles"){{
                length = 4f;
                reload = 160f;
                width = 22f;
                shots = 16;
                shootCone = 100f;
                shotDelay = 2;
                inaccuracy = 10f;
                alternate = true;
                ejectEffect = Fx.none;
                velocityRnd = 0.2f;
                spacing = 1f;
                bullet = Bullets.missileRevenant;
                shootSound = Sounds.artillery;
            }});
        }};

        reaper = new UnitType("reaper", HoverUnit::new){{
            health = 11000;
            mass = 30f;
            hitsize = 56f;
            speed = 0.01f;
            maxVelocity = 0.6f;
            drag = 0.02f;
            range = 80f;
            shootCone = 30f;
            flying = true;
            //rotateWeapons = true;
            engineOffset = 40;
            engineSize = 7.3f;
            rotatespeed = 0.01f;
            baseRotateSpeed = 0.04f;
            weapons.add(new Weapon("reaper-gun"){{
                length = 3f;
                reload = 10f;
                width = 32f;
                shots = 1;
                shootCone = 100f;

                shake = 1f;
                inaccuracy = 3f;
                alternate = true;
                ejectEffect = Fx.none;
                bullet = new BasicBulletType(7f, 42, "bullet"){
                    {
                        bulletWidth = 15f;
                        bulletHeight = 21f;
                        shootEffect = Fx.shootBig;
                    }

                    @Override
                    public float range(){
                        return 165f;
                    }
                };
                shootSound = Sounds.shootBig;
            }});
        }};


        /*
        vanguard = new UnitType("vanguard-ship"){
            float healRange = 60f;
            float healReload = 200f;
            float healPercent = 10f;

            {
                flying = true;
                drillTier = 1;
                mineSpeed = 4f;
                speed = 0.49f;
                drag = 0.09f;
                health = 200f;
                weaponOffsetX = -1;
                engineSize = 2.3f;
                weaponOffsetY = -1;
                engineColor = Pal.lightTrail;
                cellTrnsY = 1f;
                buildSpeed = 1.2f;
                weapons.add(new Weapon("vanguard-blaster"){{
                    length = 1.5f;
                    reload = 30f;
                    alternate = true;
                    inaccuracy = 6f;
                    velocityRnd = 0.1f;
                    ejectEffect = Fx.none;
                    bullet = new HealBulletType(){{
                        healPercent = 3f;
                        backColor = engineColor;
                        homingPower = 20f;
                        bulletHeight = 4f;
                        bulletWidth = 1.5f;
                        damage = 3f;
                        speed = 4f;
                        lifetime = 40f;
                        shootEffect = Fx.shootHealYellow;
                        smokeEffect = hitEffect = despawnEffect = Fx.hitYellowLaser;
                    }});
        }};
            }

            @Override
            public boolean alwaysUnlocked(){
                return true;
            }

            @Override
            public void update(Playerc player){
                if(player.timer.get(Playerc.timerAbility, healReload)){
                    if(indexer.eachBlock(player, healRange, other -> other.entity.damaged(), other -> {
                        other.entity.heal(other.entity.maxHealth() * healPercent / 100f);
                        Fx.healBlockFull.at(other.drawx(), other.drawy(), other.block().size, Pal.heal);
                    })){
                        Fx.healWave.at(player);
                    }
                }
            }
        };

        alpha = new UnitType("alpha-mech", false){
            {
                drillTier = -1;
                speed = 0.5f;
                boostSpeed = 0.95f;
                itemCapacity = 15;
                mass = 0.9f;
                health = 150f;
                buildSpeed = 0.9f;
                weaponOffsetX = 1;
                weaponOffsetY = -1;
                engineColor = Pal.heal;

                weapons.add(new Weapon("shockgun"){{
                    shake = 2f;
                    length = 0.5f;
                    reload = 70f;
                    alternate = true;
                    recoil = 4f;
                    width = 5f;
                    shootSound = Sounds.laser;

                    bullet = new LaserBulletType(){{
                        damage = 20f;
                        recoil = 1f;
                        sideAngle = 45f;
                        sideWidth = 1f;
                        sideLength = 70f;
                        colors = new Color[]{Pal.heal.cpy().a(0.4f), Pal.heal, Color.white};
                    }});
        }};
            }

            @Override
            public void update(Playerc player){
                player.heal(Time.delta() * 0.09f);
            }

        };

        delta = new UnitType("delta-mech", false){
            {
                drillPower = 1;
                mineSpeed = 1.5f;
                mass = 1.2f;
                speed = 0.5f;
                itemCapacity = 40;
                boostSpeed = 0.95f;
                buildSpeed = 1.2f;
                engineColor = Color.valueOf("ffd37f");
                health = 250f;
                weaponOffsetX = 4f;

                weapons.add(new Weapon("flamethrower"){{
                    length = 1.5f;
                    reload = 30f;
                    width = 4f;
                    alternate = true;
                    shots = 3;
                    inaccuracy = 40f;
                    shootSound = Sounds.spark;
                    bullet = new LightningBulletType(){{
                        damage = 5;
                        lightningLength = 10;
                        lightningColor = Pal.lightFlame;
                    }});
        }};
            }
        };

        tau = new UnitType("tau-mech", false){
            float healRange = 60f;
            float healAmount = 10f;
            float healReload = 160f;
            boolean wasHealed;

            {
                drillPower = 4;
                mineSpeed = 3f;
                itemCapacity = 70;
                weaponOffsetY = -1;
                weaponOffsetX = 1;
                mass = 1.75f;
                speed = 0.44f;
                drag = 0.35f;
                boostSpeed = 0.8f;
                canHeal = true;
                health = 200f;
                buildSpeed = 1.6f;
                engineColor = Pal.heal;

                weapons.add(new Weapon("heal-blaster"){{
                    length = 1.5f;
                    reload = 24f;
                    alternate = false;
                    ejectEffect = Fx.none;
                    recoil = 2f;
                    bullet = Bullets.healBullet;
                    shootSound = Sounds.pew;
                }};
            }

            @Override
            public void update(Playerc player){

                if(player.timer.get(Playerc.timerAbility, healReload)){
                    wasHealed = false;

                    Units.nearby(player.team(), player.x, player.y, healRange, unit -> {
                        if(unit.health < unit.maxHealth()){
                            Fx.heal.at(unit);
                            wasHealed = true;
                        }
                        unit.heal(healAmount);
                    });

                    if(wasHealed){
                        Fx.healWave.at(player);
                    }
                }
            }
        };

        omega = new UnitType("omega-mech", false){
            protected TextureRegion armorRegion;

            {
                drillPower = 2;
                mineSpeed = 1.5f;
                itemCapacity = 80;
                speed = 0.36f;
                boostSpeed = 0.6f;
                mass = 4f;
                shake = 4f;
                weaponOffsetX = 1;
                weaponOffsetY = 0;
                engineColor = Color.valueOf("feb380");
                health = 350f;
                buildSpeed = 1.5f;
                weapons.add(new Weapon("swarmer"){{
                    length = 1.5f;
                    recoil = 4f;
                    reload = 38f;
                    shots = 4;
                    spacing = 8f;
                    inaccuracy = 8f;
                    alternate = true;
                    ejectEffect = Fx.none;
                    shake = 3f;
                    bullet = Bullets.missileSwarm;
                    shootSound = Sounds.shootBig;
                }};
            }

            @Override
            public float getRotationAlpha(Playerc player){
                return 0.6f - player.shootHeat * 0.3f;
            }

            @Override
            public float spreadX(Playerc player){
                return player.shootHeat * 2f;
            }

            @Override
            public void load(){
                super.load();
                armorRegion = Core.atlas.find(name + "-armor");
            }

            @Override
            public void update(Playerc player){
                float scl = 1f - player.shootHeat / 2f*Time.delta();
                player.vel().scl(scl);
            }

            @Override
            public float getExtraArmor(Playerc player){
                return player.shootHeat * 30f;
            }

            @Override
            public void draw(Playerc player){
                if(player.shootHeat <= 0.01f) return;

                Shaders.build.progress = player.shootHeat;
                Shaders.build.region = armorRegion;
                Shaders.build.time = Time.time() / 10f;
                Shaders.build.color.set(Pal.accent).a = player.shootHeat;
                Draw.shader(Shaders.build);
                Draw.rect(armorRegion, player.x, player.y, player.rotation);
                Draw.shader();
            }
        };

        dart = new UnitType("dart-ship"){
            float effectRange = 60f;
            float effectReload = 60f * 5;
            float effectDuration = 60f * 10f;

            {
                flying = true;
                drillPower = 1;
                mineSpeed = 2f;
                speed = 0.5f;
                drag = 0.09f;
                health = 200f;
                weaponOffsetX = -1;
                weaponOffsetY = -1;
                engineColor = Pal.lightTrail;
                cellTrnsY = 1f;
                buildSpeed = 1.1f;
                weapons.add(new Weapon("blaster"){{
                    length = 1.5f;
                    reload = 15f;
                    alternate = true;
                    ejectEffect = Fx.shellEjectSmall;
                    bullet = Bullets.standardCopper;
                }};
            }

            @Override
            public void update(Playerc player){
                super.update(player);

                if(player.timer.get(Playerc.timerAbility, effectReload)){

                    Units.nearby(player.team(), player.x, player.y, effectRange, unit -> {
                        //unit.applyEffect(StatusEffects.overdrive, effectDuration);
                    });

                    indexer.eachBlock(player, effectRange, other -> other.entity.damaged(), other -> {
                        other.entity.applyBoost(1.5f, effectDuration);
                        Fx.healBlockFull.at(other.drawx(), other.drawy(), other.block().size, Pal.heal);
                    });

                    Fx.overdriveWave.at(player);
                }
            }
        };

        javelin = new UnitType("javelin-ship"){
            float minV = 3.6f;
            float maxV = 6f;
            TextureRegion shield;

            {
                flying = true;
                drillPower = -1;
                speed = 0.11f;
                drag = 0.01f;
                mass = 2f;
                health = 170f;
                engineColor = Color.valueOf("d3ddff");
                cellTrnsY = 1f;
                weapons.add(new Weapon("missiles"){{
                    length = 1.5f;
                    reload = 70f;
                    shots = 4;
                    inaccuracy = 2f;
                    alternate = true;
                    ejectEffect = Fx.none;
                    velocityRnd = 0.2f;
                    spacing = 1f;
                    bullet = Bullets.missileJavelin;
                    shootSound = Sounds.missile;
                }};
            }

            @Override
            public void load(){
                super.load();
                shield = Core.atlas.find(name + "-shield");
            }

            @Override
            public float getRotationAlpha(Playerc player){
                return 0.5f;
            }

            @Override
            public void update(Playerc player){
                float scl = scld(player);
                if(Mathf.chance(Time.delta() * (0.15 * scl))){
                    Fx.hitLancer.at(Pal.lancerLaser, player.x, player.y);
                    Lightning.create(player.team(), Pal.lancerLaser, 10f * Vars.state.rules.playerDamageMultiplier,
                    player.x + player.vel().x, player.y + player.vel().y, player.rotation, 14);
                }
            }

            @Override
            public void draw(Playerc player){
                float scl = scld(player);
                if(scl < 0.01f) return;
                Draw.color(Pal.lancerLaser);
                Draw.alpha(scl / 2f);
                Draw.blend(Blending.additive);
                Draw.rect(shield, player.x + Mathf.range(scl / 2f), player.y + Mathf.range(scl / 2f), player.rotation - 90);
                Draw.blend();
            }

            float scld(Playerc player){
                return Mathf.clamp((player.vel().len() - minV) / (maxV - minV));
            }
        };

        trident = new UnitType("trident-ship"){
            {
                flying = true;
                drillPower = 2;
                speed = 0.15f;
                drag = 0.034f;
                mass = 2.5f;
                turnCursor = false;
                health = 250f;
                itemCapacity = 30;
                engineColor = Color.valueOf("84f491");
                cellTrnsY = 1f;
                buildSpeed = 2.5f;
                weapons.add(new Weapon("bomber"){{
                    length = 0f;
                    width = 2f;
                    reload = 25f;
                    shots = 2;
                    shotDelay = 1f;
                    shots = 8;
                    alternate = true;
                    ejectEffect = Fx.none;
                    velocityRnd = 1f;
                    inaccuracy = 20f;
                    ignoreRotation = true;
                    bullet = new BombBulletType(16f, 25f, "shell"){{
                        bulletWidth = 10f;
                        bulletHeight = 14f;
                        hitEffect = Fx.flakExplosion;
                        shootEffect = Fx.none;
                        smokeEffect = Fx.none;
                        shootSound = Sounds.artillery;
                    }});
        }};
            }

            @Override
            public boolean canShoot(Playerc player){
                return player.vel().len() > 1.2f;
            }
        };

        glaive = new UnitType("glaive-ship"){
            {
                flying = true;
                drillPower = 4;
                mineSpeed = 1.3f;
                speed = 0.32f;
                drag = 0.06f;
                mass = 3f;
                health = 240f;
                itemCapacity = 60;
                engineColor = Color.valueOf("feb380");
                cellTrnsY = 1f;
                buildSpeed = 1.2f;

                weapons.add(new Weapon("bomber"){{
                    length = 1.5f;
                    reload = 13f;
                    alternate = true;
                    ejectEffect = Fx.shellEjectSmall;
                    bullet = Bullets.standardGlaive;
                    shootSound = Sounds.shootSnap;
                }};
            }
        };

        starter = vanguard;*/
    }
}
