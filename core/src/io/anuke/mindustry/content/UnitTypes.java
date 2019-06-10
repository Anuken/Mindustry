package io.anuke.mindustry.content;

import io.anuke.arc.collection.ObjectSet;
import io.anuke.mindustry.entities.type.base.*;
import io.anuke.mindustry.game.ContentList;
import io.anuke.mindustry.type.UnitType;
import io.anuke.mindustry.type.Weapon;

public class UnitTypes implements ContentList{
    public static UnitType
    spirit, phantom,
    wraith, ghoul, revenant, lich, reaper,
    dagger, crawler, titan, fortress, eruptor, chaosArray, eradicator;

    @Override
    public void load(){
        spirit = new UnitType("spirit", Spirit.class, Spirit::new){{
            isFlying = true;
            drag = 0.01f;
            speed = 0.4f;
            maxVelocity = 1.6f;
            range = 50f;
            health = 60;
            engineSize = 1.8f;
            engineOffset = 5.7f;
            weapon = new Weapon("heal-blaster"){{
                length = 1.5f;
                reload = 40f;
                width = 0.5f;
                roundrobin = true;
                ejectEffect = Fx.none;
                recoil = 2f;
                bullet = Bullets.healBullet;
            }};
        }};

        dagger = new UnitType("dagger", Dagger.class, Dagger::new){{
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

        crawler = new UnitType("crawler", Crawler.class, Crawler::new){{
            maxVelocity = 1.2f;
            speed = 0.26f;
            drag = 0.4f;
            hitsize = 8f;
            mass = 1.75f;
            health = 100;
            weapon = new Weapon("bomber"){{
                reload = 12f;
                ejectEffect = Fx.none;
                bullet = Bullets.explode;
            }};
        }};

        titan = new UnitType("titan", Titan.class, Titan::new){{
            maxVelocity = 0.8f;
            speed = 0.18f;
            drag = 0.4f;
            mass = 3.5f;
            hitsize = 9f;
            rotatespeed = 0.1f;
            health = 440;
            immunities.add(StatusEffects.burning);
            weapon = new Weapon("flamethrower"){{
                length = 1f;
                reload = 14f;
                roundrobin = true;
                recoil = 1f;
                ejectEffect = Fx.none;
                bullet = Bullets.basicFlame;
            }};
        }};

        fortress = new UnitType("fortress", Fortress.class, Fortress::new){{
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
            }};
        }};

        eruptor = new UnitType("eruptor", Eruptor.class, Eruptor::new){{
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
            }};
        }};

        chaosArray = new UnitType("chaos-array", Dagger.class, Dagger::new){{
            maxVelocity = 0.68f;
            speed = 0.12f;
            drag = 0.4f;
            mass = 5f;
            hitsize = 20f;
            rotatespeed = 0.06f;
            health = 4000;
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
            }};
        }};

        eradicator = new UnitType("eradicator", Dagger.class, Dagger::new){{
            maxVelocity = 0.68f;
            speed = 0.12f;
            drag = 0.4f;
            mass = 5f;
            hitsize = 20f;
            rotatespeed = 0.06f;
            health = 10000;
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
            }};
        }};

        wraith = new UnitType("wraith", Wraith.class, Wraith::new){{
            speed = 0.3f;
            maxVelocity = 1.9f;
            drag = 0.01f;
            mass = 1.5f;
            isFlying = true;
            health = 75;
            engineOffset = 5.5f;
            range = 140f;
            weapon = new Weapon("chain-blaster"){{
                length = 1.5f;
                reload = 28f;
                roundrobin = true;
                ejectEffect = Fx.shellEjectSmall;
                bullet = Bullets.standardCopper;
            }};
        }};

        ghoul = new UnitType("ghoul", Ghoul.class, Ghoul::new){{
            health = 220;
            speed = 0.2f;
            maxVelocity = 1.4f;
            mass = 3f;
            drag = 0.01f;
            isFlying = true;
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
            }};
        }};

        phantom = new UnitType("phantom", Phantom.class, Phantom::new){{
            isFlying = true;
            drag = 0.01f;
            mass = 2f;
            speed = 0.45f;
            maxVelocity = 1.9f;
            range = 70f;
            itemCapacity = 70;
            health = 220;
            buildPower = 0.9f;
            minePower = 1.1f;
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

        revenant = new UnitType("revenant", Revenant.class, Revenant::new){{
            health = 1000;
            mass = 5f;
            hitsize = 20f;
            speed = 0.1f;
            maxVelocity = 1f;
            drag = 0.01f;
            range = 80f;
            shootCone = 40f;
            isFlying = true;
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
                bullet = Bullets.missileRevenant;
            }};
        }};

        lich = new UnitType("lich", Revenant.class, Revenant::new){{
            health = 7000;
            mass = 20f;
            hitsize = 40f;
            speed = 0.01f;
            maxVelocity = 0.6f;
            drag = 0.02f;
            range = 80f;
            shootCone = 20f;
            isFlying = true;
            rotateWeapon = true;
            engineOffset = 21;
            engineSize = 5.3f;
            rotatespeed = 0.01f;
            attackLength = 90f;
            baseRotateSpeed = 0.04f;
            weapon = new Weapon("lich-missiles"){{
                length = 4f;
                reload = 180f;
                width = 22f;
                shots = 22;
                shootCone = 100f;
                shotDelay = 2;
                inaccuracy = 10f;
                roundrobin = true;
                ejectEffect = Fx.none;
                velocityRnd = 0.2f;
                spacing = 1f;
                bullet = Bullets.missileRevenant;
            }};
        }};

        reaper = new UnitType("reaper", Revenant.class, Revenant::new){{
            health = 13000;
            mass = 30f;
            hitsize = 56f;
            speed = 0.01f;
            maxVelocity = 0.6f;
            drag = 0.02f;
            range = 80f;
            shootCone = 30f;
            isFlying = true;
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
                bullet = Bullets.standardDenseBig;
            }};
        }};
    }
}
