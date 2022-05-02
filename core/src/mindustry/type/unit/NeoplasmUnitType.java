package mindustry.type.unit;

import mindustry.content.*;
import mindustry.entities.abilities.*;
import mindustry.graphics.*;
import mindustry.type.*;

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

        abilities.add(new LiquidRegenAbility(){{
            liquid = Liquids.neoplasm;
            slurpEffect = Fx.neoplasmHeal;
        }});

        //green flashing is unnecessary since they always regen
        healFlash = false;

        //TODO
        //- liquid regen ability
        //- new explode effect
    }
}
