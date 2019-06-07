package io.anuke.mindustry.entities.mechanic;

import io.anuke.arc.graphics.Color;
import io.anuke.mindustry.content.Fx;
import io.anuke.mindustry.entities.bullet.BombBulletType;
import io.anuke.mindustry.entities.type.Player;
import io.anuke.mindustry.type.Mech;
import io.anuke.mindustry.type.Weapon;

public class trident extends Mech {
    public trident(String name, boolean flying) {
        super(name, flying);
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
            roundrobin = true;
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
            }};
        }};
    }

    @Override
    public boolean canShoot(Player player){
        return player.velocity().len() > 1.2f;
    }
}
