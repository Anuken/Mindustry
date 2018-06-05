package io.anuke.mindustry.content;

import com.badlogic.gdx.utils.Array;
import io.anuke.mindustry.entities.units.UnitType;
import io.anuke.mindustry.entities.units.types.Drone;
import io.anuke.mindustry.entities.units.types.Scout;
import io.anuke.mindustry.entities.units.types.Vtol;
import io.anuke.mindustry.game.Content;
import io.anuke.mindustry.type.ContentList;
import io.anuke.mindustry.type.StatusEffect;

public class UnitTypes implements ContentList {
    public static UnitType drone, scout, vtol;

    @Override
    public void load() {
        drone = new UnitType("drone", team -> new Drone(drone, team)){{
            isFlying = true;
            drag = 0.01f;
            speed = 0.2f;
            maxVelocity = 0.8f;
            range = 50f;
        }};

        scout = new UnitType("scout", team -> new Scout(scout, team)){{
            maxVelocity = 1.1f;
            speed = 0.1f;
            drag = 0.4f;
            range = 40f;
            setAmmo(AmmoTypes.bulletIron);
        }};

        vtol = new UnitType("vtol", team -> new Vtol(vtol, team)){{
            speed = 0.3f;
            maxVelocity = 2f;
            drag = 0.01f;
            isFlying = true;
            reload = 7;
            setAmmo(AmmoTypes.bulletIron);
        }};
    }

    @Override
    public Array<? extends Content> getAll() {
        return StatusEffect.all();
    }
}
