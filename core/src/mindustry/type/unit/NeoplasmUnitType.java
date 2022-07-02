package mindustry.type.unit;

import mindustry.content.*;
import mindustry.entities.abilities.*;
import mindustry.graphics.*;
import mindustry.type.*;
import mindustry.world.meta.*;

/** This is just a preset. Contains no new behavior. */
public class NeoplasmUnitType extends UnitType{

    public NeoplasmUnitType(String name){
        super(name);

        outlineColor = Pal.neoplasmOutline;
        immunities.addAll(StatusEffects.burning, StatusEffects.melting);
        envDisabled = Env.none;
        drawCell = false;

        abilities.add(new RegenAbility(){{
            //fully regen in 70 seconds
            percentAmount = 1f / (70f * 60f) * 100f;
        }});

        abilities.add(new LiquidExplodeAbility(){{
            liquid = Liquids.neoplasm;
        }});

        abilities.add(new LiquidRegenAbility(){{
            liquid = Liquids.neoplasm;
            slurpEffect = Fx.neoplasmHeal;
        }});

        //green flashing is unnecessary since they always regen
        healFlash = true;

        healColor = Pal.neoplasm1;

        //TODO
        //- liquid regen ability
        //- new explode effect
    }
}
