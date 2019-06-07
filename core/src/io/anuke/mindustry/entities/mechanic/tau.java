package io.anuke.mindustry.entities.mechanic;

import io.anuke.mindustry.content.Bullets;
import io.anuke.mindustry.content.Fx;
import io.anuke.mindustry.entities.Effects;
import io.anuke.mindustry.entities.Units;
import io.anuke.mindustry.entities.type.Player;
import io.anuke.mindustry.graphics.Pal;
import io.anuke.mindustry.type.Mech;
import io.anuke.mindustry.type.Weapon;

public class tau extends Mech {
    float healRange = 60f;
    float healAmount = 10f;
    float healReload = 160f;
    boolean wasHealed;

    public tau(String name, boolean flying) {
        super(name, flying);
        drillPower = 4;
        mineSpeed = 3f;
        itemCapacity = 70;
        weaponOffsetY = -1;
        weaponOffsetX = 1;
        mass = 1.75f;
        speed = 0.44f;
        drag = 0.35f;
        boostSpeed = 0.8f;
        canHeal = true;
        health = 200f;
        buildPower = 1.6f;
        engineColor = Pal.heal;

        weapon = new Weapon("heal-blaster") {{
            length = 1.5f;
            reload = 24f;
            roundrobin = false;
            ejectEffect = Fx.none;
            recoil = 2f;
            bullet = Bullets.healBullet;
        }};
    }

    @Override
    public void updateAlt(Player player) {

        if (player.timer.get(Player.timerAbility, healReload)) {
            wasHealed = false;

            Units.nearby(player.getTeam(), player.x, player.y, healRange, unit -> {
                if (unit.health < unit.maxHealth()) {
                    Effects.effect(Fx.heal, unit);
                    wasHealed = true;
                }
                unit.healBy(healAmount);
            });

            if (wasHealed) {
                Effects.effect(Fx.healWave, player);
            }
        }
    }
}
