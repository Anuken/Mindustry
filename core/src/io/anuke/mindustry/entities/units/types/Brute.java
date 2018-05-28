package io.anuke.mindustry.entities.units.types;

import io.anuke.mindustry.content.AmmoTypes;
import io.anuke.mindustry.entities.units.GroundUnitType;

public class Brute extends GroundUnitType {

    public Brute(String name) {
        super(name);
        setAmmo(AmmoTypes.bulletIron);
    }

}
