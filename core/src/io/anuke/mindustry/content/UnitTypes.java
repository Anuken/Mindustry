package io.anuke.mindustry.content;

import com.badlogic.gdx.utils.Array;
import io.anuke.mindustry.entities.units.UnitType;
import io.anuke.mindustry.entities.units.types.*;
import io.anuke.mindustry.game.Content;
import io.anuke.mindustry.type.ContentList;

public class UnitTypes implements ContentList {
    public static UnitType drone, scout, vtol, monsoon, titan, fabricator;

    @Override
    public void load() {
        drone = new UnitType("drone", Drone.class, Drone::new){{
            isFlying = true;
            drag = 0.01f;
            speed = 0.2f;
            maxVelocity = 0.8f;
            ammoCapacity = 0;
            range = 50f;
            health = 45;
        }};

        scout = new UnitType("scout", Scout.class, Scout::new){{
            maxVelocity = 1.1f;
            speed = 0.2f;
            drag = 0.4f;
            range = 40f;
            weapon = Weapons.chainBlaster;
            health = 70;
        }};

        titan = new UnitType("titan", Titan.class, Titan::new){{
            maxVelocity = 0.8f;
            speed = 0.18f;
            drag = 0.4f;
            range = 10f;
            weapon = Weapons.chainBlaster;
            health = 260;
        }};

        vtol = new UnitType("vtol", Vtol.class, Vtol::new){{
            speed = 0.3f;
            maxVelocity = 2.1f;
            drag = 0.01f;
            isFlying = true;
        }};

        monsoon = new UnitType("monsoon", Monsoon.class, Monsoon::new){{
            health = 230;
            speed = 0.2f;
            maxVelocity = 1.5f;
            drag = 0.01f;
            isFlying = true;
            weapon = Weapons.bomber;
        }};

        fabricator = new UnitType("fabricator", Fabricator.class, Fabricator::new){{
            isFlying = true;
            drag = 0.01f;
            speed = 0.2f;
            maxVelocity = 0.6f;
            ammoCapacity = 0;
            range = 70f;
            itemCapacity = 70;
            health = 120;
            health = 45;
            buildPower = 0.9f;
            minePower = 1.1f;
            healSpeed = 0.3f;
        }};
    }

    @Override
    public Array<? extends Content> getAll() {
        return UnitType.all();
    }
}
