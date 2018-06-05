package io.anuke.mindustry.content;

import com.badlogic.gdx.utils.Array;
import io.anuke.mindustry.game.Content;
import io.anuke.mindustry.type.ContentList;
import io.anuke.mindustry.type.Mech;
import io.anuke.mindustry.type.Upgrade;

public class Mechs implements ContentList {
    public static Mech standard, standardShip;

    @Override
    public void load() {

        standard = new Mech("standard-mech", false){{
            drillPower = 1;
        }};

        standardShip = new Mech("standard-ship", true){{
            drillPower = 1;
        }};
    }

    @Override
    public Array<? extends Content> getAll() {
        return Upgrade.all();
    }
}
