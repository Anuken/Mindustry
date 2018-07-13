package io.anuke.mindustry.world.blocks.units;

import io.anuke.mindustry.content.StatusEffects;

public class OverdriveProjector extends Projector{

    public OverdriveProjector(String name){
        super(name);

        status = StatusEffects.overdrive;
    }
}
