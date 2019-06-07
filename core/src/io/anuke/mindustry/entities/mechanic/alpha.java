package io.anuke.mindustry.entities.mechanic;

import io.anuke.arc.graphics.Color;
import io.anuke.arc.util.Time;
import io.anuke.mindustry.content.Bullets;
import io.anuke.mindustry.content.Fx;
import io.anuke.mindustry.entities.type.Player;
import io.anuke.mindustry.type.Mech;
import io.anuke.mindustry.type.Weapon;

public class alpha extends Mech {
    public alpha(String name, boolean flying) {
        super(name, flying);
        drillPower = 1;
        mineSpeed = 1.5f;
        mass = 1.2f;
        speed = 0.5f;
        boostSpeed = 0.95f;
        buildPower = 1.2f;
        engineColor = Color.valueOf("ffd37f");
        health = 250f;

        weapon = new Weapon("blaster") {{
            length = 1.5f;
            reload = 14f;
            roundrobin = true;
            ejectEffect = Fx.shellEjectSmall;
            bullet = Bullets.standardMechSmall;
        }};
    }

    @Override
    public void updateAlt(Player player) {
        player.healBy(Time.delta() * 0.09f);
    }
}
