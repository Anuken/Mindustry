package io.anuke.mindustry.content;

import io.anuke.mindustry.type.ContentList;
import io.anuke.mindustry.type.Mech;

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
}
