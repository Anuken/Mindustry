package io.anuke.mindustry.content;

import io.anuke.arc.graphics.Color;
import io.anuke.mindustry.game.ContentList;
import io.anuke.mindustry.type.Liquid;

public class Liquids implements ContentList{
    public static Liquid water, slag, oil, cryofluid, acid;

    @Override
    public void load(){

        water = new Liquid("water", Color.valueOf("486acd")){{
            heatCapacity = 0.4f;
            tier = 0;
            effect = StatusEffects.wet;
        }};

        slag = new Liquid("slag", Color.valueOf("e37341")){{
            temperature = 1f;
            viscosity = 0.8f;
            tier = 2;
            effect = StatusEffects.melting;
        }};

        oil = new Liquid("oil", Color.valueOf("313131")){{
            viscosity = 0.7f;
            flammability = 0.6f;
            explosiveness = 0.6f;
            heatCapacity = 0.7f;
            tier = 1;
            effect = StatusEffects.tarred;
        }};

        cryofluid = new Liquid("cryofluid", Color.valueOf("6ecdec")){{
            heatCapacity = 0.9f;
            temperature = 0.25f;
            tier = 1;
            effect = StatusEffects.freezing;
        }};

        acid = new Liquid("acid", Color.valueOf("e9f9b3")){{
            heatCapacity = 0.1f; //don't use acid as coolant, it's bad
            effect = StatusEffects.corroded;
        }};
    }
}
