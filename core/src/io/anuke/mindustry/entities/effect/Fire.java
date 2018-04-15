package io.anuke.mindustry.entities.effect;

import io.anuke.ucore.entities.TimedEntity;

import static io.anuke.mindustry.Vars.effectGroup;

public class Fire extends TimedEntity {

    @Override
    public Fire add(){
        return add(effectGroup);
    }
}
