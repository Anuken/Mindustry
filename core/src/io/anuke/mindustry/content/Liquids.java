package io.anuke.mindustry.content;

import io.anuke.arc.graphics.Color;
import io.anuke.mindustry.ctype.ContentList;
import io.anuke.mindustry.type.Liquid;

public class Liquids implements ContentList{
    public static Liquid water, slag, oil, cryofluid;

    @Override
    public void load(){

        water = new Liquid("water", Color.valueOf("596ab8")){{
            heatCapacity = 0.4f;
            effect = StatusEffects.wet;
        }};

        slag = new Liquid("slag", Color.valueOf("ffa166")){{
            temperature = 1f;
            viscosity = 0.8f;
            effect = StatusEffects.melting;
        }};

        oil = new Liquid("oil", Color.valueOf("313131")){{
            viscosity = 0.7f;
            flammability = 1.2f;
            explosiveness = 1.2f;
            heatCapacity = 0.7f;
            effect = StatusEffects.tarred;
        }};

        cryofluid = new Liquid("cryofluid", Color.valueOf("6ecdec")){{
            heatCapacity = 0.9f;
            temperature = 0.25f;
            effect = StatusEffects.freezing;
        }};
    }
}
