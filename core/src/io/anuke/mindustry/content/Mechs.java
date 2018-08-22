package io.anuke.mindustry.content;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.Array;
import io.anuke.mindustry.game.Content;
import io.anuke.mindustry.graphics.Palette;
import io.anuke.mindustry.type.ContentList;
import io.anuke.mindustry.type.Mech;
import io.anuke.mindustry.type.Upgrade;

public class Mechs implements ContentList{
    public static Mech alpha, delta, tau, omega, dart, javelin, trident, halberd;

    /**These are not new mechs, just re-assignments for convenience.*/
    public static Mech starterDesktop, starterMobile;

    @Override
    public void load(){

        alpha = new Mech("alpha-mech", false){{
            drillPower = 1;
            mineSpeed = 1.5f;
            speed = 0.5f;
            boostSpeed = 0.85f;
            weapon = Weapons.blaster;
            maxSpeed = 4f;
        }};

        delta = new Mech("delta-mech", false){{
            drillPower = -1;
            speed = 0.75f;
            boostSpeed = 0.95f;
            itemCapacity = 15;
            armor = 30f;
            weaponOffsetX = -1;
            itemCapacity = 15;
            weaponOffsetY = -1;
            weapon = Weapons.shockgun;
            trailColorTo = Color.valueOf("d3ddff");
            maxSpeed = 5f;
        }};

        tau = new Mech("tau-mech", false){{
            drillPower = 3;
            mineSpeed = 3f;
            itemCapacity = 70;
            speed = 0.44f;
            drag = 0.35f;
            boostSpeed = 0.8f;
            weapon = Weapons.blaster;
            maxSpeed = 5f;
            armor = 30f;
        }};

        omega = new Mech("omega-mech", false){{
            drillPower = 2;
            mineSpeed = 1.5f;
            itemCapacity = 50;
            speed = 0.36f;
            boostSpeed = 0.6f;
            shake = 4f;
            weaponOffsetX = 1;
            weaponOffsetY = 0;
            weapon = Weapons.swarmer;
            maxSpeed = 3.5f;
            armor = 70f;
        }};

        dart = new Mech("dart-ship", true){{
            drillPower = 1;
            mineSpeed = 0.9f;
            speed = 0.4f;
            maxSpeed = 3f;
            drag = 0.1f;
            weaponOffsetX = -1;
            weaponOffsetY = -1;
            trailColor = Palette.lightTrail;
        }};

        javelin = new Mech("javelin-ship", true){{
            drillPower = -1;
            speed = 0.4f;
            maxSpeed = 3.6f;
            drag = 0.09f;
            weapon = Weapons.missiles;
            trailColor = Color.valueOf("d3ddff");
        }};

        trident = new Mech("trident-ship", true){{
            drillPower = 1;
            speed = 0.4f;
            maxSpeed = 3f;
            drag = 0.1f;
        }};

        halberd = new Mech("halberd-ship", true){{
            drillPower = 2;
            speed = 0.4f;
            maxSpeed = 3f;
            drag = 0.1f;
        }};

        starterDesktop = alpha;
        starterMobile = dart;
    }

    @Override
    public Array<? extends Content> getAll(){
        return Upgrade.all();
    }
}
