package io.anuke.mindustry.content;

import io.anuke.arc.collection.*;
import io.anuke.mindustry.entities.bullet.*;
import io.anuke.mindustry.entities.type.*;
import io.anuke.mindustry.entities.type.Bullet;
import io.anuke.mindustry.entities.type.base.*;
import io.anuke.mindustry.game.*;
import io.anuke.mindustry.gen.*;
import io.anuke.mindustry.type.*;

public class UnitTypes implements ContentList{
    public static UnitType
    draug, spirit, phantom,
    wraith, ghoul, revenant, lich, reaper,
    dagger, crawler, titan, fortress, eruptor, chaosArray, eradicator;

    @Override
    public void load(){
        draug = new UnitType("draug", Draug::new){{
            flying = true;
            drag = 0.01f;
            speed = 0.3f;
            maxVelocity = 1.2f;
            range = 50f;
            health = 80;
            minePower = 0.9f;
            engineSize = 1.8f;
            engineOffset = 5.7f;
            weapon = new Weapon("you have incurred my wrath. prepare to die."){{
                bullet = Bullets.lancerLaser;
            }};
        }};

        spirit = new UnitType("spirit", Spirit::new){{
            flying = true;
            drag = 0.01f;
            speed = 0.42f;
            maxVelocity = 1.6f;
            range = 50f;
            health = 100;
            engineSize = 1.8f;
            engineOffset = 5.7f;
            weapon = new Weapon("heal-blaster"){{
                length = 1.5f;
                reload = 40f;
                width = 0.5f;
                roundrobin = true;
                ejectEffect = Fx.none;
                recoil = 2f;
                bullet = Bullets.healBulletBig;
                shootSound = Sounds.pew;
            }};
        }};

        phantom = new UnitType("phantom", Phantom::new){{
            flying = true;
            drag = 0.01f;
            mass = 2f;
            speed = 0.45f;
            maxVelocity = 1.9f;
            range = 70f;
            itemCapacity = 70;
            health = 400;
            buildPower = 1f;
            engineOffset = 6.5f;
            toMine = ObjectSet.with(Items.lead, Items.copper, Items.titanium);
            weapon = new Weapon("heal-blaster"){{
                length = 1.5f;
                reload = 20f;
                width = 0.5f;
                roundrobin = true;
                ejectEffect = Fx.none;
                recoil = 2f;
                bullet = Bullets.healBullet;
            }};
        }};

        dagger = new UnitType("dagger", Dagger::new){{
            maxVelocity = 1.1f;
            speed = 0.2f;
            drag = 0.4f;
            hitsize = 8f;
            mass = 1.75f;
            health = 130;
            weapon = new Weapon("chain-blaster"){{
                length = 1.5f;
                reload = 28f;
                roundrobin = true;
                ejectEffect = Fx.shellEjectSmall;
                bullet = Bullets.standardCopper;
            }};
        }};

        crawler = new UnitType("crawler", Crawler::new){{
            maxVelocity = 1.27f;
            speed = 0.285f;
            drag = 0.4f;
            hitsize = 8f;
            mass = 1.75f;
            health = 120;
            weapon = new Weapon("bomber"){{
                reload = 12f;
                ejectEffect = Fx.none;
                shootSound = Sounds.explosion;
                bullet = new BombBulletType(2f, 3f, "clear"){
                    {
                        hitEffect = Fx.pulverize;
                        lifetime = 30f;
                        speed = 1.1f;
                        splashDamageRadius = 55f;
                        splashDamage = 30f;
                    }

                    @Override
                    public void init(Bullet b){
                        if(b.getOwner() instanceof Unit){
                            ((Unit)b.getOwner()).kill();
                        }
                        b.time(b.lifetime());
                    }
                };
            }};
        }};

        titan = new UnitType("titan", Titan::new){{
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
                range = 30f;
                roundrobin = true;
                recoil = 1f;
                ejectEffect = Fx.none;
                bullet = Bullets.basicFlame;
            }};
        }};

        fortress = new UnitType("fortress", Fortress::new){{
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
                roundrobin = true;
                recoil = 4f;
                shake = 2f;
                ejectEffect = Fx.shellEjectMedium;
                bullet = Bullets.artilleryUnit;
                shootSound = Sounds.artillery;
            }};
        }};

        eruptor = new UnitType("eruptor", Eruptor::new){{
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
                roundrobin = true;
                ejectEffect = Fx.none;
                bullet = Bullets.eruptorShot;
                recoil = 1f;
                width = 7f;
                shootSound = Sounds.flame;
            }};
        }};

        chaosArray = new UnitType("chaos-array", Dagger::new){{
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
                roundrobin = true;
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

        eradicator = new UnitType("eradicator", Dagger::new){{
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
                roundrobin = true;
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

        wraith = new UnitType("wraith", Wraith::new){{
            speed = 0.3f;
            maxVelocity = 1.9f;
            drag = 0.01f;
            mass = 1.5f;
            flying = true;
            health = 75;
            engineOffset = 5.5f;
            range = 140f;
            weapon = new Weapon("chain-blaster"){{
                length = 1.5f;
                reload = 28f;
                roundrobin = true;
                ejectEffect = Fx.shellEjectSmall;
                bullet = Bullets.standardCopper;
                shootSound = Sounds.shoot;
            }};
        }};

        ghoul = new UnitType("ghoul", Ghoul::new){{
            health = 220;
            speed = 0.2f;
            maxVelocity = 1.4f;
            mass = 3f;
            drag = 0.01f;
            flying = true;
            targetAir = false;
            engineOffset = 7.8f;
            range = 140f;
            weapon = new Weapon("bomber"){{
                length = 0f;
                width = 2f;
                reload = 12f;
                roundrobin = true;
                ejectEffect = Fx.none;
                velocityRnd = 1f;
                inaccuracy = 40f;
                ignoreRotation = true;
                bullet = Bullets.bombExplosive;
                shootSound = Sounds.none;
            }};
        }};

        revenant = new UnitType("revenant", Revenant::new){{
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
                roundrobin = true;
                ejectEffect = Fx.none;
                velocityRnd = 0.2f;
                spacing = 1f;
                shootSound = Sounds.missile;
                bullet = Bullets.missileRevenant;
            }};
        }};

        lich = new UnitType("lich", Revenant::new){{
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
                roundrobin = true;
                ejectEffect = Fx.none;
                velocityRnd = 0.2f;
                spacing = 1f;
                bullet = Bullets.missileRevenant;
                shootSound = Sounds.artillery;
            }};
        }};

        reaper = new UnitType("reaper", Revenant::new){{
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
                roundrobin = true;
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
    }
}
