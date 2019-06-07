package io.anuke.mindustry.entities.mechanic;

import io.anuke.mindustry.content.Bullets;
import io.anuke.mindustry.content.Fx;
import io.anuke.mindustry.graphics.Pal;
import io.anuke.mindustry.type.Mech;
import io.anuke.mindustry.type.Weapon;

public class dart extends Mech {
    public dart(String name, boolean flying) {
        super(name, flying);
        drillPower = 1;
        mineSpeed = 0.9f;
        speed = 0.5f;
        drag = 0.09f;
        health = 200f;
        weaponOffsetX = -1;
        weaponOffsetY = -1;
        engineColor = Pal.lightTrail;
        cellTrnsY = 1f;
        buildPower = 1.1f;
        weapon = new Weapon("blaster") {{
            length = 1.5f;
            reload = 15f;
            roundrobin = true;
            ejectEffect = Fx.shellEjectSmall;
            bullet = Bullets.standardCopper;
        }};
    }

    @Override
    public boolean alwaysUnlocked() {
        return true;
    }
}
