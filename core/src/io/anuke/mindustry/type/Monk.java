package io.anuke.mindustry.type;

import io.anuke.arc.Core;
import io.anuke.arc.graphics.Color;
import io.anuke.mindustry.content.Bullets;
import io.anuke.mindustry.content.Fx;

public class Monk extends Mech {

    public Monk(String name, boolean flying) {
        super(name, flying);

        drillPower = 1;
        mineSpeed = 1.5f;
        mass = 1.2f;
        speed = 0.5f;
        itemCapacity = 40;
        boostSpeed = 0.95f;
        buildPower = 1.2f;
        engineColor = Color.valueOf("ffd37f");
        health = 250f;

        weapon = new Weapon("blaster"){{
            length = 1.5f;
            reload = 14f;
            alternate = true;
            ejectEffect = Fx.shellEjectSmall;
            bullet = Bullets.standardMechSmall;
        }};
    }

    @Override
    public void load(){
        weapon.load();

        region = Core.atlas.find("monk-idle-0-0");
    }
}
