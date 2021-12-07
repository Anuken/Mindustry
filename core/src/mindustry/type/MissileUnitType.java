package mindustry.type;

import mindustry.ai.types.*;
import mindustry.gen.*;
import mindustry.world.meta.*;

/** Field template for unit types. No new functionality. */
public class MissileUnitType extends UnitType{

    public MissileUnitType(String name){
        super(name);

        playerControllable = false;
        createWreck = false;
        logicControllable = false;
        isCounted = false;
        useUnitCap = false;
        allowedInPayloads = false;
        defaultController = MissileAI::new;
        flying = true;
        constructor = TimedKillUnit::create;
        envEnabled = Env.any;
        envDisabled = Env.none;
        trailLength = 7;
        hidden = true;
        rotateSpeed = 2f;
        range = 30f;
        //TODO weapons, etc
    }
}
