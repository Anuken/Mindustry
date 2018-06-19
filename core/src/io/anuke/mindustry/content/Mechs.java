package io.anuke.mindustry.content;

import com.badlogic.gdx.utils.Array;
import io.anuke.mindustry.game.Content;
import io.anuke.mindustry.type.ContentList;
import io.anuke.mindustry.type.Mech;
import io.anuke.mindustry.type.Upgrade;

public class Mechs implements ContentList {
    public static Mech alpha, delta, tau, omega, dart, trident, javelin, halberd;

    /**These are not new mechs, just re-assignments for convenience.*/
    public static Mech starterDesktop, starterMobile;

    @Override
    public void load() {

        alpha = new Mech("alpha-mech", false){{
            drillPower = 2;
            speed = 1.1f;
            maxSpeed = 1.1f;
        }};

        delta = new Mech("delta-mech", false){{
            drillPower = -1;
            speed = 1.5f;
            maxSpeed = 1.5f;
        }};

        tau = new Mech("tau-mech", false){{
            drillPower = 2;
            speed = 1.1f;
            maxSpeed = 1.1f;
        }};

        omega = new Mech("omega-mech", false){{
            drillPower = 1;
            speed = 1.0f;
            maxSpeed = 1.0f;
        }};

        dart = new Mech("dart-ship", true){{
            drillPower = -1;
            speed = 0.4f;
            maxSpeed = 3f;
            drag = 0.1f;
        }};

        trident = new Mech("trident-ship", true){{
            drillPower = 1;
            speed = 0.4f;
            maxSpeed = 3f;
            drag = 0.1f;
        }};

        javelin = new Mech("javelin-ship", true){{
            drillPower = -1;
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
    public Array<? extends Content> getAll() {
        return Upgrade.all();
    }
}
