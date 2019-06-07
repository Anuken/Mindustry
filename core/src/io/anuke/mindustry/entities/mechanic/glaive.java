package io.anuke.mindustry.entities.mechanic;

import io.anuke.arc.graphics.Color;
import io.anuke.mindustry.content.Bullets;
import io.anuke.mindustry.content.Fx;
import io.anuke.mindustry.type.Mech;
import io.anuke.mindustry.type.Weapon;

public class glaive extends Mech {
    public glaive(String name, boolean flying) {
        super(name, flying);
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
            roundrobin = true;
            ejectEffect = Fx.shellEjectSmall;
            bullet = Bullets.standardGlaive;
        }};
    }
}
