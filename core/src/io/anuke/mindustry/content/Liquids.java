package io.anuke.mindustry.content;

import com.badlogic.gdx.graphics.Color;
import io.anuke.mindustry.resource.Liquid;

public class Liquids {

    public static final Liquid

    none = new Liquid("none", Color.CLEAR),
    water = new Liquid("water", Color.valueOf("486acd")) {
        {
            heatCapacity = 0.4f;
        }
    },
    lava = new Liquid("lava", Color.valueOf("e37341")) {
        {
            temperature = 0.7f;
            viscosity = 0.8f;
        }
    },
    oil = new Liquid("oil", Color.valueOf("313131")) {
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
    };
}
