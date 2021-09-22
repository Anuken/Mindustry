package mindustry.type;

import mindustry.content.*;
import mindustry.graphics.*;

/** This is just a preset. Contains no new behavior. */
public class NeoplasmUnitType extends UnitType{

    public NeoplasmUnitType(String name){
        super(name);

        outlineColor = Pal.neoplasmOutline;
        immunities.addAll(StatusEffects.burning, StatusEffects.melting);

        //TODO
        //- liquid regen ability
        //- liquid/neoplasm explode ability
    }
}
