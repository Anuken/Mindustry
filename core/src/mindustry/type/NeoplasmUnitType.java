package mindustry.type;

import mindustry.content.*;
import mindustry.entities.abilities.*;
import mindustry.graphics.*;

/** This is just a preset. Contains no new behavior. */
public class NeoplasmUnitType extends UnitType{

    public NeoplasmUnitType(String name){
        super(name);

        outlineColor = Pal.neoplasmOutline;
        immunities.addAll(StatusEffects.burning, StatusEffects.melting);

        abilities.add(new RegenAbility(){{
            //fully regen in 30 seconds
            percentAmount = 1f / (30f * 60f) * 100f;
        }});

        abilities.add(new LiquidExplodeAbility(){{
            liquid = Liquids.neoplasm;
        }});

        //green flashing is unnecessary since they always regen
        showHeal = false;

        //TODO
        //- liquid regen ability
    }
}
