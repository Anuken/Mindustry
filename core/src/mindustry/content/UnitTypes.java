package mindustry.content;

import mindustry.annotations.Annotations.*;
import mindustry.ctype.*;
import mindustry.gen.*;
import mindustry.type.*;

public class UnitTypes implements ContentList{
    public static UnitDef
    draug, spirit, phantom,
    wraith, ghoul, revenant, lich, reaper,
    crawler, titan, fortress, eruptor, chaosArray, eradicator;

    public static @EntityDef({Unitc.class, Legsc.class}) UnitDef dagger;

    public static UnitDef vanguard, alpha, delta, tau, omega, dart, javelin, trident, glaive;
    public static UnitDef starter;


    @Override
    public void load(){

        dagger = new UnitDef("dagger"){{
            speed = 0.2f;
            drag = 0.2f;
            hitsize = 8f;
            mass = 1.75f;
            health = 130;
            weapons.add(new Weapon("chain-blaster"){{
                reload = 28f;
                x = 4f;
                alternate = true;
                ejectEffect = Fx.shellEjectSmall;
                bullet = Bullets.standardCopper;
            }});
        }};

        /*
        draug = new UnitDef("draug", MinerDrone::new){{
            flying = true;
            drag = 0.01f;
            speed = 0.3f;
            maxVelocity = 1.2f;
            range = 50f;
            health = 80;
            minePower = 0.9f;
            engineSize = 1.8f;
            engineOffset = 5.7f;
        }};

        spirit = new UnitDef("spirit", RepairDrone::new){{
            flying = true;
            drag = 0.01f;
            speed = 0.42f;
            maxVelocity = 1.6f;
            range = 50f;
            health = 100;
            engineSize = 1.8f;
            engineOffset = 5.7f;
            weapon = new Weapon(){{
                length = 1.5f;
                reload = 40f;
                width = 0.5f;
                alternate = true;
                ejectEffect = Fx.none;
                recoil = 2f;
                bullet = Bullets.healBulletBig;
                shootSound = Sounds.pew;
            }};
        }};

        phantom = new UnitDef("phantom", BuilderDrone::new){{
            flying = true;
            drag = 0.01f;
            mass = 2f;
            speed = 0.45f;
            maxVelocity = 1.9f;
            range = 70f;
            itemCapacity = 70;
            health = 400;
            buildPower = 0.4f;
            engineOffset = 6.5f;
            toMine = ObjectSet.with(Items.lead, Items.copper, Items.titanium);
            weapon = new Weapon(){{
                length = 1.5f;
                reload = 20f;
                width = 0.5f;
                alternate = true;
                ejectEffect = Fx.none;
                recoil = 2f;
                bullet = Bullets.healBullet;
            }};
        }};

        dagger = new UnitDef("dagger", GroundUnit::new){{
            maxVelocity = 1.1f;
            speed = 0.2f;
            drag = 0.4f;
            hitsize = 8f;
            mass = 1.75f;
            health = 130;
            weapon = new Weapon("chain-blaster"){{
                length = 1.5f;
                reload = 28f;
                alternate = true;
                ejectEffect = Fx.shellEjectSmall;
                bullet = Bullets.standardCopper;
            }};
        }};

        crawler = new UnitDef("crawler", GroundUnit::new){{
            maxVelocity = 1.27f;
            speed = 0.285f;
            drag = 0.4f;
            hitsize = 8f;
            mass = 1.75f;
            health = 120;
            weapon = new Weapon(){{
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
            }};
        }};

        titan = new UnitDef("titan", GroundUnit::new){{
            maxVelocity = 0.8f;
            speed = 0.22f;
            drag = 0.4f;
            mass = 3.5f;
            hitsize = 9f;
            range = 10f;
            rotatespeed = 0.1f;
            health = 460;
            immunities.add(StatusEffects.burning);
            weapon = new Weapon("flamethrower"){{
                shootSound = Sounds.flame;
                length = 1f;
                reload = 14f;
                alternate = true;
                recoil = 1f;
                ejectEffect = Fx.none;
                bullet = Bullets.basicFlame;
            }};
        }};

        fortress = new UnitDef("fortress", GroundUnit::new){{
            maxVelocity = 0.78f;
            speed = 0.15f;
            drag = 0.4f;
            mass = 5f;
            hitsize = 10f;
            rotatespeed = 0.06f;
            targetAir = false;
            health = 750;
            weapon = new Weapon("artillery"){{
                length = 1f;
                reload = 60f;
                width = 10f;
                alternate = true;
                recoil = 4f;
                shake = 2f;
                ejectEffect = Fx.shellEjectMedium;
                bullet = Bullets.artilleryUnit;
                shootSound = Sounds.artillery;
            }};
        }};

        eruptor = new UnitDef("eruptor", GroundUnit::new){{
            maxVelocity = 0.81f;
            speed = 0.16f;
            drag = 0.4f;
            mass = 5f;
            hitsize = 9f;
            rotatespeed = 0.05f;
            targetAir = false;
            health = 600;
            immunities = ObjectSet.with(StatusEffects.burning, StatusEffects.melting);
            weapon = new Weapon("eruption"){{
                length = 3f;
                reload = 10f;
                alternate = true;
                ejectEffect = Fx.none;
                bullet = Bullets.eruptorShot;
                recoil = 1f;
                width = 7f;
                shootSound = Sounds.flame;
            }};
        }};

        chaosArray = new UnitDef("chaos-array", GroundUnit::new){{
            maxVelocity = 0.68f;
            speed = 0.12f;
            drag = 0.4f;
            mass = 5f;
            hitsize = 20f;
            rotatespeed = 0.06f;
            health = 3000;
            weapon = new Weapon("chaos"){{
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
            }};
        }};

        eradicator = new UnitDef("eradicator", GroundUnit::new){{
            maxVelocity = 0.68f;
            speed = 0.12f;
            drag = 0.4f;
            mass = 5f;
            hitsize = 20f;
            rotatespeed = 0.06f;
            health = 9000;
            weapon = new Weapon("eradication"){{
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
            }};
        }};

        wraith = new UnitDef("wraith", FlyingUnit::new){{
            speed = 0.3f;
            maxVelocity = 1.9f;
            drag = 0.01f;
            mass = 1.5f;
            flying = true;
            health = 75;
            engineOffset = 5.5f;
            range = 140f;
            weapon = new Weapon(){{
                length = 1.5f;
                reload = 28f;
                alternate = true;
                ejectEffect = Fx.shellEjectSmall;
                bullet = Bullets.standardCopper;
                shootSound = Sounds.shoot;
            }};
        }};

        ghoul = new UnitDef("ghoul", FlyingUnit::new){{
            health = 220;
            speed = 0.2f;
            maxVelocity = 1.4f;
            mass = 3f;
            drag = 0.01f;
            flying = true;
            targetAir = false;
            engineOffset = 7.8f;
            range = 140f;
            weapon = new Weapon(){{
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
            }};
        }};

        revenant = new UnitDef("revenant", HoverUnit::new){{
            health = 1000;
            mass = 5f;
            hitsize = 20f;
            speed = 0.1f;
            maxVelocity = 1f;
            drag = 0.01f;
            range = 80f;
            shootCone = 40f;
            flying = true;
            rotateWeapon = true;
            engineOffset = 12f;
            engineSize = 3f;
            rotatespeed = 0.01f;
            attackLength = 90f;
            baseRotateSpeed = 0.06f;
            weapon = new Weapon("revenant-missiles"){{
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
            }};
        }};

        lich = new UnitDef("lich", HoverUnit::new){{
            health = 6000;
            mass = 20f;
            hitsize = 40f;
            speed = 0.01f;
            maxVelocity = 0.6f;
            drag = 0.02f;
            range = 80f;
            shootCone = 20f;
            flying = true;
            rotateWeapon = true;
            engineOffset = 21;
            engineSize = 5.3f;
            rotatespeed = 0.01f;
            attackLength = 90f;
            baseRotateSpeed = 0.04f;
            weapon = new Weapon("lich-missiles"){{
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
            }};
        }};

        reaper = new UnitDef("reaper", HoverUnit::new){{
            health = 11000;
            mass = 30f;
            hitsize = 56f;
            speed = 0.01f;
            maxVelocity = 0.6f;
            drag = 0.02f;
            range = 80f;
            shootCone = 30f;
            flying = true;
            rotateWeapon = true;
            engineOffset = 40;
            engineSize = 7.3f;
            rotatespeed = 0.01f;
            baseRotateSpeed = 0.04f;
            weapon = new Weapon("reaper-gun"){{
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
            }};
        }};


        /*
        vanguard = new UnitDef("vanguard-ship"){
            float healRange = 60f;
            float healReload = 200f;
            float healPercent = 10f;

            {
                flying = true;
                drillTier = 1;
                minePower = 4f;
                speed = 0.49f;
                drag = 0.09f;
                health = 200f;
                weaponOffsetX = -1;
                engineSize = 2.3f;
                weaponOffsetY = -1;
                engineColor = Pal.lightTrail;
                cellTrnsY = 1f;
                buildPower = 1.2f;
                weapon = new Weapon("vanguard-blaster"){{
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
                    }};
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

        alpha = new UnitDef("alpha-mech", false){
            {
                drillTier = -1;
                speed = 0.5f;
                boostSpeed = 0.95f;
                itemCapacity = 15;
                mass = 0.9f;
                health = 150f;
                buildPower = 0.9f;
                weaponOffsetX = 1;
                weaponOffsetY = -1;
                engineColor = Pal.heal;

                weapon = new Weapon("shockgun"){{
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
                    }};
                }};
            }

            @Override
            public void update(Playerc player){
                player.heal(Time.delta() * 0.09f);
            }

        };

        delta = new UnitDef("delta-mech", false){
            {
                drillPower = 1;
                mineSpeed = 1.5f;
                mass = 1.2f;
                speed = 0.5f;
                itemCapacity = 40;
                boostSpeed = 0.95f;
                buildPower = 1.2f;
                engineColor = Color.valueOf("ffd37f");
                health = 250f;
                weaponOffsetX = 4f;

                weapon = new Weapon("flamethrower"){{
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
                    }};
                }};
            }
        };

        tau = new UnitDef("tau-mech", false){
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
                buildPower = 1.6f;
                engineColor = Pal.heal;

                weapon = new Weapon("heal-blaster"){{
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

        omega = new UnitDef("omega-mech", false){
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
                buildPower = 1.5f;
                weapon = new Weapon("swarmer"){{
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

        dart = new UnitDef("dart-ship"){
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
                buildPower = 1.1f;
                weapon = new Weapon("blaster"){{
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

        javelin = new UnitDef("javelin-ship"){
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
                weapon = new Weapon("missiles"){{
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

        trident = new UnitDef("trident-ship"){
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
                buildPower = 2.5f;
                weapon = new Weapon("bomber"){{
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
                    }};
                }};
            }

            @Override
            public boolean canShoot(Playerc player){
                return player.vel().len() > 1.2f;
            }
        };

        glaive = new UnitDef("glaive-ship"){
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
                buildPower = 1.2f;

                weapon = new Weapon("bomber"){{
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
