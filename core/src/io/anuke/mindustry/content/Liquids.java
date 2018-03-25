package io.anuke.mindustry.content;

import com.badlogic.gdx.graphics.Color;
import io.anuke.mindustry.resource.Liquid;

public class Liquids {

    public static final Liquid

    none = new Liquid("none", Color.CLEAR),
    water = new Liquid("water", Color.ROYAL) {
        {
            heatCapacity = 0.4f;
        }
    },
    plasma = new Liquid("plasma", Color.CORAL) {
        {
            flammability = 0.4f;
            viscosity = 0.1f;
            heatCapacity = 0.2f;
        }
    },
    lava = new Liquid("lava", Color.valueOf("e37341")) {
        {
            temperature = 0.7f;
            viscosity = 0.8f;
        }
    },
    oil = new Liquid("oil", Color.valueOf("292929")) {
        {
            viscosity = 0.7f;
            flammability = 0.6f;
            explosiveness = 0.6f;
        }
    },
    cryofluid = new Liquid("cryofluid", Color.SKY) {
        {
            heatCapacity = 0.75f;
            temperature = 0.5f;
        }
    },
    sulfuricAcid = new Liquid("sulfuricAcid", Color.YELLOW) {
        {
            flammability = 0.4f;
            explosiveness = 0.4f;
            heatCapacity = 0.4f;
        }
    };
}
