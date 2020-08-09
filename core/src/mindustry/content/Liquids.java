package mindustry.content;

import arc.graphics.Color;
import mindustry.ctype.ContentList;
import mindustry.type.Liquid;

public class Liquids implements ContentList{
    public static Liquid water, slag, oil, cryofluid;

    @Override
    public void load(){

        water = new Liquid("water", Color.valueOf("596ab8")){{
            heatCapacity = 0.4f;
            effect = StatusEffects.wet;
            alwaysUnlocked = true;
        }};

        slag = new Liquid("slag", Color.valueOf("ffa166")){{
            temperature = 1f;
            viscosity = 0.8f;
            effect = StatusEffects.melting;
            lightColor = Color.valueOf("f0511d").a(0.4f);
        }};

        oil = new Liquid("oil", Color.valueOf("313131")){{
            viscosity = 0.7f;
            flammability = 1.2f;
            explosiveness = 1.2f;
            heatCapacity = 0.7f;
            barColor = Color.valueOf("6b675f");
            effect = StatusEffects.tarred;
        }};

        cryofluid = new Liquid("cryofluid", Color.valueOf("6ecdec")){{
            heatCapacity = 0.9f;
            temperature = 0.25f;
            effect = StatusEffects.freezing;
            lightColor = Color.valueOf("0097f5").a(0.2f);
        }};
    }
}
