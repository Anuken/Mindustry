package io.anuke.mindustry.content;

import io.anuke.mindustry.content.fx.Fx;
import io.anuke.mindustry.content.fx.ShootFx;
import io.anuke.mindustry.game.ContentList;
import io.anuke.mindustry.type.ContentType;
import io.anuke.mindustry.type.Weapon;

public class Weapons implements ContentList{
    public static Weapon blaster, blasterSmall, glaiveBlaster, droneBlaster, healBlaster, healBlasterDrone, chainBlaster, shockgun,
    sapper, swarmer, bomber, bomberTrident, flakgun, flamethrower, missiles, artillery, laserBurster;

    @Override
    public void load(){

        blaster = new Weapon("blaster"){{
            length = 1.5f;
            reload = 14f;
            roundrobin = true;
            ejectEffect = ShootFx.shellEjectSmall;
            ammo = AmmoTypes.bulletMech;
        }};

        blasterSmall = new Weapon("blaster"){{
            length = 1.5f;
            reload = 15f;
            roundrobin = true;
            ejectEffect = ShootFx.shellEjectSmall;
            ammo = AmmoTypes.bulletCopper;
        }};

        glaiveBlaster = new Weapon("bomber"){{
            length = 1.5f;
            reload = 10f;
            roundrobin = true;
            ejectEffect = ShootFx.shellEjectSmall;
            ammo = AmmoTypes.bulletGlaive;
        }};

        droneBlaster = new Weapon("blaster"){{
            length = 2f;
            reload = 25f;
            width = 1f;
            roundrobin = true;
            ejectEffect = ShootFx.shellEjectSmall;
            ammo = AmmoTypes.bulletCopper;
        }};

        healBlaster = new Weapon("heal-blaster"){{
            length = 1.5f;
            reload = 24f;
            roundrobin = false;
            ejectEffect = Fx.none;
            recoil = 2f;
            ammo = AmmoTypes.healBlaster;
        }};

        missiles = new Weapon("missiles"){{
            length = 1.5f;
            reload = 60f;
            shots = 4;
            inaccuracy = 2f;
            roundrobin = true;
            ejectEffect = Fx.none;
            velocityRnd = 0.2f;
            spacing = 1f;
            ammo = AmmoTypes.weaponMissile;
        }};

        swarmer = new Weapon("swarmer"){{
            length = 1.5f;
            recoil = 4f;
            reload = 60f;
            shots = 4;
            spacing = 8f;
            inaccuracy = 8f;
            roundrobin = true;
            ejectEffect = Fx.none;
            shake = 3f;
            ammo = AmmoTypes.weaponMissileSwarm;
        }};

        chainBlaster = new Weapon("chain-blaster"){{
            length = 1.5f;
            reload = 28f;
            roundrobin = true;
            ejectEffect = ShootFx.shellEjectSmall;
            ammo = AmmoTypes.bulletCopper;
        }};

        shockgun = new Weapon("shockgun"){{
            length = 1f;
            reload = 40f;
            roundrobin = true;
            shots = 1;
            inaccuracy = 0f;
            velocityRnd = 0.2f;
            ejectEffect = Fx.none;
            ammo = AmmoTypes.shock;
        }};

        flakgun = new Weapon("flakgun"){{
            length = 1f;
            reload = 70f;
            roundrobin = true;
            shots = 1;
            inaccuracy = 3f;
            recoil = 3f;
            velocityRnd = 0.1f;
            ejectEffect = ShootFx.shellEjectMedium;
            ammo = AmmoTypes.shellCarbide;
        }};

        flamethrower = new Weapon("flamethrower"){{
            length = 1f;
            reload = 14f;
            roundrobin = true;
            recoil = 1f;
            ejectEffect = Fx.none;
            ammo = AmmoTypes.flamerThermite;
        }};

        artillery = new Weapon("artillery"){{
            length = 1f;
            reload = 60f;
            roundrobin = true;
            recoil = 5f;
            shake = 2f;
            ejectEffect = ShootFx.shellEjectMedium;
            ammo = AmmoTypes.unitArtillery;
        }};

        sapper = new Weapon("sapper"){{
            length = 1.5f;
            reload = 12f;
            roundrobin = true;
            ejectEffect = ShootFx.shellEjectSmall;
            ammo = AmmoTypes.bulletDense;
        }};

        bomber = new Weapon("bomber"){{
            length = 0f;
            width = 2f;
            reload = 12f;
            roundrobin = true;
            ejectEffect = Fx.none;
            velocityRnd = 1f;
            inaccuracy = 40f;
            ammo = AmmoTypes.bombExplosive;
        }};

        bomberTrident = new Weapon("bomber"){{
            length = 0f;
            width = 2f;
            reload = 9f;
            shots = 2;
            roundrobin = true;
            ejectEffect = Fx.none;
            velocityRnd = 1f;
            inaccuracy = 40f;
            ammo = AmmoTypes.bombExplosive;
        }};

        laserBurster = new Weapon("bomber"){{
            reload = 80f;
            shake = 3f;
            width = 0f;
            roundrobin = true;
            ejectEffect = Fx.none;
            ammo = AmmoTypes.lancerLaser;
        }};

        healBlasterDrone = new Weapon("heal-blaster"){{
            length = 1.5f;
            reload = 40f;
            width = 0.5f;
            roundrobin = true;
            ejectEffect = Fx.none;
            recoil = 2f;
            ammo = AmmoTypes.healBlaster;
        }};
    }

    @Override
    public ContentType type(){
        return ContentType.weapon;
    }
}
