package io.anuke.mindustry.content;

import io.anuke.mindustry.entities.BulletType;
import io.anuke.mindustry.graphics.fx.BulletFx;
import io.anuke.mindustry.resource.Weapon;

public class Weapons {
    public static final Weapon

    blaster = new Weapon("blaster", 12, BulletType.shot) {
        {
            effect = BulletFx.laserShoot;
            length = 2f;
        }
    },
    triblaster = new Weapon("triblaster", 16, BulletType.spread) {
        {
            shots = 3;
            effect = BulletFx.spreadShoot;
            roundrobin = true;
        }
    },
    clustergun = new Weapon("clustergun", 26f, BulletType.cluster) {
        {
            effect = BulletFx.clusterShoot;
            inaccuracy = 17f;
            roundrobin = true;
            shots = 2;
            spacing = 0;
        }
    },
    beam = new Weapon("beam", 30f, BulletType.beamlaser) {
        {
            effect = BulletFx.beamShoot;
            inaccuracy = 0;
            roundrobin = true;
            shake = 2f;
        }
    },
    vulcan = new Weapon("vulcan", 5, BulletType.vulcan) {
        {
            effect = BulletFx.vulcanShoot;
            inaccuracy = 5;
            roundrobin = true;
            shake = 1f;
            inaccuracy = 4f;
        }
    },
    shockgun = new Weapon("shockgun", 36, BulletType.shockshell) {
        {
            shootsound = "bigshot";
            effect = BulletFx.shockShoot;
            shake = 2f;
            roundrobin = true;
            shots = 7;
            inaccuracy = 15f;
            length = 3.5f;
        }
    };
}
