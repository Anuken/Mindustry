package mindustry.content;

import arc.graphics.*;
import mindustry.type.*;

public class Liquids{
    public static Liquid water, slag, oil, cryofluid,
    arkycite, gallium, neoplasm,
    ozone, hydrogen, nitrogen, cyanogen;

    public static void load(){

        water = new Liquid("water", Color.valueOf("596ab8")){{
            heatCapacity = 0.4f;
            effect = StatusEffects.wet;
            boilPoint = 0.5f;
            gasColor = Color.grays(0.9f);
        }};

        slag = new Liquid("slag", Color.valueOf("ffa166")){{
            temperature = 1f;
            viscosity = 0.7f;
            effect = StatusEffects.melting;
            lightColor = Color.valueOf("f0511d").a(0.4f);
        }};

        oil = new Liquid("oil", Color.valueOf("313131")){{
            viscosity = 0.75f;
            flammability = 1.2f;
            explosiveness = 1.2f;
            heatCapacity = 0.7f;
            barColor = Color.valueOf("6b675f");
            effect = StatusEffects.tarred;
            boilPoint = 0.65f;
            gasColor = Color.grays(0.4f);
        }};

        cryofluid = new Liquid("cryofluid", Color.valueOf("6ecdec")){{
            heatCapacity = 0.9f;
            temperature = 0.25f;
            effect = StatusEffects.freezing;
            lightColor = Color.valueOf("0097f5").a(0.2f);
            boilPoint = 0.55f;
            gasColor = Color.valueOf("c1e8f5");
        }};

        neoplasm = new CellLiquid("neoplasm", Color.valueOf("c33e2b")){{
            heatCapacity = 0.4f;
            temperature = 0.54f;
            viscosity = 0.85f;
            flammability = 0f;
            capPuddles = false;
            hidden = true;
            spreadTarget = Liquids.water;

            colorFrom = Color.valueOf("e8803f");
            colorTo = Color.valueOf("8c1225");
        }};

        arkycite = new Liquid("arkycite", Color.valueOf("84a94b")){{
            flammability = 0.4f;
            viscosity = 0.7f;
        }};

        gallium = new Liquid("gallium", Color.valueOf("9a9dbf")){{
            coolant = false;
            hidden = true;
        }};

        //TODO reactivity, etc
        ozone = new Liquid("ozone", Color.valueOf("fc81dd")){{
            gas = true;
            barColor = Color.valueOf("d699f0");
            explosiveness = 1f;
            flammability = 1f;
        }};

        //TODO combustion
        hydrogen = new Liquid("hydrogen", Color.valueOf("9eabf7")){{
            gas = true;
            flammability = 1f;
        }};

        nitrogen = new Liquid("nitrogen", Color.valueOf("efe3ff")){{
            gas = true;
        }};

        cyanogen = new Liquid("cyanogen", Color.valueOf("89e8b6")){{
            gas = true;
            flammability = 2f;
        }};
    }
}
