package io.anuke.mindustry.entities.mechanic;

import io.anuke.arc.graphics.Color;
import io.anuke.arc.math.Mathf;
import io.anuke.arc.util.Time;
import io.anuke.mindustry.content.Bullets;
import io.anuke.mindustry.content.Fx;
import io.anuke.mindustry.entities.Effects;
import io.anuke.mindustry.entities.effect.Lightning;
import io.anuke.mindustry.entities.type.Player;
import io.anuke.mindustry.graphics.Pal;
import io.anuke.mindustry.type.Mech;
import io.anuke.mindustry.type.Weapon;

public class delta extends Mech {
    float cooldown = 120;

    public delta(String name, boolean flying) {
        super(name, flying);

        drillPower = -1;
        speed = 0.75f;
        boostSpeed = 0.95f;
        itemCapacity = 15;
        mass = 0.9f;
        health = 150f;
        buildPower = 0.9f;
        weaponOffsetX = -1;
        weaponOffsetY = -1;
        engineColor = Color.valueOf("d3ddff");

        weapon = new Weapon("shockgun") {{
            shake = 2f;
            length = 1f;
            reload = 45f;
            shotDelay = 3f;
            roundrobin = true;
            shots = 2;
            inaccuracy = 0f;
            ejectEffect = Fx.none;
            bullet = Bullets.lightning;
        }};
    }

    @Override
    public void onLand(Player player) {
        if (player.timer.get(Player.timerAbility, cooldown)) {
            Effects.shake(1f, 1f, player);
            Effects.effect(Fx.landShock, player);
            for (int i = 0; i < 8; i++) {
                Time.run(Mathf.random(8f), () -> Lightning.create(player.getTeam(), Pal.lancerLaser, 17f, player.x, player.y, Mathf.random(360f), 14));
            }
        }
    }
}
